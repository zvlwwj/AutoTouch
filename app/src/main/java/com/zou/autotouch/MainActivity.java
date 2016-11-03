package com.zou.autotouch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(){
            @Override
            public void run() {
                exeCommand("getevent");
            }
        }.start();
    }

    private static boolean exeCommand(String command) {
        boolean ret = false;
        try {
            VirtualTerminal vt;
            vt = new VirtualTerminal("su");
            VirtualTerminal.VTCommandResult r = vt.runCommand(command);
            ret = r.success();
            vt.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }
}
