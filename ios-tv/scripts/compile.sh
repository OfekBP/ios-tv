for p in com.netflix.ninja il.co.yes.yesplus com.google.android.youtube.tv com.tcl.browser com.android.vending com.esaba.downloader; do
  echo "== $p $(date +%T)"
  cmd package compile -m speed -f $p 2>&1 | tail -1
done
echo done $(date +%T)
