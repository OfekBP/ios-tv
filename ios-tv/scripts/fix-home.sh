# If Home ever shows Android's "choose a launcher" box: save our launcher as the default WHILE TCL's stock launcher is
# enabled (TCL turns it back on at every boot, and Android only honours a default saved for the same set of launchers).
# Usage (Git Bash, USB debugging on):  TV_HOST=<tv ip> tools/tv.py "$(cat ios-tv/scripts/fix-home.sh | grep -v '^#')"
pm enable com.google.android.tvlauncher; sleep 1; cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
