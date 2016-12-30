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

public class GestureRecordedFragment extends Fragment {
    public GestureRecordedFragment(){

    }
    public static GestureRecordedFragment newInstance(){
        GestureRecordedFragment fragment = new GestureRecordedFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gesture_recorded,container,false);
    }
}
