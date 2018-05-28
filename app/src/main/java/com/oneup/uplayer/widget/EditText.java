package com.oneup.uplayer.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.text.NumberFormat;

public class EditText extends AppCompatEditText {
    private NumberFormat numberFormat;
    private TextWatcher textWatcher;
    private OnTextChangeListener onTextChangeListener;

    public EditText(Context context) {
        this(context, null);
    }

    public EditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectAllOnFocus(true);

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

    public String getString() {
        return getText().toString().trim();
    }

    public void setString(String s) {
        removeTextChangedListener(textWatcher);
        setText(s);
        addTextChangedListener(textWatcher);
    }

    public int getInt() {
        return length() == 0 ? 0 : Integer.parseInt(getString());
    }

    public void setInt(int i) {
        setString(numberFormat.format(i));
    }

    public long getLong() {
        return length() == 0 ? 0 : Long.parseLong(getString());
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
