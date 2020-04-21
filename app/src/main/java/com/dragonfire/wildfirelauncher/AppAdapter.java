package com.dragonfire.wildfirelauncher;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class AppAdapter extends BaseAdapter {

    private Context context;
    private List<AppObject> appObjectList;
    private AppClickListener mAppClickListener;
    private AppLongClickListener mAppLongClickListener;
    private AppActionDownListener mAppActionDownListener;
    private AppDragListener mAppDragListener;
    private long t1=0, t2=0;

    void setmAppActionDownListener(AppActionDownListener mAppActionDownListener) {
        this.mAppActionDownListener = mAppActionDownListener;
    }

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

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        //Log.d("Cook", "Action down: " + ev.getAction());
                        v.setPressed(true);
                        if(mAppActionDownListener != null) {
                            mAppActionDownListener.onAppActionDown(appObjectList.get(position), v);
                        }
                        t1 = System.currentTimeMillis();
                        return false;

                    case MotionEvent.ACTION_UP:
                        //Log.d("Cook", "Action up: " + ev.getAction());
                        v.setPressed(false);
                        t2 = System.currentTimeMillis();
                        if(Math.abs(t2-t1) < ViewConfiguration.getLongPressTimeout()) {
                            if(mAppClickListener!=null) {
                                mAppClickListener.onAppClicked(appObjectList.get(position), v);
                            }
                        }

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        v.setPressed(false);
                        //Log.d("Cook", "Action move: " + ev.getAction());
                        if(mAppDragListener!=null) {
                            mAppDragListener.onAppDragged(appObjectList.get(position), v);
                        }
                        return true;

                    default:
                        v.setPressed(false);
                        return false;

                }
            }
        });

        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Toast.makeText(context, "Long clicked on " + appObjectList.get(position).getAppname(), Toast.LENGTH_SHORT).show();
                if(mAppLongClickListener!=null) {
                    mAppLongClickListener.onAppLongClicked(appObjectList.get(position), v);
                }
                return false;
            }
        });

        return v;
    }

}
