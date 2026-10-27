package com.gathin.tvhome;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * An on-screen keyboard in the launcher's Liquid Glass design (see GlassKeyboardView). The D-pad moves a cursor over the
 * keys and OK presses one; the remote's number keys and Delete also work. Back closes it as for any keyboard. Enable it
 * with `ime enable` and `ime set`; the TV's own keyboard stays installed and `ime set` brings it back.
 */
public final class GlassKeyboardService extends InputMethodService implements GlassKeyboardView.Listener {

    private GlassKeyboardView view;
    private boolean hebrew;

    @Override
    public View onCreateInputView() {
        view = new GlassKeyboardView(this, this);
        return view;
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        return true;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (getWindow() != null && getWindow().getWindow() != null) {
            getWindow().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        if (view == null) return;
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        boolean numeric = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE;
        view.setLayout(numeric, hebrew && !numeric, false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (view == null || !isInputViewShown()) return super.onKeyDown(keyCode, event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                view.move(0, -1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                view.move(0, 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                view.move(-1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                view.move(1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (event.getRepeatCount() == 0) view.pressSelected();
                return true;
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_FORWARD_DEL:
                deleteBack();
                return true;
            default:
                if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                    commit(String.valueOf((char) ('0' + (keyCode - KeyEvent.KEYCODE_0))));
                    return true;
                }
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (view != null && isInputViewShown()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    return true;
                default:
                    break;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onKey(GlassKeyboardView.Key key, boolean shifted) {
        switch (key.type) {
            case GlassKeyboardView.CHAR:
                commit(shifted && !view.isSymbols() && !view.isHebrew() ? key.label.toUpperCase() : key.label);
                if (shifted) view.setShift(false);
                break;
            case GlassKeyboardView.SPACE:
                commit(" ");
                break;
            case GlassKeyboardView.DEL:
                deleteBack();
                break;
            case GlassKeyboardView.SHIFT:
                view.setShift(!view.isShift());
                break;
            case GlassKeyboardView.MODE:
                view.setLayout(!view.isSymbols(), hebrew, false);
                break;
            case GlassKeyboardView.LANG:
                hebrew = !hebrew;
                view.setLayout(false, hebrew, false);
                break;
            case GlassKeyboardView.ENTER:
                enter();
                break;
            default:
                break;
        }
    }

    private void commit(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(s, 1);
    }

    private void deleteBack() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    }

    private void enter() {
        EditorInfo info = getCurrentInputEditorInfo();
        boolean multiLine = info != null && (info.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        if (multiLine) {
            commit("\n");
        } else if (!sendDefaultEditorAction(true)) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        }
    }
}
