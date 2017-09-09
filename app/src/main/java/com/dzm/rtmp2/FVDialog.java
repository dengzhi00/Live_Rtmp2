package com.dzm.rtmp2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.hardware.Camera;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;


import com.dzm.collector.LiveConfig;

import java.util.List;

/**
 *
 * @author 邓治民
 * date 2017/8/29 18:43
 */
public class FVDialog extends Dialog{

    private List<Camera.Size> list;

    public FVDialog(Context context, final FVDialogListener listener) {
        super(context);
        setContentView(R.layout.activity_fv);
        ListView lv_listview = (ListView) findViewById(R.id.lv_listview);
        list = LiveConfig.getCameraSize((Activity) context);
        lv_listview.setAdapter(new FVAdapter(list));
        lv_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Camera.Size size = list.get(position);
                if(null != listener)
                    listener.fVListener(size);
                dismiss();
            }
        });
    }

    private class FVAdapter extends BaseAdapter {

        private List<Camera.Size> list;

        FVAdapter(List<Camera.Size> list){
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(null == convertView){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fv,null,false);
                viewHolder = new ViewHolder();
                viewHolder.tv_fv = (TextView) convertView.findViewById(R.id.tv_fv);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Camera.Size size = list.get(position);
            viewHolder.tv_fv.setText(size.width+"*"+size.height);
            return convertView;
        }
    }

    private class ViewHolder {
        TextView tv_fv;
    }

    public interface FVDialogListener{
        void fVListener(Camera.Size size);
    }
}
