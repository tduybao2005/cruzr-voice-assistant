# Cadebot Android / Cruzr handover

This package is the Android frontend for Cadebot. It keeps the current Mộc Lam
Coffee UI and the working voice pipeline:

`Cruzr microphone -> /stt -> recognized text -> /chat -> response -> UI`

`CadebotTts` walks a short engine cascade — `com.google.android.tts` first,
then the system default — and only accepts an engine whose `setLanguage(vi-VN)`
comes back usable, so the robot never reads Vietnamese with an English voice. It
pins one offline Vietnamese voice, reads the full `answerText` shown in the chat
(not the shortened `spokenText` the server also returns), splits long answers
into <=320-character utterances, and logs every `onStart`/`onDone`/`onStop`/
`onError`.

Before an answer reaches the engine, `normalizeForSpeech` rewrites two written
shorthands the engine mispronounces. `18.000đ` becomes `18.000 đồng` (the engine
otherwise reads the bare consonant as "đờ"), and the quantity `x` in `1x Matcha`
is dropped so it is read "một Matcha" rather than "một ích Matcha". The chat
bubble still shows the original text — only the string handed to `speak()`
changes, and the rewrite is logged as `normalized for speech: "..." -> "..."`.

Both rules are deliberately narrow, and `SpeechNormalizerTest` pins down what
they must *not* touch: "5 đứa", "90 độ", "giảm 10.000 đồng", "2 x 3", "size XL"
and "box" all pass through unchanged.

## Cruzr integration

The robot's launcher floats its Assistant wake-up microphone in a
`TYPE_SYSTEM_ALERT` window owned by `com.ubtechinc.cruzr.behavior`, which paints
over the app and swallows taps underneath it. Two things address that, in order:

- `robot/CruzrAssistant.kt` sends the supported switch,
  `AssistantManager.showOrHidePart(TYPE_HIDE_PART_WAKEUP)`, by reflection. It is
  a no-op until `cruzr-sdk-2.8.0.jar` is dropped into `app/libs/` — see
  `app/libs/README.md`. The jar is UBTECH-licensed and is not redistributed here.
- `robot/CruzrKioskWindow.kt` works without the SDK: it hosts the whole Compose
  tree in a `TYPE_SYSTEM_ERROR` window (layer 24) instead of the activity's own
  window (layer 2), so Cadebot paints above the button and receives every touch
  in its bounds. The activity remains the lifecycle, ViewModel, saved-state and
  back-press owner, so Hilt, Navigation, the IME and the payment flow are
  unchanged. On a device where the overlay cannot be created it returns `null`
  and the app falls back to a normal activity window.

Neither path disables `com.ubtrobot.service.speech`, `com.ubtrobot.skill.launcher`,
`com.ubtechinc.cruzr.mini.launcher` or `com.ubtechinc.cruzr.behavior`.

## Leaving the QR screen

Cruzr runs full-screen with no navigation bar, so the payment screen used to be a
dead end for a customer who changed their mind. It now has the same back arrow as
the other screens; it calls `CartViewModel.cancelPayment()`, which stops the
status poll and clears the order fields while leaving the cart untouched, then
pops back to the cart so the order can be adjusted and re-submitted.

Note the trade-off: backing out abandons that payment attempt in the app. If a
customer has already transferred, the money is still recorded server-side and the
webhook still marks the order PAID, but the app will not show the success screen
for it.

## Fitting the panel

Cruzr's 1920x1080 panel at 320dpi gives 540dp of height, while the screens were
laid out for roughly 690dp — Home's third CTA ("Gọi nhân viên") and the
order-success buttons fell off the bottom edge.

`ui/theme/ScaledToScreen.kt` fixes that by overriding `LocalDensity` for the
whole app instead of editing individual paddings. Every `dp` and `sp` in the UI
keeps its original value and is scaled by the same derived factor
(`screenHeightDp / 690`, capped at 1), so the design keeps its exact proportions
and simply renders smaller — about 0.79x on this robot. A device with enough
height renders at native density and is unaffected.

Because the fix lives entirely in the density override, every screen file is
byte-for-byte identical to the pre-Cruzr source. Hiding the wake-up button is
likewise done outside the UI, in `robot/CruzrKioskWindow.kt` and `MainActivity`.

## Kiosk start-up

