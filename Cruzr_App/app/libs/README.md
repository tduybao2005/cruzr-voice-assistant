# Optional native libraries

## `cruzr-sdk-2.8.0.jar`

Drop the UBTECH Cruzr SDK jar here to hide the green Assistant wake-up
microphone that the Cruzr launcher floats above Cadebot:

```
app/libs/cruzr-sdk-2.8.0.jar
```

`app/build.gradle.kts` already declares `implementation(fileTree("libs") { include("*.jar", "*.aar") })`,
so no Gradle change is needed. `CruzrAssistant.hideSystemWakeupButton()` finds
`AssistantManager` by reflection at startup and calls
`showOrHidePart(TYPE_HIDE_PART_WAKEUP)`.

Without the jar the app builds and runs exactly the same; it only logs

```
CadebotCruzr: Cruzr SDK absent (no AssistantManager on the classpath) ...
```

and leaves the system button visible. The SDK is UBTECH-licensed, so it is not
committed to this handover package.

Nothing in this integration disables `com.ubtrobot.service.speech`,
`com.ubtrobot.skill.launcher`, `com.ubtechinc.cruzr.mini.launcher` or
`com.ubtechinc.cruzr.behavior`.
