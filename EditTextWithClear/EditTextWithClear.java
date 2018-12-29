package com.zhangke.websocketdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.zld.zld_face_rec_app.R;
import com.zld.zld_face_rec_app.common.ZLDApplication;
import com.zldlib.util.UiUtil;

/**
 * 带有清除按钮的输入框
 * <p>
 * Created by ZhangKe on 2018/12/19.
 */
public class EditTextWithClear extends AppCompatEditText {

    private Bitmap clearBitmap;
    private Paint mPaint;
    private int clearImageWidth, clearImageHeight;
    private int clearXThreshold;//清除按钮可点击范围大小阈值

    public EditTextWithClear(Context context) {
        super(context);
        init();
    }

    public EditTextWithClear(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditTextWithClear(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        clearXThreshold = dip2px(30F);
        mPaint = new Paint();
        clearImageHeight = clearImageWidth = dip2px(15F);

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidate();
            }
        });

        setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (onFocusChangeListener != null) {
                onFocusChangeListener.onFocusChange(v, hasFocus);
            }
            invalidate();
        });
    }

    private Rect srcRect, dstRect;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!TextUtils.isEmpty(getText()) && isFocused()) {
            if (clearBitmap == null) {
                createClearBitmap();
            }
            if (srcRect == null) {
                srcRect = new Rect();
            }
            if (dstRect == null) {
                dstRect = new Rect();
            }
            srcRect.left = 0;
            srcRect.top = 0;
            srcRect.right = clearImageWidth;
            srcRect.bottom = clearImageHeight;

            dstRect.right = getWidth();
            dstRect.left = dstRect.right - clearImageWidth;
            dstRect.top = (getHeight() - clearImageHeight) / 2;
            dstRect.bottom = dstRect.top + clearImageHeight;
            canvas.drawBitmap(clearBitmap, srcRect, dstRect, mPaint);
        }
    }

    private void createClearBitmap() {
        clearBitmap = Bitmap.createBitmap(clearImageWidth, clearImageHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(clearBitmap);
        Matrix matrix = new Matrix();
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.mipmap.gray_delete);
        matrix.postScale((float) clearImageWidth / (float) image.getWidth(), (float) clearImageHeight / (float) image.getHeight());
        canvas.drawBitmap(image,
                matrix,
                mPaint);
    }

    private OnFocusChangeListener onFocusChangeListener;

    public void setCustomOnFocusChangeListener(OnFocusChangeListener listener) {
        this.onFocusChangeListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && (event.getX() + clearXThreshold > getWidth())) {
            setText("");
            return true;
        }else{
            return super.onTouchEvent(event);
        }
    }

    private int dip2px(float dipValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) ((double) dipValue * (double) scale + 0.5);
    }
}
