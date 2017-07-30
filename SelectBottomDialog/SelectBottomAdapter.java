package com.zhangke.widget;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zhangke.simplemail.R;

import java.util.List;

/**
 * Created by ZhangKe at 2017/7/30
 */
public class SelectBottomAdapter extends BaseAdapter {

    private Context context;
    private List<SelectBottomDialog.SelectDialogEntity> listData;
    private LayoutInflater inflater;

    public SelectBottomAdapter(Context context, List<SelectBottomDialog.SelectDialogEntity> listData) {
        this.context = context;
        this.listData = listData;

        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return listData == null ? 0 : listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = inflater.inflate(R.layout.adapter_select_bottom, null, false);
            holder = new ViewHolder();
            holder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tv_name.setText(listData.get(position).getName());
        return convertView;
    }

    class ViewHolder{
        TextView tv_name;
    }
}
