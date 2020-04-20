package com.dragonfire.wildfirelauncher;

import android.view.View;

import java.util.List;

public class HomescreenObject {
    private List<AppObject> folder;
    private int x, y;
    private boolean isDir;
    private View icon;
    private View label;

    public HomescreenObject(List<AppObject> folder, int x, int y, boolean isDir, View icon, View label) {
        this.folder = folder;
        this.x = x;
        this.y = y;
        this.isDir = isDir;
        this.icon = icon;
        this.label = label;
    }

    public List<AppObject> getFolder() {
        return folder;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isDir() {
        return isDir;
    }

    public View getIcon() {
        return icon;
    }

    public View getLabel() {
        return label;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }
}
