package com.dragonfire.wildfirelauncher;

import android.graphics.Bitmap;

class AppObject {

    private String appname, packagename;
    private Bitmap appicon;
    private long usagetime;
    private int notifcount;

    public AppObject(String appname, String packagename, Bitmap appicon) {
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

    public Bitmap getAppicon() {
        return appicon;
    }

    public void setUsagetime(long usagetime) {
        this.usagetime = usagetime;
    }

    public void setNotifcount(int notifcount) {
        this.notifcount = notifcount;
    }

    public int getNotifcount() {
        return notifcount;
    }

    public long getUsagetime() {
        return usagetime;
    }
}
