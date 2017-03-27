package com.zhangke.window;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.List;


/**
 * @author 张可
 * @version 1.0
 * @date 2017.03.27
 */
public class NumberInputView extends View {

    /**
     * 输入框个数
     */
    private int count = 4;
    /**
     * 每个框大小
     */
    private int boxSize;
    /**
     * 文本大小
     */
    private int textSize;
    /**
     * 文本颜色
     */
    private int textColor;
    /**
     * padding
     */
    private int padding;

    /**
     * 当前已输入的文本
     */
    private StringBuilder currentNumber = new StringBuilder();

    private InputMethodManager inputMethodManager;
    private Paint boxPaint = new Paint();
    private TextPaint textPaint = new TextPaint();

    private float textBaseY = 0f;

    public NumberInputView(@NonNull Context context) {
        super(context);
        init();
    }

    public NumberInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (null != attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberInputView);
            count = a.getInteger(R.styleable.NumberInputView_box_count, 4);
            a.recycle();
        }
        init();
    }

    public NumberInputView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (null != attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberInputView);
            count = a.getInteger(R.styleable.NumberInputView_box_count, 4);
            a.recycle();
        }
        init();
    }

    private void init() {
        boxSize = UiTools.dip2px(getContext(), 50);
        padding = UiTools.dip2px(getContext(), 5);
        textSize = UiTools.sp2px(getContext(), 20);
        textColor = getContext().getResources().getColor(R.color.text_black);
        List l;

        boxPaint.setAntiAlias(true);
        boxPaint.setColor(getContext().getResources().getColor(R.color.text_black));
        boxPaint.setStyle(Paint.Style.STROKE);

        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float fontHeight = fontMetrics.bottom - fontMetrics.top;

        inputMethodManager = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        setFocusableInTouchMode(true);
        showInputMethod();
        setBackground(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        width = count * boxSize + padding * 2;
        height = boxSize + padding * 2;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawFrame(canvas);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float fontHeight = fontMetrics.bottom - fontMetrics.top;
        if (textBaseY == 0)
            textBaseY = getHeight() - (getHeight() - fontHeight) / 2 - fontMetrics.bottom;
        int y = (int) textBaseY;

        if (!TextUtils.isEmpty(currentNumber)) {
            if (currentNumber.length() > count) {
                currentNumber.delete(count, currentNumber.length() - 1);
            }
            for (int i = 0; i < currentNumber.length(); i++) {
                canvas.drawText("" + currentNumber.charAt(i),
                        padding + boxSize * i + (boxSize / 2),
                        y,
                        textPaint);
            }
        }

    }

    /**
     * 绘制边框
     *
     * @param canvas
     */
    private void drawFrame(Canvas canvas) {
        RectF oval = new RectF(padding,
                padding,
                getWidth() - padding,
                getHeight() - padding);
        canvas.drawRoundRect(oval, 10, 10, boxPaint);

        for (int i = 1; i < count; i++) {
            canvas.drawLine(padding + boxSize * i,
                    padding,
                    padding + boxSize * i,
                    padding + boxSize,
                    boxPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showInputMethod();
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //接收按键事件，67是删除键(backspace),7-16就是0-9
        if (keyCode == 67 && currentNumber.length() > 0) {
            currentNumber.deleteCharAt(currentNumber.length() - 1);
            //重新绘图
            invalidate();
        } else if (keyCode >= 7 && keyCode <= 16 && currentNumber.length() < count) {
            currentNumber.append(keyCode - 7);
            invalidate();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_NUMBER;//定义软键盘样式为数字键盘
        return super.onCreateInputConnection(outAttrs);
    }

    /**
     * 获取当前已输入文本
     *
     * @return
     */
    public String getCurrentNumber() {
        return currentNumber.toString();
    }

    /**
     * 设置当前显示验证码
     *
     * @param currentNumber
     */
    public void setCurrentNumber(String currentNumber) {
        if (!TextUtils.isEmpty(this.currentNumber)) {
            this.currentNumber.delete(0, currentNumber.length() - 1);
        }
        if (!TextUtils.isEmpty(currentNumber)) {
            if (currentNumber.length() > count) {
                currentNumber.substring(0, count);
            }
            this.currentNumber.append(currentNumber);
        }
        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    /**
     * 打开输入法
     */
    private void showInputMethod() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                inputMethodManager.viewClicked(NumberInputView.this);
                inputMethodManager.showSoftInput(NumberInputView.this,
                        InputMethodManager.SHOW_FORCED);
            }
        }, 100);
    }

    /**
     * 关闭输入法
     */
    private void closeInputMethod() {
        post(new Runnable() {
            @Override
            public void run() {
                if (inputMethodManager.isActive()) {
                    inputMethodManager.hideSoftInputFromInputMethod(NumberInputView.this.getWindowToken(),
                            0);
                }
            }
        });
    }
}
