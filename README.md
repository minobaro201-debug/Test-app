# FloatingTest APK

A minimal Android test app that only requests the **Draw Over Other Apps** (floating window overlay) permission.

## What it does

- On launch, shows whether the overlay permission is granted or not
- Has a button that opens the system settings screen to grant the permission
- That's it — no other permissions, no internet access, nothing else

## How to build the APK

### Option A: GitHub Actions (recommended, no Android Studio needed)

1. Push this folder to a GitHub repository
2. Go to **Actions** tab → **Build APK** → **Run workflow**
3. Once it finishes, download the APK from the **Artifacts** section of the run

### Option B: Android Studio locally

1. Open this folder in Android Studio
2. Wait for Gradle sync to finish
3. Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Permission requested

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw over other apps (floating window) — the only permission this app requests |
