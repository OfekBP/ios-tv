#!/system/bin/sh
# Round 2 (2026-11-01): the Play Store, Downloader, BrowseHere and YES+ may not run in the background.
# They still open normally. Undo: replace "deny" with "allow".
for p in com.android.vending com.esaba.downloader com.tcl.browser il.co.yes.yesplus; do
  cmd appops set $p RUN_IN_BACKGROUND deny
  cmd appops set $p RUN_ANY_IN_BACKGROUND deny
done
