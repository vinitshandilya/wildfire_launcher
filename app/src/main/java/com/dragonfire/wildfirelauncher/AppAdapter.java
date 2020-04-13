package com.dragonfire.wildfirelauncher;

import android.content.ClipData;
import android.content.ClipDescription;
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
    private AppDragListener mAppDragListener;
    private long t1=0, t2=0;

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

        /*v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAppClickListener!=null) {
                    mAppClickListener.onAppClicked(appObjectList.get(position), v);
                }
            }
        });*/

        /*v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(mAppLongClickListener!=null) {
                    mAppLongClickListener.onAppLongClicked(appObjectList.get(position), v);
                    return true;
                }
                else return false;
            }
        });*/

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d("COOK", "ACTION_DOWN: " + ev.getX() + ", " + ev.getY());
                        t1 = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_UP:
                        Log.d("COOK", "ACTION_UP: " + ev.getX() + ", " + ev.getY());
                        t2 = System.currentTimeMillis();
                        if(Math.abs(t2-t1) <=200) {
                            Toast.makeText(context, "Click event", Toast.LENGTH_SHORT).show();
                            if(mAppClickListener!=null) {
                                mAppClickListener.onAppClicked(appObjectList.get(position), v);
                            }
                        }
                        else if(Math.abs(t2-t1) > 200) {
                            Toast.makeText(context, "Long click event", Toast.LENGTH_SHORT).show();
                            if(mAppLongClickListener!=null) {
                                mAppLongClickListener.onAppLongClicked(appObjectList.get(position), v);
                                return true;
                            }
                            else return false;
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        Log.d("COOK", "ACTION_MOVE: " + ev.getX() + ", " + ev.getY());

                        ClipData.Item item = new ClipData.Item(appObjectList.get(position).getAppname()+"~"+appObjectList.get(position).getPackagename()+"~"+appObjectList.get(position).getAppicon());
                        ClipData dragData = new ClipData(
                                (CharSequence) v.getTag(),
                                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                                item);
                        v.findViewById(R.id.appicondrawable).startDrag(dragData,  // the data to be dragged
                                new View.DragShadowBuilder(v.findViewById(R.id.appicondrawable)),  // the drag shadow builder
                                null,      // no need to use local data
                                0          // flags (not currently used, set to 0)
                        );

                        if(mAppDragListener!=null) {
                            mAppDragListener.onAppDragged(appObjectList.get(position), v);
                        }

                        return true;

                    default:
                        return false;

                }
            }
        });

        return v;
    }
}
