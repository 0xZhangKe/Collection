package com.zhangke.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.view.KeyboardShortcutGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.zhangke.simplemail.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhangKe at 2017/7/27
 */
public class SelectBottomDialog extends BottomSheetDialog {

    private View rootView;
    private TextView tvTitle;
    private ViewGroup vg_tab;
    private View viewScrollBar;
    private ListView listView;

    private List<SelectDialogEntity> listData = new ArrayList<>();
    private SelectBottomAdapter listAdapter;

    public SelectBottomDialog(@NonNull Context context) {
        super(context);
        init();
    }

    public SelectBottomDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }

    public SelectBottomDialog(@NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }

    private void init() {
        rootView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_bottom, null);
        tvTitle = (TextView) rootView.findViewById(R.id.tv_title);
        vg_tab = (ViewGroup) rootView.findViewById(R.id.vg_tab);
        viewScrollBar = rootView.findViewById(R.id.view_scroll_bar);
        listView = (ListView) rootView.findViewById(R.id.list_view);

        rootView.findViewById(R.id.img_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });

        for(int i = 0; i < 30; i++){
            listData.add(new SelectDialogEntity(i, "" + i));
        }

        listAdapter = new SelectBottomAdapter(getContext(), listData);
        listView.setAdapter(listAdapter);

        setContentView(rootView);
    }

    @Override
    public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> data, @Nullable Menu menu, int deviceId) {

    }

    public static class SelectDialogEntity {
        private int id;
        private String name;
        private boolean isSelected = false;

        public SelectDialogEntity(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }
}
