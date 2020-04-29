package com.dragonfire.wildfirelauncher;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
        AppClickListener, AppDragListener, AppLongClickListener, AppActionDownListener {

    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 10;
    private GestureDetectorCompat mDetector;
    private BottomSheetBehavior mBottomSheetBehavior;
    private List<AppObject> installedAppList;
    private List<AppObject> timeSortedApps;
    private AppAdapter gridAdapter;
    private boolean drawerExpanded=false;
    private int currentDrawerState=-1;
    private GridView drawerGridView;
    private EditText searchbar;
    private PackageListener mPackageListener;
    private PopupWindow popupWindow;
    private PopupWindow folderpopupwindow;
    private View bottomSheet;
    private List<HomescreenObject> homescreenObjects;
    private Vibrator vb;
    private boolean homeapplongpressed;
    private boolean sortbyusage = false;

    DisplayMetrics displaymetrics;
    int screenHight, screenWidth;

    private static final int NUM_PAGES = 5;
    private ViewPager2 viewPager;
    String TAG="Wildfire";

    AppObject myapp;
    View longclickedview;
    boolean longclicked;
    HomescreenObject touchedhomescreenobject;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RelativeLayout homescreen = findViewById(R.id.homescreen);

        displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
        vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        viewPager = findViewById(R.id.pager);
        FragmentStateAdapter pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        mDetector = new GestureDetectorCompat(this,this);

        bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable(true);

        drawerGridView = findViewById(R.id.grid);

        installedAppList = new ArrayList<>();
        homescreenObjects = new ArrayList<>();
        timeSortedApps = new ArrayList<>();

        getInstalledAppList(); // fill in the installedAppList

        searchbar = findViewById(R.id.searchbar);
        gridAdapter = new AppAdapter(getApplicationContext(), installedAppList);
        gridAdapter.setmAppClickListener(this);
        gridAdapter.setmAppDragListener(this);
        gridAdapter.setmAppActionDownListener(this);
        gridAdapter.setmAppLongClickListener(this);
        drawerGridView.setAdapter(gridAdapter);

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
                    int alpha = (int) (255*slideOffset);
                    bottomSheet.setBackgroundColor(Color.argb(alpha, 255, 255, 255));
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
                gridAdapter.setmAppActionDownListener(MainActivity.this);

                for(AppObject currentApp : installedAppList) {
                    if(currentApp.getAppname().toLowerCase().contains(s.toString().toLowerCase())) {
                        filteredApps.add(currentApp);
                        gridAdapter.notifyDataSetChanged();
                    }
                }
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
        homescreen.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;

                    case DragEvent.ACTION_DROP:
                        boolean drop_area_empty = true;
                        final List<AppObject> folder = new ArrayList<>();
                        int W = homescreen.getWidth();
                        int H = homescreen.getHeight();
                        int cursor_x = (int) event.getX();
                        int cursor_y = (int) event.getY();
                        final int snap_row = Math.round(cursor_y/(H/6));
                        final int snap_col = Math.round(cursor_x/(W/5));

                        for(final HomescreenObject homescreenObject : homescreenObjects) {
                            if(snap_col == homescreenObject.getX() && snap_row == homescreenObject.getY()) {
                                drop_area_empty=false; // drop area is not empty
                                homescreenObject.getFolder().add(myapp);
                                homescreenObject.setDir(true);

                                homescreen.removeView(homescreenObject.getIcon());
                                homescreen.removeView(homescreenObject.getLabel());
                                final ImageView folderview = new ImageView(getBaseContext());

                                ArrayList<Bitmap> tinyicons = new ArrayList<>();

                                int i=0;
                                for(AppObject ao : homescreenObject.getFolder()) {
                                    if(i>=4)
                                        break;
                                    else {
                                        tinyicons.add(getBitmapFromDrawable(ao.getAppicon()));
                                    }
                                    i++;
                                }
                                folderview.setImageBitmap(generateFolderIcon(tinyicons));
                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
                                params.topMargin = snap_row * (H/6);
                                params.leftMargin = snap_col * (W/5);
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
                                int label_height = label.getMeasuredHeight(); //get height
                                int label_width = label.getMeasuredWidth();  //get width
                                labelparams.topMargin = snap_row * (H/6) + 125;
                                labelparams.leftMargin = snap_col * (W/5) + 60 - (label_width/2);
                                homescreen.addView(label, labelparams);

                                folderview.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if(touchedhomescreenobject!=null) {
                                            folderpopupwindow = setFolderPopup(homescreenObject.getFolder());
                                            folderpopupwindow.showAsDropDown(v);
                                        }

                                    }
                                });

                                // Enable drag on folder icon once added on homescreen
                                folderview.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        switch(event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                touchedhomescreenobject = homescreenObject;
                                        }
                                        return false;
                                    }
                                });

                                break;
                            }
                        }
                        if(drop_area_empty) { // single app drop
                            final ImageView appicon = new ImageView(getBaseContext());
                            appicon.setImageDrawable(myapp.getAppicon());

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, 120); // size of the icons
                            params.topMargin = snap_row * (H/6);
                            params.leftMargin = snap_col * (W/5);
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
                            labelparams.topMargin = snap_row * (H/6) + 125;
                            labelparams.leftMargin = snap_col * (W/5) + 60 - (label_width/2);
                            homescreen.addView(label, labelparams);

                            folder.add(myapp); // wrap single app in a list
                            final HomescreenObject homescreenObject = new HomescreenObject(folder, snap_col, snap_row, false, appicon, label);
                            homescreenObjects.add(homescreenObject);

                            ClipData.Item item = event.getClipData().getItemAt(0);
                            final String dragData = item.getText().toString();
                            final String[] app = dragData.split("~");
                            appicon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(touchedhomescreenobject!=null) {
                                        launchApp(app[1]); // launch app from package name
                                    }

                                }
                            });

                            // Enable drag on icon once added on homescreen
                            appicon.setOnTouchListener(new View.OnTouchListener() {
                                @SuppressLint("ClickableViewAccessibility")
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    switch(event.getAction()) {
                                        case MotionEvent.ACTION_DOWN:
                                            touchedhomescreenobject = new HomescreenObject(folder, snap_col, snap_row, false, appicon, label);
                                            break;
                                        case MotionEvent.ACTION_MOVE:
                                            if(homeapplongpressed) {
                                                ClipData.Item item = new ClipData.Item(myapp.getAppname()+"~"+myapp.getPackagename()+"~"+myapp.getAppicon());
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

                                                for(HomescreenObject hso : homescreenObjects) {
                                                    if(hso.getX() == snap_col && hso.getY() == snap_row) {
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

                        return true;

                    default:
                        return false;

                }
            }
        });

        ImageView changeWallpaper = findViewById(R.id.changewallpaper);
        final Unsplash unsplash = new Unsplash("m7_7e-ldwcFyQ1SkbFJcNNFwE8TkIVWe1itmKvV3yrs");

        final String[] categories = {"nature", "wildlife", "starry sky", "landscapes", "sexy", "monuments",
        "natural history", "abstract", "amoled", "dark", "neon", "sensual", "lighthouse", "astronomy", "high quality",
        "buildings", "Lingerie", "Summer", "airplanes", "moon", "cute", "dogs", "cats",
                "gods", "marvel", "bikini", "sports", "india", "hawaii", "winter", "Christmas", "couple", "love",
                "sweden", "europe", "fashion", "kiss", "romance",
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
                gridAdapter.setmAppActionDownListener(MainActivity.this);

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onAppClicked(AppObject appObject, View clickedView) {
        launchApp(appObject.getPackagename());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onAppLongClicked(AppObject appObject, View clickedView) {
        vb.vibrate(30);
        Bitmap bitmap = getBitmapFromDrawable(myapp.getAppicon());
        Palette p = Palette.from(bitmap).generate();
        String popupBg = String.format("#%06X", (0xFFFFFF & p.getDominantColor(Color.BLACK)));
        //String popupTextColor = String.format("#%06X", (0xFFFFFF & p.getMutedColor(Color.WHITE)));
        popupWindow = setPopupWindow(popupBg);
        popupWindow.showAsDropDown(clickedView);
        longclicked=true;
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
    public void onAppActionDown(AppObject appObject, View clickedView) {
        myapp = appObject;
        longclickedview = clickedView; // keep track of dragged app object

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
                if(peek<screenHight/2)
                    peek=0;
                mBottomSheetBehavior.setPeekHeight(peek);
                if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                    drawerExpanded = false;
            }
        }
        else {
            if(event1.getY() > event2.getY()) { // upward scroll when drawer is not open
                mBottomSheetBehavior.setPeekHeight(peek);
                double ratio = (double)peek/screenHight;
                double alpha = 255 * ratio;
                bottomSheet.setBackgroundColor(Color.argb((int)alpha, 255, 255, 255));

                if(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                    drawerExpanded = true;
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
        //Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) { // Otherwise the touch and fling won't work together
        super.dispatchTouchEvent(ev);
        return mDetector.onTouchEvent(ev);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
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

    @Override
    public void onAppDragged(AppObject appObject, View clickedView) {
        myapp = appObject;

        if(longclicked) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            hideKeypad();
            enableDrag(appObject, longclickedview);
            longclicked = false;
        }

        if(folderpopupwindow!=null && folderpopupwindow.isShowing()) {
            folderpopupwindow.dismiss();
        }
        if(popupWindow!=null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public class PackageListener extends BroadcastReceiver { // need to fix

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onPostResume() {
        super.onPostResume();
        sortAppsByTime(); // fill in the timeSortedApps

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
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            searchbar.setText("");
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
        ScreenSlidePagerAdapter(FragmentActivity fa) {
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

        View view = inflater.inflate(R.layout.folder_popup_layout, null);
        GridView foldergrid = view.findViewById(R.id.foldergrid);
        AppAdapter foldergridadapter = new AppAdapter(getApplicationContext(), applist);
        foldergridadapter.setmAppClickListener(this);
        foldergridadapter.setmAppDragListener(this);
        foldergridadapter.setmAppActionDownListener(this);
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

    @NonNull
    private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
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
        int w = 0, h = 0;
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
            if(i==0) {
                top=0; left=0;
            }
            else if(i==1) {
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

        for(AppObject x : timeSortedApps) {
            Log.d(TAG, x.getAppname() + " : " + x.getUsagetime());
        }

    }

}
