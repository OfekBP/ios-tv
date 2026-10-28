package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;

/**
 * The big iOS 26 lock screen clock (center_clock): SF Pro Compressed Black numerals stretched tall, with round colon dots,
 * filled with a glass gradient that is solid at the top and lets the wallpaper through toward the bottom. One draw per
 * minute, no animation, no layout work.
 */
public final class TallClock extends View {

    private static final float TEXT_DP = 205f;
    private static final float SCALE_X = 1.0f;
    private static final float SCALE_Y = 1.9f;
    /** Colon dot diameter as a share of the numeral height, and its two centres from the top. */
    private static final float DOT_DIAMETER = 0.2f;
    private static final float DOT_TOP = 0.33f;
    private static final float DOT_BOTTOM = 0.70f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Rect digit = new Rect();
    private final RectF dot = new RectF();
    private final float density;
    private String hours = "";
    private String minutes = "";
    private float capHeight;
    private boolean registered;

    private final BroadcastReceiver ticker = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    public TallClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        paint.setTypeface(getResources().getFont(R.font.sf_compressed_black));
        paint.setTextSize(TEXT_DP * density);
        paint.getTextBounds("0", 0, 1, digit);
        capHeight = digit.height();
        // Glass: solid white at the top, fading to a faint veil at the bottom so the picture shows through.
        paint.setShader(new LinearGradient(0, -capHeight, 0, 0,
                new int[]{0xFAFFFFFF, 0xEBFFFFFF, 0xB0FFFFFF, 0x66FFFFFF},
                new float[]{0f, 0.42f, 0.78f, 1f}, Shader.TileMode.CLAMP));
        update();
    }

    private void update() {
        Calendar now = Calendar.getInstance();
        int h = now.get(Calendar.HOUR_OF_DAY);
        int m = now.get(Calendar.MINUTE);
        // The TV's own 12/24 hour setting (Settings > Date & time > Use 24-hour format).
        boolean h24 = android.text.format.DateFormat.is24HourFormat(getContext());
        int shown = h24 ? h : (h % 12 == 0 ? 12 : h % 12);
        String nextHours = h24 ? String.format(Locale.US, "%02d", shown) : String.format(Locale.US, "%d", shown);
        String nextMinutes = String.format(Locale.US, "%02d", m);
        if (!nextHours.equals(hours) || !nextMinutes.equals(minutes)) {
            hours = nextHours;
            minutes = nextMinutes;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!registered) {
            IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
            f.addAction(Intent.ACTION_TIME_CHANGED);
            f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getContext().registerReceiver(ticker, f);
            registered = true;
        }
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (registered) {
            getContext().unregisterReceiver(ticker);
            registered = false;
        }
        super.onDetachedFromWindow();
    }

    /** Width of the colon block in text space: the two dots plus the air around them. */
    private float colonWidth() {
        return capHeight * DOT_DIAMETER * 1.9f;
    }

    private float contentWidth() {
        return (paint.measureText(hours) + colonWidth() + paint.measureText(minutes)) * SCALE_X;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float pad = 18f * density;
        int w = (int) (contentWidth() + pad * 2);
        int h = (int) (capHeight * SCALE_Y + pad * 2);
        setMeasuredDimension(resolveSize(w, widthSpec), resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float pad = 18f * density;
        canvas.save();
        canvas.translate((getWidth() - contentWidth()) / 2f, pad + capHeight * SCALE_Y);
        canvas.scale(SCALE_X, SCALE_Y);
        float x = 0f;
        canvas.drawText(hours, x, 0, paint);
        x += paint.measureText(hours);
        // Round dots: drawn as ovals in the stretched space so they come out as circles on screen.
        float radius = capHeight * DOT_DIAMETER / 2f;
        float cx = x + colonWidth() / 2f;
        for (float centre : new float[]{DOT_TOP, DOT_BOTTOM}) {
            float cy = -capHeight * (1f - centre);
            dot.set(cx - radius / SCALE_X, cy - radius / SCALE_Y, cx + radius / SCALE_X, cy + radius / SCALE_Y);
            canvas.drawOval(dot, paint);
        }
        x += colonWidth();
        canvas.drawText(minutes, x, 0, paint);
        canvas.restore();
    }
}
