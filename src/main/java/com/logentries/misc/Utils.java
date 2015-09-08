package com.logentries.misc;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import android.os.Build;
import android.util.Log;

public class Utils {

    private static final String TAG = "LogentriesAndroidLogger";

    /** Reg.ex. that is used to check correctness of HostName if it is defined by user */
    private static final Pattern HOSTNAME_REGEX = Pattern.compile("[$/\\\"&+,:;=?#|<>_* \\[\\]]");

    private static String traceID = "";
    private static String hostName = "";

    // Requires at least API level 9 (v. >= 2.3).
    static {
        try {
            traceID = computeTraceID();
        } catch(NoSuchAlgorithmException ex) {
            Log.e(TAG, "Cannot get traceID from device's properties!");
            traceID = "unknown";
        }

        try {
            hostName = getProp("net.hostname");
            if(hostName.equals("")) { // We have failed to get the real host name
                                      // so, use the default one.
                hostName = InetAddress.getLocalHost().getHostName();
            }
        }
        catch (UnknownHostException e) {
            // We cannot resolve local host name - so won't use it at all.
        }
    }

    private static String getProp(String propertyName) {

        if(propertyName == null || propertyName.isEmpty()) {
           return "";
        }

        try {
            Method getString = Build.class.getDeclaredMethod("getString", String.class);
            getString.setAccessible(true);
            return getString.invoke(null, propertyName).toString();
        } catch (Exception ex) {
            // Ignore the exception - we simply couldn't access the property;
            Log.e(TAG, ex.getMessage());
        }

        return "";
    }

    private static String computeTraceID() throws NoSuchAlgorithmException {

        String fingerprint = getProp("ro.build.fingerprint");
        String displayId = getProp("ro.build.display.id");
        String hardware = getProp("ro.hardware");
        String device = getProp("ro.product.device");
        String rilImei = getProp("ril.IMEI");

        MessageDigest hashGen = MessageDigest.getInstance("MD5");
        byte[] digest = null;
        if(fingerprint.isEmpty() & displayId.isEmpty() & hardware.isEmpty() & device.isEmpty() & rilImei.isEmpty()) {
            Log.e(TAG, "Cannot obtain any of device's properties - will use default Trace ID source.");

            Double randomTrace = Math.random() + Math.PI;
            String defaultValue = randomTrace.toString();
            randomTrace = Math.random() + Math.PI;
            defaultValue += randomTrace.toString().replace(".", "");
            // The code below fixes one strange bug, when call to a freshly installed app crashes at this
            // point, because random() produces too short sequence. Note, that this behavior does not
            // occur for the second and all further launches.
            defaultValue = defaultValue.length() >= 36 ? defaultValue.substring(2, 34) :
                    defaultValue.substring(2);

            hashGen.update(defaultValue.getBytes());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(fingerprint).append(displayId).append(hardware).append(device).append(rilImei);
            hashGen.update(sb.toString().getBytes());
        }

        digest = hashGen.digest();
        StringBuilder conv = new StringBuilder();
        for (byte b: digest) {
            conv.append(String.format("%02x", b & 0xff).toUpperCase());
        }

        return conv.toString();
    }

    public static String getTraceID() {
        return traceID;
    }

    public static String getFormattedTraceID(boolean toJSON) {
        if(toJSON) {
            return "\"TraceID\": \"" + traceID + "\"";
        }
        return "TraceID=" + traceID;
    }

    public static String getHostName() {
        return hostName;
    }

    public static String getFormattedHostName(boolean toJSON) {
        if(toJSON) {
            return "\"Host\": \"" + hostName + "\"";
        }
        return "Host=" + hostName;
    }

    /** Formats given message to make it suitable for ingestion by Logentris endpoint.
     *  If isUsingHttp == true, the method produces such structure:
        {"event": {"Host": "SOMEHOST", "Timestamp": 12345, "DeviceID": "DEV_ID", "Message": "MESSAGE"}}

        If isUsingHttp == false the output will be like this:
        Host=SOMEHOST Timestamp=12345 DeviceID=DEV_ID MESSAGE
     * @param message
     * @param logHostName - if set to true - "Host"=HOSTNAME parameter is appended to the message.
     * @param isUsingHttp
     *
     * @return
     */
    public static String formatMessage(String message, boolean logHostName, boolean isUsingHttp) {
        StringBuilder sb = new StringBuilder();

        if(isUsingHttp) {
            // Add 'event' structure.
            sb.append("{\"event\": {");
        }

        if(logHostName) {
            sb.append(Utils.getFormattedHostName(isUsingHttp));
            sb.append(isUsingHttp ? ", " : " ");
        }

        sb.append(Utils.getFormattedTraceID(isUsingHttp)).append(" ");
        sb.append(isUsingHttp ? ", " : " ");

        long timestamp = System.currentTimeMillis(); // Current time in UTC in milliseconds.
        if(isUsingHttp) {
            sb.append("\"Timestamp\": ").append(Long.toString(timestamp)).append(", ");
        } else {
            sb.append("Timestamp=").append(Long.toString(timestamp)).append(" ");
        }

        // Append the event data
        if(isUsingHttp) {
            sb.append("\"Message\": \"").append(message);
            sb.append("\"}}");
        } else {
            sb.append(message);
        }

        return sb.toString();
    }

    public static boolean checkValidUUID(String uuid){
        if(uuid != null && !uuid.isEmpty()) {
            try {

                UUID u = UUID.fromString(uuid);
                return true;

            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }

    public static boolean checkIfHostNameValid(String hostName) {
        return !HOSTNAME_REGEX.matcher(hostName).find();
    }

    public static String[] splitStringToChunks(String source, int chunkLength) {
        if(chunkLength < 0) {
            throw new IllegalArgumentException("Chunk length must be greater or equal to zero!");
        }

        int srcLength = source.length();
        if(chunkLength == 0 || srcLength <= chunkLength) {
            return new String[] { source };
        }

        ArrayList<String> chunkBuffer = new ArrayList<String>();
        int splitSteps = srcLength / chunkLength + (srcLength % chunkLength > 0 ? 1 : 0);

        int lastCutPosition = 0;
        for(int i = 0; i < splitSteps; ++i) {

            if(i < splitSteps - 1) {
                // Cut out the chunk of the requested size.
                chunkBuffer.add(source.substring(lastCutPosition, lastCutPosition + chunkLength));
            }
            else
            {
                // Cut out all that left to the end of the string.
                chunkBuffer.add(source.substring(lastCutPosition));
            }

            lastCutPosition += chunkLength;
        }

        return chunkBuffer.toArray(new String[chunkBuffer.size()]);
    }
}