package com.zou.autotouch.service;

import android.os.ParcelFileDescriptor;
import android.util.Log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
//        fixSendEvent();
            for (String str: cmdstrs) {
                Log.i(TAG,"cmdstr after: "+str);
            }
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
        StringBuffer buffer = null;
        try {
            initializeSession();
            Thread.sleep(1000);
        for(int i=0;i<cmdstrs.size();i++){
                String cmd = cmdstrs.get(i);
//                Log.i(TAG, "cmdstr after: " + cmd);
//            out.write((cmd+"\n").getBytes());
//            out.flush();
//            if(buffer == null){
//                buffer = new StringBuffer();
//            }
//            buffer.append(cmd).append("\n");
            Command command = new Command(0, cmd);
            RootTools.getShell(true).add(command);
        }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
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
    }
}
//sendevent /dev/input/event0 3 57 4891
//        sendevent /dev/input/event0 1 330 1
//        sendevent /dev/input/event0 1 325 1
//        sendevent /dev/input/event0 3 53 151
//        sendevent /dev/input/event0 3 54 1208
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 152
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 154
//        sendevent /dev/input/event0 3 54 1209
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 156
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 160
//        sendevent /dev/input/event0 3 54 1210
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 164
//        sendevent /dev/input/event0 3 54 1211
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 170
//        sendevent /dev/input/event0 3 54 1212
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 175
//        sendevent /dev/input/event0 3 54 1213
//        sendevent /dev/input/event0 3 49 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 181
//        sendevent /dev/input/event0 3 54 1214
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 188
//        sendevent /dev/input/event0 3 54 1215
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 196
//        sendevent /dev/input/event0 3 49 6
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 204
//        sendevent /dev/input/event0 3 54 1216
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 212
//        sendevent /dev/input/event0 3 54 1218
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 219
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 227
//        sendevent /dev/input/event0 3 54 1220
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 235
//        sendevent /dev/input/event0 3 54 1221
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 243
//        sendevent /dev/input/event0 3 54 1222
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 252
//        sendevent /dev/input/event0 3 54 1223
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 262
//        sendevent /dev/input/event0 3 54 1224
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 274
//        sendevent /dev/input/event0 3 54 1225
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 284
//        sendevent /dev/input/event0 3 54 1227
//        sendevent /dev/input/event0 3 49 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 298
//        sendevent /dev/input/event0 3 54 1228
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 305
//        sendevent /dev/input/event0 3 54 1229
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 314
//        sendevent /dev/input/event0 3 54 1230
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 323
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 334
//        sendevent /dev/input/event0 3 54 1232
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 345
//        sendevent /dev/input/event0 3 54 1233
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 355
//        sendevent /dev/input/event0 3 54 1234
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 365
//        sendevent /dev/input/event0 3 54 1235
//        sendevent /dev/input/event0 3 48 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 380
//        sendevent /dev/input/event0 3 48 6
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 389
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 398
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 408
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 418
//        sendevent /dev/input/event0 3 54 1236
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 432
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 446
//        sendevent /dev/input/event0 3 54 1237
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 458
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 470
//        sendevent /dev/input/event0 3 54 1238
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 479
//        sendevent /dev/input/event0 3 54 1237
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 492
//        sendevent /dev/input/event0 3 54 1238
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 503
//        sendevent /dev/input/event0 3 54 1239
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 517
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 531
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 543
//        sendevent /dev/input/event0 3 54 1241
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 554
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 564
//        sendevent /dev/input/event0 3 54 1240
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 578
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 591
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 604
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 617
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 630
//        sendevent /dev/input/event0 3 54 1241
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 640
//        sendevent /dev/input/event0 3 54 1242
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 646
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 658
//        sendevent /dev/input/event0 3 54 1243
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 673
//        sendevent /dev/input/event0 3 54 1244
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 684
//        sendevent /dev/input/event0 3 54 1245
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 699
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 709
//        sendevent /dev/input/event0 3 54 1246
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 718
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 729
//        sendevent /dev/input/event0 3 54 1247
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 736
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 751
//        sendevent /dev/input/event0 3 54 1248
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 765
//        sendevent /dev/input/event0 3 54 1249
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 780
//        sendevent /dev/input/event0 3 49 6
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 785
//        sendevent /dev/input/event0 3 54 1250
//        sendevent /dev/input/event0 3 49 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 795
//        sendevent /dev/input/event0 3 54 1251
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 805
//        sendevent /dev/input/event0 3 49 6
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 817
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 824
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 840
//        sendevent /dev/input/event0 3 49 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 850
//        sendevent /dev/input/event0 3 54 1252
//        sendevent /dev/input/event0 3 49 6
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 863
//        sendevent /dev/input/event0 3 49 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 873
//        sendevent /dev/input/event0 3 49 6
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 881
//        sendevent /dev/input/event0 3 49 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 888
//        sendevent /dev/input/event0 3 49 6
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 905
//        sendevent /dev/input/event0 3 54 1253
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 913
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 927
//        sendevent /dev/input/event0 3 54 1254
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 940
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 947
//        sendevent /dev/input/event0 3 54 1255
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 957
//        sendevent /dev/input/event0 3 54 1256
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 966
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 974
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 986
//        sendevent /dev/input/event0 3 54 1257
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 998
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1015
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1022
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1024
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1037
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1043
//        sendevent /dev/input/event0 3 54 1256
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1050
//        sendevent /dev/input/event0 3 54 1255
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1062
//        sendevent /dev/input/event0 3 54 1254
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1073
//        sendevent /dev/input/event0 3 54 1253
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1082
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1095
//        sendevent /dev/input/event0 3 54 1252
//        sendevent /dev/input/event0 3 49 5
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1102
//        sendevent /dev/input/event0 3 54 1251
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1110
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1117
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1123
//        sendevent /dev/input/event0 3 54 1250
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1134
//        sendevent /dev/input/event0 3 54 1249
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1144
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1154
//        sendevent /dev/input/event0 3 54 1248
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1166
//        sendevent /dev/input/event0 3 54 1247
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1174
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1185
//        sendevent /dev/input/event0 3 54 1246
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1193
//        sendevent /dev/input/event0 3 54 1245
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1199
//        sendevent /dev/input/event0 3 54 1244
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1208
//        sendevent /dev/input/event0 3 54 1243
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1217
//        sendevent /dev/input/event0 3 54 1242
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1227
//        sendevent /dev/input/event0 3 54 1241
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1240
//        sendevent /dev/input/event0 3 54 1240
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1249
//        sendevent /dev/input/event0 3 54 1239
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1258
//        sendevent /dev/input/event0 3 54 1238
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1264
//        sendevent /dev/input/event0 3 54 1237
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1271
//        sendevent /dev/input/event0 3 54 1236
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1281
//        sendevent /dev/input/event0 3 54 1234
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1289
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1294
//        sendevent /dev/input/event0 3 54 1233
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1307
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1311
//        sendevent /dev/input/event0 3 54 1232
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1321
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1329
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 54 1231
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1339
//        sendevent /dev/input/event0 3 54 1232
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1343
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1347
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1350
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1351
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1353
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1355
//        sendevent /dev/input/event0 3 54 1233
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1356
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1357
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1359
//        sendevent /dev/input/event0 0 0 0
//        sendevent /dev/input/event0 3 53 1360
//        sendevent /dev/input/event0 0 0 0
