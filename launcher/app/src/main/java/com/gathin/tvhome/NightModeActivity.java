package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Night mode: pure black with only the clock, very dim, for sleeping with the TV as a bedside clock. Up and Down set how
 * dim it is (remembered); every other key goes back. The clock drifts a few pixels each minute so the same pixels do not
 * stay lit, the screen stays on, and the window asks for the lowest backlight (some TVs ignore that).
 */
public final class NightModeActivity extends Activity {

    private static final float[] LEVELS = {0.05f, 0.09f, 0.14f, 0.22f, 0.34f};
    private static final long DRIFT_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TallClock clock;
    private float density;
    private int level;
    private int step;
    /** Hour (0-23) the screen leaves Night mode by itself; -1 is off. Left/Right change it. */
    private static final int[] WAKE_HOURS = {-1, 6, 7, 8, 9};
    private int wakeIndex;
    private android.widget.TextView hint;

    private final Runnable drift = new Runnable() {
        @Override
        public void run() {
            // A slow walk over a small square around the centre.
            int[] dx = {0, 14, 0, -14, -14, 0, 14, 0};
            int[] dy = {0, 0, 14, 0, -14, -14, 0, 14};
            wakeIfDue();
            step = (step + 1) % dx.length;
            clock.animate().translationX(dx[step] * density).translationY(dy[step] * density).setDuration(2000).start();
            handler.postDelayed(this, DRIFT_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.screenBrightness = 0.01f;
        getWindow().setAttributes(attrs);

        SharedPreferences prefs = getSharedPreferences("night", MODE_PRIVATE);
        level = Math.max(0, Math.min(LEVELS.length - 1, prefs.getInt("dim", 2)));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        clock = new TallClock(this, null);
        clock.setScaleX(0.8f);
        clock.setScaleY(0.8f);
        wakeIndex = Math.max(0, Math.min(WAKE_HOURS.length - 1, prefs.getInt("wake", 0)));
        clock.setAlpha(0f);
        root.addView(clock, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        hint = new android.widget.TextView(this);
        hint.setTextColor(0xFFFFFFFF);
        hint.setTextSize(18f);
        hint.setTypeface(getResources().getFont(R.font.sf_medium));
        hint.setAlpha(0f);
        root.addView(hint, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM));
        ((FrameLayout.LayoutParams) hint.getLayoutParams()).bottomMargin = (int) (60 * density);
        setContentView(root);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // The clock fades in slowly, so the room does not flash when the screen goes black.
        clock.animate().alpha(LEVELS[level]).setDuration(2500).start();
        hideSystemUi();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        handler.postDelayed(drift, DRIFT_MS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(drift);
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                int next = level + (keyCode == KeyEvent.KEYCODE_DPAD_UP ? 1 : -1);
                level = Math.max(0, Math.min(LEVELS.length - 1, next));
                clock.setAlpha(LEVELS[level]);
                getSharedPreferences("night", MODE_PRIVATE).edit().putInt("dim", level).apply();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                int n = wakeIndex + (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                wakeIndex = Math.max(0, Math.min(WAKE_HOURS.length - 1, n));
                getSharedPreferences("night", MODE_PRIVATE).edit().putInt("wake", wakeIndex).apply();
                hint.setText(WAKE_HOURS[wakeIndex] < 0 ? "Wake-up off" : String.format(java.util.Locale.US, "Wake up at %02d:00", WAKE_HOURS[wakeIndex]));
                hint.animate().cancel();
                hint.setAlpha(Math.min(0.5f, LEVELS[level] + 0.12f));
                hint.animate().alpha(0f).setStartDelay(2500).setDuration(800).start();
                return true;
            default:
                if (event.getRepeatCount() == 0) finish();
                return true;
        }
    }

    /** Back to Home at the chosen wake-up hour (checked once a minute together with the drift). */
    private void wakeIfDue() {
        int hour = WAKE_HOURS[wakeIndex];
        if (hour < 0) return;
        java.util.Calendar now = java.util.Calendar.getInstance();
        if (now.get(java.util.Calendar.HOUR_OF_DAY) == hour && now.get(java.util.Calendar.MINUTE) == 0) finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
