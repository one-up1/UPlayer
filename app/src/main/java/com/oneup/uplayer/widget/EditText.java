package com.oneup.uplayer.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;

import java.text.NumberFormat;

public class EditText extends AppCompatEditText implements View.OnLongClickListener {
    private NumberFormat numberFormat;
    private TextWatcher textWatcher;
    private OnTextChangeListener onTextChangeListener;

    public EditText(Context context) {
        this(context, null);
    }

    public EditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectAllOnFocus(true);
        setOnLongClickListener(this);

        numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(false);

        textWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (onTextChangeListener != null) {
                    onTextChangeListener.onTextChange(EditText.this, s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        addTextChangedListener(textWatcher);
    }

    @Override
    public boolean onLongClick(View v) {
        setString(null);
        return true;
    }

    public String getString() {
        String s = getText().toString().trim();
        return s.isEmpty() ? null : s;
    }

    public void setString(String s) {
        removeTextChangedListener(textWatcher);
        setText(s == null ? "" : s);
        addTextChangedListener(textWatcher);
    }

    public int getInt() {
        String s = getString();
        return s == null ? 0 : Integer.parseInt(s);
    }

    public void setInt(int i) {
        setString(numberFormat.format(i));
    }

    public long getLong() {
        String s = getString();
        return s == null ? 0 : Long.parseLong(s);
    }

    public void setLong(long l) {
        setString(numberFormat.format(l));
    }

    public void setMinimumIntegerDigits(int i) {
        numberFormat.setMinimumIntegerDigits(i);
    }

    public void setOnTextChangeListener(OnTextChangeListener l) {
        onTextChangeListener = l;
    }

    public interface OnTextChangeListener {
        void onTextChange(EditText editText, String s);
    }
}
