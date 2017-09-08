package com.example.admin.btwifichat.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.admin.btwifichat.R;

/**
 * Created by admin on 2017/3/29.
 */

public class SwitchButton extends View implements View.OnClickListener {

    //bitmap
    private Bitmap mSwitchBottom,mSwitchFrame,mSwitchThumb,mSwitchMask;
    private float mCurrentX=0;
    //默认开关是开的
    private boolean mSwitchOn=true;
    //最大移动距离
    private int mMoveLength;
    //第一次按下的有效区域
    private float mLastX;
    //绘制的目标区域的大小
    private Rect mDest;
    //截取原图片的大小
    private Rect mSrc;
    //移动的偏移量
    private int mDeltX;
    private Paint mPaint;
    private boolean mFlag;

    private OnChangeListener mListener;


    public SwitchButton(Context context) {
        this(context,null);
    }

    public SwitchButton(Context context, AttributeSet attrs) {
        this(context,attrs,0);
    }

    public SwitchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化相关资源
     * */
    private void init() {
        mSwitchBottom= BitmapFactory.decodeResource(getResources(),R.mipmap.switch_bottom);
        mSwitchFrame=BitmapFactory.decodeResource(getResources(),R.mipmap.switch_frame);
        mSwitchThumb=BitmapFactory.decodeResource(getResources(),R.mipmap.switch_btn_press);
        mSwitchMask=BitmapFactory.decodeResource(getResources(),R.mipmap.switch_mask);

        setOnClickListener(this);

        mMoveLength=mSwitchBottom.getWidth()-mSwitchFrame.getWidth();
        mDest=new Rect(0,0,mSwitchFrame.getWidth(),mSwitchBottom.getHeight());
        mSrc=new Rect();
        mPaint=new Paint();
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setAlpha(255);

        //处理两图相交的问题
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimension(mSwitchFrame.getWidth(),mSwitchFrame.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //往右偏移且开关是开的状态
        if (mDeltX>0||mDeltX==0&&mSwitchOn){
            if (mSrc!=null){
                mSrc.set(mMoveLength-mDeltX,0,
                        mSwitchBottom.getWidth()-mDeltX,mSwitchFrame.getHeight());

            }
        }//左移开关是关的状态
        else if (mDeltX<0||mDeltX==0&&!mSwitchOn){
            if (mSrc!=null){
                mSrc.set(-mDeltX,0,mSwitchFrame.getWidth()-mDeltX,
                        mSwitchFrame.getHeight());
            }
        }

        //创建一个新的图层到栈中
        int count=canvas.saveLayer(new RectF(mDest),null,Canvas.MATRIX_SAVE_FLAG
                |Canvas.CLIP_SAVE_FLAG
                |Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
                |Canvas.FULL_COLOR_LAYER_SAVE_FLAG
                |Canvas.CLIP_TO_LAYER_SAVE_FLAG);

        canvas.drawBitmap(mSwitchBottom,mSrc,mDest,null);
        canvas.drawBitmap(mSwitchThumb,mSrc,mDest,null);
        canvas.drawBitmap(mSwitchFrame,0,0,null);
        canvas.drawBitmap(mSwitchMask,0,0,mPaint);

        //画布返回到count之前的状态
        canvas.restoreToCount(count);
    }

    @Override
    public void onClick(View v) {

        mDeltX=mSwitchOn ? mMoveLength:-mMoveLength;
        mSwitchOn=!mSwitchOn;

        if (mListener!=null){
            mListener.onChange(this,mSwitchOn);
        }
        invalidate();
        mDeltX=0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX=event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                mCurrentX=event.getX();
                mDeltX= (int) (mCurrentX-mLastX);

                //如果开关开着向左滑动，或者开关关着向右滑动（这时候是不需要处理的）
                if ((mDeltX<0&&mSwitchOn)||(mDeltX>0&&!mSwitchOn)){
                    mFlag=true;
                    mDeltX=0;
                }

                //如果滑动的距离大于最大位移距离
                if (Math.abs(mDeltX)>mMoveLength){
                    mDeltX=mDeltX>0 ? mMoveLength: -mMoveLength;

                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:

                if (Math.abs(mDeltX)>0&& Math.abs(mDeltX)<mMoveLength/2){
                    mDeltX=0;
                    invalidate();
                    return true;

                }else if (Math.abs(mDeltX)>mMoveLength/2&& Math.abs(mDeltX)<mMoveLength){

                    mDeltX=mDeltX>0?mMoveLength:-mMoveLength;
                    mSwitchOn=!mSwitchOn;

                    if (mListener!=null){
                        mListener.onChange(this,mSwitchOn);
                    }
                    invalidate();
                    mDeltX=0;
                    return true;

                }else if (mDeltX==0&&mFlag){
                    //这时候是不要处理的，因为已经开关已经滑动过了
                    mDeltX=0;
                    mFlag=false;
                    return true;
                }
                break;
        }
        invalidate();
        return super.onTouchEvent(event);
    }

    public void setOnChangeListener(OnChangeListener listener){
        mListener=listener;
    }

    public interface OnChangeListener{
         void onChange(SwitchButton sb,boolean state);
    }
}
