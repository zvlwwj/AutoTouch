package com.zou.autotouch;

import android.app.Activity;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


import jackpal.androidterm.TermExec;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private ListView lv_main;
    private Button start,stop;
    private Button btn1;
    private ParcelFileDescriptor mTermFd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
        setListener();
    }

    private void initData() {
        try {
            mTermFd = ParcelFileDescriptor.open(new File("/dev/ptmx"), ParcelFileDescriptor.MODE_READ_WRITE);
            final InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(mTermFd);
            OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(mTermFd);
            initializeSession();
            new Thread(){
                @Override
                public void run() {
                    byte[] buffer = new byte[4096];
                    while (true){
                        try {
                            int read = in.read(buffer);
                            String str = new String(buffer,0,read);
                            Log.i(TAG,"read: "+str);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
            out.write("su\n".getBytes());
            out.flush();
            out.write("getevent\n".getBytes());
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeSession() throws IOException {
        String path = System.getenv("PATH");
        path = checkPath(path);

        String[] env = new String[3];
        env[0] = "TERM=screen";
        env[1] = "PATH=" + path;
        env[2] = "HOME=/data/user/0/jackpal.androidterm/app_HOME";

        int mProcId = createSubprocess("/system/bin/sh -", env);
    }

    private int createSubprocess(String shell, String[] env) throws IOException {
        ArrayList<String> argList = parse(shell);
        String arg0;
        String[] args;

        try {
            arg0 = argList.get(0);
            File file = new File(arg0);
            if (!file.exists()) {
                throw new FileNotFoundException(arg0);
            } else if (!file.canExecute()) {
                throw new FileNotFoundException(arg0);
            }
            args = argList.toArray(new String[1]);
        } catch (Exception e) {
            arg0 = argList.get(0);
            args = argList.toArray(new String[1]);
        }

        return TermExec.createSubprocess(mTermFd, arg0, args, env);
    }
    private ArrayList<String> parse(String cmd) {
        final int PLAIN = 0;
        final int WHITESPACE = 1;
        final int INQUOTE = 2;
        int state = WHITESPACE;
        ArrayList<String> result =  new ArrayList<String>();
        int cmdLen = cmd.length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cmdLen; i++) {
            char c = cmd.charAt(i);
            if (state == PLAIN) {
                if (Character.isWhitespace(c)) {
                    result.add(builder.toString());
                    builder.delete(0,builder.length());
                    state = WHITESPACE;
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    builder.append(c);
                }
            } else if (state == WHITESPACE) {
                if (Character.isWhitespace(c)) {
                    // do nothing
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    state = PLAIN;
                    builder.append(c);
                }
            } else if (state == INQUOTE) {
                if (c == '\\') {
                    if (i + 1 < cmdLen) {
                        i += 1;
                        builder.append(cmd.charAt(i));
                    }
                } else if (c == '"') {
                    state = PLAIN;
                } else {
                    builder.append(c);
                }
            }
        }
        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return result;
    }

    private String checkPath(String path) {
        String[] dirs = path.split(":");
        StringBuilder checkedPath = new StringBuilder(path.length());
        for (String dirname : dirs) {
            File dir = new File(dirname);
            if (dir.isDirectory() && dir.canExecute()) {
                checkedPath.append(dirname);
                checkedPath.append(":");
            }
        }
        return checkedPath.substring(0, checkedPath.length()-1);
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

                Toast.makeText(getApplicationContext(),"btn1 onclick",Toast.LENGTH_SHORT).show();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"stop onclick",Toast.LENGTH_SHORT).show();
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
            }
        });
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

    //开始记录用户数据
    private void recordEvent(){
//        new Thread(){
//            @Override
//            public void run() {
//                try {
//                    Command command = new Command(0,"getevent");
//                    RootTools.getShell(true).add(command);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (TimeoutException e) {
//                    e.printStackTrace();
//                } catch (RootDeniedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }.start();
    }
}
