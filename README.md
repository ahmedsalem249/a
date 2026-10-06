# ACLClouds Auto Renew — Android App

This is the Android mobile client for the ACLClouds Auto Renew system.

### Architecture

Phone app -> your backend -> ACLClouds

The phone app does **not** need to keep five ACLClouds passwords. The backend does the login/check/renew automation.

### What is ready

- Five account cards.
- Live countdown per account.
- Status and expiry display.
- Manual Check button.
- Backend URL setting.
- Android 8+.
- No third-party Android UI libraries are required.

### What is waiting

The exact ACLClouds Renew flow is intentionally not guessed. Once you send a screenshot with the Renew button visible, we can complete the Playwright adapter in the backend and wire the successful renewal state into this app.

### Build

Open this folder in Android Studio and build a debug APK.

Do not place real ACLClouds credentials in this Android project.

## Fastest way to get the APK

The repository includes `.github/workflows/build-apk.yml`. Upload the project to GitHub, then run **Actions → Build Android APK → Run workflow**. After it finishes, download the `ACLCloudsAutoRenew-debug-apk` artifact and install the APK on Android.


## Fixed build
This version fixes the Android compilation issue from the first GitHub Actions run and uses the current setup-java v5 action.
