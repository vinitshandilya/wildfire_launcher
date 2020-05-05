package com.dragonfire.wildfirelauncher;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class PagerAdapter extends FragmentPagerAdapter {
    private List<BlankPage> pages;

    PagerAdapter(@NonNull FragmentManager fm, int behavior, List<BlankPage> pages) {
        super(fm, behavior);
        this.pages = pages;

    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return pages.get(position);
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    public BlankPage getPageAt(int position) {
        return pages.get(position);
    }


}
