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

    private final Runnable drift = new Runnable() {
        @Override
        public void run() {
            // A slow walk over a small square around the centre.
            int[] dx = {0, 14, 0, -14, -14, 0, 14, 0};
            int[] dy = {0, 0, 14, 0, -14, -14, 0, 14};
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
        clock.setAlpha(LEVELS[level]);
        root.addView(clock, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        setContentView(root);
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
            default:
                if (event.getRepeatCount() == 0) finish();
                return true;
        }
    }
}
