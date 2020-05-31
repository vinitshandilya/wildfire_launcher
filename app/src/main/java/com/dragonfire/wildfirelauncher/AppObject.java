package com.dragonfire.wildfirelauncher;

import android.graphics.Bitmap;

class AppObject {

    private String appname, packagename, category;
    private Bitmap appicon;
    private long usagetime;
    private int notifcount;

    AppObject(String appname, String packagename, Bitmap appicon, String category) {
        this.appname = appname;
        this.packagename = packagename;
        this.appicon = appicon;
        this.category = category;
    }

    String getAppname() {
        return appname;
    }

    String getPackagename() {
        return packagename;
    }

    Bitmap getAppicon() {
        return appicon;
    }

    public String getCategory() {
        return category;
    }

    void setUsagetime(long usagetime) {
        this.usagetime = usagetime;
    }

    void setNotifcount(int notifcount) {
        this.notifcount = notifcount;
    }

    int getNotifcount() {
        return notifcount;
    }

    long getUsagetime() {
        return usagetime;
    }
}
