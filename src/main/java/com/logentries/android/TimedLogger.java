package com.logentries.android;

/*
 * Logentries Timed-upload Logger
 * Copyright 2011 Logentries, JLizard
 * Caroline Fenlon <carfenlon@gmail.com>
 */

import android.content.Context;
import android.os.Handler;

/**
 * Logentries Timed-upload Logger:
 * Controls the uploading of logs at a fixed interval.
 * @author Caroline Fenlon <carfenlon@gmail.com>
 */
public class TimedLogger extends AndroidLogger {
	private static TimedLogger loggerInstance;
	private Handler handler;

	/**
	 * When subclassing: just call super(context, userkey, hostname, logname) in constructor
	 * @param context <i>getApplicationContext()</i> in an Activity
	 * @param userkey key corresponding to account
	 * @param hostname host to store logs
	 * @param logname file to store events
	 */
    protected TimedLogger(Context ctx, String key) { // Dummy placeholder to get it compiling
        super(ctx, key);
    }
//	protected TimedLogger(Context context, String userkey, String hostname, String logname) {
//		super(context, userkey, hostname, logname);
//		handler = new Handler();
//	}

//	public static synchronized TimedLogger getLogger(Context context, String userkey, String hostname, String logname) {
//		if(loggerInstance == null) {
//			loggerInstance = new TimedLogger(context, userkey, hostname, logname);
//		}
//		return loggerInstance;
//	}

	/**
	 * Start uploading logs at intervals of timeDelay seconds
	 * @param timeDelay time between uploads in seconds
	 */
	public void start(final float timeDelay) {
		final Runnable r = new Runnable() {
		    public void run() {
		    	uploadAllLogs();
		    	handler.postDelayed(this, (long)timeDelay*1000);
		    }
		};
		handler.postDelayed(r, (long)timeDelay*1000);
	}

	/**
	 * Always false for timed upload - call <i>uploadAllLogs</i> to send any local logs.
	 */
	public boolean getImmediateUpload() {
		return false;
	}

}
