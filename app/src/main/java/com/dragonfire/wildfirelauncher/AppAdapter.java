package com.dragonfire.wildfirelauncher;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppAdapter extends BaseAdapter {

    private Context context;
    private List<AppObject> appObjectList;
    private AppClickListener mAppClickListener;
    private AppLongClickListener mAppLongClickListener;
    private AppDragListener mAppDragListener;

    void setmAppLongClickListener(AppLongClickListener mAppLongClickListener) {
        this.mAppLongClickListener = mAppLongClickListener;
    }

    void setmAppClickListener(AppClickListener mAppClickListener) {
        this.mAppClickListener = mAppClickListener;
    }

    void setmAppDragListener(AppDragListener mAppDragListener) {
        this.mAppDragListener = mAppDragListener;
    }

    AppAdapter(Context context, List<AppObject> appObjectList) {
        this.context = context;
        this.appObjectList = appObjectList;
    }

    @Override
    public int getCount() {
        return appObjectList.size();
    }

    @Override
    public Object getItem(int position) {
        return appObjectList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v;
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            v = inflater.inflate(R.layout.app_item, parent, false);
        }
        else {
            v = convertView;
        }

        ImageView icon = v.findViewById(R.id.appicondrawable);
        TextView appname = v.findViewById(R.id.appname);

        icon.setImageDrawable(appObjectList.get(position).getAppicon());
        appname.setText(appObjectList.get(position).getAppname());

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAppClickListener!=null) {
                    mAppClickListener.onAppClicked(appObjectList.get(position), v);
                }
            }
        });

        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(mAppLongClickListener!=null) {
                    mAppLongClickListener.onAppLongClicked(appObjectList.get(position), v);
                    return true;
                }
                else return false;
            }
        });

        v.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                Log.d("Agent", event.getAction() + "");
                if(mAppDragListener!=null) {
                    mAppDragListener.onAppDragged(appObjectList.get(position), v, event);
                    Log.d("Agent", appObjectList.get(position).getAppname());
                    return true;

                }
                else return false;

            }
        });

        return v;
    }

}
