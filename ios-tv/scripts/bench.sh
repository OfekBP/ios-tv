for a in com.netflix.ninja/.MainActivity com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity com.android.vending/com.google.android.finsky.tvmainactivity.TvMainActivity; do
  p=${a%%/*}; am force-stop $p; sleep 2
  echo "$p $(am start -W -n $a 2>&1 | grep -E 'TotalTime' | tr -d '\r')"
  sleep 3; am force-stop $p
done
am start -a android.intent.action.MAIN -c android.intent.category.HOME -n com.gathin.tvhome/.MainActivity >/dev/null
