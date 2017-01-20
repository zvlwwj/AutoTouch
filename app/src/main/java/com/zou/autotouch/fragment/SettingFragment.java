package com.zou.autotouch.fragment;

import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.zou.autotouch.R;


/**
 * Created by zou on 2017/1/5.
 */

public class SettingFragment extends Fragment {
    private BroadcastReceiver bluetoothReceive;
    public SettingFragment(){

    }
    public static SettingFragment newInstance(){
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        init();
        return inflater.inflate(R.layout.fragment_setting,container,false);
    }

    private void init() {

    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(bluetoothReceive);
        super.onDestroy();
    }
}
