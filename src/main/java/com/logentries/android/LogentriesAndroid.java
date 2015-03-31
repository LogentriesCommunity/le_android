package com.logentries.android;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.*;
import android.util.Log;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLSocket;
import android.os.Build;

/**
 *
 * VERSION 2.1
 */
public class LogentriesAndroid extends Handler {

	/*
	 * Constants
	 */
	/** Current Version Number. */
	static final String VERSION = "2.0";
	/** Size of the internal event queue. */
	static final int QUEUE_SIZE = 32768;
	/** Logentries API server address */
	static final String LE_API = "api.logentries.com";
	/** Logentries Port number for TLS Token-Based Logging */
	static final int LE_PORT = 20000;
	/** Tag for Logentries Debug Messages to LogCat */
	static final String TAG = "Logentries";
	/** Minimal delay between attempts to reconnect in milliseconds. */
	static final int MIN_DELAY = 100;
	/** Maximal delay between attempts to reconnect in milliseconds. */
	static final int MAX_DELAY = 10000;
	/** UTF-8 output character set. */
	static final Charset UTF8 = Charset.forName( "UTF-8");
	/** Error message displayed when invalid API key is detected. */
	static final String INVALID_TOKEN = "It appears your Token UUID parameter is incorrect!";
	/*
	 * Fields
	 */
	/** Destination token */
	String m_token;
	/** Debug flag */
	boolean debug;
	/** Indicator if the socket appender has been started. */
	boolean started;
	
	/** DataHub IP Address */
	String m_datahub_address;
	/** DataHub Port - default 10000 */
	int m_datahub_port;
	/** DataHub Enabled */
	boolean datahub_enabled = false;
	String m_customID="";
	

	/** Asynchronous socket appender */
	SocketAppender appender;
	/** Message queue. */
	ArrayBlockingQueue<String> queue;

	/*
	 * Internal classes
	 */

	/**
	 * Asynchronous over the socket appender
	 *
	 * @author Mark Lacomber
	 *
	 */
	class SocketAppender extends Thread {
		/** Socket connection. */
		Socket s;
		/** SSLSocket connection. */
		SSLSocket socket;
		/** SSLSocketFactory for sslsocket. */
		SSLSocketFactory socketFactory;
		/** Output log stream. */
		OutputStream stream;
		/** Random number generator for delays between reconnection attempts. */
		final Random random = new Random();

		/**
		 * Initializes the socket appender
		 */
		SocketAppender(){
			super("Logentries Socket Appender");
			// Don't block shut down
			setDaemon(true);
		}

		 
		/**
		 * Opens connection to Logentries
		 *
		 * @throws IOException
		 * @throws CertificateException
		 */
		void openConnection() throws IOException {
			try{
				dbg( "Reopening connection to Logentries API server");

				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);

				socketFactory = new EasySSLSocketFactory(trustStore);
				socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				
				if (datahub_enabled)
				{
					s = new Socket(m_datahub_address, m_datahub_port);
					s.setTcpNoDelay(true);
					stream = s.getOutputStream();
				}
				else
				{
					s = new Socket(LE_API, LE_PORT);
					socket = (SSLSocket)socketFactory.createSocket(s, "", 0, true);
					socket.setTcpNoDelay(true);
					stream = socket.getOutputStream();
				}
				
				dbg( "Connection established");
			} catch (KeyStoreException e) {
				// Ignored
			} catch (UnrecoverableKeyException e) {
				// Ignored
			} catch (CertificateException e) {
				// Ignored
			} catch (NoSuchAlgorithmException e) {
				// Ignored
			} catch (KeyManagementException e) {
				// Ignored
			}
		}

		/**
		 * Tries to open connection to Logentries until it succeeds
		 *
		 * @throws InterruptedException
		 */
		void reopenConnection() throws InterruptedException {
			// Close the previous connection
			closeConnection();

			//Try to open the connection until we get through
			int root_delay = MIN_DELAY;
			while(true)
			{
				try{
					openConnection();

					// Success, leave
					return;
				} catch (IOException e) {
					// Get information if in debug mode
					dbg( "Unable to connect to Logentries");
					e.printStackTrace();
				}

				// Wait between connection attempts
				root_delay *= 2;
				if (root_delay > MAX_DELAY)
					root_delay= MAX_DELAY;
				int wait_for = root_delay + random.nextInt( root_delay);
				dbg( "Waiting for " + wait_for + "ms");
				Thread.sleep( wait_for);
			}
		}

		/**
		 * Closes the connection. Ignore errors
		 */
		void closeConnection() {
			if (stream != null){
				try{
					stream.close();
				} catch (IOException e){
					// Nothing we can do here
				}
			}
			stream = null;
			if (socket != null) {
				try{
					socket.close();
				} catch (IOException e){
					// Nothing we can do here
				}
			}
			socket = null;
		}

