package com.zou.autotouch.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.zou.autotouch.R;
import com.zou.autotouch.service.RecordSession;

/**
 * Created by zou on 2016/12/30.
 */

public class GestureRecordedFragment extends Fragment {
    private Button btn_test;
    private View mView;
    RecordSession session;
    public GestureRecordedFragment(){

    }
    public static GestureRecordedFragment newInstance(){
        GestureRecordedFragment fragment = new GestureRecordedFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_gesture_recorded,container,false);
        initView();        
        return mView;
    }

    private void initView() {
        session = new RecordSession();
        btn_test = (Button) mView.findViewById(R.id.btn_test);
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                session.test();
            }
        });
    }
}
