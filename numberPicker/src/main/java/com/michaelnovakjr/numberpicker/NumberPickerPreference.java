
package com.michaelnovakjr.numberpicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

@SuppressLint("NewApi")
public class NumberPickerPreference extends DialogPreference {
    private NumberPicker mPickerCompat;
    private android.widget.NumberPicker mPickerNative;

    private int mStartRange;
    private int mEndRange;
    private int mDefault;

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs == null) {
            return;
        }

        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.numberpicker);
        mStartRange = arr.getInteger(R.styleable.numberpicker_startRange, 0);
        mEndRange = arr.getInteger(R.styleable.numberpicker_endRange, 200);
        mDefault = arr.getInteger(R.styleable.numberpicker_defaultValue, 0);

        arr.recycle();

        setDialogLayoutResource(R.layout.pref_number_picker);
    }

    private boolean isNativePicker() {
        return Build.VERSION.SDK_INT >= 11;
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public NumberPickerPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        if (isNativePicker()) {
            mPickerNative = (android.widget.NumberPicker) view.findViewById(R.id.pref_num_picker);
            mPickerNative.setMinValue(mStartRange);
            mPickerNative.setMaxValue(mEndRange);
            mPickerNative.setValue(getValue());
        } else {
            mPickerCompat = (NumberPicker) view.findViewById(R.id.pref_num_picker);
            mPickerCompat.setRange(mStartRange, mEndRange);
            mPickerCompat.setCurrent(getValue());
        }

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (isNativePicker()) {
            mPickerNative.clearFocus();
        } else {
            mPickerCompat.clearFocus();
        }

        final int origValue = getValue();
        final int curValue = isNativePicker() ? mPickerNative.getValue() : mPickerCompat.getCurrent();

        if (positiveResult && (curValue != origValue)) {
            if (callChangeListener(curValue)) {
                saveValue(curValue);
            }
        }
    }

    public void setRange(int start, int end) {
        mStartRange = start;
        mEndRange = end;
    }

    private boolean saveValue(int val) {
        return persistInt(val);
    }

    private int getValue() {
        return getSharedPreferences().getInt(getKey(), mDefault);
    }
}
