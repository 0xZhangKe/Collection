package com.zhangke.eventtest;

import android.content.Context;
import android.graphics.Matrix;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * 支持双指缩小/放大图片，四指截图的 ImageView
 * <p>
 * Created by ZhangKe on 2019/6/10.
 */
public class GestureImageView extends AppCompatImageView {

    private static final String TAG = "GestureImageView";

    /**
     * 手指 ID 的默认值
     */
    private final int DEFAULT_POINT_ID = Integer.MAX_VALUE;
    /**
     * 用于吸收点击事件，
     * 当 DOWN 方法与 UP 方法间隔小于改阈值则认定为点击事件
     */
    private final long CLICK_THRESHOLD = 500L;

    /**
     * 最大放大倍数
     */
    private float MAX_MULTIPLE = 10.0F;
    /**
     * 最小缩放倍数
     */
    private float MIN_MULTIPLE = 0.1F;

    /**
     * 上次点击 DOWN 事件时间
     */
    private long lastClickTime = 0;
    /**
     * 被追踪的两个手指的 ID
     */
    private int[] pointerIds = new int[2];
    /**
     * 手指按下时通过 MotionEvent#getX(int) 方法获取到的 X 的值
     */
    private float[] pointerDownX = new float[2];
    /**
     * 手指按下时通过 MotionEvent#getY(int) 方法获取到的 Y 的值
     */
    private float[] pointerDownY = new float[2];
    /**
     * 两个手指上次点下时的距离
     */
    private float lastDistance = 0.0F;
    /**
     * 图片实际宽度
     */
    private float imageRealWidth = 0.0F;
    /**
     * 图片实际高度
     */
    private float imageRealHeight = 0.0F;

    private Matrix mImageMatrix;

    public GestureImageView(Context context) {
        super(context);
        init();
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GestureImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
    }

    /**
     * 此方法被调用后调用 getWidth 方法才能获取到数值，
     * onMeasure 只能获取到 getMeasureWidth
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        adjustDrawable();
    }

    /**
     * 将图片调整至 ImageView 的中间
     */
    public void adjustDrawable() {
        Log.i(TAG, "adjustDrawable");
        scaleAndTranslate(-1.0F, -1.0F);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetTouchStatus();
                pointerIds[0] = event.getPointerId(0);
                pointerDownX[0] = event.getX();
                pointerDownY[0] = event.getY();
                lastClickTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                resetTouchStatus();
                if (System.currentTimeMillis() - lastClickTime < CLICK_THRESHOLD) {
                    //处理点击事件
                    performClick();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (needProcessMove()) {
                    int firstPointerIndex = event.findPointerIndex(pointerIds[0]);
                    int secondPointerIndex = event.findPointerIndex(pointerIds[1]);
                    if (firstPointerIndex == -1 || secondPointerIndex == -1) {
                        resetTouchStatus();
                    } else {
                        float firstPointX = event.getX(firstPointerIndex);
                        float firstPointY = event.getY(firstPointerIndex);
                        float secondPointX = event.getX(secondPointerIndex);
                        float secondPointY = event.getY(secondPointerIndex);
                        float distance = getDistance(firstPointX, firstPointY, secondPointX, secondPointY);
                        scaleImage(distance - lastDistance);
                        lastDistance = distance;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                resetTouchStatus();
                break;
            case MotionEvent.ACTION_OUTSIDE:
                resetTouchStatus();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerIds[1] == DEFAULT_POINT_ID) {
                    pointerIds[1] = event.getPointerId(event.getActionIndex());
                    int pointerIndex = event.findPointerIndex(pointerIds[1]);
                    pointerDownX[1] = event.getX(pointerIndex);
                    pointerDownY[1] = event.getY(pointerIndex);
                    lastDistance = getDistance(pointerDownX[0], pointerDownY[0], pointerDownX[1], pointerDownY[1]);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int curPointId = event.getPointerId(event.getActionIndex());
                if (pointerIds[0] == curPointId) {
                    pointerIds[0] = DEFAULT_POINT_ID;
                } else if (pointerIds[1] == curPointId) {
                    pointerIds[1] = DEFAULT_POINT_ID;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void resetTouchStatus() {
        lastClickTime = 0L;
        pointerIds[0] = DEFAULT_POINT_ID;
        pointerIds[1] = DEFAULT_POINT_ID;
    }

    private boolean needProcessMove() {
        return pointerIds[0] != DEFAULT_POINT_ID && pointerIds[1] != DEFAULT_POINT_ID;
    }

    /**
     * 获取两个点的距离
     */
    private float getDistance(float x, float y, float toX, float toY) {
        float height = Math.abs(toX - x);
        float width = Math.abs(toY - y);
        return (float) Math.sqrt(height * height + width * width);
    }

    /**
     * 缩放图片
     *
     * @param distance 双指滑动距离初始状态下的距离，可为负数
     */
    private void scaleImage(float distance) {
        float scale = getScale(distance);
        scaleAndTranslate(scale, scale);
    }

    /**
     * 获取缩放值
     */
    private float getScale(float distance) {
        if (Math.abs(distance - 0.0F) < 1.0F || getDrawable() == null) {
            return 0F;
        }
        if (imageRealWidth == 0.0F) {
            imageRealWidth = getDrawable().getIntrinsicWidth();
        }
        int drawableWidth = getDrawable().getIntrinsicWidth();
//        float curScale = imageRealWidth / drawableWidth;
        float willWidth = imageRealWidth + distance;
        Log.i(TAG, String.format("distance:%s, willWidth:%s", distance, willWidth));
        return willWidth / drawableWidth;
    }

    private void scaleAndTranslate(float scaleX, float scaleY) {
        if (getDrawable() == null) {
            return;
        }
        if (imageRealWidth == 0.0F) {
            imageRealWidth = getDrawable().getIntrinsicWidth();
        }
        if (imageRealHeight == 0.0F) {
            imageRealHeight = getDrawable().getIntrinsicHeight();
        }
        if (imageRealWidth == 0.0F || imageRealHeight == 0.0F) {
            return;
        }
        if (scaleX == -1.0F || (needScale(scaleX) && needScale(scaleY))) {
            if (mImageMatrix == null) {
                mImageMatrix = new Matrix();
            } else {
                mImageMatrix.reset();
            }
            if (scaleX != -1.0F) {
                mImageMatrix.postScale(scaleX, scaleY);
                imageRealWidth = getDrawable().getIntrinsicWidth() * scaleX;
                imageRealHeight = getDrawable().getIntrinsicHeight() * scaleY;
            }
            float translateX;
            float translateY;
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            translateX = (viewWidth - imageRealWidth) / 2.0F;
            translateY = (viewHeight - imageRealHeight) / 2.0F;
            if (translateX + translateY != 0) {
                mImageMatrix.postTranslate(translateX, translateY);
            }
            float[] values = new float[9];
            mImageMatrix.getValues(values);
            Log.i(TAG, String.format("imageRealWidth:%s, imageRealHeight:%s, scaleX:%s, scaleY:%s, translateX:%s, translateY:%s",
                    imageRealWidth, imageRealHeight, values[Matrix.MSCALE_X],
                    values[Matrix.MSCALE_Y], values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]));
            setImageMatrix(mImageMatrix);
        }
    }

    private boolean needScale(float scale) {
        return scale >= MIN_MULTIPLE && scale <= MAX_MULTIPLE && scale != 1.0F;
    }
}
