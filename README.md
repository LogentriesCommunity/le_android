Logging support for Android devices
===================================

Build requirements: Android SDK 1.5+
Runtime requirements: Android OS 1.5+


Setup
-----

Set up an account with Logentries at <https://logentries.com/>, and register a
host by selecting Hosts and then New (in the top right).  The host name you
enter will be required as a parameter when using the Logger.
	
Retrieve the userkey corresponding to your account. see the logentries getsetup
page -> Go with the API -> Android -> Getting the user key

Next, download the android source code from the logentries github account and add the android package to your Android project.
	
Add the android.permission.INTERNET <uses-permission> to the project manifest
file.

Use
---

In the desired Activity class, ``import le.android.Logger``

To create an instance of the Logger object in an Activity class:
    Logger logger = Logger.getLogger(Context context, String userkey, String hostname, String logname);
Where
 - context: use ``getApplicationContext()``
 - userkey: see the logentries getsetup page -> Go with the API -> Android -> Getting the user key
 - hostname: the name selected when creating the host
 - logname: the name you wish to call the log file to store events - note: a
   new log file will be created in your account the first time the library
   runs.

Log events are created using the warn, error, debug, info and verbose methods of Logger, which differ only in terms of the severity level 
they report.
Eg: ``logger.error("Log Event Contents");`` creates the log ``"Sat Jul 30 16:04:36 GMT+00:00 2011 severity=ERROR: Log Event Contents".``
Each method corresponds to those used in ``android.util.Log``.
	
Use ``logger.setImmediateUpload(boolean)`` to control the buffering of logs
 - ``false``: logs are saved offline until ``logger.uploadAllLogs()`` is called
   Logs must be saved to a file when the application is closed: logger.saveLogs()
 - ``true``: logs are uploaded immediately
    The default value is ``true``.
    ``logger.getImmediateUpload()`` returns the current value.

The logs will now be available in your Logentries account under hostname > logname.

	
Timedlogger
-----------

To upload log events at a particular interval, use ``TimedLogger`` (a subclass
of ``Logger``).
	
    TimedLogger logger = TimedLogger.getLogger(context, userkey, hostname, logname)
    logger.start(float timeDelay)
  - timeDelay: time in seconds between uploads

Create logs in the same way as with ``Logger`` -
eg. ``logger.warn(String logContents)``.

``logger.uploadAllLogs()`` may still be used to force the upload of any saved
events.
	

Modification
------------

To extend or alter the function of the library, subclass ``le.android.Logger``.
	
The protected Logger constructor requires the same parameters as
``getLogger()``, and can simply call ``super()`` with them.

To add methods for the creation of different log severities, 
call ``process(String logContents, String severity)`` with the desired severity
indicator.

``saveLogs()`` and ``getSavedLogs()`` store and access log events in a file on
the device's internal storage between sessions.  This is private to the app and
unseen by the user.  Override these methods to save logs to a different
location when the app is not in use.  While the app is open, logs are saved to
the protected ``List<String>`` logList.