		/**
		 * Initializes the connection and starts to log
		 */
	 @Override
		public void run(){
			try{
				// Open connection
				reopenConnection();

				// Send data in queue
				while (true) {
					// Take data from queue
					String data = m_token + queue.take();
					data = data.trim().replace('\n', '\u2028') + '\n';

					byte[] msg = data.getBytes(UTF8);

					// Send data, reconnect if needed
					while (true){
						try{
                            stream.write( msg);
                            stream.flush();
						} catch (IOException e) {
							// Reopen the lost connection
                            reopenConnection();
							continue;
						}
						break;
					}
				}
			} catch (Exception e){
				// We got interupted, stop
				dbg( "Asynchronous socket writer interrupted");
			}

			closeConnection();
		}
	}

	/**
	 * custom Android SSLSocketFactory
	 */
	class EasySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public EasySSLSocketFactory(KeyStore keystore) throws NoSuchAlgorithmException,
			KeyManagementException, KeyStoreException, UnrecoverableKeyException {

			super(keystore);

			TrustManager manager = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			sslContext.init(null, new TrustManager[]{ manager }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port,
				boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port,
					autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	public LogentriesAndroid( String token, boolean debug)
	{
		this.m_token = token;
		this.debug = debug;
		
		queue = new ArrayBlockingQueue<String>( QUEUE_SIZE);

		appender = new SocketAppender();
	}
	
	
	public LogentriesAndroid(String datahub_address, int datahub_port, boolean debug)
	{
		if (!datahub_address.equals("") || datahub_address!=null)
		{
			this.debug = debug;
			this.datahub_enabled=true;
			this.m_token="";

			// set DataHub IP Address here
			m_datahub_address = datahub_address;
						
			// set DataHub port here 		
			m_datahub_port = datahub_port;	
		}
				
		queue = new ArrayBlockingQueue<String>( QUEUE_SIZE);

		appender = new SocketAppender();
	}
	
	public LogentriesAndroid(String datahub_address, int datahub_port, boolean debug, String customID)
	{
		if (!datahub_address.equals("") || datahub_address!=null)
		{	
			this.debug = debug;
			this.datahub_enabled=true;
			this.m_token="";
			this.m_customID = customID;
			
			// set DataHub IP Address here
			m_datahub_address = datahub_address;
			
			// set DataHub port here 		
			m_datahub_port = datahub_port;
		}
				
		queue = new ArrayBlockingQueue<String>( QUEUE_SIZE);

		appender = new SocketAppender();
	}
	

	
	
	/**
	 * Checks that key and location are set.
	 */
	public boolean checkCredentials() {
		if (m_token == null)
			return false;

		//Quick test to see if LOGENTRIES_TOKEN is a valid UUID
		UUID u = UUID.fromString(m_token);
		if (!u.toString().equals(m_token))
		{
			dbg(INVALID_TOKEN);
			return false;
		}

		return true;
	}

	/**
	 * format and upload a LogRecord
	 * @param record the LogRecord to upload
	 */
		public void publish(LogRecord record) {
		Log.w(TAG, "publish");
		Date dateTime = new Date(record.getMillis());
		String MESSAGE = this.format(dateTime, record.getMessage(), record.getLevel());
		
		// Append message with deviceID (Requires API 9 or above
		MESSAGE = "deviceID="+Build.SERIAL +" " + MESSAGE;
		
		// Append message with customID
		if (!m_customID.equals("")){
			MESSAGE = "customID="+m_customID +" " + MESSAGE;
		}
	
		if (!datahub_enabled){
									
			if (!started && checkCredentials()) {
				dbg( "Starting Logentries asynchronous socket appender");
				appender.start();
				started = true;
				}
			}
		else if (!started){
			appender.start();
			started=true;
		}
		// Try to offer data to the queue
		boolean is_full = !queue.offer( MESSAGE);

		// If it's full, remove the latest item and try again
		if (is_full){
			queue.poll();
			queue.offer( MESSAGE);
		}
	}

	/**
	 * @param date log time
	 * @param logData log message
	 * @param level log severity level
	 * @return eg. <i>Mon 29 Aug 09:06:48 +0000 2011, severity=DEBUG, 	log message</i>
	 */
	public String format(Date date, String logData, Level level) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM HH:mm:ss Z yyyy");
		String log = sdf.format(date) + ", severity=" + level.toString() + ", " + logData + "\n";
		return log;
	}

	public void dbg(String debugMessage)
	{
		if (debug)
		{
			Log.e(TAG, debugMessage);
		}
	}

	@Override
	/**
	 * Interrupts the background logging thread
	 */
	public void close() {
		// Interrupt the background thread
		appender.interrupt();
	}

	@Override
	public void flush() {
		// Don't need to do anything here
	}

	public void upload(String toUpload) {
		Log.w(TAG, "upload");
		LogRecord record = new LogRecord(Level.INFO, toUpload);
		publish(record);
	}
}
