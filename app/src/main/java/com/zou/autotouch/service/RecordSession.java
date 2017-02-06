package com.zou.autotouch.service;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.TimeoutException;

import jackpal.androidterm.TermExec;
import rootshell.exceptions.RootDeniedException;
import rootshell.execution.Command;
import roottools.RootTools;

/**
 * Created by zou on 2017/1/9.
 */

public class RecordSession {
    private static final String TAG = "RecordSession";
    private ParcelFileDescriptor mTermFd;
    private InputStream in;
    private OutputStream out;
//    private OutputStream out_play;
    private boolean stop;
    private StringBuffer stringBuffer;
    private ArrayList<String> cmdstrs;
    private ArrayList<Integer> EventEndCounts;
    private ArrayList<StringBuffer> events;
    public RecordSession(){
        try {
            initializeSession();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRecord(){
        try {
            new Thread(){
                @Override
                public void run() {
                    byte[] buffer = new byte[4096];
                    while (true){
                        try {
                            if(!stop) {
                                int read = in.read(buffer);
                                String str = new String(buffer, 0, read);
                                if(stringBuffer == null){
                                    stringBuffer = new StringBuffer();
                                }
                                stringBuffer.append(str);
                                Log.i(TAG, "read: " + str);
                            }else{
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
            out.write("getevent\n".getBytes());
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeSession() throws IOException {
        stop =false;
        mTermFd = ParcelFileDescriptor.open(new File("/dev/ptmx"), ParcelFileDescriptor.MODE_READ_WRITE);
        in = new ParcelFileDescriptor.AutoCloseInputStream(mTermFd);
        out = new ParcelFileDescriptor.AutoCloseOutputStream(mTermFd);
        out.write("su\n".getBytes());
        out.flush();
//        Process process = Runtime.getRuntime().exec("su");
//        out_play = process.getOutputStream();

        String path = System.getenv("PATH");
        path = checkPath(path);
        String[] env = new String[3];
        env[0] = "TERM=screen";
        env[1] = "PATH=" + path;
        env[2] = "HOME=/data/user/0/jackpal.androidterm/app_HOME";

        int mProcId = createSubprocess("/system/bin/sh -", env);
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

    public void stopRecord(){
        try {
            stop = true;
            mTermFd.close();
            in.close();
            out.close();
            String[] inputs = stringBuffer.toString().split("\n");

            for(int i=0;i<inputs.length;i++){
                if(cmdstrs == null){
                    cmdstrs = new ArrayList<String>();
                }
                if(inputs[i].startsWith("/dev/input/event")){
                    String cmdstr = format(inputs[i]);
                    cmdstrs.add(cmdstr);
                    Log.i(TAG,"cmdstr before: "+cmdstr);
                }
            }
            removeLastClickEvent();//去除最后一个单机事件
            EventEndCounts = new ArrayList<Integer>();
            events = new ArrayList<>();
            for(int i=0;i<cmdstrs.size();i++){
                String cmd = cmdstrs.get(i);
                if(cmd.contains("0 0 0")) {
                    int lastEventEndCount = 0;
                    StringBuffer buffer = new StringBuffer();
                    if(EventEndCounts.size()>0) {
                        lastEventEndCount = EventEndCounts.get(EventEndCounts.size()-1);
                    }
                    for(int j = lastEventEndCount;j<i;j++){
                        buffer.append(cmdstrs.get(j)).append("\n");
                    }
                    events.add(buffer);
                    EventEndCounts.add(i);
                }
            }
//        fixSendEvent();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fixSendEvent() {
        ArrayList<String> eventx = new ArrayList<String>();
        ArrayList<String> eventy = new ArrayList<String>();
        ArrayList<String> tracking_id = new ArrayList<String>();
        for(int i=0;i<cmdstrs.size();i++){
            String str = cmdstrs.get(i);
            if(str.contains(" 3 53 ")){
                eventx.add(str);
                if(!cmdstrs.get(i+1).contains(" 3 54 ")){
                    cmdstrs.add(i+1,eventy.get(eventy.size()-1));
                }
            }
            if(str.contains(" 3 54 ")){
                eventy.add(str);
                if(!cmdstrs.get(i-1).contains(" 3 53 ")){
                    cmdstrs.add(i,eventx.get(eventx.size()-1));
                }
            }
            if(str.contains(" 3 57 ")){
                tracking_id.add(str);
            }
            if(str.contains(" 0 0 0")){
                if(i<cmdstrs.size()-1&&!cmdstrs.get(i+1).contains(" 3 57 ")){
                    cmdstrs.add(i+1,tracking_id.get(tracking_id.size()-1));
                }
            }
        }
    }

    private void removeLastClickEvent() {
        //最后一个click事件出现时，会有两个sendevent /dev/input/event0 3 57 3331
        //删除从倒数第二个sendevent /dev/input/event0 3 57 3331 事件之后的所有事件
        ArrayList<Integer> eventClickNum = null;
        int lastClickNum = 0;
        for(int i=0;i<cmdstrs.size();i++){
            if(cmdstrs.get(i).contains("3 57 ")){
                if(eventClickNum == null){
                    eventClickNum = new ArrayList<Integer>();
                }
                eventClickNum.add(i);
            }
        }
        if(eventClickNum!=null&&eventClickNum.size()>=2){
            lastClickNum = eventClickNum.get(eventClickNum.size()-2);
            for(int i = cmdstrs.size()-1;i>=lastClickNum;i--){
                cmdstrs.remove(i);
            }
        }
    }

    public void play(){
        for(int i=0;i<events.size();i++){
            Command command = new Command(0, events.get(i).toString());
            try {
                RootTools.getShell(true).add(command);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (RootDeniedException e) {
                e.printStackTrace();
            }
        }
    }

    public void touchdown(int x,int y){
        Command command = new Command(0, "sendevent /dev/input/event0 3 57 4246\nsendevent /dev/input/event0 1 330 1\nsendevent /dev/input/event0 1 325 1\nsendevent /dev/input/event0 3 53 "+x+"\nsendevent /dev/input/event0 3 54 "+y+"\nsendevent /dev/input/event0 0 0 0\n");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }

    public void touchmove(int x,int y){
        Command command = new Command(0,"sendevent /dev/input/event0 3 53 "+x+"\nsendevent /dev/input/event0 3 54 "+y+"\n");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }

    public void touchup(){
        Command command = new Command(0,"sendevent /dev/input/event0 3 57 -1\nsendevent /dev/input/event0 1 330 0\nsendevent /dev/input/event0 1 325 0\nsendevent /dev/input/event0 0 0 0\n");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }

    private String format(String cmdstr){
        StringBuffer newstr=new StringBuffer();
        cmdstr = cmdstr.replace(":","");
        String[] strs_16 =  cmdstr.split(" ");
        for(int i=1;i<strs_16.length;i++){
            if(strs_16[i].endsWith("\r")){
                strs_16[i] = strs_16[i].replace("\r","");
            }
            if("ffffffff".equals(strs_16[i])){
                strs_16[i]="-1";
            }else {
                strs_16[i] = Integer.parseInt(strs_16[i], 16) +"";
            }
        }
        newstr.append("sendevent");
        for(int i=0;i<strs_16.length;i++){
            newstr = newstr.append(" "+strs_16[i]);
        }
        return newstr.toString();
    }

    public void test() {
//        try {
//            initializeSession();
//            out.write("sendevent /dev/input/event0 3 57 8755\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 1 330 1\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 1 325 1\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 3 53 1209\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 3 54 166\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 0 0 0\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 3 49 5\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 0 0 0\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 3 57 -1\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 1 330 0\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 1 325 0\n".getBytes());
//            out.flush();
//            out.write("sendevent /dev/input/event0 0 0 0\n".getBytes());
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Command command = new Command(0,"/data/data/com.zou.autotouch/lib/libhello.so");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }
}
