package com.dragonfire.wildfirelauncher;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
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
    private List<AppObject> installedAppList;
    private AppAdapter gridAdapter;
    private boolean drawerExpanded=false;
    private GridView drawerGridView;
    private EditText searchbar;
    private Intent launchApp;
    private PackageListener mPackageListener;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        View bottomSheet = findViewById( R.id.bottom_sheet );
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
        RelativeLayout homescreen = findViewById(R.id.homescreen_layout);
        homescreen.setOnLongClickListener(new View.OnLongClickListener() {
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

    }

    @Override
    public void onAppClicked(AppObject appObject, View clickedView) {
        launchApp = getPackageManager().getLaunchIntentForPackage(appObject.getPackagename());
        if(launchApp!=null) {
            startActivity(launchApp);
        }
    }

    @Override
    public void onAppLongClicked(final AppObject appObject, View clickedView) {
        PopupMenu popupMenu = new PopupMenu(getBaseContext(), clickedView);
        popupMenu.inflate(R.menu.drawer_popup);

        Object menuHelper;
        Class[] argTypes;
        try {
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            menuHelper = fMenuHelper.get(popupMenu);
            argTypes = new Class[]{boolean.class};
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", argTypes).invoke(menuHelper, true);
        } catch (Exception e) {

        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.drawer_popup_id1:
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
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

        if(event1.getY() - event2.getY() > 50){ // swipe up
            if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

        }
        if(event2.getY() - event1.getY() > 50){ // swipe down
            if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                try {
                    Object sbservice = getSystemService("statusbar");
                    Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                    Method showsb;
                    if (Build.VERSION.SDK_INT >= 17) {
                        showsb = statusbarManager.getMethod("expandNotificationsPanel");
                    } else {
                        showsb = statusbarManager.getMethod("expand");
                    }
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
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        //Log.d(DEBUG_TAG, "onScroll: " + event1.toString() + event2.toString());
        hideKeypad();
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
        RelativeLayout homescreen = findViewById(R.id.homescreen_layout);

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

    }
}
