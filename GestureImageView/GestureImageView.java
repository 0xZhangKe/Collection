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
     * 两个手指最开始点下时的距离
     */
    private float initDistance = 0.0F;
    /**
     * 图片实际宽度
     */
    private float imageRealWidth = 0;

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //是否处理该事件
        boolean handled = false;
        //屏幕上手指数量
        int count = event.getPointerCount();
        String eventType = "";
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetTouchStatus();
                pointerIds[0] = event.getPointerId(0);
                pointerDownX[0] = event.getX();
                pointerDownY[0] = event.getY();
                eventType = "DOWN";
                lastClickTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                resetTouchStatus();
                eventType = "UP";
                if (System.currentTimeMillis() - lastClickTime < CLICK_THRESHOLD) {
                    handled = performClick();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                eventType = "MOVE";
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
                        scaleImage(distance);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                resetTouchStatus();
                eventType = "CANCEL";
                break;
            case MotionEvent.ACTION_OUTSIDE:
                resetTouchStatus();
                eventType = "OUTSIDE";
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                eventType = "POINTER_DOWN";
                if (pointerIds[1] == DEFAULT_POINT_ID) {
                    pointerIds[1] = event.getPointerId(event.getActionIndex());
                    int pointerIndex = event.findPointerIndex(pointerIds[1]);
                    pointerDownX[1] = event.getX(pointerIndex);
                    pointerDownY[1] = event.getY(pointerIndex);
                    initDistance = getDistance(pointerDownX[0], pointerDownY[0], pointerDownX[1], pointerDownY[1]);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                eventType = "POINTER_UP";
                int curPointId = event.getPointerId(event.getActionIndex());
                if (pointerIds[0] == curPointId) {
                    pointerIds[0] = DEFAULT_POINT_ID;
                } else if (pointerIds[1] == curPointId) {
                    pointerIds[1] = DEFAULT_POINT_ID;
                }
                break;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(eventType);
        builder.append(",count:");
        builder.append(count);
        builder.append("\n");
        for (int i = 0; i < count; i++) {
            //PointerId
            int pointId = event.getPointerId(i);
            //pointerIndex
            int pointerIndex = event.findPointerIndex(pointId);
            builder.append(String.format("index:%s, pointId:%s, pointerIndex:%s\n", i, pointId, pointerIndex));
        }
        Log.i(TAG, builder.toString());
        if (handled) {
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        Log.i(TAG, "performClick()");
        return super.performClick();
    }

    private void resetTouchStatus() {
        lastClickTime = 0L;
        pointerIds[0] = DEFAULT_POINT_ID;
        pointerIds[1] = DEFAULT_POINT_ID;
        Matrix matrix = getMatrix();
        if (matrix != null) {
            float[] array = new float[9];
            matrix.getValues(array);
            float scaleX = array[0];
            if (scaleX > 0) {
                imageRealWidth = getWidth() * scaleX;
            }
        }
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
        Matrix matrix = getMatrix();
        float scale = getScale(distance);
        matrix.setScale(scale, scale);
        setImageMatrix(matrix);
    }

    /**
     * 获取缩放值
     */
    private float getScale(float distance) {
        if (Math.abs(distance - 0.0F) < 1.0F) {
            return 0F;
        }
        float anchorWidth;
        if (imageRealWidth == 0) {
            anchorWidth = getDrawable().getIntrinsicWidth();
        } else {
            anchorWidth = imageRealWidth;
        }
        float willWidth = anchorWidth + distance / 2;
        return willWidth / getDrawable().getIntrinsicWidth();
    }
}
