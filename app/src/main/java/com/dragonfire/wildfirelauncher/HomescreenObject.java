package com.dragonfire.wildfirelauncher;
import android.view.View;

import java.util.List;

class HomescreenObject {
    private List<AppObject> folder;
    private int x, y;
    private boolean isDir;
    private View icon;
    private View label;
    private int pageNo;

    HomescreenObject(List<AppObject> folder, int x, int y, boolean isDir, View icon, View label, int pageNo) {
        this.folder = folder;
        this.x = x;
        this.y = y;
        this.isDir = isDir;
        this.icon = icon;
        this.label = label;
        this.pageNo = pageNo;
    }

    List<AppObject> getFolder() {
        return folder;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    boolean isDir() {
        return isDir;
    }

    View getIconView() {
        return icon;
    }

    View getLabel() {
        return label;
    }

    void setX(int x) {
        this.x = x;
    }

    void setY(int y) {
        this.y = y;
    }

    int getPageNo() {
        return pageNo;
    }

}
