# Alhazen – Android app

This project turns `Alhazen.html` into a real Android app (APK).
The whole website runs **offline inside the app** – no internet needed.

## Option A – Get the APK free with GitHub (no install needed)

1. Create a free account at github.com and make a **new repository** (e.g. `alhazen-app`).
2. Click **"uploading an existing file"**, drag in **all files and folders** from this zip
   (including the hidden `.github` folder), and press **Commit changes**.
3. Open the **Actions** tab. The job "Build Alhazen APK" starts by itself (about 3–5 minutes).
4. When it shows a green tick, open the run and download **Alhazen-APK** at the bottom.
5. Unzip it, copy `Alhazen.apk` to your phone, open it, and allow
   "Install unknown apps" when Android asks.

> Tip: if the `.github` folder doesn't upload, create the file
> `.github/workflows/build-apk.yml` in GitHub (Add file → Create new file) and paste its contents.

## Option B – Android Studio (on your own computer)

1. Install Android Studio, then **File → Open** this folder.
2. Let it sync (it downloads what it needs).
3. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. The APK appears in `app/build/outputs/apk/`.

## Updating the app later

Replace `app/src/main/assets/index.html` with your new HTML and build again.
Raise `versionCode` in `app/build.gradle` (1 → 2 → 3…) for each new version.

## What works inside the app

- All labs, simulations, Ray Book, Challenger, Board, Eye Lab, Modern Ray
- Progress, sign-in and theme are saved on the phone
- Ray Silo: Camera / Upload buttons open the phone's picker
- "Save" and "Backup" write files to the phone's **Downloads** folder
- Back button goes back; links to websites open in the browser

Known limits: the "Open" button for stored PDFs may not open a viewer, and the
"Install" button from the website is hidden (it isn't needed in an app).
