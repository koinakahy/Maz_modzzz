# maz_modzzz

Cool dark launcher app. Pick an installed app, give it your own **name**, **tag** (like `d0`) and **logo**,
then launch it from the app or pin it to your home screen as `[d0] Facebook`.

## Two modes
| Mode | What it does | Status |
|---|---|---|
| Shortcut | Opens the real app. Home-screen shortcut shows your name + logo. | Works out of the box |
| Virtual | Runs the app inside this app (like KOS / Parallel Space), so Recents can show your name. | Needs the BlackBox core module (see below) |

In Shortcut mode, Recents / notifications / Settings still show the real app's name. Android decides that.

## Build on your phone with GitHub (free)
1. Make a GitHub account. Create a new empty repo (e.g. `maz_modzzz`).
2. Create a token: GitHub > Settings > Developer settings > Personal access tokens > Tokens (classic)
   > tick **repo** and **workflow** > copy it.
3. Install Termux (from F-Droid). Then run:
```
pkg install git unzip
termux-setup-storage
cd ~
unzip ~/storage/downloads/maz_modzzz.zip
cd maz_modzzz
git init
git branch -M main
git add .
git commit -m "first"
git remote add origin https://github.com/YOUR_USERNAME/maz_modzzz.git
git push -u origin main
```
   Username = your GitHub name, password = the token.
4. Open the repo > **Actions** tab > "Build APK" > wait ~5 min > download artifact **maz_modzzz-debug-apk**.
5. Unzip it, install the APK (allow unknown sources).

## Xiaomi / HyperOS tip
For home-screen shortcuts: Settings > Apps > Manage apps > maz_modzzz > Other permissions > **Home screen shortcuts** > Allow.

## Turning on Virtual mode
1. Get a BlackBox fork's source that includes the `Bcore` module (search GitHub for "BlackBox virtual engine").
2. Copy its core module folder into this project as `bcore/` (next to `app/`).
3. Push again. The build detects `bcore/` automatically and compiles `app/src/engine`.
4. Expect to fix small API differences; paste the build log to Claude.
Compiling its native C++ code needs the Android NDK, which GitHub's builders have.