`MainActivity` declares `android.intent.category.HOME`, so Cadebot can be picked
as the robot's home app; the Cruzr launcher stays installed and can be chosen
again from the same chooser. Independently, `robot/BootReceiver.kt` starts the
app on `BOOT_COMPLETED` and on the firmware's own `com.ubtech.cruzr.BOOTED`
broadcast, which covers the case where the Cruzr launcher remains the default.

To make Cadebot the default home app, press the robot's home button once and
pick Cadebot with "Always". To undo it: Settings > Apps > Cadebot > Clear
defaults.

The app is configured for the public gateway `https://cadebot.example.com`.
The gateway owns the model/API secrets; no model API key belongs in this APK.

## Requirements

- JDK 11
- Android SDK with platform 35 and build-tools 35.x
- Android SDK Platform-Tools (`adb`) for deployment
- Cruzr Android 5.1.1 / API 22, connected over Wi-Fi

## Build on another machine

```bash
cp local.properties.example local.properties
# Edit sdk.dir to the Android SDK path on this machine.
bash ./gradlew :app:assembleRelease --no-daemon --no-configuration-cache
```

The APK is generated at:

`app/build/outputs/apk/release/app-release.apk`

The Cruzr build settings are already fixed in `app/build.gradle.kts`:

- `applicationId`: `com.baxailab.cadebot`
- `minSdk` / `targetSdk`: 22
- ABI: `armeabi-v7a`
- release signing: `signing/cadebot-cruzr-debug.keystore` — **not in this
  repository**

The signing key is a development key, not a server or API key, but a signing
key still identifies a build as coming from this project, so it is kept out of
the published tree. Without it, uninstall the old app before installing a build
signed with a different key: `adb install -r` refuses a signature change.

## Install over the existing app

Replace the robot address below with the current Cruzr IP. The robot and laptop
must be reachable on the same network.

```bash
ADB="$ANDROID_HOME/platform-tools/adb"
ROBOT="192.168.x.x:5555"
APK="app/build/outputs/apk/release/app-release.apk"

"$ADB" connect "${ROBOT%:*}"
"$ADB" -s "$ROBOT" install -r "$APK"
"$ADB" -s "$ROBOT" shell am force-stop com.baxailab.cadebot
"$ADB" -s "$ROBOT" shell monkey -p com.baxailab.cadebot 1
```

If the installed app has a different signing certificate, use the explicit
replacement flow instead; it resets only Cadebot app data and does not modify
the Cruzr launcher:

```bash
"$ADB" -s "$ROBOT" uninstall com.baxailab.cadebot
"$ADB" -s "$ROBOT" install "$APK"
```

## Test voice and inspect logs

Tap the small microphone in the Cadebot chat input, speak for 3–5 seconds, and
stop recording. Then run:

```bash
"$ADB" -s "$ROBOT" logcat -c
# perform the test on Cruzr
"$ADB" -s "$ROBOT" logcat -d -v time | \
  grep -Ei "CadebotStt|CadebotChat|SSL|Handshake|UnknownHost|ConnectException|SocketTimeout|FATAL EXCEPTION|AndroidRuntime"
```

Successful output includes `STT HTTP 200`, `transcription result present=true`,
and `CHAT HTTP 200`. Add `CadebotTts` and `CadebotCruzr` to the grep to see the
engine cascade, the pinned voice, utterance progress, and which Cruzr path is
active.

## Verified on the robot (Cruzr, Android 5.1.1 / API 22, armeabi-v7a)

| Check | Result |
|---|---|
| `adb install -r` over the previous build | Success, same keystore |
| Mic record start/stop | 49-50 KB `.m4a`, no crash |
| `POST /stt` | HTTP 200, `transcription result present=true` |
| Recognised text lands in the chat box and is sent on | Yes |
| `POST /chat` | HTTP 200, 1.5-2.9 s |
| Answer shown in full | Yes |
| Google TTS reads Vietnamese to the end | `setLanguage=1`, voice `vi-vn-x-vid-local`, `onDone` on every utterance, no `onStop`/`onError` |
| QR payment | Order created, QR rendered, countdown running, no TLS error |
| QR contents | VietQR BIN `970448` (OCB), account `SEPNLDV35383`, correct amount and description |
| System wake-up mic hidden | Yes, via `CruzrKioskWindow` |
| Boot start-up | `com.ubtech.cruzr.BOOTED` broadcast starts the app |
| Unit tests | 12 run, 0 failed |
