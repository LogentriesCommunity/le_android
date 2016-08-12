package com.logentries.logger;

import android.content.Context;

import java.io.IOException;

public class AndroidLogger {

    private static AndroidLogger instance;

    private AsyncLoggingWorker loggingWorker;

    private AndroidLogger(Context context, boolean useHttpPost, boolean useSsl, boolean isUsingDataHub, String dataHubAddr, int dataHubPort,
                          String token, boolean logHostName, boolean useEncryptedHTTP) throws IOException {
        loggingWorker = new AsyncLoggingWorker(context, useSsl, useHttpPost, isUsingDataHub, token, dataHubAddr, dataHubPort, logHostName, useEncryptedHTTP);
    }

    public static synchronized AndroidLogger createInstance(Context context, boolean useHttpPost, boolean useSsl, boolean isUsingDataHub,
                                                         String dataHubAddr, int dataHubPort, String token, boolean logHostName)
            throws IOException {
        return createInstance(context, useHttpPost, useSsl, isUsingDataHub, dataHubAddr, dataHubPort, token, logHostName, false);
    }

    public static synchronized AndroidLogger createInstance(Context context, boolean useHttpPost, boolean useSsl, boolean isUsingDataHub,
                                                            String dataHubAddr, int dataHubPort, String token, boolean logHostName, boolean useEncryptedHTTP)
            throws IOException {
        if(instance != null) {
            instance.loggingWorker.close();
        }

        instance = new AndroidLogger(context, useHttpPost, useSsl, isUsingDataHub, dataHubAddr, dataHubPort, token, logHostName, useEncryptedHTTP);
        return instance;
    }

    public static synchronized AndroidLogger getInstance() {
        if(instance != null) {
            return instance;
        } else {
            throw new IllegalArgumentException("Logger instance is not initialized. Call createInstance() first!");
        }
    }

    public void log(String message) {
        loggingWorker.addLineToQueue(message);
    }

}
