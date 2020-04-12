package com.dragonfire.wildfirelauncher;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener, AppLongClickListener, AppClickListener {

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;
    private BottomSheetBehavior mBottomSheetBehavior;
    private View bottomSheet;
    private List<AppObject> installedAppList;
    private AppAdapter gridAdapter;
    private boolean drawerExpanded=false;
    private int currentDrawerState=-1;
    private GridView drawerGridView;
    private EditText searchbar;
    private PackageListener mPackageListener;
    private PopupMenu popupMenu;

    DisplayMetrics displaymetrics;
    int screenHight, screenWidth;

    private static final int NUM_PAGES = 5;
    private ViewPager2 viewPager;
    String TAG="Wildfire";

    AppObject myapp;
    View longclickedview;



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;

        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.pager);
        FragmentStateAdapter pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        bottomSheet = findViewById( R.id.bottom_sheet );
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable(false);

        drawerGridView = findViewById(R.id.grid);

        installedAppList = new ArrayList<>();
        getInstalledAppList(); // fill in the installedAppList
        searchbar = findViewById(R.id.searchbar);
        gridAdapter = new AppAdapter(getApplicationContext(), installedAppList);

        gridAdapter.setmAppClickListener(this);
        gridAdapter.setmAppLongClickListener(this);

        drawerGridView.setAdapter(gridAdapter);

        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                currentDrawerState = newState;
                if(newState == BottomSheetBehavior.STATE_HIDDEN && drawerGridView.getChildAt(0).getY()!=0) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                if(newState == BottomSheetBehavior.STATE_DRAGGING && drawerGridView.getChildAt(0).getY()!=0) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                if(newState==BottomSheetBehavior.STATE_EXPANDED) {
                    drawerExpanded = true;
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // DARK STATUS ICONS.
                }
                if(newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    drawerExpanded = false;
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN); // LIGHT STATUS ICONS.
                }

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //Log.d("slideoffset", slideOffset + "");
                if(drawerExpanded) {
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN); // LIGHT STATUS ICONS.
                }
                if(!drawerExpanded) {

                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // DARK STATUS ICONS.
                }


            }
        });

        searchbar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                List<AppObject> filteredApps = new ArrayList<>();
                gridAdapter = new AppAdapter(getBaseContext(), filteredApps);
                drawerGridView.setAdapter(gridAdapter);
                gridAdapter.setmAppClickListener(MainActivity.this);
                gridAdapter.setmAppLongClickListener(MainActivity.this);
                for(AppObject currentApp : installedAppList) {
                    if(currentApp.getAppname().toLowerCase().contains(s.toString().toLowerCase())) {
                        Log.d("Wildfire", currentApp.getAppname());
                        filteredApps.add(currentApp);
                        gridAdapter.notifyDataSetChanged();
                    }
                }

            }
        });

        // Long hold to select widgets
        viewPager.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                selectWidget();
                return true;
            }
        });

        // Broadcast receiver when packages are installed
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        mPackageListener = new PackageListener();
        registerReceiver(mPackageListener, filter);

        // Drag receiver on home screen grid
        final RelativeLayout homescreen = findViewById(R.id.homescreen);
        homescreen.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        Log.d(TAG, "HOME: ACTION_DRAG_STARTED");
                        Log.d(TAG, v.toString());
                        //img.setColorFilter(Color.argb(100, 255, 255, 255));

                    case DragEvent.ACTION_DRAG_ENTERED:
                        Log.d(TAG, "HOME: ACTION_DRAG_ENTERED");
                        Log.d(TAG, v.toString());
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        try {
                            popupMenu.dismiss();
                        }
                        catch (Exception e) {
                            System.out.println(e.toString());
                        }

                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        Log.d(TAG, v.toString());
                        Log.d(TAG, "HOME: ACTION_DRAG_EXITED");

                        break;
                    case DragEvent.ACTION_DROP:
                        //Log.d(TAG, v.toString());
                        Log.d(TAG, "HOME: ACTION_DRAG_DROP");
                        ImageView appicon = new ImageView(getBaseContext());
                        appicon.setImageDrawable(myapp.getAppicon());

                        int W = homescreen.getWidth();
                        int H = homescreen.getHeight();

                        int cursor_x = (int) event.getX();
                        int cursor_y = (int) event.getY();

                        int snap_row = Math.round(cursor_y/(H/6));
                        int snap_col = Math.round(cursor_x/(H/6));

                        Log.d(TAG, "Vinit (W, H) => " + W +", "+ H);
                        Log.d(TAG, "Vinit (X, Y) => " + cursor_x +", "+ cursor_y);
                        Log.d(TAG, "Vinit (snaprow, snapcol) => " + snap_row +", "+ snap_col);

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
                        params.topMargin = snap_row * (H/6);
                        params.leftMargin = snap_col * (H/6);
                        homescreen.addView(appicon, params);

                        // Add label
                        TextView label = new TextView(getBaseContext());
                        label.setText(myapp.getAppname());
                        label.setSingleLine();
                        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                        label.setTextColor(Color.WHITE);
                        RelativeLayout.LayoutParams labelparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        labelparams.topMargin = snap_row * (H/6) + 120;
                        labelparams.leftMargin = snap_col * (H/6);
                        homescreen.addView(label, labelparams);

                        ClipData.Item item = event.getClipData().getItemAt(0);
                        final String dragData = item.getText().toString();
                        final String[] app = dragData.split("~");
                        appicon.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Gets the item containing the dragged data
                                Log.d(TAG, "Dragged data is " + dragData);
                                launchApp(app[1]); // launch app from package name
                            }
                        });

                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        Log.d(TAG, v.toString());
                        Log.d(TAG, "HOME: ACTION_DRAG_ENDED");


                        break;

                    default:
                        break;

                }

                return true;
            }
        });




    }

    @Override
    public void onAppClicked(AppObject appObject, View clickedView) {
        launchApp(appObject.getPackagename());
    }

    private void launchApp(String packagename) {
        Intent in = getPackageManager().getLaunchIntentForPackage(packagename);
        if(in !=null) {
            startActivity(in);
        }
    }

    @Override
    public void onAppLongClicked(final AppObject appObject, View clickedView) {

        popupMenu = new PopupMenu(getBaseContext(), clickedView);
        popupMenu.inflate(R.menu.drawer_popup);
        longclickedview = clickedView;
        myapp = appObject;

        Log.d(TAG, "Long clicked on: " + appObject.getAppname());

        Object menuHelper;
        Class[] argTypes;
        try {
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            menuHelper = fMenuHelper.get(popupMenu);
            argTypes = new Class[]{boolean.class};
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", argTypes).invoke(menuHelper, true);
        } catch (Exception e) {}

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.drawer_popup_id1:
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + appObject.getPackagename()));
                        startActivity(intent);
                        return true;
                    case R.id.drawer_popup_id2:
                        Toast.makeText(getBaseContext(), "Item2", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.drawer_popup_id3:
                        Toast.makeText(getBaseContext(), "Item3", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.drawer_popup_id4:
                        Toast.makeText(getBaseContext(), "Item5", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.drawer_popup_id5:
                        Uri packageURI = Uri.parse("package:" + appObject.getPackagename());
                        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                        startActivity(uninstallIntent);

                        return true;

                    default:
                        return false;
                }
            }
        });
        popupMenu.show();

        // Drag code here inside applongclick
        ClipData.Item item = new ClipData.Item(appObject.getAppname()+"~"+appObject.getPackagename()+"~"+appObject.getAppicon());
        ClipData dragData = new ClipData(
                (CharSequence) clickedView.getTag(),
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                item);
        clickedView.startDrag(dragData,  // the data to be dragged
                new View.DragShadowBuilder(clickedView),  // the drag shadow builder
                null,      // no need to use local data
                0          // flags (not currently used, set to 0)
        );
    }

    // Gesture intercept
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        //Log.d(DEBUG_TAG,"onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        //Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());

        if(event1.getY() - event2.getY() > 200){ // swipe up
            if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

        }
        if(event2.getY() - event1.getY() > 300){ // swipe down
            if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                try {
                    Object sbservice = getSystemService("statusbar");
                    Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                    Method showsb;
                    showsb = statusbarManager.getMethod("expandNotificationsPanel");
                    showsb.invoke(sbservice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        //Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
        if(currentDrawerState == BottomSheetBehavior.STATE_COLLAPSED) {
            selectWidget();
        }
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        //Log.d(DEBUG_TAG, "onScroll: " + event1.toString() + event2.toString());
        hideKeypad();
        try{
            popupMenu.dismiss();
        } catch(Exception e) {

        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        //Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        //Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        //Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        //Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) { // Otherwise the touch and fling won't work together
        super.dispatchTouchEvent(ev);
        return mDetector.onTouchEvent(ev);
    }

    private void getInstalledAppList() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> untreatedApplist = getApplicationContext().getPackageManager().queryIntentActivities(intent, 0);
        for(ResolveInfo untreatedapp : untreatedApplist) {
            String appname = untreatedapp.activityInfo.loadLabel(getPackageManager()).toString();
            String packagename = untreatedapp.activityInfo.packageName;
            Drawable icon = untreatedapp.activityInfo.loadIcon(getPackageManager());
            installedAppList.add(new AppObject(appname, packagename, icon));
        }
        Collections.sort(installedAppList, new Comparator<AppObject>() {
            @Override
            public int compare(AppObject o1, AppObject o2) {
                return o1.getAppname().toLowerCase().compareTo(o2.getAppname().toLowerCase());
            }
        });
    }

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    void selectWidget() {
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new AppWidgetHost(this, 2048);
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent);
        startActivityForResult(pickIntent, 0);
    }
    void addEmptyData(Intent pickIntent) {
        ArrayList customInfo = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList customExtras = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 0) {
                configureWidget(data);
            } else if (requestCode == 5) {
                createWidget(data);
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, 5);
        } else {
            createWidget(data);
        }
    }

    public void createWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        AppWidgetHostView hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);
        ViewPager2 homescreen = findViewById(R.id.pager);

        // specify position of widget
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

        homescreen.addView(hostView, params);
    }

    public class PackageListener extends BroadcastReceiver { // need to fix

        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Broadcast received");
            installedAppList.clear();
            getInstalledAppList(); //refill the installed apps list
            gridAdapter.notifyDataSetChanged();
        }
    }

    private void hideKeypad() {
        try {
            searchbar.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(drawerGridView.getWindowToken(), 0);
        } catch(Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            //currentDrawerState = BottomSheetBehavior.STATE_COLLAPSED;
            unregisterReceiver(mPackageListener);
        } catch (Exception e) {}
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mPackageListener, filter);
    }

    @Override
    public void onBackPressed() {
        if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            //super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }

    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            return new ScreenSlidePageFragment();
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {

        // The drag shadow image, defined as a drawable thing
        private static Drawable shadow;

        // Defines the constructor for myDragShadowBuilder
        MyDragShadowBuilder(View v) {

            // Stores the View parameter passed to myDragShadowBuilder.
            super(v);

            // Creates a draggable image that will fill the Canvas provided by the system.
            shadow = new ColorDrawable(Color.BLUE);
        }

        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            // Defines local variables
            int width, height;

            // Sets the width of the shadow to half the width of the original View
            width = getView().getWidth() / 2;

            // Sets the height of the shadow to half the height of the original View
            height = getView().getHeight() / 2;

            // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
            // Canvas that the system will provide. As a result, the drag shadow will fill the
            // Canvas.
            shadow.setBounds(0, 0, width, height);

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height);

            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(width / 2, height / 2);
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system constructs
        // from the dimensions passed in onProvideShadowMetrics().
        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draws the ColorDrawable in the Canvas passed in from the system.
            shadow.draw(canvas);
        }
    }
}
