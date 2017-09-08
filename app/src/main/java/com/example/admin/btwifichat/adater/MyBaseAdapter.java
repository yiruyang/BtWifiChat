package com.example.admin.btwifichat.adater;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

/**
 * Created by admin on 2017/4/14.
 */

public class MyBaseAdapter extends AdapterView<Adapter> {

    public MyBaseAdapter(Context context) {
        super(context);
    }

    public MyBaseAdapter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyBaseAdapter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyBaseAdapter(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public Adapter getAdapter() {
        return null;
    }

    @Override
    public void setAdapter(Adapter adapter) {

    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setSelection(int position) {

    }

}
