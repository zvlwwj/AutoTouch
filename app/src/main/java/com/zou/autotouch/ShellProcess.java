package com.zou.autotouch;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class ShellProcess {

	private static final int Length_ProcStat = 9;
	private static final String TAG = "AndroidSunlogin";

	private ShellProcess() {
	}

	/*
	 * 执行shell 命令
	 * @param command
	 * @param isroot
	 * @return 命令行的输出 <pid, name>
	 */
	public static HashMap<String, String> execCommand(String command, boolean isroot) {
		return parseShellResult(executeCommand(command, isroot));
	}

	/*
	 * 执行shell 命令
	 * @param command
	 * @param isroot
	 * @return 命令行的输出 <pid, array{pid,ppid,name}>
	 */
	public static HashMap<String, ArrayList<String>> execCommand2(String command, boolean isroot) {
		return parseShellResult2(executeCommand(command, isroot));
	}

	/*
	 * 执行shell 命令
	 * @param command
	 * @param isroot
	 * @return 命令行的输出
	 */
	public static String executeCommand(String command, boolean isroot) {
		// start the ls command running
		String output_string = "";
		DataOutputStream ostream;
		// 实际启动一个子进程,没有控制台,需要用输出流来得到shell执行后的输出
		InputStream istream;

		if (command.isEmpty()) return output_string;
		try {
			Process process = null;
			if (isroot) {
				process = RootTools.checkSuPermission(false);
			} else {
				process = Runtime.getRuntime().exec("sh");
			}
			if (null == process) return output_string;

			ostream = new DataOutputStream(process.getOutputStream());
			istream = process.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(istream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			ostream.writeBytes(command + "\n");
			ostream.writeBytes(RootTools.LINUX_CMD_EXIT);
			ostream.flush();

			// read the ls output
			String line = "";
			StringBuilder output_strbuilder = new StringBuilder(line);
			while ((line = bufferedreader.readLine()) != null) {
				output_strbuilder.append(line).append('\n');
			}
			output_string = output_strbuilder.toString();
			// 使用exec执行不会等执行成功以后才返回,它会立即返回
			// 使用wairFor()可以等待命令执行完成以后才返回
			process.waitFor();
			process.destroy();
			//Log.w(TAG, "------ [debug] executeCommand:" + command + " , result:" + output_string);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output_string;
	}

	/**
	 * 只执行不要结果
	 * @param command
	 * @param isroot
     * @return
     */
	public static String executeCommandNoResult(String command, boolean isroot) {
		// start the ls command running
		String output_string = "";
		DataOutputStream ostream;
		// 实际启动一个子进程,没有控制台,需要用输出流来得到shell执行后的输出
		InputStream istream = null;

		if (command.isEmpty()) return output_string;
		try {
			Process process = null;
			if (isroot) {
				process = RootTools.checkSuPermission(false);
			} else {
				process = Runtime.getRuntime().exec("sh");
			}
			if (null == process) return output_string;

			ostream = new DataOutputStream(process.getOutputStream());
			istream = process.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(istream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			ostream.writeBytes(command + "\n");
			ostream.writeBytes(RootTools.LINUX_CMD_EXIT);
			ostream.flush();

			// 使用exec执行不会等执行成功以后才返回,它会立即返回
			// 使用wairFor()可以等待命令执行完成以后才返回
			process.waitFor();
			process.destroy();
			//Log.w(TAG, "------ [debug] executeCommand:" + command + " , result:" + output_string);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CloseUtils.closeQuietly(istream);
		}
		return output_string;
	}
	/*
	 * 解析shell命令行的输出，保存pid和进程名到 hashmap
	 * @param cmdout
	 * @return 
	 */
	public static HashMap<String, String> parseShellResult(String cmdout) {
		String tempString = "";
		String[] rows = null;
		String[] columns = null;
		HashMap<String, String> processMap = new HashMap<String, String>();

		if (null == cmdout || cmdout.isEmpty()) return processMap;

		rows = cmdout.split("[\n]+"); // 使用正则表达式分割字符串

  		for (int i = 0; i < rows.length; i++) {
			tempString = rows[i];
			tempString = tempString.trim();
			columns = tempString.split("[ ]+");
			if (columns.length == Length_ProcStat) {
				processMap.put(columns[1], columns[8]);
			}
		}

		return processMap;
	}

	/*
	 * 解析shell命令行的输出，保存pid和进程名到 hashmap
	 * @param cmdout
	 * @return 
	 */
	public static HashMap<String, ArrayList<String>> parseShellResult2(String cmdout) {
		String tempString = "";
		String[] rows = null;
		String[] columns = null;

		HashMap<String, ArrayList<String>> processMap = new HashMap<String, ArrayList<String>>();

		if (null == cmdout || cmdout.isEmpty()) return processMap;

		rows = cmdout.split("[\n]+"); // 使用正则表达式分割字符串

		for (int i = 0; i < rows.length; i++) {
			tempString = rows[i];
			tempString = tempString.trim();
			columns = tempString.split("[ ]+");
			if (columns.length == Length_ProcStat) {
				ArrayList<String> array = new ArrayList<String>();
				array.add(columns[1]);
				array.add(columns[2]);
				array.add(columns[8]);
				processMap.put(columns[1], array);
			}
		}

		return processMap;
	}

	/*
	 * 执行shell 命令
	 * @param command
	 * @param isroot
	 * @return 命令行的输出 <pid, name>
	 */
	public static HashMap<String, String> execGlobalCommand(String command, boolean isroot) {
		return parseShellResult(executeGlobalCommand(command, isroot));
	}

	/*
	 * 执行shell 命令
	 * @param command
	 * @param isroot
	 * @return 命令行的输出
	 */
	public static HashMap<String, ArrayList<String>> execGlobalCommand2(String command, boolean isroot) {
		return parseShellResult2(executeGlobalCommand(command, isroot));
	}

	/*
	 * 执行shell 命令
	 * @param command
	 * @param isroot
	 * @return 命令行的输出
	 */
	public static String executeGlobalCommand(String command, boolean isroot) {
		// start the ls command running
		String output_string = "";
		DataOutputStream ostream;
		// 实际启动一个子进程,没有控制台,需要用输出流来得到shell执行后的输出
		InputStream istream;

		if (command.isEmpty())
			return output_string;
		try {
			Process process = null;
			if (isroot) {
				process = RootTools.checkGlobalSuPermission(false);
			} else {
				process = Runtime.getRuntime().exec("sh");
			}
			if (null == process)
				return output_string;

			ostream = new DataOutputStream(process.getOutputStream());
			istream = process.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(istream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			ostream.writeBytes(command + "\n");
			//ostream.writeBytes(RootTools.LINUX_CMD_EXIT); // 不加会阻塞
			ostream.flush();

			// read the ls output
			String line = "";
			StringBuilder output_strbuilder = new StringBuilder(line);
			while ((line = bufferedreader.readLine()) != null) {
				output_strbuilder.append(line).append('\n');
			}
			output_string = output_strbuilder.toString();
			// 使用exec执行不会等执行成功以后才返回,它会立即返回
			// 使用wairFor()可以等待命令执行完成以后才返回
			// process.waitFor();
			// process.destroy();
			//Log.w(TAG, "------ [debug] executeGlobalCommand:" + command + " , result:" + output_string);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output_string;
	}
}
