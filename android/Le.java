package le.android;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
/**
 * @author Mark Lacomber, marklacomber@gmail.com - 22/08/11
 * modified by Caroline Fenlon - 29/08/11
 * 	- added custom SLLSOcketFactory
 * 	- added format, upload methods
 * 	- altered publish method
 */
public class Le extends Handler {

	private SSLSocket sock;
	private OutputStream conn;
	private String m_key;
	private String m_location;
	
	/**
	 * Connects to logentries.com and uploads log events
	 * @param key account userkey
	 * @param location hostname/filename
	 */
	public Le(String key, String location)
	{
		this.m_key = key;
		this.m_location = location;
		
		try{
			this.createSocket(key, location);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create the TCP connection with Logentries.
	 * @param key userkey of account to accept log events
	 * @param location hostname/filename
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 */
	public void createSocket(String key, String location) throws IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, KeyStoreException, UnrecoverableKeyException
	{
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);

		SSLSocketFactory factory = new EasySSLSocketFactory(trustStore);
		factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Socket s = new Socket("api.logentries.com", 443);
		sock = (SSLSocket)factory.createSocket(s, "", 0, false);
		conn = sock.getOutputStream();
		String buff = "PUT /" + key + "/hosts/" + location + "/?realtime=1 HTTP/1.1\r\n";
		conn.write(buff.getBytes(), 0, buff.length());
		buff = "Host: api.logentries.com\r\n";
		conn.write(buff.getBytes(), 0, buff.length());
		buff = "Accept-Encoding: identity\r\n";
		conn.write(buff.getBytes(), 0, buff.length());
		buff = "Transfer_Encoding: chunked\r\n\r\n";
		conn.write(buff.getBytes(), 0, buff.length());
	}
	
	/**
	 * Close the connection to the logentries server
	 */
	public void close() throws SecurityException {
		try {
			conn.close();
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Flush the socket's OutputStream
	 */
	public void flush() {
		try {
			conn.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * format and upload a LogRecord
	 * @param record the LogRecord to upload
	 */
	public void publish(LogRecord record) {
		Date dateTime = new Date(record.getMillis());
		
		String MESSAGE = this.format(dateTime, record.getMessage(), record.getLevel()); 
		upload(MESSAGE);
	}
	
	/**
	 * @param date log time
	 * @param logData log message
	 * @param level log severity level
	 * @return eg. <i>Mon 29 Aug 09:06:48 +0000 2011, severity=DEBUG, 	log message</i>
	 */
	public String format(Date date, String logData, Level level) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM HH:mm:ss Z yyyy");
		String log = sdf.format(date) + ", severity=" + level.toString() + ", " + logData + "\r\n";
		return log;
	}
	
	/**
	 * Writes the formatted log event to the OutputStream of the Socket
	 * @param logData formatted event
	 */
	public void upload(String logData) {
		try {
			conn.write(logData.getBytes(), 0, logData.length());
		} catch (IOException e) {
			try{
				this.createSocket(this.m_key, this.m_location);
				conn.write(logData.getBytes(), 0, logData.length());
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (KeyManagementException e1) {
				e1.printStackTrace();
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
			} catch (CertificateException e1) {
				e1.printStackTrace();
			} catch (KeyStoreException e1) {
				e1.printStackTrace();
			} catch (UnrecoverableKeyException e1) {
				e1.printStackTrace();
			}
		}
	}
}

/**
 * custom Android SSLSocketFactory
 * @author Caroline Fenlon <carfenlon@gmail.com>
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
