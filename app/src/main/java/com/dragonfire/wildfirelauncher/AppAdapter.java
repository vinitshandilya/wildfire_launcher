package com.dragonfire.wildfirelauncher;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
    private String TAG = "VINIT";

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

        final AppObject appObject = appObjectList.get(position);

        ImageView icon = v.findViewById(R.id.appicondrawable);
        TextView appname = v.findViewById(R.id.appname);
        TextView badge = v.findViewById(R.id.badge);
        appname.setText(appObject.getAppname());
        icon.setImageBitmap(appObject.getAppicon());

        if(appObject.getNotifcount() == 0) {
            badge.setVisibility(View.GONE);
        }
        else {
            badge.setVisibility(View.VISIBLE);
            try {
                badge.setText(appObject.getNotifcount() + "");
            } catch(Exception e) {
                Log.d("VINIT", e.toString());
            }
        }

        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.getParent().requestDisallowInterceptTouchEvent(true); // LOCK THE DRAWER FIRST
                Log.d(TAG, "LONG CLICK ON " + appObject.getAppname());
                mAppLongClickListener.onAppLongClicked(appObject, v);

                //+------- set touch listener -----+//
                v.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch(event.getAction()) {

                            case MotionEvent.ACTION_DOWN:
                                Log.d(TAG, "ACTION DOWN " + appObject.getAppname());
                                return false;

                            case MotionEvent.ACTION_MOVE:
                                Log.d(TAG, "ACTION MOVE " + appObject.getAppname());
                                mAppDragListener.onAppDragged(appObject, v);
                                return true;

                            case MotionEvent.ACTION_UP:
                                Log.d(TAG, "ACTION UP " + appObject.getAppname());
                                v.setOnTouchListener(null);
                                return true;

                            case MotionEvent.ACTION_CANCEL:
                                Log.d(TAG, "ACTION CANCEL " + appObject.getAppname());
                                v.setOnTouchListener(null);
                                return true;

                            default:
                                return false;

                        }
                    }
                });
                return true; // END OF LONG CLICK LISTENER
            }
        });

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAppClickListener.onAppClicked(appObject, v);
            }
        });

        return v;
    }

}