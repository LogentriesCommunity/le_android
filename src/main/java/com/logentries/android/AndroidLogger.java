package com.example.library;
/*
 * Logentries Android Logger
 * Copyright 2011 Logentries, JLizard
 * Caroline Fenlon <carfenlon@gmail.com>
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Logentries Android Logger<br/>
 * Controls the creation and upload of log events to a Logentries account.<br/>
 * For interval-controlled uploading, see <b>le.android.TimedLogger</b>.
 * @author Caroline Fenlon
 * 29/08/11
 * Modified by Sean 07/03/14
 * 	- implemented Uncaught Exception Handler
 * 	- added isOnline/getAllLogcat methods
 * 	- added Network Receiver
 * 	- removed getSavedLogs, saveLogs, uploadAllLogs
 */
public class AndroidLogger implements UncaughtExceptionHandler{
	private static AndroidLogger loggerInstance;

	private LogentriesAndroid le = null;
	private Logger logger = null;
	private static boolean immediateUpload = true;
	private Context context = null;
	protected List<String> logList = null;
	UncaughtExceptionHandler defaultUEH;
	private static NetworkReceiver receiver= new NetworkReceiver();

	/**
	 * When subclassing: just call super(context, token) in constructor
	 * @param context <i>getApplicationContext()</i> in an Activity
	 * @param token uuid corresponding to logfile on Logentries
	 */
	protected AndroidLogger(Context context, String token) {
		defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
		this.context = context;
		logger = Logger.getLogger("root");
		le = new LogentriesAndroid(token, true,context);
		logger.addHandler(le);
		immediateUpload=isOnline();
		receiver = new NetworkReceiver();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(receiver, filter);
	}


	/**
	 * Singleton - only one Logger object allowed
	 * @param context <i>getApplicationContext()</i> in Activity
	 * @param token uuid corresponding to logfile on Logentries
	 * @return an instance of the Logger object
	 */
	public static synchronized AndroidLogger getLogger(Context context, String token) {
		if(loggerInstance == null) {
			loggerInstance = new AndroidLogger(context, token);
		}
		return loggerInstance;
	}

	/**
	 * Cloning fails - singleton class
	 */
	public Logger clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * @param upload true if events are to be uploaded to Logentries immediately
	 */
	public static void setImmediateUpload(boolean upload) {
		immediateUpload = upload;
	}

	/**
	 * 
	 * @return true if wifi or mobile connection present
	 */
	public boolean isOnline() {
		ConnectivityManager cm =(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
	/**
	 * @return true if events are to be uploaded immediately, false otherwise
	 * default value: true
	 */
	public boolean getImmediateUpload() {
		return immediateUpload;
	}

	/**
	 * disconnect from theLogentries server
	 */
	public void closeConnection() {
		le.close();
	}

	/**
	 * Flush the OutputStream used in the LE connection
	 */
	public void flushConnection() {
		le.flush();
	}


	/**
	 * Creates and uploads/stores a log event with severity <b>severe</b>
	 * Java Logger priority: 1000 (highest)
	 * @param logContents the textual contents of the log
	 */
	public void severe(String logMessage) {
		process(logMessage, AndroidLevel.SEVERE);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>error</b>
	 * Java Logger priority: 950
	 * @param logContents the textual contents of the log
	 */
	public void error(String logMessage) {
		process(logMessage, AndroidLevel.ERROR);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>warning</b>
	 * Java Logger priority: 900
	 * @param logContents the textual contents of the log
	 */
	public void warn(String logMessage) {
		process(logMessage, AndroidLevel.WARNING);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>debug</b>
	 * Java Logger priority: 850
	 * @param logContents the textual contents of the log
	 */
	public void debug(String logMessage) {
		process(logMessage, AndroidLevel.DEBUG);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>info</b>
	 * Java Logger priority: 800
	 * @param logContents the textual contents of the log
	 */
	public void info(String logMessage) {
		process(logMessage, AndroidLevel.INFO);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>config</b>
	 * Java Logger priority: 700
	 * @param logContents the textual contents of the log
	 */
	public void config(String logMessage) {
		process(logMessage, AndroidLevel.CONFIG);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>fine</b>
	 * Java Logger priority: 500
	 * @param logContents the textual contents of the log
	 */
	public void fine(String logMessage) {
		process(logMessage, AndroidLevel.FINE);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>finer</b>
	 * Java Logger priority: 400
	 * @param logContents the textual contents of the log
	 */
	public void finer(String logMessage) {
		process(logMessage, AndroidLevel.FINER);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>finest</b>
	 * Java Logger priority: 300
	 * @param logContents the textual contents of the log
	 */
	public void finest(String logMessage) {
		process(logMessage, AndroidLevel.FINEST);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>verbose</b>
	 * Java Logger priority: 0
	 * @param logContents the textual contents of the log
	 */
	public void verbose(String logMessage) {
		process(logMessage, AndroidLevel.VERBOSE);
	}

	/**
	 * Composes a log event with a timestamp and severity and uploads or stores it.
	 * @param log The contents of the log (not including timestamp and severity) to be processed
	 * @param level The severity level to be incorporated into the log event
	 */
	protected void process(String logMessage, Level level) {
		if(getImmediateUpload()) {
			le.publish(new LogRecord(level, logMessage));
		} else {
			//format and pass along to saving thread
			Date currentTime = new Date();
			String event = le.format(currentTime, logMessage, level);
			le.saveLog(event);
		}
	}
	/**
	 * removes the broadcast receiver from the context it was bound to.
	 * Stops listening for connection changes
	 */
	public void stopReceiver(){
		context.unregisterReceiver(receiver);
	}
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		//print the exception
		printLogs(e);
		//throw exception as expected, crashing the app
		defaultUEH.uncaughtException(t, e);
	}
	/**
	 * Prints the stacktrace of a thrown exception
	 * @param e The thrown Exception to print
	 */
	public void printLogs(Throwable e){
		//System.out.println(debuglog.toString());
		final StringWriter result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		String stacktrace = result.toString();
		process(stacktrace, AndroidLevel.ERROR);
	}
	/**
	 * Prints everything outputted to logcat, requires READ_LOGS permission, unused
	 */
	public void getAllLogcat(){
		StringBuilder debuglog=new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec("logcat -d");//get logcat
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				debuglog.append(line);
				debuglog.append("\r\n");
			}
			Runtime.getRuntime().exec("logcat -c");//clear logcat
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	


}

/**
 * To create more logging levels, subclass AndroidLevel with a class containing
 * static Level objects.
 * <br/><i>public static Level myLevel = new ExtendedLevel(levelName, levelPriority);</i>
 * @author Caroline
 * 29/08/11
 */
class AndroidLevel extends Level {

	protected AndroidLevel(String name, int level) {
		super(name, level);
	}

	public static Level ERROR = new AndroidLevel("ERROR", 950);
	public static Level DEBUG = new AndroidLevel("DEBUG", 850);
	public static Level VERBOSE = new AndroidLevel("VERBOSE", 0);
}

/**
 * Constantly listens to Internet connectivity changes, updating immediateUpload boolean
 * Is bound to the context of the activity, dies when the activity dies
 * @author Sean
 *
 */
class NetworkReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {  
		ConnectivityManager cm =(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			Log.i("NET", "connected" );
			AndroidLogger.setImmediateUpload(true);
		}
		else{
			Log.i("NET", "not connected" );
			AndroidLogger.setImmediateUpload(false);
		}
	}
}