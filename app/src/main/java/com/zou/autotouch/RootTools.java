package com.zou.autotouch;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Root工具. 获取root权限时:<br/>
 * 1. 启动一个sh, 输入su, 获取输出流验证<br/>
 * 2. 执行su命令, 获取输出流验证<br/>
 * 部分(极少)手机因为sh文件导致通用id命令无法正常得到输出结果, 所以应使用第2种方式.
 */
public class RootTools extends Object {

	private static final String TAG = "AndroidSunlogin";

	public static final String LINUX_CMD_SH = "sh";
	public static final String LINUX_CMD_SU = "su";
	public static final String LINUX_CMD_ID = "id\n";
	public static final String LINUX_CMD_EXIT = "exit\n";

	private static final String LINUX_CMD_EXPORT_LIB = "export LD_LIBRARY_PATH=/vendor/lib:/system/lib\n";

	private static final String ID_UID_GID_REGEX = ".*uid=(\\d*).*gid=(\\d*).*";
	
	//尽可能使用同一进程
	private static ProcessBuilder mSuProcessBuilder = null;
	private static Process mSuProcess = null;
	
	// just for debug
	private RootTools() {
	}

	/**
	 * 判断手机是否root(依据是: bin目录下有su文件)
	 */
	public static boolean isRootAvailable() {
		String[] places = { "/sbin/", "/system/bin/", "/system/xbin/", "/bin/",	"/"};
		for (String where : places) {
			File file = new File(where + "su");
			if (file.exists()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isRootPermission() {
		boolean bsu = requestSuPermission();
		return bsu;
	}
	
	/**
	 * 检测请求root权限， 是否被用户允许
	 * @return object, 用户允许; null 用户拒绝
	 */
	public static Process checkGlobalSuPermission(boolean needexit) {
		boolean result = false;
		
		try {
			if (null == mSuProcessBuilder) {
				mSuProcessBuilder = new ProcessBuilder(LINUX_CMD_SU);
				//LogUtil.d(TAG, "------ [debug] checkGlobalSuPermission, new ProcessBuilder");
			}
			mSuProcessBuilder.redirectErrorStream(true);
			if (null == mSuProcess) {
				mSuProcess = mSuProcessBuilder.start();
			}
			
			DataOutputStream ostream = new DataOutputStream(mSuProcess.getOutputStream());
			InputStreamReader istream = new InputStreamReader(mSuProcess.getInputStream());
			BufferedReader reader = new BufferedReader(istream);
	
			ostream.writeBytes(LINUX_CMD_ID);
			if (needexit)
				ostream.writeBytes(LINUX_CMD_EXIT);
			ostream.flush();
	
			String line = null;
			// 此处循环读取, 是因为部分手机ID命令输入uid=0时前后有特殊符号
			int uid = -1, gid = -1;
			while (null != (line = reader.readLine())) {
				if (!TextUtils.isEmpty(line.trim())) {
					Pattern pattern = Pattern.compile(ID_UID_GID_REGEX);
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						uid = Integer.parseInt(matcher.group(1));
						gid = Integer.parseInt(matcher.group(2));
						break;
					}
				}
			}
			if (0 == uid && 0 == gid) {
				result = true;
			}
	    	//LogUtil.d(TAG, "[debug] checkSuPermission, result:" + line);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		if (!result) { 
			mSuProcess = null;
		}
    	return mSuProcess;
	}
	
	
	/**
	 * 检测请求root权限， 是否被用户允许
	 * @return object, 用户允许; null 用户拒绝
	 */
	public static Process checkSuPermission(boolean needexit) {
		boolean result = false;
		
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(LINUX_CMD_SU);
			builder.redirectErrorStream(true);
			process = builder.start();
			
			DataOutputStream ostream = new DataOutputStream(process.getOutputStream());
			InputStreamReader istream = new InputStreamReader(process.getInputStream());
			BufferedReader reader = new BufferedReader(istream);
	
			ostream.writeBytes(LINUX_CMD_ID);
			if (needexit)
				ostream.writeBytes(LINUX_CMD_EXIT);
			ostream.flush();
	
			String line = null;
			// 此处循环读取, 是因为部分手机ID命令输入uid=0时前后有特殊符号
			int uid = -1, gid = -1;
			while (null != (line = reader.readLine())) {
				if (!TextUtils.isEmpty(line.trim())) {
					Pattern pattern = Pattern.compile(ID_UID_GID_REGEX);
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						uid = Integer.parseInt(matcher.group(1));
						gid = Integer.parseInt(matcher.group(2));
						break;
					}
				}
			}
			if (0 == uid && 0 == gid) {
				result = true;
			}
	    	//LogUtil.d(TAG, "[debug] checkSuPermission, result:" + line);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		if (!result) { 
			process = null;
		}
    	return process;
	}
	
	/**
	 * 要求获取root权限
	 * @return
	 */
	public static boolean requestSuPermission() {
		boolean result = false;
		try {
			Process process = checkSuPermission(true);
			if (null != process) {
				result = true;
				process.waitFor();
				process.destroy();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * @param command 
     * @return 应用程序是/否获取Root权限
     */
    public static String runSuCommand(String command)
    {
        Process process = null;
        DataOutputStream ostream = null;
        InputStream istream = null;
        StringBuffer cmdout = new StringBuffer();
        
        try {
            process = checkSuPermission(false);
            if (null == process) return "";
            
            ostream = new DataOutputStream(process.getOutputStream());
            istream = process.getInputStream();
            
            ostream.writeBytes(command + "\n");
            ostream.writeBytes(LINUX_CMD_EXIT);
            ostream.flush();
            
    		BufferedReader bufreader = new BufferedReader(new InputStreamReader(istream));
            String line = null;
			while ((line = bufreader.readLine()) != null) {
		        cmdout.append(line).append(System.getProperty("line.separator"));
			}
	    	process.waitFor();
	    	process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ostream != null) {
                    ostream.close();
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        return cmdout.toString().trim();
    }	

	/**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * @param command 
     * @return 应用程序是/否获取Root权限
     */
    public static String runSuCommandWithGlobalProcess(String command)
    {
        Process process = null;
        DataOutputStream ostream = null;
        InputStream istream = null;
        StringBuffer cmdout = new StringBuffer();
        
        try {
            process = checkGlobalSuPermission(false);
            if (null == process) return "";
            
            ostream = new DataOutputStream(process.getOutputStream());
            istream = process.getInputStream();
            
            ostream.writeBytes(command + "\n");
//            ostream.writeBytes(LINUX_CMD_EXIT);
            ostream.flush();
            
    		BufferedReader bufreader = new BufferedReader(new InputStreamReader(istream));
            String line = null;
			while ((line = bufreader.readLine()) != null) {
		        cmdout.append(line).append(System.getProperty("line.separator"));
			}
	    	process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ostream != null) {
                    ostream.close();
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        return cmdout.toString().trim();
    }	
    
	/**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * @param command 
     * @return 应用程序是/否获取Root权限
     */
    public static boolean runCommandWithSu(String command)
    {
        Process process = null;
        DataOutputStream os = null;
        boolean result = false;
        
        try {
            process = checkSuPermission(false);
            if (null == process) return false;
            
            os = new DataOutputStream(process.getOutputStream());
            
            os.writeBytes(command + "\n");
            os.writeBytes(LINUX_CMD_EXIT);
            os.flush();
            
			result = true;
			process.waitFor();
			process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        return result;
    }	
}
