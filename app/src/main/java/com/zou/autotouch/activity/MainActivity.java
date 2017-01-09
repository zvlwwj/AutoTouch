package com.zou.autotouch.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.zou.autotouch.R;
import com.zou.autotouch.fragment.GestureGroupFragment;
import com.zou.autotouch.fragment.GestureRecordedFragment;
import com.zou.autotouch.fragment.SettingFragment;
import com.zou.autotouch.service.FloatViewService;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


import jackpal.androidterm.TermExec;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ParcelFileDescriptor mTermFd;
    private BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setListener();
        Intent intent = new Intent(MainActivity.this, FloatViewService.class);
        startService(intent);
    }
    private void initView(){
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        Fragment fragment = GestureRecordedFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_content,fragment).setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).commitAllowingStateLoss();
    }
    private void setListener(){
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                switch (item.getItemId()){
                    case R.id.item_gesture_recorded:
                        fragment = GestureRecordedFragment.newInstance();
                        break;
                    case R.id.item_gesture_group:
                        fragment = GestureGroupFragment.newInstance();
                        break;
                    case R.id.item_setting:
                        fragment = SettingFragment.newInstance();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fl_content,fragment).setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).commitAllowingStateLoss();

                return true;
            }
        });
//        start.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getApplicationContext(),"start onclick",Toast.LENGTH_SHORT).show();
//                recordEvent();
//            }
//        });

//        btn1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                /dev/input/event0: 0003 0039 00000d03
//                        /dev/input/event0: 0001 014a 00000001
//                        /dev/input/event0: 0001 0145 00000001
//                        /dev/input/event0: 0003 0035 0000022c
//                        /dev/input/event0: 0003 0036 000001a3
//                        /dev/input/event0: 0003 0030 00000007
//                        /dev/input/event0: 0003 0031 00000006
//                        /dev/input/event0: 0000 0000 00000000
//                        /dev/input/event0: 0003 0039 ffffffff
//                        /dev/input/event0: 0001 014a 00000000
//                        /dev/input/event0: 0001 0145 00000000
//                        /dev/input/event0: 0000 0000 00000000
//                try {
//                    vt.runCommand("sendevent /dev/input/event0 3 57 3331");
//                    vt.runCommand("sendevent /dev/input/event0 1 330 1");
//                    vt.runCommand("sendevent /dev/input/event0 1 325 1");
//                    vt.runCommand("sendevent /dev/input/event0 3 53 556");
//                    vt.runCommand("sendevent /dev/input/event0 3 54 419");
//                    vt.runCommand("sendevent /dev/input/event0 3 48 7");
//                    vt.runCommand("sendevent /dev/input/event0 3 49 6");
//                    vt.runCommand("sendevent /dev/input/event0 0 0 0");
//                    vt.runCommand("sendevent /dev/input/event0 3 57 -1");
//                    vt.runCommand("sendevent /dev/input/event0 1 330 0");
//                    vt.runCommand("sendevent /dev/input/event0 1 325 0");
//                    vt.runCommand("sendevent /dev/input/event0 0 0 0");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

//                Toast.makeText(getApplicationContext(),"btn1 onclick",Toast.LENGTH_SHORT).show();
//            }
//        });

//        stop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getApplicationContext(),"stop onclick",Toast.LENGTH_SHORT).show();
//                String last_input=vt.getLastInputString();
//                String[] last_inputs = last_input.split("\n");
//                for(int i=0;i<last_inputs.length;i++){
//                    if(cmdstrs == null){
//                        cmdstrs = new ArrayList<String>();
//                    }
//                    if(last_inputs[i].startsWith("/dev/input/event")){
//                        String cmdstr = format(last_inputs[i]);
//                        cmdstrs.add(cmdstr);
//                        Log.i(TAG,"cmdstr : "+cmdstr);
//                    }
//                }
//            }
//        });
    }

    private String format(String cmdstr){
        StringBuffer newstr=new StringBuffer();
        cmdstr.replace(":","");
        String[] strs_16 =  cmdstr.split(" ");
        for(int i=1;i<strs_16.length;i++){
            if("ffffffff".equals(strs_16[i])){
                strs_16[i]="-1";
            }else {
                strs_16[i] = Integer.parseInt(strs_16[i], 16) + "";
            }
        }
        newstr.append("sendevent");
        for(int i=0;i<strs_16.length;i++){
            newstr = newstr.append(" "+strs_16[i]);
        }
        return newstr.toString();
    }
}
