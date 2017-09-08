package com.example.admin.btwifichat.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.admin.btwifichat.R;

/**
 * Created by admin on 2017/4/6.
 */

public class TitleLayout extends LinearLayout implements View.OnClickListener {

    private TextView backText,title;


    public TitleLayout(final Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.activity_head,this);

        backText=(TextView)findViewById(R.id.back_text);
        title= (TextView) findViewById(R.id.activity_title);

        backText.setOnClickListener(this);
        findViewById(R.id.back_btn).setOnClickListener(this);

    }

    public void setTitle(String text){
        title.setText(text);
    }

    public void setBackText(String text){
        backText.setText(text);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_btn:
            case R.id.back_text:
                ((Activity) getContext()).finish();
                break;

        }
    }
}
