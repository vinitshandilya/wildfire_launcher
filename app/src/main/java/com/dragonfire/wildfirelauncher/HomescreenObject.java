package com.dragonfire.wildfirelauncher;

public class HomescreenObject {
    private AppObject appObject;
    private int x, y;
    private boolean isDir;

    HomescreenObject(AppObject appObject, int x, int y, boolean isDir) {
        this.appObject = appObject;
        this.x = x;
        this.y = y;
        this.isDir = isDir;
    }

    public AppObject getAppObject() {
        return appObject;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    public boolean isDir() {
        return isDir;
    }
}
