package com.logentries.android;

/*
 * Logentries Android Logger
 * Copyright 2011 Logentries, JLizard
 * Caroline Fenlon <carfenlon@gmail.com>
 */

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.content.Context;

/**
 * Logentries Android Logger<br/>
 * Controls the creation and upload of log events to a Logentries account.<br/>
 * For interval-controlled uploading, see <b>le.android.TimedLogger</b>.
 * @author Caroline Fenlon
 * 29/08/11
 */
public class AndroidLogger{
	private static AndroidLogger loggerInstance;

	private LogentriesAndroid le = null;
	private String logFileAddress = "logentries_saved_logs.log";
	private Logger logger = null;
	private boolean immediateUpload = true;
	private Context context = null;
	protected List<String> logList = null;

	/**
	 * When subclassing: just call super(context, token) in constructor
	 * @param context <i>getApplicationContext()</i> in an Activity
	 * @param token uuid corresponding to logfile on Logentries
	 */
	protected AndroidLogger(Context context, String token) {
		this.context = context;
		logger = Logger.getLogger("root");
		le = new LogentriesAndroid(token, true);
		logger.addHandler(le);
		//logList = new ArrayList<String>();
		//getSavedLogs();
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
	public void setImmediateUpload(boolean upload) {
		immediateUpload = upload;
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
	 * Add logs from file to the list of logs to upload
	 * (eg from previous session)
	 */
	public void getSavedLogs() {
		try{
			//Load logs from file system into a List
			DataInputStream dis = new DataInputStream(context.openFileInput(logFileAddress));
			String log = dis.readLine();
			while(log != null) {
				logList.add(log + "\r\n");
				log = dis.readLine();
			}

			//Then remove saved log file
			File dir = context.getFilesDir();
		    File file = new File(dir, logFileAddress);
		  	file.delete();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves logs in a private file for access when the app opens again.<br/>
	 * Use when shutting down the app or ending logging.
	 */
	public void saveLogs() {
		try {
			FileOutputStream fos = context.openFileOutput(logFileAddress, Context.MODE_PRIVATE);
			for(int i=0; i<logList.size(); i++) {
				String log = logList.get(i);
				fos.write((log).getBytes());
				logList.remove(log);
			}
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Attempts to upload any logs stored offline.
	 */
	public void uploadAllLogs() {
		String toUpload = "";
		Iterator<String> it = logList.iterator();
		while(it.hasNext()) {
			String event = it.next();
			toUpload += event;
			it.remove();
		}
		//le.upload(toUpload);
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
		if(this.immediateUpload) {
			le.publish(new LogRecord(level, logMessage.replace('\n', '\u2028')));
		} else {
			//add to list of offline logs
			Date currentTime = new Date();
			String event = le.format(currentTime, logMessage, level);
			logList.add(event);
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