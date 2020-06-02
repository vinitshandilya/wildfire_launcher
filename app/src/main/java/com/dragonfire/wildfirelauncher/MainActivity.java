package com.dragonfire.wildfirelauncher;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.MotionEventCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import in.srain.cube.views.GridViewWithHeaderAndFooter;
import io.github.douglasjunior.androidSimpleTooltip.ArrowDrawable;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;

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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kc.unsplash.Unsplash;
import com.kc.unsplash.models.Photo;
import com.kc.unsplash.models.SearchResults;
import com.rd.PageIndicatorView;
import com.rd.animation.type.AnimationType;
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

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
        AppClickListener, AppDragListener, AppLongClickListener, NotificationInterface, FragmentLoadListener, CategorySelectListener {

    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 10;
    private GestureDetectorCompat mDetector;
    private BottomSheetBehavior mBottomSheetBehavior;
    private List<AppObject> installedAppList;
    private List<AppObject> timeSortedApps;
    private AppAdapter gridAdapter;
    private AppAdapter recentappadapter;
    private boolean drawerExpanded=false;
    private GridViewWithHeaderAndFooter drawerGridView;
    private EditText searchbar;
    private PackageListener mPackageListener;
    private PopupWindow folderpopupwindow;
    private List<HomescreenObject> homescreenObjects;
    private List<AppObject> first4;
    private Vibrator vb;
    private boolean sortbyusage = false;
    private List<BlankPage> pages;
    private PagerAdapter pagerAdapter;
    private ViewPager pager;
    private RelativeLayout dock;
    private boolean widgettouched = false;
    private View touchedwidget;
    private boolean scrolled = false;
    private View leftbar, rightbar;
    private SimpleTooltip tooltip;
    private boolean homeAppLongClicked = false;
    private RelativeLayout homescreen, indicatorlayout;
    private ArrayList<String> appCategories = new ArrayList<>();
    private ArrayList<AppObject> initialHomeApps = new ArrayList<>();
    private CategoryAdapter categoryAdapter;
    private TextView headerTitle;
    private GridView recentappsGridView;

    private float dist_y;
    private float init_y;

    DisplayMetrics displaymetrics;
    int screenHight, screenWidth;

    float dragdist_y = 0;

    AppObject myapp;
    HomescreenObject longClickedHomeApp;
    boolean drawerAppLongClicked;

    static AppWidgetManager mAppWidgetManager;
    static CustomAppWidgetHost mAppWidgetHost;
    static int APPWIDGET_HOST_ID = 222;
    static int REQUEST_PICK_APPWIDGET = 10;
    static int REQUEST_CREATE_APPWIDGET = 11;
    static int appWidgetId;

    final static Unsplash unsplash = new Unsplash("m7_7e-ldwcFyQ1SkbFJcNNFwE8TkIVWe1itmKvV3yrs");

    final String[] categories = {"nature", "wildlife", "starry sky", "landscapes", "sexy", "monuments",
            "natural history", "abstract", "amoled", "dark", "neon", "sensual", "lighthouse", "astronomy", "high quality",
            "buildings", "Lingerie", "Summer", "airplanes", "moon", "cute", "dogs", "cats",
            "gods", "marvel", "bikini", "sports", "india", "hawaii", "nude", "winter", "Christmas", "couple", "love",
            "sweden", "europe", "fashion", "kiss", "romance", "Europe", "trending", "underwear", "bra", "blonde",
            "funny", "quotes", "humor"};

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!notificationAccessAllowed()) {
            Toast.makeText(this, "Please grant notification permission", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }

        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        assert appOps != null;
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        if(mode != AppOpsManager.MODE_ALLOWED) {
            finish();
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
        }

        displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
        vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Get launcher dock
        dock = findViewById(R.id.dock);
        indicatorlayout = findViewById(R.id.indicator_layout);

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

        PageIndicatorView pageIndicatorView = findViewById(R.id.pageIndicatorView);
        // https://github.com/romandanylyk/PageIndicatorView
        pageIndicatorView.setAnimationType(AnimationType.WORM);
        pageIndicatorView.setSelectedColor(Color.WHITE);
        pageIndicatorView.setUnselectedColor(Color.parseColor("#20FFFFFF"));
        pageIndicatorView.setCount(5); // specify total count of indicators
        pageIndicatorView.setSelection(2);

        mDetector = new GestureDetectorCompat(this,this);

        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable(true);

        drawerGridView = findViewById(R.id.grid);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View headerview = layoutInflater.inflate(R.layout.drawer_header, null);
        headerTitle = headerview.findViewById(R.id.header_title);
        drawerGridView.addHeaderView(headerview);

        new MyNotificationListenerService().setListener(this);

        installedAppList = new ArrayList<>();
        homescreenObjects = new ArrayList<>();
        timeSortedApps = new ArrayList<>();
        first4 = new ArrayList<>();

        // Initialize category recyclerView for showing the App category
        RecyclerView category_recyclerView = headerview.findViewById(R.id.category_recyclerView);
        category_recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        category_recyclerView.setLayoutManager(layoutManager);
        category_recyclerView.setNestedScrollingEnabled(false); // Very important or else drawer apps wouldn't scroll
        appCategories.add("All apps");
        categoryAdapter = new CategoryAdapter(this, appCategories);
        categoryAdapter.setmCategorySelectListener(this);
        category_recyclerView.setAdapter(categoryAdapter);

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
        recentappsGridView = headerview.findViewById(R.id.recent_apps_grid_view);
        recentappsGridView.setAdapter(recentappadapter);

        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //currentDrawerState = newState;
                dragdist_y = 0;
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

                    /*homescreen.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            //selectWidget();
                            return true;
                        }
                    });*/
                }

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                if(drawerExpanded) {
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN); // LIGHT STATUS ICONS.

                    double ratio = Math.abs((screenHight-(double)bottomSheet.getY())/screenHight);
                    double alpha = 238 * ratio;
                    //Log.d("COOK", "DOWN SCROLL ALPHA: " + alpha);
                    //bottomSheet.setBackgroundColor(Color.argb((int)alpha, 255, 255, 255));
                    //coordinatorLayout.setBackgroundColor(Color.argb((int)alpha, 255, 255, 255));

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

                if(!s.toString().isEmpty())
                    recentappsGridView.setVisibility(View.GONE);
                else
                    recentappsGridView.setVisibility(View.VISIBLE);
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

        mAppWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        mAppWidgetHost = new CustomAppWidgetHost(getApplicationContext(), APPWIDGET_HOST_ID);
        appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //Log.d("PAGER", "onPageScrolled");
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //Log.d("PAGER", "onPageScrollStateChanged");
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            }
        });

        dock.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:

                        return true;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        //Log.d("COOK", "Dock entered");
                        //prepareDock();
                        prepareTargetArea(dock, 0);
                        return true;

                    case DragEvent.ACTION_DROP:

                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:

                        return true;

                    default:
                        return false;
                }
            }
        });

        final Handler handler = new Handler();

        leftbar = findViewById(R.id.leftbar);
        leftbar.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:

                        return true;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        //Log.d("COOK", "Scroll left");
                        if(!scrolled) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        vb.vibrate(20);
                                        pager.setCurrentItem(pager.getCurrentItem()-1);
                                    }
                                    catch (Exception e) {
                                        //Log.d("COOK", "Can't scroll left anymore");
                                    }
                                    scrolled = true;
                                }
                            }, 300);
                        }

                        return true;

                    case DragEvent.ACTION_DROP:

                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:

                        return true;

                    default:
                        return false;
                }
            }
        });

        rightbar = findViewById(R.id.rightbar);
        rightbar.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:

                        return true;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        //Log.d("COOK", "Scroll right");
                        if(!scrolled) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        vb.vibrate(20);
                                        scrollToNextPage(pager.getCurrentItem());
                                    }
                                    catch (Exception e) {
                                    }
                                    scrolled = true;
                                }
                            }, 300);
                        }
                        return true;

                    case DragEvent.ACTION_DROP:

                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:

                        return true;

                    default:
                        return false;
                }
            }
        });

        leftbar.setVisibility(View.GONE);
        rightbar.setVisibility(View.GONE);


    } // onCreate ends here

    @Override
    public void onFragmentLoaded(View fragmentContainer, int index) { // will be called 1 by 1 for each page that will be loaded
        if(fragmentContainer!=null) {
            homescreen = fragmentContainer.findViewById(R.id.fragmentxml);
            setUpDownGesture(homescreen);
            setUpDownGesture(dock);
            setUpDownGesture(indicatorlayout);
            prepareTargetArea(homescreen, index);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onAppClicked(AppObject appObject, View clickedView) {
        launchApp(appObject.getPackagename());
    }

    @Override
    public void onAppDragged(AppObject appObject, View draggedView, MotionEvent event) {
        myapp = appObject;
        enableDrawerAppDrag(appObject, draggedView); //build shadow, tag dragged data
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        hideKeypad();
        if(tooltip.isShowing()) {
            tooltip.dismiss();
        }

        if(folderpopupwindow!=null && folderpopupwindow.isShowing()) {
            folderpopupwindow.dismiss();
        }
    }

    private void enableDrawerAppDrag(AppObject ao, View draggedview) {
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onAppLongClicked(AppObject appObject, View clickedView) {
        scrolled = false;
        leftbar.setVisibility(View.VISIBLE);
        rightbar.setVisibility(View.VISIBLE);
        myapp = appObject;
        vb.vibrate(15);
        Bitmap bitmap = myapp.getAppicon();
        Palette p = Palette.from(bitmap).generate();
        String popupBg = String.format("#%06X", (0xFFFFFF & p.getDominantColor(Color.BLACK)));

        showAppContextMenu(clickedView, popupBg, false);

        drawerAppLongClicked =true;
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
        init_y = event.getRawY();

        return true;
    }


    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        if(event1.getY() - event2.getY() > 200){ // swipe up
            if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                //bottomSheet.setBackgroundColor(Color.parseColor("#EEFFFFFF"));
                //coordinatorLayout.setBackgroundColor(Color.parseColor("#EEFFFFFF"));
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

        }
        if(event2.getY() - event1.getY() > 200){ // swipe down
            pullDownNotificationTray();

        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        scrolled = false; // or else left pane app drop won't work
        if(mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            vb.vibrate(20);
            if(widgettouched) {
                leftbar.setVisibility(View.VISIBLE);
                rightbar.setVisibility(View.VISIBLE);
                ClipData.Item item = new ClipData.Item("test" + "~" + "test");
                ClipData dragData = new ClipData(
                        (CharSequence) touchedwidget.getTag(),
                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                        item);
                touchedwidget.startDrag(dragData,  // the data to be dragged
                        new View.DragShadowBuilder(touchedwidget),  // the drag shadow builder
                        null,      // no need to use local data
                        0          // flags (not currently used, set to 0)
                );

                View myview = getSupportFragmentManager().getFragments().get(pager.getCurrentItem()).getView();
                assert myview != null;
                RelativeLayout widget_container = myview.findViewById(R.id.fragmentxml);
                widget_container.removeView(touchedwidget);
            }
            else {
                // Either long clicked on empty area
                // or long clicked on the home app
                int W = homescreen.getWidth();
                int H = homescreen.getHeight();
                int cursor_x = (int) event.getX();
                int cursor_y = (int) event.getY();
                final int snap_row = Math.round(cursor_y / (H / 6));
                final int snap_col = Math.round(cursor_x / (W / 5));

                if(getHsoOnHome(snap_row, snap_col, event.getRawY()) == null) {
                    selectWidget();
                }
                else {

                }

            }
        }
    }

    private HomescreenObject getHsoOnHome(int snap_row, int snap_col, float rawy) {
        HomescreenObject hso = null;
        for(HomescreenObject h : homescreenObjects) {
            if(rawy < screenHight-dock.getHeight()) {
                if(h.getX() == snap_col && h.getY() == snap_row && h.getPageNo() != -1) {
                    hso=h;
                    break;
                }
            }
            else {
                if(h.getX() == snap_col && h.getPageNo() == -1) {
                    hso=h;
                    break;
                }
            }
        }
        return hso;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {

        int peek = (int) (screenHight - event2.getY());

        if(drawerExpanded) {
            hideKeypad();
            try {
                tooltip.dismiss();
            } catch (Exception e) {}
            drawerAppLongClicked = false;

            if(event1.getY() <= event2.getY()) { // down scroll when drawer is expanded
                if(peek<screenHight/2)
                    peek=0;
                mBottomSheetBehavior.setPeekHeight(peek);
                if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                    drawerExpanded = false;
            }
        }
        else {
            if(event1.getY() > event2.getY()) { // upward scroll when drawer is not open
                dragdist_y = dragdist_y + distanceY;
                dragdist_y = Math.max(0, dragdist_y);
                //drawerExpanded = mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED; // true or false
                //double ratio = Math.abs((double)mBottomSheetBehavior.getPeekHeight()/screenHight);
                //double alpha = 238 * ratio;
                //Log.d("COOK", "UPSCROLL ALPHA: " + alpha);
                //bottomSheet.setBackgroundColor(Color.argb((int)alpha, 255, 255, 255));
                //coordinatorLayout.setBackgroundColor(Color.argb((int)alpha, 255, 255, 255));
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

    void selectWidget() {
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent);
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    void addEmptyData(Intent pickIntent) {
        ArrayList<AppWidgetProviderInfo> customInfo =
                new ArrayList<AppWidgetProviderInfo>();
        pickIntent.putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
        pickIntent.putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if(requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
                Log.d("COOK", "usage stats permission granted");
            }

            if (requestCode == REQUEST_PICK_APPWIDGET) {
                Log.d("VINIT", "Configure widget");
                configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                Log.d("VINIT", "Create widget");
                createWidget(data);
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId =
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            Log.d("VINIT", "configureWidget: configure not null");
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            Log.d("VINIT", "configureWidget: configure is null");
            createWidget(data);
        }
    }

    public void createWidget(Intent data) {

        if(data != null) {
            Bundle extras = data.getExtras();
            assert extras != null;
            int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

            //Log.d("VINIT", "createWidget: " + appWidgetInfo.label);

            CustomAppWidgetHostView hostView = (CustomAppWidgetHostView) mAppWidgetHost.createView(getApplicationContext(), appWidgetId, appWidgetInfo);
            hostView.setAppWidget(appWidgetId, appWidgetInfo);
            View v = getSupportFragmentManager().getFragments().get(pager.getCurrentItem()).getView();

            hostView.measure(0,0);
            int ww = hostView.getMeasuredWidth();
            int wh = hostView.getMeasuredHeight();
            //Log.d("COOK", "before_width: " + ww + ", before_height: " + wh);

            float scalingFactor = 1.0f; // scale
            hostView.setScaleX(scalingFactor);
            hostView.setScaleY(scalingFactor);

            assert v != null;
            RelativeLayout widget_container = v.findViewById(R.id.fragmentxml);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            widget_container.addView(hostView, params);

            leftbar.setVisibility(View.GONE);
            rightbar.setVisibility(View.GONE);

        }
        else {
            Log.d("VINIT", "createWidget: null data");
        }
    }

    // GestureDetector.OnDoubleTapListener

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {

        unsplash.searchPhotos("wallpapers", 1, 20, Unsplash.ORIENTATION_PORTRAIT, new Unsplash.OnSearchCompleteListener() {
            @Override
            public void onComplete(SearchResults results) {
                List<Photo> photos = results.getResults();
                int index = (new Random()).nextInt(photos.size()-1);
                Log.d("unsplash", "category: " + "wallpapers" + ", photo number: " + index);
                setWallpaper(photos.get(index).getUrls().getRegular());
            }

            @Override
            public void onError(String error) {
                Log.d("unsplash", error);
                Toast.makeText(getBaseContext(), "Couldn't download. Try again", Toast.LENGTH_SHORT).show();
            }
        });

        /*final int cat_index = (new Random()).nextInt(categories.length - 1);
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
        });*/

        /*unsplash.getRandomPhotos(null, true, null, null,
                null, null, Unsplash.ORIENTATION_PORTRAIT, 30, new Unsplash.OnPhotosLoadedListener() {
                    @Override
                    public void onComplete(List<Photo> photos) {
                        for(Photo photo : photos) {
                            Log.d("unsplash:random photos ", photo.getUrls().getRegular());
                        }
                        int index = (new Random()).nextInt(10);
                        setWallpaper(photos.get(index).getUrls().getRegular());
                    }

                    @Override
                    public void onError(String error) {
                        Log.d("unsplash", error);
                    }
                });*/

        /*unsplash.getPhotos(1, 50, Order.LATEST, new Unsplash.OnPhotosLoadedListener() {
            @Override
            public void onComplete(List<Photo> photos) {
                int photoCount = photos.size();
                Log.d("unsplash", photoCount + "");
                for(Photo photo : photos) {
                    Log.d("unsplash", photo.getUrls().getRegular());
                }
                int index = (new Random()).nextInt(photoCount-1);
                setWallpaper(photos.get(index).getUrls().getRegular());
            }

            @Override
            public void onError(String error) {
                Log.v("Error", error);
            }
        });*/

        return false;
    }

    private void setWallpaper(String url) {
        Picasso.with(getBaseContext()).load(url).into(new Target() {
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Log.d("unsplash", "Getting ready to get the image");
                Toast.makeText(getBaseContext(), "Downloading wallpaper", Toast.LENGTH_SHORT).show();
                //Here you should place a loading gif in the ImageView
                //while image is being obtained.
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d("unsplash", "The image was obtained correctly");
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

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public void onAppCategorySelected(String category) {
        vb.vibrate(20);
        List<AppObject> filteredApps = new ArrayList<>();
        gridAdapter = new AppAdapter(getBaseContext(), filteredApps);
        drawerGridView.setAdapter(gridAdapter);
        gridAdapter.setmAppClickListener(MainActivity.this);
        gridAdapter.setmAppLongClickListener(MainActivity.this);
        gridAdapter.setmAppDragListener(MainActivity.this);

        if(!category.equals("All apps")) {
            recentappsGridView.setVisibility(View.GONE);
            for(AppObject currentApp : installedAppList) {
                if(currentApp.getCategory().equals(category)) {
                    filteredApps.add(currentApp);
                    gridAdapter.notifyDataSetChanged();
                }
            }
        }
        else {
            recentappsGridView.setVisibility(View.VISIBLE);
            filteredApps.addAll(installedAppList);
            gridAdapter.notifyDataSetChanged();
        }

        headerTitle.setText(category + " (" + filteredApps.size() + ")");
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

        /*ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f); // Grayscale bitmaps*/

        /*float[] colorMatrix_Negative = {
                -1.0f, 0, 0, 0, 255, //red
                0, -1.0f, 0, 0, 255, //green
                0, 0, -1.0f, 0, 255, //blue
                0, 0, 0, 1.0f, 0 //alpha
                 };
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(colorMatrix_Negative);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(f);*/ // Inverted colors

        //Palette p = Palette.from(output).generate();
        //String dominant = String.format("#%06X", (0xFFFFFF & p.getDominantColor(Color.BLACK)));

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
            PackageManager packageManager = getApplicationContext().getPackageManager();
            List<ResolveInfo> untreatedApplist = packageManager.queryIntentActivities(intent, 0);
            for(ResolveInfo untreatedapp : untreatedApplist) {
                String appname = untreatedapp.activityInfo.loadLabel(getPackageManager()).toString();
                String packagename = untreatedapp.activityInfo.packageName;
                String categoryTitle = "Others";
                try {
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packagename, 0);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        int appCategory = applicationInfo.category;
                        categoryTitle = (String) ApplicationInfo.getCategoryTitle(getApplicationContext(), appCategory);
                        if(categoryTitle == null)
                            categoryTitle = "Others";
                    }

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    categoryTitle = "Others";
                }

                if(!appCategories.contains(categoryTitle))
                    appCategories.add(categoryTitle);

                Log.d("CHIKU", "Package: " + packagename + ", Title: " + appname + ", Category: " + categoryTitle);


                Drawable icon = untreatedapp.activityInfo.loadIcon(getPackageManager());
                if(!packagename.equals(getApplicationContext().getPackageName())) {
                    AppObject ao = new AppObject(appname, packagename, getRoundedBitmapIcon(icon), categoryTitle);
                    installedAppList.add(ao);

                    if(packagename.toLowerCase().contains("phone") ||
                            packagename.toLowerCase().contains("message") ||
                            packagename.toLowerCase().contains("browser") ||
                            packagename.toLowerCase().contains("whatsapp") ||
                            packagename.toLowerCase().contains("camera") ||
                            packagename.toLowerCase().contains("chrome")) {
                        initialHomeApps.add(ao);
                    }

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
            //gridAdapter.notifyDataSetChanged();
            //categoryAdapter.notifyDataSetChanged();

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            gridAdapter.notifyDataSetChanged();
            recentappadapter.notifyDataSetChanged();
            categoryAdapter.notifyDataSetChanged();

            for(int i=0; i< Math.min(initialHomeApps.size(), 5); i++) {
                List<AppObject> folder = new ArrayList<>();
                folder.add(initialHomeApps.get(i));
                ImageView appiconview = new ImageView(getBaseContext());
                appiconview.setImageBitmap(initialHomeApps.get(i).getAppicon());
                TextView label = new TextView(getBaseContext());
                label.setText(initialHomeApps.get(i).getAppname());
                label.setSingleLine();
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                label.setTextColor(Color.WHITE);
                HomescreenObject hso = new HomescreenObject(folder, i, 0, false, appiconview, label, -1);
                addToHomeScreen(hso, dock, dock.getWidth(), dock.getHeight());
            }
        }
    }

    private void prepareTargetArea(final RelativeLayout targetLayout, final int currPageindex) {

        targetLayout.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, final DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        try {
                            tooltip.dismiss();
                        } catch (Exception e) {
                        }
                        return true;
                    case DragEvent.ACTION_DROP:
                        // Dropped item can be an AppObject, or a Widget
                        leftbar.setVisibility(View.GONE);
                        rightbar.setVisibility(View.GONE);

                        if(widgettouched && touchedwidget!=null) { // widget is dropped on homescreen
                            RelativeLayout widget_container = v.findViewById(R.id.fragmentxml);
                            touchedwidget.measure(0, 0);
                            int ww = touchedwidget.getMeasuredWidth();
                            widget_container.removeView(touchedwidget);

                            int W = targetLayout.getWidth();
                            int H = targetLayout.getHeight();
                            int cursor_x = (int) event.getX();
                            int cursor_y = (int) event.getY();
                            final int snap_row = Math.round(cursor_y / (H / 6));
                            final int snap_col = Math.round(cursor_x / (W / 5));

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT); // size of the widget
                            params.topMargin = snap_row * (H / 6);
                            params.leftMargin = snap_col * (W / 5) + (W/10) - (ww/2);

                            widget_container.addView(touchedwidget, params);
                            widgettouched = false;
                        }

                        else {
                            final List<AppObject> appgroup = new ArrayList<>();
                            int W = targetLayout.getWidth();
                            int H = targetLayout.getHeight();
                            int cursor_x = (int) event.getX();
                            int cursor_y = (int) event.getY();
                            final int snap_row = Math.round(cursor_y / (H / 6));
                            final int snap_col = Math.round(cursor_x / (W / 5));

                            if(drawerAppLongClicked) { // app dragged from drawer, drop on homescreen
                                //Log.d("CHIKU", "Drawer app long clicked: " + myapp.getAppname());
                                drawerAppLongClicked = false;
                                appgroup.add(myapp);
                                ImageView appiconview = new ImageView(getBaseContext());
                                appiconview.setImageBitmap(myapp.getAppicon());
                                TextView label = new TextView(getBaseContext());
                                label.setText(myapp.getAppname());
                                label.setSingleLine();
                                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                                label.setTextColor(Color.WHITE);
                                int pageno=-1;
                                if(targetLayout.getId() != dock.getId())
                                    pageno = currPageindex;
                                HomescreenObject hso = new HomescreenObject(appgroup, snap_col, snap_row, false, appiconview, label, pageno);
                                addToHomeScreen(hso, targetLayout, W, H);
                            }

                            else if(homeAppLongClicked) { // dragged hso already on homescreen, drop on homescreen
                                //Log.d("CHIKU", "Home app long clicked: " + longClickedHomeApp.getFolder().get(0).getAppname());
                                //Log.d("COOK", "Home app long clicked");
                                longClickedHomeApp.setY(snap_row);
                                longClickedHomeApp.setX(snap_col);
                                if(targetLayout.getId() == dock.getId()) {
                                    longClickedHomeApp.setPageNo(-1);
                                }
                                else {
                                    longClickedHomeApp.setPageNo(currPageindex);
                                }
                                addToHomeScreen(longClickedHomeApp, targetLayout, W, H);
                                homeAppLongClicked = false;
                            }
                        }

                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:
                        widgettouched = false;
                        //homeapplongpressed = false;
                        return true;

                    default:
                        return false;

                }
            }
        });
    }

    private void addToHomeScreen(HomescreenObject hso, final RelativeLayout targetLayout, int W, int H) {
        // Check if the object is not dropped on an existing item
        // if yes, then merge the dropped app with the existing one
        // and update the homescreenObjects list with the newly merged object

        for(HomescreenObject homeobject : homescreenObjects) {
            if(targetLayout.getId() == dock.getId()) { // don't check vertical alignment of dock apps
                if (hso.getX() == homeobject.getX() && hso.getPageNo() == homeobject.getPageNo()) {
                    hso = mergeHomeApps(hso, homeobject);
                    targetLayout.removeView(homeobject.getIconView());
                    targetLayout.removeView(homeobject.getLabel());
                    homescreenObjects.remove(homeobject);
                    break;
                }
            }
            else { // also check the vertical alignment of homescreen apps
                if (hso.getX() == homeobject.getX() && hso.getY() == homeobject.getY() && hso.getPageNo() == homeobject.getPageNo()) {
                    hso = mergeHomeApps(hso, homeobject);
                    targetLayout.removeView(homeobject.getIconView());
                    targetLayout.removeView(homeobject.getLabel());
                    homescreenObjects.remove(homeobject);
                    break;
                }
            }

        }

        // Add icon to targetlayout
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
        params.topMargin = hso.getY() * (H / 6);

        if(targetLayout.getId() == dock.getId())
            params.topMargin = dock.getHeight()/2 - 60;

        params.leftMargin = hso.getX() * (W / 5) + (W/10) - 60;
        targetLayout.addView(hso.getIconView(), params);

        // Add label to targetlayout
        RelativeLayout.LayoutParams labelparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        hso.getLabel().measure(0, 0);
        int label_width = hso.getLabel().getMeasuredWidth();  //get width
        labelparams.topMargin = hso.getY() * (H / 6) + 125;

        if(targetLayout.getId() == dock.getId())
            labelparams.topMargin = dock.getHeight()/2 - 60 + 125;

        labelparams.leftMargin = hso.getX() * (W / 5) + (W/10) - (label_width/2);
        targetLayout.addView(hso.getLabel(), labelparams);
        homescreenObjects.add(hso);

        final HomescreenObject clickedHso = hso;

        //------------ Attach click listener on added homescreenobject ------------//

        clickedHso.getIconView().setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M) // required for setFolderPopup
            @Override
            public void onClick(View v) {
                if(clickedHso.isDir()) {
                    folderpopupwindow = setFolderPopup(clickedHso.getFolder());
                    folderpopupwindow.showAsDropDown(v);
                }
                else {
                    launchApp(clickedHso.getFolder().get(0).getPackagename());
                }

            }
        });

        //------------ Attach longclick listener on added homescreenobject ------------//

        clickedHso.getIconView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //show context menu
                //make home apps draggable
                homeAppLongClicked = true; // very important
                longClickedHomeApp = clickedHso; // very important
                scrolled = false; // or else left pane app drop won't work
                String popupBg = "#FFFFFF";
                if(!clickedHso.isDir()) {
                    myapp = clickedHso.getFolder().get(0);
                    Bitmap bitmap = myapp.getAppicon();
                    Palette p = Palette.from(bitmap).generate();
                    popupBg = String.format("#%06X", (0xFFFFFF & p.getDominantColor(Color.BLACK)));
                }
                if(targetLayout == dock) {
                    showAppContextMenu(clickedHso.getIconView(), popupBg, true);
                }
                else {
                    showAppContextMenu(clickedHso.getIconView(), popupBg, false);
                }


                clickedHso.getIconView().setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch(event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                break;
                            case MotionEvent.ACTION_MOVE:
                                tooltip.dismiss();
                                leftbar.setVisibility(View.VISIBLE);
                                rightbar.setVisibility(View.VISIBLE);

                                ClipData.Item item = new ClipData.Item(clickedHso.getLabel()+"~"+clickedHso.getIconView());
                                ClipData dragData = new ClipData(
                                        (CharSequence) clickedHso.getIconView().getTag(),
                                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                                        item);
                                clickedHso.getIconView().startDrag(dragData,  // the data to be dragged
                                        new View.DragShadowBuilder(clickedHso.getIconView()),  // the drag shadow builder
                                        null,      // no need to use local data
                                        0          // flags (not currently used, set to 0)
                                );

                                targetLayout.removeView(clickedHso.getIconView());
                                targetLayout.removeView(clickedHso.getLabel());

                                for (HomescreenObject hso : homescreenObjects) {
                                    if (hso.getX() == clickedHso.getX() && hso.getY() == clickedHso.getY() && hso.getPageNo() == clickedHso.getPageNo()) {
                                        homescreenObjects.remove(hso);
                                        break;
                                    }
                                }
                                break;

                            case MotionEvent.ACTION_CANCEL:
                                break;

                        }
                        return false;
                    }
                });

                return false;
            }
        });

        for(HomescreenObject h : homescreenObjects) {
            Log.d("CHIKU", ((TextView)h.getLabel()).getText().toString() + ", X: " + h.getX() + ", Y: " + h.getY() + ", pageNo: " + h.getPageNo());
            if(h.isDir()) {
                for(AppObject ao : h.getFolder()) {
                    Log.d("CHIKU", "\t|_" + ao.getAppname());
                }
            }
        }
    }

    public void scrollToNextPage(int currentindex) {
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

    private class CustomAppWidgetHost extends AppWidgetHost {

        CustomAppWidgetHost(Context context, int hostId) {
            super(context, hostId);
        }

        @Override
        protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
            // pass back our custom AppWidgetHostView
            return new CustomAppWidgetHostView(context);
        }
    }

    private class CustomAppWidgetHostView extends AppWidgetHostView {

        private OnLongClickListener longClick;
        private long down;

        public CustomAppWidgetHostView(Context context) {
            super(context);
        }

        public CustomAppWidgetHostView(Context context, int animationIn, int animationOut) {
            super(context, animationIn, animationOut);
        }

        @Override
        public void setOnLongClickListener(OnLongClickListener l) {
            this.longClick = l;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            switch(MotionEventCompat.getActionMasked( ev ) ) {

                case MotionEvent.ACTION_DOWN:
                    widgettouched = true;
                    touchedwidget = CustomAppWidgetHostView.this;
                    down = System.currentTimeMillis();
                    break;

                case MotionEvent.ACTION_MOVE:
                    boolean upVal = System.currentTimeMillis() - down > 300L;
                    if( upVal ) {
                        longClick.onLongClick( CustomAppWidgetHostView.this );
                    }
                    break;
            }
            return false;
        }
    }

    private void showAppContextMenu(final View anchorView, String bgcolor, boolean showindock) {

        int ycoord = anchorView.getTop();
        LayoutInflater inflater = (LayoutInflater)
                getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View popupView = inflater.inflate(R.layout.app_context_menu_layout, null);

        popupView.measure(0,0);
        int popupheight = popupView.getMeasuredHeight();
        int gravity = Gravity.TOP;
        int arrowDir = ArrowDrawable.BOTTOM;

        if(ycoord <= popupheight && !showindock) {
            gravity = Gravity.BOTTOM;
            arrowDir = ArrowDrawable.TOP;
        }

        tooltip = new SimpleTooltip.Builder(this)
                .anchorView(anchorView)
                .gravity(gravity)
                .dismissOnOutsideTouch(true)
                .dismissOnInsideTouch(false)
                .modal(true)
                .arrowDirection(arrowDir)
                .arrowColor(Color.parseColor(bgcolor))
                //.animated(true)
                .margin(0.0f)
                .overlayOffset(10.0f)
                .arrowHeight(20.0f)
                .arrowWidth(20.0f)
                .contentView(R.layout.app_context_menu_layout)
                .focusable(true)
                .build();

        LinearLayout popuplayout = tooltip.findViewById(R.id.popupcontainer);
        GradientDrawable drawable =  new GradientDrawable();
        drawable.setCornerRadius(30);
        drawable.setColor(Color.parseColor(bgcolor));
        popuplayout.setBackground(drawable);

        final TextView appinfo = tooltip.findViewById(R.id.popup_appinfo);
        final TextView edit = tooltip.findViewById(R.id.popup_edit);
        final TextView hide = tooltip.findViewById(R.id.popup_hide);
        final TextView lock = tooltip.findViewById(R.id.popup_lock);
        final TextView uninstall = tooltip.findViewById(R.id.popup_uninstall);

        String textColor;
        double darkness = 1-(0.299*Color.red(Color.parseColor(bgcolor)) + 0.587*Color.green(Color.parseColor(bgcolor)) +
                0.114*Color.blue(Color.parseColor(bgcolor)))/255;
        if(darkness<0.5)
            textColor="#000000";
        else
            textColor="#FFFFFF";

        appinfo.setTextColor(Color.parseColor(textColor));
        edit.setTextColor(Color.parseColor(textColor));
        hide.setTextColor(Color.parseColor(textColor));
        lock.setTextColor(Color.parseColor(textColor));
        uninstall.setTextColor(Color.parseColor(textColor));

        appinfo.findViewById(R.id.popup_appinfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v2) {
                if (tooltip.isShowing()) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + myapp.getPackagename()));
                    startActivity(intent);
                    tooltip.dismiss();
                }
            }
        });

        uninstall.findViewById(R.id.popup_uninstall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v2) {
                if (tooltip.isShowing()) {
                    Uri packageURI = Uri.parse("package:" + myapp.getPackagename());
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                    startActivity(uninstallIntent);
                    tooltip.dismiss();
                }
            }
        });

        tooltip.show();
    }

    private HomescreenObject mergeHomeApps(HomescreenObject hso1, HomescreenObject hso2) {
        List<AppObject> mergedFolder = hso2.getFolder();
        mergedFolder.addAll(hso1.getFolder());

        final ImageView folderImageView = new ImageView(getBaseContext());
        ArrayList<Bitmap> tinyicons = new ArrayList<>();
        int i = 0;
        for (AppObject ao : mergedFolder) {
            if (i >= 4)
                break;
            else {
                tinyicons.add(ao.getAppicon());
            }
            i++;
        }
        folderImageView.setImageBitmap(generateFolderIcon(tinyicons));

        TextView folderLabel = new TextView(getBaseContext());
        folderLabel.setText("Untitled");
        folderLabel.setSingleLine();
        folderLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        folderLabel.setTextColor(Color.WHITE);
        return new HomescreenObject(mergedFolder, hso1.getX(), hso1.getY(), true, folderImageView, folderLabel, hso1.getPageNo());
    }

    private void setUpDownGesture(RelativeLayout view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dist_y = 0;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        //finger_lifted = false;
                        dist_y = screenHight - event.getRawY();
                        dist_y = Math.max(dist_y, 0);
                        if(init_y > event.getRawY()) { // scroll up
                            mBottomSheetBehavior.setPeekHeight((int)dragdist_y);
                        }
                        else { //scroll down
                            //Log.d("COOK", "ACTION_MOVE_DOWN: " + dist_y);
                            if(dist_y > 200){ // swipe down
                                pullDownNotificationTray();
                            }
                        }
                        drawerExpanded = mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED; // true or false

                        return true;

                    case MotionEvent.ACTION_UP:
                        dragdist_y = 0;

                        if(dist_y >= screenHight/2) { // expand the drawer
                            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // DARK STATUS ICONS.

                        }
                        else { // collapse the drawer
                            //mBottomSheetBehavior.setPeekHeight(0, true);
                            //mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                            getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN); // LIGHT STATUS ICONS.
                        }

                        drawerExpanded = mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED; // true or false



                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        mBottomSheetBehavior.setPeekHeight(0, true);
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        drawerExpanded = mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED; // true or false
                        return true;
                }
                return true;
            }
        });
    }

    public void pullDownNotificationTray() {
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

}