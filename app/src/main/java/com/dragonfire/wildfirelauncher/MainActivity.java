package com.dragonfire.wildfirelauncher;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;
import in.srain.cube.views.GridViewWithHeaderAndFooter;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.WallpaperManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kc.unsplash.Unsplash;
import com.kc.unsplash.models.Photo;
import com.kc.unsplash.models.SearchResults;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,
        AppClickListener, AppDragListener, AppLongClickListener, NotificationInterface, FragmentLoadListener {

    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 10;
    private GestureDetectorCompat mDetector;
    private BottomSheetBehavior mBottomSheetBehavior;
    private List<AppObject> installedAppList;
    private List<AppObject> timeSortedApps;
    private AppAdapter gridAdapter;
    private AppAdapter recentappadapter;
    private boolean drawerExpanded=false;
    private int currentDrawerState=-1;
    private GridViewWithHeaderAndFooter drawerGridView;
    private EditText searchbar;
    private PackageListener mPackageListener;
    private PopupWindow popupWindow;
    private PopupWindow folderpopupwindow;
    private View bottomSheet;
    private List<HomescreenObject> homescreenObjects;
    private List<AppObject> first4;
    private Vibrator vb;
    private boolean homeapplongpressed;
    private boolean sortbyusage = false;
    private float edge = 0.0f;
    private List<BlankPage> pages;
    private PagerAdapter pagerAdapter;
    private ViewPager pager;
    private boolean dragentered = false;
    private boolean dropped = false;
    private boolean dockentered = false;
    private RelativeLayout dock;
    private boolean edgetouched = false;

    DisplayMetrics displaymetrics;
    int screenHight, screenWidth;

    AppObject myapp;
    boolean longclicked;
    HomescreenObject touchedhomescreenobject;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
        vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Get launcher dock
        dock = findViewById(R.id.dock);

        // Define view pager
        pager = findViewById(R.id.vpPager);
        pager.setOffscreenPageLimit(5);

        // Define a list of blank pages
        pages = new ArrayList<>();

        BlankPage homepage = BlankPage.newInstance("Home", "Page-1", pages.size());
        homepage.setmFragmentLoadListener(this);
        pages.add(homepage);
        pagerAdapter = new PagerAdapter(getSupportFragmentManager(), 0, pages);
        pager.setAdapter(pagerAdapter);
        //addNewPage(); // Add another page

        mDetector = new GestureDetectorCompat(this,this);

        bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable(true);

        drawerGridView = findViewById(R.id.grid);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View headerview = layoutInflater.inflate(R.layout.drawer_header, null);
        drawerGridView.addHeaderView(headerview);

        new MyNotificationListenerService().setListener(this);

        if(! notificationAccessAllowed()) {
            Toast.makeText(this, "Please grant notification permission", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }

        installedAppList = new ArrayList<>();
        homescreenObjects = new ArrayList<>();
        timeSortedApps = new ArrayList<>();
        first4 = new ArrayList<>();

        LoadInstalledApps appLoader = new LoadInstalledApps();
        appLoader.execute();

        searchbar = findViewById(R.id.searchbar);
        gridAdapter = new AppAdapter(getApplicationContext(), installedAppList);
        gridAdapter.setmAppClickListener(this);
        gridAdapter.setmAppDragListener(this);
        gridAdapter.setmAppLongClickListener(this);
        drawerGridView.setAdapter(gridAdapter);

        recentappadapter = new AppAdapter(getApplicationContext(), first4);
        recentappadapter.setmAppClickListener(MainActivity.this);
        recentappadapter.setmAppDragListener(MainActivity.this);
        recentappadapter.setmAppLongClickListener(MainActivity.this);
        GridView headergrid = headerview.findViewById(R.id.recent_apps_grid_view);
        headergrid.setAdapter(recentappadapter);

        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                currentDrawerState = newState;
                if(newState == BottomSheetBehavior.STATE_HIDDEN && drawerGridView.getChildAt(0).getY()!=0) {
                    //mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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

                    double ratio = Math.abs((screenHight-(double)bottomSheet.getY())/screenHight);
                    double alpha = 255 * ratio;
                    bottomSheet.setBackgroundColor(Color.argb((int)alpha, 255, 255, 255));

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
                gridAdapter.setmAppDragListener(MainActivity.this);

                for(AppObject currentApp : installedAppList) {
                    if(currentApp.getAppname().toLowerCase().contains(s.toString().toLowerCase())) {
                        filteredApps.add(currentApp);
                        gridAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

        // Broadcast receiver when packages are installed or uninstalled.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        mPackageListener = new PackageListener();
        registerReceiver(mPackageListener, filter);

        ImageView changeWallpaper = findViewById(R.id.changewallpaper);
        final Unsplash unsplash = new Unsplash("m7_7e-ldwcFyQ1SkbFJcNNFwE8TkIVWe1itmKvV3yrs");

        final String[] categories = {"nature", "wildlife", "starry sky", "landscapes", "sexy", "monuments",
                "natural history", "abstract", "amoled", "dark", "neon", "sensual", "lighthouse", "astronomy", "high quality",
                "buildings", "Lingerie", "Summer", "airplanes", "moon", "cute", "dogs", "cats",
                "gods", "marvel", "bikini", "sports", "india", "hawaii", "nude", "winter", "Christmas", "couple", "love",
                "sweden", "europe", "fashion", "kiss", "romance", "Europe", "trending", "underwear", "bra", "blonde",
                "funny", "quotes", "humor"};

        changeWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int cat_index = (new Random()).nextInt(categories.length - 1);
                unsplash.searchPhotos(categories[cat_index], 1, 10, Unsplash.ORIENTATION_PORTRAIT, new Unsplash.OnSearchCompleteListener() {
                    @Override
                    public void onComplete(SearchResults results) {
                        List<Photo> photos = results.getResults();
                        int index = (new Random()).nextInt(photos.size()-1);
                        Log.d("unsplash", "category: " + categories[cat_index] + ", photo number: " + index);
                        setWallpaper(photos.get(index).getUrls().getRegular());
                    }

                    @Override
                    public void onError(String error) {
                        Log.d("unsplash", error);
                        Toast.makeText(getBaseContext(), "Couldn't download. Try again", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        ImageView sortbtn = findViewById(R.id.sortbtn);
        sortbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!sortbyusage) {
                    Toast.makeText(MainActivity.this, "sorting by usage", Toast.LENGTH_SHORT).show();
                    gridAdapter = new AppAdapter(getApplicationContext(), timeSortedApps);
                    sortbyusage = true;
                }
                else {
                    Toast.makeText(MainActivity.this, "sorting by A-Z", Toast.LENGTH_SHORT).show();
                    gridAdapter = new AppAdapter(getApplicationContext(), installedAppList);
                    sortbyusage = false;
                }
                drawerGridView.setAdapter(gridAdapter);
                gridAdapter.setmAppClickListener(MainActivity.this);
                gridAdapter.setmAppLongClickListener(MainActivity.this);
                gridAdapter.setmAppDragListener(MainActivity.this);

            }
        });

    }

    @Override
    public void onFragmentLoaded(View fragmentContainer, int index) { // will be called 1 by 1 for each page that will be loaded
        if(fragmentContainer!=null) {
            TextView tv = fragmentContainer.findViewById(R.id.fragtext);
            Log.d("VINIT", "Fragment loaded: " + tv.getText().toString() + " Index: " + index);
            prepareTarget(fragmentContainer, index);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onAppClicked(AppObject appObject, View clickedView) {
        launchApp(appObject.getPackagename());
    }

    @Override
    public void onAppDragged(AppObject appObject, View clickedView, MotionEvent event) {
        myapp = appObject;
        enableDrag(appObject, clickedView); //build shadow, tag dragged data
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        hideKeypad();
        if(popupWindow!=null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

        if(folderpopupwindow!=null && folderpopupwindow.isShowing()) {
            folderpopupwindow.dismiss();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onAppLongClicked(AppObject appObject, View clickedView) {
        myapp = appObject;
        vb.vibrate(15);
        Bitmap bitmap = myapp.getAppicon();
        Palette p = Palette.from(bitmap).generate();
        String popupBg = String.format("#%06X", (0xFFFFFF & p.getDominantColor(Color.BLACK)));
        popupWindow = setPopupWindow(popupBg);
        popupWindow.showAsDropDown(clickedView);
        longclicked=true;

        dock.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        //Log.d("VINIT", "DOCK: ACTION_DRAG_STARTED");
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        //Log.d("VINIT", "DOCK: ACTION_DRAG_ENTERED");
                        dockentered = true;
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        //Log.d("VINIT", "DOCK: ACTION_DRAG_EXITED");
                        dockentered = false;
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        //Log.d("VINIT", "DOCK: ACTION_DRAG_ENDED");
                        dockentered = false;
                        return true;
                    default:
                        return false;
                }
            }
        });

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

        return true;
    }


    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        if(event1.getY() - event2.getY() > 200){ // swipe up
            if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheet.setBackgroundColor(Color.WHITE);
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

        }
        if(event2.getY() - event1.getY() > 200){ // swipe down
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
        if(currentDrawerState == BottomSheetBehavior.STATE_COLLAPSED ||
                currentDrawerState == BottomSheetBehavior.STATE_HIDDEN) {
            vb.vibrate(30);
            if(touchedhomescreenobject!=null) {
                if(touchedhomescreenobject.getFolder().size()==1) { // home screen icon long clicked
                    Toast.makeText(getBaseContext(), touchedhomescreenobject.getFolder().get(0).getAppname() + " long pressed", Toast.LENGTH_SHORT).show();
                    myapp = touchedhomescreenobject.getFolder().get(0);
                    homeapplongpressed = true;
                }
                else { // home screen folder clicked
                    Toast.makeText(getBaseContext(), touchedhomescreenobject.getFolder().size() + " long pressed", Toast.LENGTH_SHORT).show();
                }
                touchedhomescreenobject = null;
            }
            else {
                selectWidget();
            }
        }

        longclicked = true;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        int peek = (int) (screenHight - event2.getY());
        if(drawerExpanded) {
            hideKeypad();
            try {
                popupWindow.dismiss();
            } catch (Exception e) {}
            longclicked = false;

            if(event1.getY() <= event2.getY()) { // down scroll when drawer is expanded
                //swipeup = false;
                if(peek<screenHight/2)
                    peek=0;
                mBottomSheetBehavior.setPeekHeight(peek);
                if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                    drawerExpanded = false;
            }
        }
        else {
            if(event1.getY() > event2.getY()) { // upward scroll when drawer is not open
                //swipeup = true;
                mBottomSheetBehavior.setPeekHeight(peek);
                double ratio = Math.abs((double)mBottomSheetBehavior.getPeekHeight()/screenHight);
                double alpha = 255 * ratio;
                bottomSheet.setBackgroundColor(Color.argb((int)alpha, 255, 255, 255));
                drawerExpanded = mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
                //swipeup = !drawerExpanded;

            }


        }

        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        //Log.d("Agent", "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {

        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) { // Otherwise the touch and fling won't work together
        super.dispatchTouchEvent(ev);
        return mDetector.onTouchEvent(ev);
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
        ViewPager2 homescreen = findViewById(R.id.vpPager);

        // specify position of widget
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

        homescreen.addView(hostView, params);
    }

    public class PackageListener extends BroadcastReceiver { // need to fix

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent!=null) {
                String[] action = Objects.requireNonNull(intent.getAction()).split("\\.");
                String[] packagearray = Objects.requireNonNull(intent.getData()).toString().split(":");
                String actionStr = action[action.length-1];
                String package_name = packagearray[packagearray.length-1];

                if(actionStr.equals("PACKAGE_REMOVED")) {
                    installedAppList.remove(getAppObjectByPackageName(package_name));
                    gridAdapter.notifyDataSetChanged();
                }
                if(actionStr.equals("PACKAGE_ADDED")) {
                    installedAppList.clear();
                    gridAdapter.notifyDataSetChanged();
                    (new LoadInstalledApps()).execute();
                }
            }
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
            //unregisterReceiver(mPackageListener);
        } catch (Exception e) {}
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onPostResume() {
        super.onPostResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_INSTALL);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mPackageListener, filter);
    }

    @Override
    public void onBackPressed() {
        if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            searchbar.setText("");
        }
    }

    private void launchApp(String packagename) {
        Intent in = getPackageManager().getLaunchIntentForPackage(packagename);
        if(in !=null) {
            startActivity(in);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public PopupWindow setPopupWindow(String bgcolor) {
        final PopupWindow popupWindow = new PopupWindow();
        LayoutInflater inflater = (LayoutInflater)
                getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.popup_layout, null);
        view.getBackground().setColorFilter(Color.parseColor(bgcolor), PorterDuff.Mode.SRC_ATOP);

        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int measuredHeight = view.getMeasuredHeight();
        int measuredWidth = view.getMeasuredWidth();

        TextView header = view.findViewById(R.id.popup_header);
        TextView info = view.findViewById(R.id.popup_appinfo);
        TextView edit = view.findViewById(R.id.popup_edit);
        TextView hide = view.findViewById(R.id.popup_hide);
        TextView lock = view.findViewById(R.id.popup_lock);
        TextView uninstall = view.findViewById(R.id.popup_uninstall);

        String textColor;
        double darkness = 1-(0.299*Color.red(Color.parseColor(bgcolor)) + 0.587*Color.green(Color.parseColor(bgcolor)) +
                0.114*Color.blue(Color.parseColor(bgcolor)))/255;
        if(darkness<0.5)
            textColor="#000000";
        else
            textColor="#FFFFFF";

        info.setTextColor(Color.parseColor(textColor));
        edit.setTextColor(Color.parseColor(textColor));
        hide.setTextColor(Color.parseColor(textColor));
        lock.setTextColor(Color.parseColor(textColor));
        uninstall.setTextColor(Color.parseColor(textColor));

        header.setText(myapp.getAppname());

        popupWindow.setFocusable(true);
        popupWindow.setWidth((int) (measuredWidth * 1.15));
        popupWindow.setHeight(measuredHeight);
        popupWindow.setElevation(30.f);
        popupWindow.setClippingEnabled(false);
        popupWindow.setOverlapAnchor(true);
        popupWindow.setTouchable(true);
        popupWindow.setContentView(view);

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + myapp.getPackagename()));
                startActivity(intent);
                popupWindow.dismiss();
            }
        });
        uninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri packageURI = Uri.parse("package:" + myapp.getPackagename());
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                startActivity(uninstallIntent);
                popupWindow.dismiss();
            }
        });
        return popupWindow;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private PopupWindow setFolderPopup(List<AppObject> applist) {
        LayoutInflater inflater = (LayoutInflater)
                getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        assert inflater != null;
        View view = inflater.inflate(R.layout.folder_popup_layout, null);
        GridView foldergrid = view.findViewById(R.id.foldergrid);
        AppAdapter foldergridadapter = new AppAdapter(getApplicationContext(), applist);
        foldergridadapter.setmAppClickListener(this);
        foldergridadapter.setmAppDragListener(this);
        foldergridadapter.setmAppLongClickListener(this);
        foldergrid.setAdapter(foldergridadapter);

        final PopupWindow pw = new PopupWindow();

        view.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int measuredHeight = view.getMeasuredHeight();
        int measuredWidth = view.getMeasuredWidth();

        pw.setFocusable(true);
        pw.setWidth(measuredWidth * 2);
        pw.setHeight(measuredHeight * 2);
        pw.setElevation(30.f);
        pw.setClippingEnabled(false);
        pw.setOverlapAnchor(true);
        pw.setTouchable(true);
        pw.setContentView(view);

        return pw;

    }

    private void setWallpaper(String url) {
        Picasso.with(getBaseContext()).load(url).into(new Target() {
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Log.d("Wallpaper", "Getting ready to get the image");
                Toast.makeText(getBaseContext(), "Downloading wallpaper", Toast.LENGTH_SHORT).show();
                //Here you should place a loading gif in the ImageView
                //while image is being obtained.
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d("Wallpaper", "The image was obtained correctly");
                Toast.makeText(getBaseContext(), "Wallpaper set", Toast.LENGTH_SHORT).show();
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(getBaseContext());
                try {
                    wallpaperManager.setBitmap(bitmap);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.d("Wallpaper", "The image was not obtained");
                Toast.makeText(getBaseContext(), "Download failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableDrag(AppObject ao, View draggedview) {
        ClipData.Item item = new ClipData.Item(ao.getAppname()+"~"+ao.getPackagename()+"~"+ao.getAppicon());
        ClipData dragData = new ClipData(
                (CharSequence) draggedview.getTag(),
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                item);
        draggedview.findViewById(R.id.appicondrawable).startDrag(dragData,  // the data to be dragged
                new View.DragShadowBuilder(draggedview.findViewById(R.id.appicondrawable)),  // the drag shadow builder
                null,      // no need to use local data
                0          // flags (not currently used, set to 0)
        );
    }

    private Bitmap generateFolderIcon(ArrayList<Bitmap> bitmaps) {
        int w, h;
        w = 2 * bitmaps.get(0).getWidth();
        h = 2 * bitmaps.get(0).getHeight();

        Bitmap foldericon = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(foldericon);
        Paint bgpaint = new Paint();
        bgpaint.setColor(Color.TRANSPARENT);
        bgpaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPaint(bgpaint);
        int top = 0;
        int left = 0;
        for (int i = 0; i < bitmaps.size(); i++) {
            if(i==1) {
                top = top+bitmaps.get(i).getHeight();
                left = left+bitmaps.get(i).getWidth();
            }
            else if(i==2) {
                top = 0;
                left = bitmaps.get(i).getWidth();
            }
            else if(i==3) {
                top = top+bitmaps.get(i).getHeight();
                left = 0;
            }

            canvas.drawBitmap(bitmaps.get(i), left, top, null);
        }
        return foldericon;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void sortAppsByTime() {
        timeSortedApps.clear();
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        assert appOps != null;
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());

        if(mode != AppOpsManager.MODE_ALLOWED) {
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
        }

        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        assert mUsageStatsManager != null;
        Map<String, UsageStats> lUsageStatsMap = mUsageStatsManager.
                queryAndAggregateUsageStats(System.currentTimeMillis() - 86400000, System.currentTimeMillis()); // last 24 hours

        for(AppObject ao : installedAppList) {
            try {
                long totalTimeUsageInMillis = Objects.requireNonNull(lUsageStatsMap.get(ao.getPackagename())).getTotalTimeInForeground();
                ao.setUsagetime(totalTimeUsageInMillis);
                timeSortedApps.add(ao);
            }
            catch(Exception e) {
                ao.setUsagetime(0);
            }
        }

        Collections.sort(timeSortedApps, new Comparator<AppObject>() {
            @Override
            public int compare(AppObject o1, AppObject o2) {
                return Long.compare(o2.getUsagetime(), o1.getUsagetime());
            }
        });

    }

    private Bitmap getRoundedBitmapIcon(Drawable drawable) {
        int ICON_SIZE=10;
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        Bitmap output = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int radius = Math.min(bmp.getWidth()/2, bmp.getHeight()/2) - ICON_SIZE;
        float cx = (float)bmp.getWidth()/2;
        float cy = (float)bmp.getHeight()/2;
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return output;
    }

    @Override
    public void onNotificationAdded(Bundle bundle) {
        String package_name = bundle.getString("PACKAGE_NAME", "");
        String title = bundle.getString(Notification.EXTRA_TITLE);
        String text = bundle.getString(Notification.EXTRA_TEXT);
        String ticker = bundle.getString("TICKER_TEXT", "");

        String str = "notif added: package name: " + package_name + " title: " + title + " subtext: " + text + " ticker: " + ticker;
        Log.d("VINIT", str);

        AppObject app = getAppObjectByPackageName(package_name);
        if(app!=null) {
            Log.d("VINIT", app.getAppname() + " notification received");
            app.setNotifcount(app.getNotifcount()+1);
            gridAdapter.notifyDataSetChanged();
            recentappadapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onNotificationRemoved(Bundle bundle) {
        String package_name = bundle.getString("PACKAGE_NAME", "");
        String title = bundle.getString(Notification.EXTRA_TITLE);
        String text = bundle.getString(Notification.EXTRA_TEXT);
        String ticker = bundle.getString("TICKER_TEXT", "");

        String str = "notif removed: package name: " + package_name + " title: " + title + " subtext: " + text + " ticker: " + ticker;
        Log.d("VINIT", str);

        AppObject app = getAppObjectByPackageName(package_name);
        if(app!=null) {
            // adapter will take care of removing the badge if required
            app.setNotifcount(Math.max(app.getNotifcount() - 1, 0));
            gridAdapter.notifyDataSetChanged();
            recentappadapter.notifyDataSetChanged();
        }
    }

    public boolean notificationAccessAllowed() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners.contains(packageName);
    }

    public AppObject getAppObjectByPackageName(String packagename) {
        AppObject app = null;
        for(AppObject ao : installedAppList) {
            if(ao.getPackagename().equals(packagename)) {
                app = ao;
                break;
            }
        }
        return app;
    }

    private class LoadInstalledApps extends AsyncTask<Void, Void, Void> {
        Intent intent;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
        @Override
        protected Void doInBackground(Void... voids) {
            List<ResolveInfo> untreatedApplist = getApplicationContext().getPackageManager().queryIntentActivities(intent, 0);
            for(ResolveInfo untreatedapp : untreatedApplist) {
                String appname = untreatedapp.activityInfo.loadLabel(getPackageManager()).toString();
                String packagename = untreatedapp.activityInfo.packageName;
                Drawable icon = untreatedapp.activityInfo.loadIcon(getPackageManager());
                if(!packagename.equals(getApplicationContext().getPackageName())) {
                    installedAppList.add(new AppObject(appname, packagename, getRoundedBitmapIcon(icon)));
                    //publishProgress();
                }
            }
            Collections.sort(installedAppList, new Comparator<AppObject>() {
                @Override
                public int compare(AppObject o1, AppObject o2) {
                    return o1.getAppname().toLowerCase().compareTo(o2.getAppname().toLowerCase());
                }
            });

            sortAppsByTime(); // fill in the timeSortedApps
            first4.add(timeSortedApps.get(0));
            first4.add(timeSortedApps.get(1));
            first4.add(timeSortedApps.get(2));
            first4.add(timeSortedApps.get(3));

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            gridAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            gridAdapter.notifyDataSetChanged();
            recentappadapter.notifyDataSetChanged();
        }
    }

    private void prepareTarget(final View fragmentContainer, final int currPageindex) {
        final RelativeLayout homescreen = fragmentContainer.findViewById(R.id.fragmentxml);
        homescreen.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, final DragEvent event) {
                dragentered = true;
                dropped = false;

                if(event.getX() == 0.0)
                    edgetouched = true;
                else
                    edgetouched = false;

                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        //Log.d("VINIT", "prepareTarget: " + "ACTION_DRAG_STARTED");
                        return true;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        //Log.d("VINIT", "prepareTarget: " + "ACTION_DRAG_ENTERED");
                        if (event.getX() == 0 && event.getY() == 0 && dragentered && !dropped) { // condition to check finger dragged to screen edge
                            dragentered = false;
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    vb.vibrate(20);
                                    Log.d("VINIT", "X: " + event.getX() + ", Y: " + event.getY() +
                                            ", dragentered: " + dragentered + ", dockentered: " + dockentered + ", dropped: " + dropped);
                                    if(dockentered) {
                                        prepareDock();
                                    }
                                    else {
                                        if(!dropped && edgetouched) {
                                            scrollToNextPage(currPageindex);
                                            edgetouched = false;
                                        }
                                    }
                                }
                            }, 1000);
                        }

                        try {
                            popupWindow.dismiss();
                        } catch (Exception e) {
                        }
                        return true;

                    case DragEvent.ACTION_DROP:
                        //Log.d("VINIT", "prepareTarget: " + "ACTION_DROP");
                        dropped = true;
                        pager.setCurrentItem(currPageindex, false);
                        longclicked = false;
                        boolean drop_area_empty = true;
                        final List<AppObject> folder = new ArrayList<>();
                        int W = homescreen.getWidth();
                        int H = homescreen.getHeight();
                        int cursor_x = (int) event.getX();
                        int cursor_y = (int) event.getY();
                        final int snap_row = Math.round(cursor_y / (H / 6));
                        final int snap_col = Math.round(cursor_x / (W / 5));

                        for (final HomescreenObject homescreenObject : homescreenObjects) {
                            if (snap_col == homescreenObject.getX() && snap_row == homescreenObject.getY() && currPageindex == homescreenObject.getPageNo()) {
                                drop_area_empty = false; // drop area is not empty
                                homescreenObject.getFolder().add(myapp);
                                homescreenObject.setDir(true);

                                homescreen.removeView(homescreenObject.getIcon());
                                homescreen.removeView(homescreenObject.getLabel());
                                final ImageView folderview = new ImageView(getBaseContext());

                                ArrayList<Bitmap> tinyicons = new ArrayList<>();

                                int i = 0;
                                for (AppObject ao : homescreenObject.getFolder()) {
                                    if (i >= 4)
                                        break;
                                    else {
                                        tinyicons.add(ao.getAppicon());
                                    }
                                    i++;
                                }
                                folderview.setImageBitmap(generateFolderIcon(tinyicons));
                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
                                params.topMargin = snap_row * (H / 6);
                                params.leftMargin = snap_col * (W / 5);
                                homescreen.addView(folderview, params);

                                // Add label
                                TextView label = new TextView(getBaseContext());
                                label.setText("New folder");
                                label.setSingleLine();
                                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                                label.setTextColor(Color.WHITE);
                                RelativeLayout.LayoutParams labelparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                                label.measure(0, 0);       //must call measure!
                                int label_width = label.getMeasuredWidth();  //get width
                                labelparams.topMargin = snap_row * (H / 6) + 125;
                                labelparams.leftMargin = snap_col * (W / 5) + 60 - (label_width / 2);
                                homescreen.addView(label, labelparams);

                                folderview.setOnClickListener(new View.OnClickListener() {
                                    @RequiresApi(api = Build.VERSION_CODES.M)
                                    @Override
                                    public void onClick(View v) {
                                        if (touchedhomescreenobject != null) {
                                            folderpopupwindow = setFolderPopup(homescreenObject.getFolder());
                                            folderpopupwindow.showAsDropDown(v);
                                        }

                                    }
                                });

                                // Enable drag on folder icon once added on homescreen
                                folderview.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                touchedhomescreenobject = homescreenObject;
                                        }
                                        return false;
                                    }
                                });

                                break;
                            }
                        }
                        if (drop_area_empty) { // single app drop
                            final ImageView appicon = new ImageView(getBaseContext());
                            appicon.setImageBitmap(myapp.getAppicon());

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
                            params.topMargin = snap_row * (H / 6);
                            params.leftMargin = snap_col * (W / 5);
                            homescreen.addView(appicon, params);

                            // Add label
                            final TextView label = new TextView(getBaseContext());
                            label.setText(myapp.getAppname());
                            label.setSingleLine();
                            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                            label.setTextColor(Color.WHITE);
                            RelativeLayout.LayoutParams labelparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT);
                            label.measure(0, 0);       //must call measure!
                            int label_height = label.getMeasuredHeight(); //get height
                            int label_width = label.getMeasuredWidth();  //get width
                            labelparams.topMargin = snap_row * (H / 6) + 125;
                            labelparams.leftMargin = snap_col * (W / 5) + 60 - (label_width / 2);
                            homescreen.addView(label, labelparams);

                            folder.add(myapp); // wrap single app in a list
                            final HomescreenObject homescreenObject = new HomescreenObject(folder, snap_col, snap_row, false, appicon, label, currPageindex);
                            homescreenObjects.add(homescreenObject);

                            ClipData.Item item = event.getClipData().getItemAt(0);
                            final String dragData = item.getText().toString();
                            final String[] app = dragData.split("~");
                            appicon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (touchedhomescreenobject != null) {
                                        launchApp(app[1]); // launch app from package name
                                    }

                                }
                            });

                            // Enable drag on icon once added on homescreen
                            appicon.setOnTouchListener(new View.OnTouchListener() {
                                @SuppressLint("ClickableViewAccessibility")
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    switch (event.getAction()) {
                                        case MotionEvent.ACTION_DOWN:
                                            touchedhomescreenobject = new HomescreenObject(folder, snap_col, snap_row, false, appicon, label, currPageindex);
                                            break;
                                        case MotionEvent.ACTION_MOVE:
                                            if (homeapplongpressed) {
                                                ClipData.Item item = new ClipData.Item(myapp.getAppname() + "~" + myapp.getPackagename() + "~" + myapp.getAppicon());
                                                ClipData dragData = new ClipData(
                                                        (CharSequence) v.getTag(),
                                                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                                                        item);
                                                v.startDrag(dragData,  // the data to be dragged
                                                        new View.DragShadowBuilder(v),  // the drag shadow builder
                                                        null,      // no need to use local data
                                                        0          // flags (not currently used, set to 0)
                                                );

                                                homescreen.removeView(v);
                                                homescreen.removeView(label);

                                                for (HomescreenObject hso : homescreenObjects) {
                                                    if (hso.getX() == snap_col && hso.getY() == snap_row) {
                                                        homescreenObjects.remove(hso);
                                                        break;
                                                    }
                                                }
                                                homeapplongpressed = false;
                                            }
                                    }
                                    return false;
                                }
                            });
                        }

                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:
                        dropped = true;
                        //Log.d("VINIT", "prepareTarget: " + "ACTION_DRAG_ENDED");
                        return true;

                    default:
                        return false;

                }
            }
        });
    }

    public void scrollToNextPage(int currentindex) {
        dropped = true;
        //newpagereqd = false;
        if(currentindex == pages.size()-1) {
            int pageno = currentindex + 2;
            BlankPage anotherpage = BlankPage.newInstance("New", "Page-" + pageno, currentindex + 1);
            anotherpage.setmFragmentLoadListener(this);
            pages.add(anotherpage);
            pagerAdapter.notifyDataSetChanged();
        }
        pager.setCurrentItem(currentindex + 1, true);




    }

    public void prepareDock() {

        dock.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        //Log.d("VINIT", "prepareDock: " + "ACTION_DRAG_STARTED");
                        return true;

                    case DragEvent.ACTION_DRAG_ENTERED:

                        return true;

                    case DragEvent.ACTION_DROP:
                        //Log.d("VINIT", "prepareDock: " + "ACTION_DROP");
                        longclicked = false;
                        boolean drop_area_empty = true;
                        final List<AppObject> folder = new ArrayList<>();
                        int W = dock.getWidth();
                        int H = dock.getHeight();
                        int cursor_x = (int) event.getX();
                        int cursor_y = (int) event.getY();
                        final int snap_row = Math.round(cursor_y / (H));
                        final int snap_col = Math.round(cursor_x / (W / 5));

                        for (final HomescreenObject homescreenObject : homescreenObjects) {
                            if (snap_col == homescreenObject.getX() && snap_row == homescreenObject.getY()) {
                                drop_area_empty = false; // drop area is not empty
                                homescreenObject.getFolder().add(myapp);
                                homescreenObject.setDir(true);

                                dock.removeView(homescreenObject.getIcon());
                                dock.removeView(homescreenObject.getLabel());
                                final ImageView folderview = new ImageView(getBaseContext());

                                ArrayList<Bitmap> tinyicons = new ArrayList<>();

                                int i = 0;
                                for (AppObject ao : homescreenObject.getFolder()) {
                                    if (i >= 4)
                                        break;
                                    else {
                                        tinyicons.add(ao.getAppicon());
                                    }
                                    i++;
                                }
                                folderview.setImageBitmap(generateFolderIcon(tinyicons));
                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
                                params.topMargin = dock.getHeight()/2 - 60;
                                params.leftMargin = snap_col * (W / 5);
                                dock.addView(folderview, params);

                                // Add label
                                TextView label = new TextView(getBaseContext());
                                label.setText("New folder");
                                label.setSingleLine();
                                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                                label.setTextColor(Color.WHITE);
                                RelativeLayout.LayoutParams labelparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                                label.measure(0, 0);       //must call measure!
                                int label_width = label.getMeasuredWidth();  //get width
                                labelparams.topMargin = dock.getHeight()/2 - 60 + 125;
                                labelparams.leftMargin = snap_col * (W / 5) + 60 - (label_width / 2);
                                dock.addView(label, labelparams);

                                folderview.setOnClickListener(new View.OnClickListener() {
                                    @RequiresApi(api = Build.VERSION_CODES.M)
                                    @Override
                                    public void onClick(View v) {
                                        if (touchedhomescreenobject != null) {
                                            folderpopupwindow = setFolderPopup(homescreenObject.getFolder());
                                            folderpopupwindow.showAsDropDown(v);
                                        }

                                    }
                                });

                                // Enable drag on folder icon once added on homescreen
                                folderview.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                touchedhomescreenobject = homescreenObject;
                                        }
                                        return false;
                                    }
                                });

                                break;
                            }
                        }
                        if (drop_area_empty) { // single app drop
                            final ImageView appicon = new ImageView(getBaseContext());
                            appicon.setImageBitmap(myapp.getAppicon());

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
                            params.topMargin = dock.getHeight()/2 - 60;
                            params.leftMargin = snap_col * (W / 5);
                            dock.addView(appicon, params);

                            // Add label
                            final TextView label = new TextView(getBaseContext());
                            label.setText(myapp.getAppname());
                            label.setSingleLine();
                            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                            label.setTextColor(Color.WHITE);
                            RelativeLayout.LayoutParams labelparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT);
                            label.measure(0, 0);       //must call measure!
                            int label_height = label.getMeasuredHeight(); //get height
                            int label_width = label.getMeasuredWidth();  //get width
                            labelparams.topMargin = dock.getHeight()/2 - 60 + 125;
                            labelparams.leftMargin = snap_col * (W / 5) + 60 - (label_width / 2);
                            dock.addView(label, labelparams);

                            folder.add(myapp); // wrap single app in a list
                            final HomescreenObject homescreenObject = new HomescreenObject(folder, snap_col, snap_row, false, appicon, label, 0);
                            homescreenObjects.add(homescreenObject);

                            ClipData.Item item = event.getClipData().getItemAt(0);
                            final String dragData = item.getText().toString();
                            final String[] app = dragData.split("~");
                            appicon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (touchedhomescreenobject != null) {
                                        launchApp(app[1]); // launch app from package name
                                    }

                                }
                            });

                            // Enable drag on icon once added on homescreen
                            appicon.setOnTouchListener(new View.OnTouchListener() {
                                @SuppressLint("ClickableViewAccessibility")
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    switch (event.getAction()) {
                                        case MotionEvent.ACTION_DOWN:
                                            touchedhomescreenobject = new HomescreenObject(folder, snap_col, snap_row, false, appicon, label, 0);
                                            break;
                                        case MotionEvent.ACTION_MOVE:
                                            if (homeapplongpressed) {
                                                ClipData.Item item = new ClipData.Item(myapp.getAppname() + "~" + myapp.getPackagename() + "~" + myapp.getAppicon());
                                                ClipData dragData = new ClipData(
                                                        (CharSequence) v.getTag(),
                                                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                                                        item);
                                                v.startDrag(dragData,  // the data to be dragged
                                                        new View.DragShadowBuilder(v),  // the drag shadow builder
                                                        null,      // no need to use local data
                                                        0          // flags (not currently used, set to 0)
                                                );

                                                dock.removeView(v);
                                                dock.removeView(label);

                                                for (HomescreenObject hso : homescreenObjects) {
                                                    if (hso.getX() == snap_col && hso.getY() == snap_row) {
                                                        homescreenObjects.remove(hso);
                                                        break;
                                                    }
                                                }
                                                homeapplongpressed = false;
                                            }
                                    }
                                    return false;
                                }
                            });
                        }

                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:
                        dropped = true;
                        dockentered = false;
                        //Log.d("VINIT", "prepareDock: " + "ACTION_DRAG_ENDED");
                        return true;

                    default:
                        return false;



                }
            }
        });

    }

}