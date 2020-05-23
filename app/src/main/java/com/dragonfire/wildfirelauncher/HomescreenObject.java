package com.dragonfire.wildfirelauncher;

import android.view.View;

import java.util.List;

public class HomescreenObject {
    private List<AppObject> folder;
    private int x, y;
    private boolean isDir;
    private View icon;
    private View label;
    private int pageNo;

    public HomescreenObject(List<AppObject> folder, int x, int y, boolean isDir, View icon, View label, int pageNo) {
        this.folder = folder;
        this.x = x;
        this.y = y;
        this.isDir = isDir;
        this.icon = icon;
        this.label = label;
        this.pageNo = pageNo;
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

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setIcon(View icon) {
        this.icon = icon;
    }

    public void setLabel(View label) {
        this.label = label;
    }

    public int getPageNo() {
        return pageNo;
    }
}
