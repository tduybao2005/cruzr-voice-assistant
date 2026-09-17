# Cruzr app

The Android client that ran on a UBTECH Cruzr service robot: the customer talks
to it, browses the menu, and places an order on the robot's own screen.

This is the build that shipped. `../Cadebot_UI/` is the earlier tablet client
the project started from, kept for comparison.

## Why it is pinned to API 22

```kotlin
minSdk    = 22
targetSdk = 22   // Cruzr runs Android 5.1.1
```

The Cruzr runs Android 5.1.1, which is API 22. Nothing in this module may use
an API newer than that, and `targetSdk` is pinned rather than merely tolerated,
because a higher target changes runtime behaviour the robot's platform does not
implement. The earlier tablet client targets API 35 and cannot be installed on
the robot at all.

## What is not here

- **The signing key.** A development key, but it still identifies builds as
  coming from this project. See [HANDOVER.md](HANDOVER.md).
- **The release APK.** Build it from source.
- **The shop's real data.** The menu, campaign and FAQ fixtures under
  `app/src/main/assets/config/` describe a fictional café, the same one the
  backend's knowledge base uses.

## Building

```bash
./gradlew :app:assembleRelease
```

Deployment steps, the ABI the robot needs, and what to check after installing
are in [HANDOVER.md](HANDOVER.md).
