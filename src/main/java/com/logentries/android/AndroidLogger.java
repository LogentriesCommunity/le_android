package com.logentries.android;

/*
 * Logentries Android Logger
 * Copyright 2011 Logentries, JLizard
 * Caroline Fenlon <carfenlon@gmail.com>
 * 
 * updated 2014-10-21 for DataHub 
 */

import java.io.*;
import java.net.Inet4Address;
import java.net.UnknownHostException;
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
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Logentries Android Logger<br/>
 * Controls the creation and upload of log events to a Logentries account.<br/>
 * For interval-controlled uploading, see <b>le.android.TimedLogger</b>.
 */

public class AndroidLogger{
	private static final String TAG = "AndroidLogger";

	private static AndroidLogger loggerInstance;

	private LogentriesAndroid le = null;
	private String logFileAddress = "logentries_saved_logs.log";
	private Logger logger = null;
	private boolean immediateUpload = true;
	private Context context = null;
	protected List<String> logList = null;
	private String ip = null;
	private boolean logIp = false;
	

	/**
	 * When subclassing: just call super(context, token) in constructor
	 * @param context <i>getApplicationContext()</i> in an Activity
	 * @param token uuid corresponding to logfile on Logentries
	 */
	protected AndroidLogger(Context context, String token, boolean logIp) {
		this.context = context;
		logger = Logger.getLogger("root");
		le = new LogentriesAndroid(token, true);
		logger.addHandler(le);
		this.logIp = logIp;
		if(this.logIp){
			ip = getPublicIP();
		}
		logList = new ArrayList<String>();
		//getSavedLogs();
	}
	
// Constructor for without IP address	
	protected AndroidLogger(Context context, String token) {
		this.context = context;
		logger = Logger.getLogger("root");
		le = new LogentriesAndroid(token, true);
		logger.addHandler(le);
		
		logList = new ArrayList<String>();
		//getSavedLogs();
	}


//  constructor for DataHub - without customID	
	protected AndroidLogger(Context context, String datahub_address, int datahub_port) {
		this.context = context;
				
		logger = Logger.getLogger("root");  // this gets the singleton instance of the logger
						
		le = new LogentriesAndroid(datahub_address, datahub_port, true);
				
		logger.addHandler(le);


		logList = new ArrayList<String>();
		//getSavedLogs();
	}

//  constructor for DataHub - with customID	
	protected AndroidLogger(Context context, String datahub_address, int datahub_port, String customID) {
		this.context = context;

		logger = Logger.getLogger("root");  // this gets the singleton instance of the logger
						
		le = new LogentriesAndroid(datahub_address, datahub_port, true, customID);
				
		logger.addHandler(le);

		logList = new ArrayList<String>();
//		getSavedLogs();
	}
	

	public static String getPublicIP(){
		String ip = "";
		try {
			String url = "http://icanhazip.com/";

			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);

			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			ip = result.toString();

		} catch(IOException ioException){
			try{
				ip = Inet4Address.getLocalHost().getHostAddress();
			}catch(UnknownHostException unknownHostException ){

			}

		}

		return ip;
	}

	/**
	 * Singleton - only one Logger object allowed
	 * @param context <i>getApplicationContext()</i> in Activity
	 * @param token uuid corresponding to logfile on Logentries
	 * @return an instance of the Logger object
	 */
	public static synchronized AndroidLogger getLogger(Context context, String token, boolean logIp) {
		if(loggerInstance == null) {
			loggerInstance = new AndroidLogger(context, token, logIp);
		}
		return loggerInstance;
	}


	public static synchronized AndroidLogger getLogger(Context context, String token) {
		if(loggerInstance == null) {
			loggerInstance = new AndroidLogger(context, token);
		}
		return loggerInstance;
	}


	public static synchronized AndroidLogger getLogger(Context context, String datahub_address, int datahub_port) {
		
		if(loggerInstance == null) {
			loggerInstance = new AndroidLogger(context, datahub_address, datahub_port);
		}
		return loggerInstance;
	}

public static synchronized AndroidLogger getLogger(Context context, String datahub_address, int datahub_port, String customID) {
		
		if(loggerInstance == null) {
			loggerInstance = new AndroidLogger(context, datahub_address, datahub_port, customID);
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
			BufferedReader d = new BufferedReader(new InputStreamReader(dis));
			String log = d.readLine();

			while(log != null) {
				logList.add(log + "\r\n");
				log = d.readLine();

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
		Log.w(TAG, "uploadAllLogs");
		String toUpload = "";
		Iterator<String> it = logList.iterator();
		while(it.hasNext()) {
			String event = it.next();
			toUpload += event;
			it.remove();
		}
		le.upload(toUpload);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>severe</b>
	 * Java Logger priority: 1000 (highest)
	 * param logContents the textual contents of the log
	 */
	public void severe(String logMessage) {
		process(logMessage, AndroidLevel.SEVERE);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>error</b>
	 * Java Logger priority: 950
	 * param logContents the textual contents of the log
	 */
	public void error(String logMessage) {
		process(logMessage, AndroidLevel.ERROR);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>warning</b>
	 * Java Logger priority: 900
	 * param logContents the textual contents of the log
	 */
	public void warn(String logMessage) {
		process(logMessage, AndroidLevel.WARNING);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>debug</b>
	 * Java Logger priority: 850
	 * param logContents the textual contents of the log
	 */
	public void debug(String logMessage) {
		process(logMessage, AndroidLevel.DEBUG);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>info</b>
	 * Java Logger priority: 800
	 * param logContents the textual contents of the log
	 */
	public void info(String logMessage) {
		process(logMessage, AndroidLevel.INFO);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>config</b>
	 * Java Logger priority: 700
	 * param logContents the textual contents of the log
	 */
	public void config(String logMessage) {
		process(logMessage, AndroidLevel.CONFIG);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>fine</b>
	 * Java Logger priority: 500
	 * param logContents the textual contents of the log
	 */
	public void fine(String logMessage) {
		process(logMessage, AndroidLevel.FINE);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>finer</b>
	 * Java Logger priority: 400
	 * param logContents the textual contents of the log
	 */
	public void finer(String logMessage) {
		process(logMessage, AndroidLevel.FINER);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>finest</b>
	 * Java Logger priority: 300
	 * param logContents the textual contents of the log
	 */
	public void finest(String logMessage) {
		process(logMessage, AndroidLevel.FINEST);
	}

	/**
	 * Creates and uploads/stores a log event with severity <b>verbose</b>
	 * Java Logger priority: 0
	 * param logContents the textual contents of the log
	 */
	public void verbose(String logMessage) {
		process(logMessage, AndroidLevel.VERBOSE);
	}

	public void setLogIp(boolean logIp) {
		this.logIp = logIp;
	}

	/**
	 * Composes a log event with a timestamp and severity and uploads or stores it.
	 * param log The contents of the log (not including timestamp and severity) to be processed
	 * param level The severity level to be incorporated into the log event
	 */
	protected void process(String logMessage, Level level) {
		if(logIp){
			logMessage = "ip:" + ip + ", " + logMessage;
		}
		if(this.immediateUpload) {
			le.publish(new LogRecord(level, logMessage));
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