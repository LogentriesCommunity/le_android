package le.android;

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
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

/**
 * Logentries Android Logger
 * Controls the creation and upload of log events to a Logentries account.<br/>
 * For interval-controlled uploading, see <b>le.android.TimedLogger</b>.
 * @author Caroline Fenlon
 */
public class Logger {
	private static Logger loggerInstance;
	
	private Boolean IMMEDIATE_UPLOAD = null;
	
	/**
	 * Name of file to store preferences for the Logentries library
	 */
	protected final String PREFS_FILE = "le_prefs";
	private String logFileAddress = "savedlogs.log";
	
	/**
	 * List containing events yet to be uploaded.<br/>
	 * Once events have been successfully uploaded, they are removed from the list.
	 */
	protected List<String> logList = null;
	private Context context;
	private String userkey,
		hostname,
		logname;

	/**
	 * When subclassing: just call super(context, userkey, hostname, logname) in constructor
	 * @param context <i>getApplicationContext()</i> in an Activity
	 * @param userkey key corresponding to account
	 * @param hostname host to store logs
	 * @param logname file to store events
	 */
	protected Logger(Context context, String userkey, String hostname, String logname) {
		this.context = context;
		this.userkey = userkey;
		this.hostname = hostname;
		this.logname = logname;
		logList = new ArrayList<String>();
		
		getSavedLogs();
	}
	
	/**
	 * Singleton - only one Logger object allowed
	 * @param context <i>getApplicationContext()</i> in Activity
	 * @param userkey key corresponding to app developer's Logentries account
	 * @param hostname host to store logs
	 * @param logname file to store uploaded events
	 * @return an instance of the Logger object
	 */
	public static synchronized Logger getLogger(Context context, String userkey, String hostname, String logname) {
		if(loggerInstance == null) {
			loggerInstance = new Logger(context, userkey, hostname, logname);
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
	 * @return true if events are to be uploaded immediately, false otherwise
	 */
	public boolean getImmediateUpload() {
		System.out.println("trying");
		if(IMMEDIATE_UPLOAD == null) {
			System.out.println("GETTING UPLOAD MODE");
			// Get upload preference from config file
			SharedPreferences settings = context.getSharedPreferences(PREFS_FILE, 0);
			IMMEDIATE_UPLOAD = settings.getBoolean("immediate_upload", true);
		}
		return IMMEDIATE_UPLOAD;
	}
	
	/**
	 * Sets the local variable and updates the config file if necessary
	 * @param immediateUpload true if logs are to be uploaded immediately
	 */
	public void setImmediateUpload(boolean immediateUpload) {
		IMMEDIATE_UPLOAD = getImmediateUpload();
		// If the value is changing, update it in the config. file
		if( IMMEDIATE_UPLOAD != immediateUpload ) {
			IMMEDIATE_UPLOAD = immediateUpload;
			SharedPreferences settings = context.getSharedPreferences(PREFS_FILE, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("immediate_upload", IMMEDIATE_UPLOAD);
			editor.commit();
		}
	}
	
	/**
	 * Creates and uploads/stores a log event with severity <b>info</b>
	 * @param logContents the textual contents of the log
	 */
	public void info(String logContents) {
		process(logContents, "INFO");
	}
	
	/**
	 * Creates and uploads/stores a log event with severity <b>warn</b>
	 * @param logContents the textual contents of the log
	 */
	public void warn(String logContents) {
		process(logContents, "WARN");
	}
	
	/**
	 * Creates and uploads/stores a log event with severity <b>error</b>
	 * @param logContents the textual contents of the log
	 */
	public void error(String logContents) {
		process(logContents, "ERROR");
	}
	
	/**
	 * Creates and uploads/stores a log event with severity <b>debug</b>
	 * @param logContents the textual contents of the log
	 */
	public void debug(String logContents) {
		process(logContents, "DEBUG");
	}
	
	/**
	 * Creates and uploads/stores a log event with severity <b>verbose</b>
	 * @param logContents the textual contents of the log
	 */
	public void verbose(String logContents) {
		process(logContents, "VERBOSE");
	}
	
	/**
	 * Composes a log event and uploads or stores it. 
	 * @param log The contents of the log (not including timestamp and severity) to be processed
	 * @param severity The severity to be incorporated into the log event
	 */
	protected void process(String log, String severity) {
		String event = getTimestamp() + " severity=" + severity + ": " + log;
		IMMEDIATE_UPLOAD = getImmediateUpload();
		System.out.println(IMMEDIATE_UPLOAD);
		if(IMMEDIATE_UPLOAD) {
			upload(event);
		} else {
			logList.add(event);
		}
	}
	
	/**
	 * Attempts to upload any logs stored offline.
	 * If upload fails, they will be retained until <i>uploadAllLogs()</i> is called again.
	 */
	public void uploadAllLogs() {
		for(String log : logList) {
			try{
				upload(log);
			} catch(RejectedExecutionException e) {
				System.out.println("Too many threads");
			}
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
				fos.write((log + "\n").getBytes());
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
	 * Add logs from file to the list of logs to upload
	 * (eg from previous session)
	 */
	protected void getSavedLogs() {
		try{
			DataInputStream dis = new DataInputStream(context.openFileInput(logFileAddress));
			String log = dis.readLine();
			while(log != null) {
				logList.add(log);
				log = dis.readLine();
			}
			
			//Then remove logs
			File dir = context.getFilesDir();
		    File file = new File(dir, logFileAddress);
		  	file.delete();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void upload(String log) {
		// Create HttpClient if necessary, perform HttpPut
		LogUploader uploader = new LogUploader();
		uploader.execute(log);
	}
 	
	/**
	 * @return a String representation of the current time. Default uses 
	 * <a href="http://download.oracle.com/javase/1.4.2/docs/api/java/util/Date.html#toString()">
	 * <i>Date.toString()</i></a>
	 */
	protected String getTimestamp() {
		return new Date().toString();
	}
	
	private class LogUploader extends AsyncTask <String, Void, Void> {
		private String log;
		private int response;
		
		@Override
		protected Void doInBackground(String... params) {
			log = params[0];
			LEHttpClient.startClient();
			response = LEHttpClient.putRequest(userkey, hostname, logname, log);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v){
			if(response == 200) {
				logList.remove(log);
			}
		}
		
	}
}
