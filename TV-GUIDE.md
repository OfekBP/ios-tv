# Your TV, explained (TCL UnionTV, glass home screen)

Everything below works with the remote only. You don't need the computer or me for any of it.

## Home screen
| You want to | Do this |
|---|---|
| Open an app | Move with the arrows, press **OK** |
| Change the order of your apps | **Hold OK** (or press the remote's **Menu** key) on an app, choose **Move**. OK picks it up, the arrows move it, **Back** puts it down |
| Remove an app from Home | **Hold OK** on it, choose **Remove from Home** (it stays installed) |
| See app info / clear data / uninstall your own apps | **Hold OK** on an app, choose **App info** |
| Add any installed app to Home | Press **Up** to the top buttons, choose the **grid button**, press **OK** on an app ("Add to Home") |
| Switch to HDMI / Live TV | Top button **Inputs** (the remote's Input button works too) |
| Wi-Fi | Top button **Wi-Fi** |
| TV settings (picture, sound, network, apps) | Top button **gear**. **Hold OK** on the gear for the launcher menu: Reorder apps, Launcher settings |

## Night mode (bedside clock)
Top button **moon** (left of the gear): the screen turns black with only a very dim tall clock. **Up / Down** make it brighter or dimmer (remembered). Any other button goes back to Home. The clock drifts a few pixels each minute and the TV stays on. The TV's backlight can't be switched off from an app, so use the TV's own backlight setting for the lowest light.

## Keyboard
A glass keyboard replaces the TCL one. Arrows move, **OK** presses a key, **Back** closes it. The number keys on the remote type digits, Delete deletes.
Keys: **shift**, **123** (numbers and symbols), **עב / EN** (Hebrew / English), **space**, **return**.
TCL's firmware resets the keyboard at every start, so the launcher switches back to the glass one automatically. To use another keyboard, pick it in Settings > Device Preferences > Keyboard (the launcher leaves any non-TCL choice alone). TCL's own keyboard is also reset to the glass one at the next start.

## If the home screen ever looks wrong
Press **Home**. The launcher restarts its helpers by itself (Home key, redirect from TCL's stock home, keyboard). A restart of the TV also fixes it.

## Speed
The apps (Netflix, YES+, YouTube, BrowseHere, Play Store, Downloader) are pre-compiled, so they open much faster (Netflix went from 56 s to under 2 s cold). If an app is slow after a big Play Store update, it recompiles itself in the background.

## Screensaver
After 10 minutes on Home the screen shows the big clock over your wallpaper. Any key returns.

## Installing apps
Open **Google Play** from Home. After installing, add the app with the grid button.

## What still needs the computer (and how to do it later)
Changing the wallpaper or the look is done from the PC folder `C:\cytv`:
1. On the TV: Settings > Device Preferences > About, press OK on "Build" 7 times; Developer options > turn on **USB debugging** (and Network debugging if shown).
2. On the PC (same Wi-Fi), in a terminal: `set TV_HOST=<TV IP>` then `tools\theme.sh work\ios` (Git Bash). Wallpaper is `work\ios\wallpaper.jpg`, settings are `work\ios\config.json`.
3. When finished, turn USB debugging off again.

## Undo everything (also needs USB debugging)
- Stock launcher back: `tools/restore.sh work/batch1-telemetry-ads.txt` (and the other batch files), `pm enable com.google.android.tvlauncher`
- Removed TCL apps (Media Center, T-Channel): see `work\removed-apps.txt`
- TCL keyboard: see `work\keyboard-undo.txt`
- A full backup of the original package list and settings is in `backup\`
