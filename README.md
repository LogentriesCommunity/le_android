Logging support for Android devices
===================================

[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](https://github.com/mdp/rotp/blob/master/LICENSE)

Build requirements: Android SDK 2.3+

Runtime requirements: Android OS 2.3+

Features
--------

- Log events are forwarded over TCP to a specific log

	A unique Token UUID for the log is appended to each log event that is sent

- Send Logs via SSL (only available with the TCP Token method)

	The library can send logs via Token TCP over TLS/SSL to port 443

	Note that the library itself does not validate or manage TLS/SSL certificates!

- Storing logs offline and sending when connected.  

	While sending logs, if the device looses connection, logs are stored locally until a connection is reestablished

	10mb queue limit

- TraceID

	Each log event sent contains the device TraceID which is a unique 35 character ID.

- Datahub support

	Log events can be forward by TCP Token to Datahub

- Send Logs via HTTP POST (Note that this option will be deprecated in the near future!)

	Option of changing from Token TCP to using HTTP POST sending to the endpoint 'http://js.logentries.com/v1/logs/LOG-TOKEN'

	It is recommended to use the Token TCP default, which also has the option of using TLS/SSL

Setup
-----

Set up an account with Logentries at <https://logentries.com/>, and create a logfile, by clicking + Add New button and selecting the Manual Configuration Option at the bottom. Select Token TCP as the source type and copy the Token UUID printed in green.

Next, download the library jar file [here](https://github.com/logentries/le_android/tree/master/lib) and place it in the /yourProject/libs folder of your Android project.

Add the permission "android.permission.INTERNET" to the project manifest file.


Use
---

In the desired Activity class, ``import com.logentries.logger.AndroidLogger;``

The following simple example shows the library used in a basic Android application Activity - where the logger is set
to use TCP with a Token UUID "159axea4-xxxx-xxxx-xxxx-xxxxxxxxxxxx" - this is a dummy token for demonstration purposes only.
Remember to replace the Token with one taken from the Log created from the earlier setup.

When a new instance of the Activity is created, a simple log event message is sent. For further information on the Android
 Activity and its life cycle, refer to the official Android developer documentation.

		import android.app.Activity;
		import android.os.Bundle;
		import com.logentries.logger.AndroidLogger;
		import java.io.IOException;

		public class MyActivity extends Activity {
			private AndroidLogger logger = null;

			/**
			 * Called when the activity is first created.
			 */
			@Override
			public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
				setContentView(R.layout.main);
				try {
					logger = AndroidLogger.createInstance(getApplicationContext(), false, false, false, null, 0, "159axea4-xxxx-xxxx-xxxx-xxxxxxxxxxxx", true, true);
				} catch (IOException e) {
					e.printStackTrace();
				}
				logger.log("MyActivity has been created");
			}
		}

The number and type of arguments of the 'AndroidLogger.createInstance' are as follows:

(Context context, boolean useHttpPost, boolean useSsl, boolean isUsingDataHub, String dataHubAddr, int dataHubPort, String token, boolean logTraceID, boolean logHostName)

Note that exceptions are generate where mutually exclusive settings collide - these are:
	"useHttpPost" and "useSsl" cannot be both true - HTTP is not available with TLS/SSL
	"useHttpPost" and "isUsingDataHub" cannot be both true - use one or the other only

- 'context' : for example, if in an Activity class, use ``getApplicationContext()``, or if in an Application class, use ``getBaseContext()``.

- 'useHttpPost' : if set true, use HTTP (note cannot be used with TLS/SSL or the Datahub)

- 'useSsl' : if set true, the data sent using the default TCP Token, will be done over an SSL Socket
 	Note that the library itself does not validate or manage TLS/SSL certificates - it will use the default TrustManager
 	and KeyManager used by the application or host.

- 'isUsingDataHub' : if set true, library will forward log events to a Datahub (requires Datahub IP Address and Port)

- 'dataHubAddr' : is a String of the IP Address of your DataHub machine.

- 'dataHubPort' : is an int of the port number of your incoming connection on your DataHub machine.
 	The default is port 10000, but this can be changed to any port by altering the /etc/leproxy/leproxyLocal.config file
 	 on your DataHub machine and restarting the leproxy daemon using "sudo service leproxy restart".

- 'token' : the Token UUID, this is unique to the log to which the log events are sent
 	This can be copied from the log in the the Logentries Account

- 'logTraceID' : if set true will return trace id in log event

- 'logHostName' : if set true will return host name in log event
