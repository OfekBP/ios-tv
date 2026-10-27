package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * The on-screen keyboard drawn in the launcher's Liquid Glass style (see GlassKeyboardService): one glass panel, glass
 * keys, the selected key solid white. Pure drawing and a selection cursor moved by the D-pad: no child views, no
 * animation, so a key press costs one invalidate.
 */
final class GlassKeyboardView extends View {

    static final int CHAR = 0, SHIFT = 1, DEL = 2, SPACE = 3, ENTER = 4, MODE = 5, LANG = 6;

    static final class Key {
        final String label;
        final int type;
        final float weight;
        final RectF rect = new RectF();

        Key(String label, int type, float weight) {
            this.label = label;
            this.type = type;
            this.weight = weight;
        }
    }

    interface Listener {
        void onKey(Key key, boolean shifted);
    }

    private static final String[] EN_1 = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
    private static final String[] EN_2 = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
    private static final String[] EN_3 = {"z", "x", "c", "v", "b", "n", "m"};
    private static final String[] HE_1 = {"ק", "ר", "א", "ט", "ו", "ן", "ם", "פ"};
    private static final String[] HE_2 = {"ש", "ד", "ג", "כ", "ע", "י", "ח", "ל", "ך", "ף"};
    private static final String[] HE_3 = {"ז", "ס", "ב", "ה", "נ", "מ", "צ", "ת", "ץ"};
    private static final String[] NUM_1 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    private static final String[] NUM_2 = {"-", "/", ":", ";", "(", ")", "$", "&", "@", "\""};
    private static final String[] NUM_3 = {".", ",", "?", "!", "'", "#", "%"};

    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF panel = new RectF();
    private final RectF selected = new RectF();
    private final int[] onScreen = new int[2];
    private final LiquidGlass glass;
    private final List<List<Key>> rows = new ArrayList<>();
    private final Listener listener;

    private boolean symbols;
    private boolean hebrew;
    private boolean shift;
    private int selRow = 1;
    private int selCol = 3;

