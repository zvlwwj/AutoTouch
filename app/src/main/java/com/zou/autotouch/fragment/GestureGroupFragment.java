package com.zou.autotouch.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zou.autotouch.R;

/**
 * Created by zou on 2016/12/30.
 */

public class GestureGroupFragment extends Fragment {
    public GestureGroupFragment(){

    }
    public static GestureGroupFragment newInstance(){
        GestureGroupFragment fragment = new GestureGroupFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gesture_group,container,false);
    }
}
