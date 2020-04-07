package com.dragonfire.wildfirelauncher;

import android.graphics.drawable.Drawable;

public class AppObject {

    String appname, packagename;
    Drawable appicon;

    public AppObject(String appname, String packagename, Drawable appicon) {
        this.appname = appname;
        this.packagename = packagename;
        this.appicon = appicon;
    }

    public String getAppname() {
        return appname;
    }

    public String getPackagename() {
        return packagename;
    }

    public Drawable getAppicon() {
        return appicon;
    }
}
