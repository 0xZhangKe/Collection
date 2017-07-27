package com.zhangke.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;

import java.util.List;

/**
 * Created by ZhangKe at 2017/7/27
 */
public class SelectBottomDialog extends BottomSheetDialog{

    public SelectBottomDialog(@NonNull Context context) {
        super(context);
    }

    public SelectBottomDialog(@NonNull Context context, int theme) {
        super(context, theme);
    }

    public SelectBottomDialog(@NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> data, @Nullable Menu menu, int deviceId) {

    }
}
