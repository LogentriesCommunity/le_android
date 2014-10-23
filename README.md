Logging support for Android devices
===================================

Build requirements: Android SDK 2.3+
Runtime requirements: Android OS 2.3+


Setup
-----

Set up an account with Logentries at <https://logentries.com/>, and create a logfile, by clicking + Add New button and selecting the Manual Configuration Option at the bottom. Select Token TCP as the source type and copy the Token UUID printed in green.


Next, download the library jar file [here](https://github.com/logentries/le_android/raw/master/lib/logentries-android-2.1.2.jar) and place it in the /lib folder of your Android project.

Add the android.permission.INTERNET <uses-permission> to the project manifest file.

Use
---

In the desired Activity class, ``import com.logentries.android.AndroidLogger``

To create an instance of the Logger object in an Activity class:

    AndroidLogger logger = AndroidLogger.getLogger(Context context, String token, boolean logIp);
Where

 - context: for example, if in an Activity class, use ``getApplicationContext()``, or if in an Application class, use ``getBaseContext()``.

 - token: is the Token UUID we copied earlier which represents the logfile on Logentries

 - logIp: Is an option to log the users IP address. The IP logged will be the public if it is available otherwise it is gotten via the Host Address.

Log events are created using the following methods of the AndroidLogger class, which differ only in terms of the severity level they report:

 - severe, debug, info, config, fine, finer, finest, verbose

Eg: ``logger.error("Log Event Contents");`` creates the log ``Sat Jul 30 16:04:36 GMT+00:00 2011, severity=ERROR, Log Event Contents``.

Each method corresponds to those used in android.util.Log and java.util.logging.Logger.


DATAHUB INTEGRATION
--------------------

Additional constructors for AndroidLogger have been created for use with Logentries DataHub.
There are two additional constructors added, one with a customID and one without customID.  They are respectively:

	  AndroidLogger logger = AndroidLogger.getLogger(Context context, String datahub_address, int datahub_port);
and
 	 AndroidLogger logger = AndroidLogger.getLogger(Context context, String datahub_address, int datahub_port, String customID);

 - context: for example, if in an Activity class, use ``getApplicationContext()``, or if in an Application class, use ``getBaseContext()``.

 - datahub_address: is a String of the IP Address of your DataHub machine.

 - datahub_port: is an int of the port number of your incoming connection on your DataHub machine.  The default is port 10000, but this can be changed
to any port by altering the /etc/leproxy/leproxyLocal.config file on your DataHub machine and restarting the leproxy daemon using "sudo service leproxy restart".

- customID - is a String of the customID you may wish to add to your log events.  This is different from the deviceID which is now automatically set and entered into your logs.


The logger will now add the android deviceID as a Key Value Pair in your logs.  This requires API 9 and above.

NOTE: You cannot use both Token-based and DataHub-based AndroidLogger constructors in your application.  You can only use one or the other.


TODO
----
Use ``logger.setImmediateUpload(boolean)`` to control the buffering of logs

 - ``false``: logs are saved offline until ``logger.uploadAllLogs()`` is called
   Logs must be saved to a file when the application is closed: logger.saveLogs()

 - ``true``: logs are uploaded immediately
    The default value is ``true``.
    ``logger.getImmediateUpload()`` returns the current value.

The logs will now be available in your Logentries account under hostname > logname.


Timedlogger - TODO
-----------

To upload log events at a particular interval, use ``TimedLogger`` (a subclass
of ``AndroidLogger``).

    TimedLogger logger = TimedLogger.getLogger(context, userkey, hostname, logname)
    logger.start(float timeDelay)
  - timeDelay: time in seconds between uploads

Create logs in the same way as with ``Logger`` -
eg. ``logger.warn(String logContents)``.

``logger.uploadAllLogs()`` may still be used to force the upload of any saved
events.


Modification - TODO
------------

To extend or alter the function of the library, subclass ``le.android.AndroidLogger``.

The protected AndroidLogger constructor requires the same parameters as
``getLogger()``, and can simply call ``super()`` with them.

To add methods for the creation of different log severities,
call ``process(String logContents, String severity)`` with the desired severity
indicator.

``saveLogs()`` and ``getSavedLogs()`` store and access log events in a file on
the device's internal storage between sessions.  This is private to the app and
unseen by the user.  Override these methods to save logs to a different
location when the app is not in use.  While the app is open, logs are saved to
the protected ``List<String>`` logList.
