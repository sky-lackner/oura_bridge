# Oura Bridge

A small Android app that copies your Oura sleep sessions in Health Connect into new
sleep sessions that Pokémon Sleep can read.

```
Oura app ──writes──▶ Health Connect ──read by──▶ Oura Bridge ──writes copy──▶ Health Connect ──read by──▶ Pokémon Sleep
```

It runs every hour in the background and can also be run by hand. Re-running it never makes
duplicates: each copy is tied to the ID of the Oura record it came from.

## Why the app ID matters (read this first)

Health Connect labels every record with the package name of the app that wrote it, and apps
can't change that label. Pokémon Sleep only accepts sleep from **Google Health / Fitbit,
Samsung Health and Garmin Connect**. Nobody outside the Pokémon Sleep team knows exactly what
it checks, so there are two possibilities:

1. **It checks the device info attached to each record.** The bridge handles this: pick a
   supported "pretend device" in the app (Galaxy Ring is the default, since it's also a ring).
   A normal build should work.
2. **It checks which app wrote the record.** Then a normal build will be ignored, and the
   only way around it is to build the bridge under the package name of a supported app
   *that isn't installed on your phone* (Android won't install two apps with the same
   package name). Garmin Connect is usually the easiest, since few Oura owners have it:

   ```
   ./gradlew assembleDebug -PappId=com.garmin.android.apps.connectmobile
   ```

   Other options: `com.sec.android.app.shealth` (Samsung Health) and `com.fitbit.FitbitMobile`
   (Fitbit / Google Health). Uninstall the real app first if you have it.

Try option 1 first. If Pokémon Sleep doesn't list the bridge as a data source, or the sync
finds no sleep, rebuild with option 2.

This is a personal workaround. It may go against Pokémon Sleep's terms, and an update to
Pokémon Sleep or Health Connect could stop it working at any time.

## Build and install

**1. Install Android Studio.** Download it free from https://developer.android.com/studio
and install it. On first launch, choose the **Standard** setup and let it download the
Android SDK (this takes a while).

**2. Open the project.** Unzip `OuraBridge.zip`. In Android Studio, choose **File → Open**
(or **Open** on the welcome screen), select the `OuraBridge` folder (the one that contains
`settings.gradle.kts`) and click OK. If asked, choose **Trust Project**.

**3. Wait for the sync to finish.** Android Studio now downloads the build tools and libraries
the project needs. This is the "Gradle sync", and it starts on its own; you don't need to
click anything. Progress shows in the status bar at the bottom of the window. It's done when
the bar goes quiet and the green ▶ **Run** button in the top toolbar becomes clickable. The
first sync can take 5–15 minutes.

- If the sync doesn't start, click the elephant icon (**Sync Project with Gradle Files**) in
  the top-right toolbar, or use **File → Sync Project with Gradle Files**.
- If it fails, the **Build** panel at the bottom shows the error. Often there's a blue link to
  click, such as "Install missing SDK" or "Upgrade". Otherwise, copy the error text and ask
  for help.

**4. Turn on USB debugging on your phone.** Go to **Settings → About phone** and tap
**Build number** 7 times until it says you're a developer. Then go to **Settings → System →
Developer options** and turn on **USB debugging**.

**5. Install the app.** Plug your phone in with USB and tap **Allow** on the phone when it
asks about USB debugging. Your phone's name appears in the device menu next to the ▶ button.
Click ▶ **Run**, and the app installs and opens on your phone.

**Using the other app ID (option 2 above):** open **View → Tool Windows → Terminal** in
Android Studio and run:

```
./gradlew installDebug -PappId=com.garmin.android.apps.connectmobile
```

On Windows, type `gradlew` instead of `./gradlew`. This builds the app and installs it on the
connected phone. Uninstall the normal Oura Bridge first so you don't have two copies.

Pokémon Sleep's Health Connect sync needs Android 14 or later, so the app does too.

## Setup on the phone

1. **Oura → Health Connect:** in the Oura app, turn on the Health Connect integration and
   allow it to write **Sleep**.
2. **Bridge:** open Oura Bridge, choose a pretend device, tap **Grant Health Connect access**
   and allow everything, *including access to data in the background*. Then tap **Sync now**.
   You should see your recent nights listed.
3. **Pokémon Sleep:** Main Menu → Settings and More → Smartwatch (Health Connect) → Pair a
   Device. Allow it to read Sleep and choose the bridge as the data source if asked.
4. In the morning, after Oura has synced, open Oura Bridge (or wait for the hourly sync),
   then in Pokémon Sleep tap Sync/Settings → Sync Data.

Pokémon Sleep only accepts sleep recorded **after** you link it to Health Connect, so start
with the night after setup.

## Troubleshooting

- **"No Oura sleep found":** open the Oura app so it syncs, and check in Health Connect → Data
  and access → Sleep that Oura entries are there.
- **Pokémon Sleep sees nothing:** check in Health Connect → App permissions → Pokémon Sleep that
  Sleep is allowed, then try the other app ID (see above).
- **Two sessions per night in other apps:** Health Connect now has both Oura's original and the
  copy. In Health Connect → Data and access → Sleep → Data sources and priority, put Oura on top
  so other apps keep using the original.

## Files

- `SleepMirror.kt`: reads Oura's sessions (last 3 days) and writes the copies, sleep stages included
- `SyncWorker.kt`: the hourly background sync
- `Config.kt`: the pretend-device presets, look-back window and Oura package name
- `MainActivity.kt`: the one-screen UI and the privacy screen Health Connect requires