    GlassKeyboardView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        density = context.getResources().getDisplayMetrics().density;
        glass = new LiquidGlass(density, 0x6610141A);
        Typeface face = context.getResources().getFont(R.font.sf_medium);
        text.setTypeface(face);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(21f * density);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1f * density);
        rim.setColor(0x4DFFFFFF);
        setFocusable(false);
        rebuild();
    }

    /** Layouts: letters (English or Hebrew) or numbers and punctuation. */
    void setLayout(boolean symbolsOn, boolean hebrewOn, boolean shiftOn) {
        symbols = symbolsOn;
        hebrew = hebrewOn;
        shift = shiftOn;
        rebuild();
    }

    boolean isSymbols() { return symbols; }
    boolean isHebrew() { return hebrew; }
    boolean isShift() { return shift; }

    void setShift(boolean on) {
        if (shift != on) {
            shift = on;
            rebuild();
        }
    }

    private void rebuild() {
        rows.clear();
        List<Key> r1 = new ArrayList<>(), r2 = new ArrayList<>(), r3 = new ArrayList<>(), r4 = new ArrayList<>();
        String[] a = symbols ? NUM_1 : hebrew ? HE_1 : EN_1;
        String[] b = symbols ? NUM_2 : hebrew ? HE_2 : EN_2;
        String[] c = symbols ? NUM_3 : hebrew ? HE_3 : EN_3;
        for (String s : a) r1.add(new Key(s, CHAR, 1f));
        for (String s : b) r2.add(new Key(s, CHAR, 1f));
        if (!symbols && !hebrew) r3.add(new Key("⇧", SHIFT, 1.5f));
        else if (symbols) r3.add(new Key("ABC", MODE, 1.5f));
        for (String s : c) r3.add(new Key(s, CHAR, 1f));
        r3.add(new Key("⌫", DEL, 1.5f));
        r4.add(new Key(symbols ? "ABC" : "123", MODE, 1.5f));
        r4.add(new Key(hebrew ? "EN" : "עב", LANG, 1.2f));
        r4.add(new Key("space", SPACE, 4.6f));
        r4.add(new Key(".", CHAR, 1f));
        r4.add(new Key("return", ENTER, 1.7f));
        rows.add(r1);
        rows.add(r2);
        rows.add(r3);
        rows.add(r4);
        selRow = Math.min(selRow, rows.size() - 1);
        selCol = Math.min(selCol, rows.get(selRow).size() - 1);
        requestLayout();
        invalidate();
    }

    private float keyHeight() { return 46f * density; }
    private float gap() { return 8f * density; }
    private float pad() { return 14f * density; }
    private float panelWidth(int available) { return Math.min(available - 40f * density, 860f * density); }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = (int) (4 * keyHeight() + 3 * gap() + 2 * pad() + 22f * density);
        setMeasuredDimension(w, h);
    }

    private void layoutKeys() {
        float pw = panelWidth(getWidth());
        float left = (getWidth() - pw) / 2f;
        float top = 0f;
        panel.set(left, top, left + pw, top + 4 * keyHeight() + 3 * gap() + 2 * pad());
        float inner = pw - 2 * pad();
        // Row 0 sets the unit; the short second letter row reuses it, centred; the others fill the width.
        List<Key> first = rows.get(0);
        float firstWeight = 0;
        for (Key k : first) firstWeight += k.weight;
        float unit0 = (inner - (first.size() - 1) * gap()) / firstWeight;
        for (int r = 0; r < rows.size(); r++) {
            List<Key> row = rows.get(r);
            float weight = 0;
            for (Key k : row) weight += k.weight;
            float unit = (r == 1 && row.size() <= first.size()) ? unit0 : (inner - (row.size() - 1) * gap()) / weight;
            float width = weight * unit + (row.size() - 1) * gap();
            float x = left + pad() + (inner - width) / 2f;
            float y = top + pad() + r * (keyHeight() + gap());
            for (Key k : row) {
                float kw = k.weight * unit;
                k.rect.set(x, y, x + kw, y + keyHeight());
                x += kw + gap();
            }
        }
    }

    /** Moves the cursor; returns false at an edge so the caller can leave it alone. */
    boolean move(int dRow, int dCol) {
        layoutKeys();
        if (dRow != 0) {
            int next = selRow + dRow;
            if (next < 0 || next >= rows.size()) return false;
            float cx = rows.get(selRow).get(selCol).rect.centerX();
            List<Key> row = rows.get(next);
            int best = 0;
            float bestDist = Float.MAX_VALUE;
            for (int i = 0; i < row.size(); i++) {
                float d = Math.abs(row.get(i).rect.centerX() - cx);
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
            selRow = next;
            selCol = best;
        } else {
            int next = selCol + dCol;
            if (next < 0 || next >= rows.get(selRow).size()) return false;
            selCol = next;
        }
        invalidate();
        return true;
    }

    void pressSelected() {
        Key key = rows.get(selRow).get(selCol);
        listener.onKey(key, shift);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        layoutKeys();
        getLocationOnScreen(onScreen);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        if (Frost.shared != null) glass.setFrost(Frost.shared, dm.widthPixels, dm.heightPixels);
        glass.draw(canvas, panel, 28f * density, onScreen[0], onScreen[1]);
        for (int r = 0; r < rows.size(); r++) {
            List<Key> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                Key k = row.get(c);
                boolean isSel = r == selRow && c == selCol;
                boolean special = k.type != CHAR && k.type != SPACE;
                boolean on = k.type == SHIFT && shift;
                if (isSel) {
                    selected.set(k.rect);
                    selected.inset(-3f * density, -3f * density);
                    fill.setColor(0xF7FFFFFF);
                    canvas.drawRoundRect(selected, 13f * density, 13f * density, fill);
                    text.setColor(0xFF1C1C1E);
                } else {
                    fill.setColor(on ? 0x99FFFFFF : special ? 0x2EFFFFFF : 0x3DFFFFFF);
                    canvas.drawRoundRect(k.rect, 11f * density, 11f * density, fill);
                    canvas.drawRoundRect(k.rect, 11f * density, 11f * density, rim);
                    text.setColor(on ? 0xFF1C1C1E : 0xFFFFFFFF);
                }
                String label = k.label;
                boolean small = k.type == SPACE || k.type == ENTER || k.type == MODE || k.type == LANG;
                text.setTextSize((small ? 17f : 22f) * density);
                if (k.type == CHAR && shift && !symbols && !hebrew) label = label.toUpperCase();
                float cy = k.rect.centerY() - (text.descent() + text.ascent()) / 2f;
                canvas.drawText(label, k.rect.centerX(), cy, text);
            }
        }
    }
}
