package le.android;

/*
 * Logentries HTTP Client
 * Copyright 2011 Logentries, JLizard
 * Caroline Fenlon <carfenlon@gmail.com>
 */

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;

import java.security.KeyStore;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

/**
 * Logentries HTTP Client
 * Copyright 2011 Logentries, JLizard
 * Client for uploading content to the Logentries server.
 * @author Caroline Fenlon <carfenlon@gmail.com>
 */
public class LEHttpClient extends DefaultHttpClient{
	private static HttpClient client;
	
	private static int response;
	
	public static int getResponse() {
		return response;
	}
	
	/**
	 * @param userkey 
	 * @param host
	 * @param logname
	 * @param requestData
	 * @return HTTP Response status code: 200 if request successful<br/>
	 * -1 indicates exception in attempting request
	 */
	public static int putRequest(String userkey, String host, String logname, String requestData) {
		HttpResponse response;
		try {
			String path = "/" + userkey + "/hosts/" + host + "/" + logname + "/";
			URI address = new URI("https", null, "api.logentries.com", 443, path, "realtime=1", null);
			
			HttpPut httpPut = new HttpPut(address);
			HttpEntity entity = new StringEntity(requestData);
			httpPut.setEntity(entity);
			
			// Perform the request and return the response
			response = client.execute(httpPut);
			
			return response.getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * @return HttpClient to perform log uploads
	 */
	public static HttpClient startClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new EasySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
			
			client = new DefaultHttpClient(ccm, params);
			return client;
		} catch (Exception e) {
			client = new DefaultHttpClient();
			return client;
		}
	}
}

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
