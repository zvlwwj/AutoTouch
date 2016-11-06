package com.zou.autotouch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ListView lv_main;
    private Button start,stop;
    private Button btn1;
    private VirtualTerminal vt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
        setListener();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_add:
                Toast.makeText(this,"开始录制脚本",Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initData() {
        try {
            vt = new VirtualTerminal("su");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initView(){
        lv_main = (ListView) findViewById(R.id.lv_main);
        start = (Button) findViewById(R.id.btn_start);
        stop = (Button) findViewById(R.id.btn_stop);
        btn1 = (Button) findViewById(R.id.btn1);
    }
    private void setListener(){
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"start onclick",Toast.LENGTH_SHORT).show();
                recordEvent();
            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"btn1 onclick",Toast.LENGTH_SHORT).show();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vt.shutdown();
                Toast.makeText(getApplicationContext(),"stop onclick",Toast.LENGTH_SHORT).show();
                Log.i(TAG,"input_buffer ; "+vt.getInputString());
            }
        });
    }

    //开始记录用户数据
    private void recordEvent(){
        new Thread(){
            @Override
            public void run() {
                exeCommand("getevent");
            }
        }.start();
    }

    private boolean exeCommand(String command) {
        boolean ret = false;
        try {
            VirtualTerminal.VTCommandResult r = vt.runCommand(command);
            ret = r.success();
            vt.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }
}
