package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;

/**
 * Screensaver in the Home screen's design: the same wallpaper, the date above the big glass clock (TallClock), and
 * nothing else. The picture is decoded once at half size and nothing animates except a few pixels of drift each minute,
 * so the same pixels do not stay lit. Any key wakes the TV back to where it was.
 */
public class GlassDream extends DreamService {

    private static final long DRIFT_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout block;
    private float density;
    private int step;
    private Bitmap picture;

    private final Runnable drift = new Runnable() {
        @Override
        public void run() {
            int[] dx = {0, 12, 0, -12, -12, 0, 12, 0};
            int[] dy = {0, 0, 10, 0, -10, -10, 0, 10};
            step = (step + 1) % dx.length;
            block.animate().translationX(dx[step] * density).translationY(dy[step] * density).setDuration(2000).start();
            handler.postDelayed(this, DRIFT_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        density = getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        Config cfg = Config.load(this);
        File file = cfg.wallpaper != null ? cfg.wallpaper : (cfg.wallpapers.isEmpty() ? null : cfg.wallpapers.get(0));
        if (cfg.wallpaperEnabled && file != null && file.exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            picture = BitmapFactory.decodeFile(file.getPath(), o);
            if (picture != null) {
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageBitmap(picture);
                // A little darker than Home: the screensaver sits on screen for hours.
                image.setColorFilter(0xFFCCCCCC, android.graphics.PorterDuff.Mode.MULTIPLY);
                root.addView(image, new FrameLayout.LayoutParams(-1, -1));
            }
        }

        block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER_HORIZONTAL);

        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEE MMM d");
        date.setFormat24Hour("EEE MMM d");
        date.setTextColor(0xF2FFFFFF);
        date.setTextSize(24f);
        date.setIncludeFontPadding(false);
        date.setTypeface(getResources().getFont(R.font.sf_medium));
        date.setShadowLayer(8f * density, 0f, 2f * density, 0x40000000);
        block.addView(date, new LinearLayout.LayoutParams(-2, -2));

        TallClock clock = new TallClock(this, null);
        block.addView(clock, new LinearLayout.LayoutParams(-2, -2));

        root.addView(block, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        setContentView(root);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        handler.postDelayed(drift, DRIFT_MS);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(drift);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(drift);
        picture = null;
        super.onDetachedFromWindow();
    }
}
