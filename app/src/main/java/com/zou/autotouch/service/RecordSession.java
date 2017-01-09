package com.zou.autotouch.service;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import jackpal.androidterm.TermExec;

/**
 * Created by zou on 2017/1/9.
 */

public class RecordSession {
    private static final String TAG = "RecordSession";
    private ParcelFileDescriptor mTermFd;
    public void startRecord(){
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

    }
}
