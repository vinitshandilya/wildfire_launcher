package com.dragonfire.wildfirelauncher;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class BlankPage extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private FragmentLoadListener mFragmentLoadListener;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View pageView;

    public BlankPage() {
        // Required empty public constructor
    }

    public void setmFragmentLoadListener(FragmentLoadListener mFragmentLoadListener) {
        this.mFragmentLoadListener = mFragmentLoadListener;
    }

    public static BlankPage newInstance(String param1, String param2) {
        BlankPage fragment = new BlankPage();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        pageView = inflater.inflate(R.layout.fragment_blank_page, container, false);
        TextView tv = pageView.findViewById(R.id.fragtext);
        tv.setText(mParam1 + ", " + mParam2);
        mFragmentLoadListener.onFragmentLoaded(pageView); // callback to mainactivity
        return pageView;
    }

}
