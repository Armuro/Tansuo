package edu.umich.si.inteco.tansuo.app.data;

/**
 * Created by Armuro on 7/7/14.
 */

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.contextmanager.ContextExtractor;
import edu.umich.si.inteco.tansuo.app.util.DatabaseNameManager;
import edu.umich.si.inteco.tansuo.app.util.FileHelper;
import edu.umich.si.inteco.tansuo.app.util.RecordingAndAnnotateManager;
import edu.umich.si.inteco.tansuo.app.util.ScheduleAndSampleManager;


public class RemoteDBHelper {

    /** Single instance of our HttpClient */
    private static DefaultHttpClient mHttpClient;
    /** The time it takes for client to timeout */
    public static final int HTTP_TIMEOUT = 10000; // millisecond
    public static final int SOCKET_TIMEOUT = 20000; // millisecond
    public static final int MINIMUM_UPDATE_FREQUENCY = 30*GlobalNames.MILLISECONDS_PER_MINUTE; // 30 minutes

    public static final int POST_DATA_TYPE_JSON = 0;
    public static final int POST_DATA_TYPE_STRING = 1;
    public static final int POST_DATA_TYPE_FILE = 2;

    private static long lastBackgroundRecordingUpdateTime=0;
    private static long lastLogfileUpdateTime=0;
    private static long lastSessionUpdateTime=0;

    public static final String DATA_TYPE_BACKGROUND_RECORDING = "background_recording";
    public static final String DATA_TYPE_SESSION_RECORDING = "session_recording";
    public static final String DATA_TYPE_PHONE_LOG = "phone_log";



    /** Tag for logging. */
    private static final String LOG_TAG = "RemoteDBHelper";

    public RemoteDBHelper(){
    }


/*
    private static DefaultHttpClient getSSLHttpClient(boolean isTLS, InputStream trustStoreInputStream, String trustStorePsw)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
        DefaultHttpClient client = null;
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        Scheme http = new Scheme("http", PlainSocketFactory.getSocketFactory(), 80);
        schemeRegistry.register(http);
        if(isTLS) {
            KeyStore trustKeyStore = null;
            char[] trustStorePswCharArray = null;
            if(trustStorePsw!=null) {
                trustStorePswCharArray = trustStorePsw.toCharArray();
            }
            trustKeyStore = KeyStore.getInstance("BKS");
            trustKeyStore.load(trustStoreInputStream, trustStorePswCharArray);
            SSLSocketFactory sslSocketFactory = null;
            sslSocketFactory = new SSLSocketFactory(trustKeyStore);
            Scheme https = new Scheme("https", sslSocketFactory, 5002);
            schemeRegistry.register(https);
        }
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, HTTP_TIMEOUT);
        ClientConnectionManager clientConnectionManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
        client = new DefaultHttpClient(clientConnectionManager, httpParams);
        return client;
    }
*/

    /***
     * trust all hsot....
     */
    private static void trustAllHosts() {

        X509TrustManager easyTrustManager = new X509TrustManager() {

            public void checkClientTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            public void checkServerTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }


        };

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {easyTrustManager};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };


    /**
     * Synchronize with the database. If the interval from the last update time is too short, we will abort.
     */
    public static void syncWithRemoteDatabase() {

        //if we just updated the database, then we don't need to update again right away

        //if (ContextExtractor.isWifiConnected()) {

            Log.d(LOG_TAG, "[syncWithRemoteDatabase] the wifi is connected, we can submit the data");
            long now = ScheduleAndSampleManager.getCurrentTimeInMillis();

            if (now- getLastBackgroundRecordingUpdateTime() >= MINIMUM_UPDATE_FREQUENCY ) {

                queryRemoteDB(DATA_TYPE_BACKGROUND_RECORDING);
            }

            if (now- getLastLogfileUpdateTime() >= MINIMUM_UPDATE_FREQUENCY) {

                queryRemoteDB(DATA_TYPE_PHONE_LOG);
            }

            if (now- getLastSessionUpdateTime() >= MINIMUM_UPDATE_FREQUENCY) {

                queryRemoteDB(DATA_TYPE_SESSION_RECORDING);
            }
        //}

    }

    public static String postLogFiles(Date lastSyncDate){

        //we will get the latest uploaded log file and then decide which log file to upload
        File logFile[] = FileHelper.getLogDirectory().listFiles();

        ///compare the lastSyncDate and the date of the file to determine whether we should upload the file
        for (int i=0; i<logFile.length; i++) {

            //the date of the file..
            int eindex = logFile[i].getPath().indexOf(".txt");
            String fileDateStr = logFile[i].getPath().substring(eindex-10, eindex);

            SimpleDateFormat sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW_DAY);
            try {
                Date fileDate = sdf.parse(fileDateStr);

                //if the file is later than the last sync date time, upload the file
                if (fileDate.getTime() > lastSyncDate.getTime()) {
                    //post the file..
                    Log.d(LOG_TAG, "[test post file] upload " + logFile[i].getPath());

                    String[] param = {GlobalNames.WEB_SERVICE_URL_POST_FILES, logFile[i].getPath()} ;
                    //post the session document
                    new HttpAsyncPostFileTask().execute(param);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }


        }


        String res = null;

        try {
            //res = HTTPClient.executeHttpPost(GlobalNames.WEB_SERVICE_URL_POST_SESSION);
        }catch(Exception e){
            Log.e(LOG_TAG, "data base errror:" + e.getMessage() + ""+"");
            //FileHelper.WriteFile(pathfilename, new Date().toString() + "\t"+ LOG_TAG + "\t" + e.getMessage() + ""  + ":" + e.getCause().toString()   + "\r\n");
        }

        return res;


    }



    public static void postBackgroundRecordingDocuments(long lastSyncHourTime) {

        ArrayList<JSONObject> documents = RecordingAndAnnotateManager.getBackgroundRecordingDocuments(lastSyncHourTime);

        if (documents!=null) {
            for (int i= 0; i<documents.size(); i++) {
                String json = documents.get(i).toString();
                new HttpAsyncPostJsonTask().execute(GlobalNames.WEB_SERVICE_URL_POST_BACKGROUND_RECORDING, json, DATA_TYPE_BACKGROUND_RECORDING);
            }
        }

    }



    public static void postSessionDocuments(long lastSyncHourTime) {

        ArrayList<JSONObject> documents = RecordingAndAnnotateManager.getSessionecordingDocuments(lastSyncHourTime);

        if (documents!=null) {
            for (int i= 0; i<documents.size(); i++) {
                String json = documents.get(i).toString();
                new HttpAsyncPostJsonTask().execute(GlobalNames.WEB_SERVICE_URL_POST_SESSION, json, DATA_TYPE_SESSION_RECORDING);
            }
        }

    }


    public static String postSessionDocument(JSONObject sessionDocument){

        String json = sessionDocument.toString();

        //post the session document
        new HttpAsyncPostJsonTask().execute(GlobalNames.WEB_SERVICE_URL_POST_SESSION, json, DATA_TYPE_SESSION_RECORDING);

        String res = null;

        try {
            //res = HTTPClient.executeHttpPost(GlobalNames.WEB_SERVICE_URL_POST_SESSION);
        }catch(Exception e){
            Log.e(LOG_TAG, "data base errror:" + e.getMessage() + ""+"");
            //FileHelper.WriteFile(pathfilename, new Date().toString() + "\t"+ LOG_TAG + "\t" + e.getMessage() + ""  + ":" + e.getCause().toString()   + "\r\n");
        }

        return res;

    }


    public static void queryRemoteDB(String queryType) {
            new HttpAsyncQuery().execute(GlobalNames.WEB_SERVICE_URL_QUERY_LAST_SYNC_BACKGROUND_RECORDING, queryType);
    }



    public static void queryLastFileDay(String address){


        //if today is 11th, we check whether files until 10th have been uploaded
        //get lastSynhour by query the MongoDB


        InputStream inputStream = null;
        String result = "";
         /*
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);

        //create query json
        JSONObject obj = new JSONObject();
        try {
            obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE, DATA_TYPE_PHONE_LOG);
            obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String json = obj.toString();

        //set json to StringEntity
        StringEntity se = null;
        try {
            se = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //set httpPost Entity
        httpPost.setEntity(se);
        //Set headers to inform server about the type of the content
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        //Execute POST request to the given URL
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpclient.execute(httpPost);
            if (httpResponse != null) {
                inputStream = httpResponse.getEntity().getContent();
                if(inputStream != null){
                    result = convertInputStreamToString(inputStream);
                    Log.d(LOG_TAG, "[queryLastFileDay] the query result is " + result);
                }
                else
                    result = "Did not work!";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        try {

            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(LOG_TAG, "[queryLastFileDay] connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                Log.d(LOG_TAG, "[queryLastFileDay] [using https]");
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }


            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.connect();


            JSONObject obj = new JSONObject();
            try {
                obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE, DATA_TYPE_PHONE_LOG);
                obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String json = obj.toString();
            OutputStreamWriter wr= new OutputStreamWriter(conn.getOutputStream());
            wr.write(obj.toString());
            wr.close();

            int responseCode = conn.getResponseCode();
            inputStream = conn.getInputStream();
            result = convertInputStreamToString(inputStream);
            Log.d(LOG_TAG, "[queryLastFileDay] the query responseCode "  + responseCode + " result is " + result);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }





        //based on the result (e.g.2014-07-10 00:00:00), we select the file to upload..
        SimpleDateFormat sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW_HOUR_MIN);
        try {
            Date date = sdf.parse(result);
            Log.d(LOG_TAG, "[queryLastFileDay] the date of the result is" + date);
            postLogFiles(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }


    public static void queryLastSyncSession(String address) {

        //get lastSynhour by query the MongoDB
        InputStream inputStream = null;
        String result = "";
        /*
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);

        //create query json
        JSONObject obj = new JSONObject();
        try {
            obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE, DATA_TYPE_SESSION_RECORDING);
            obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String json = obj.toString();

        //set json to StringEntity
        StringEntity se = null;
        try {
            se = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //set httpPost Entity
        httpPost.setEntity(se);
        //Set headers to inform server about the type of the content
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        //Execute POST request to the given URL
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpclient.execute(httpPost);
            if (httpResponse != null) {
                inputStream = httpResponse.getEntity().getContent();
                if(inputStream != null){
                    result = convertInputStreamToString(inputStream);
                    Log.d(LOG_TAG, "[queryLastSyncSession] the query result is " + result);
                }
                else
                    result = "Did not work!";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
*/


        try {

            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(LOG_TAG, "[queryLastFileDay] connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                Log.d(LOG_TAG, "[queryLastFileDay] [using https]");
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }


            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.connect();


            JSONObject obj = new JSONObject();
            try {
                obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE, DATA_TYPE_SESSION_RECORDING);
                obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String json = obj.toString();
            OutputStreamWriter wr= new OutputStreamWriter(conn.getOutputStream());
            wr.write(obj.toString());
            wr.close();

            int responseCode = conn.getResponseCode();
            inputStream = conn.getInputStream();
            result = convertInputStreamToString(inputStream);
            Log.d(LOG_TAG, "[queryLastFileDay] the query responseCode "  + responseCode + " result is " + result);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //get result


        try {
            JSONObject resultJson = new JSONObject(result);
            String queryType = resultJson.getString(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE);
            Log.d(LOG_TAG, "[queryLastSyncSession] query type " + queryType);
            if (resultJson.has(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_HAS_DOCUMENT)) {
                boolean hasDocument = Boolean.parseBoolean(resultJson.getString(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_HAS_DOCUMENT));
                Log.d(LOG_TAG, "[queryLastSyncSession] the session recording has document? " + hasDocument);

                //if the database has no background recording yet, we should submit all background recording
                if (hasDocument) {

                    //submit background recording until the recent hour
                    String lastSyncStartTimeStr = resultJson.getString(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_START_TIME);

                    Log.d(LOG_TAG, "[queryLastSyncSession] the startime of the last session is " + lastSyncStartTimeStr);

                    SimpleDateFormat sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW);
                    try {
                        Date lastSynhHour = sdf.parse(lastSyncStartTimeStr);
                        postSessionDocuments(lastSynhHour.getTime());

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }


                }
                //no document
                else {

                    postSessionDocuments(0);

                }


            }

        } catch (JSONException e) {
            e.printStackTrace();
        }



    }


    /**
     *
     * @param address
     */
    public static void queryLastBackgroundRecordingLogSyncHour(String address){

        //get lastSynhour by query the MongoDB
        InputStream inputStream = null;
        String result = "";

        /*
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);

        //create query json
        JSONObject obj = new JSONObject();
        try {
            obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE, DATA_TYPE_BACKGROUND_RECORDING);
            obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String json = obj.toString();

        //set json to StringEntity
        StringEntity se = null;
        try {
            se = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //set httpPost Entity
        httpPost.setEntity(se);
        //Set headers to inform server about the type of the content
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        //Execute POST request to the given URL
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpclient.execute(httpPost);
            if (httpResponse != null) {
                inputStream = httpResponse.getEntity().getContent();
                if(inputStream != null){
                    result = convertInputStreamToString(inputStream);
                    Log.d(LOG_TAG, "[queryLastBackgroundRecordingLogSyncHour] the query result is " + result);
                }
                else
                    result = "Did not work!";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
*/



        try {

            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(LOG_TAG, "[queryLastFileDay] connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                Log.d(LOG_TAG, "[queryLastFileDay] [using https]");
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }


            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.connect();


            JSONObject obj = new JSONObject();
            try {
                obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE, DATA_TYPE_BACKGROUND_RECORDING);
                obj.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String json = obj.toString();
            OutputStreamWriter wr= new OutputStreamWriter(conn.getOutputStream());
            wr.write(obj.toString());
            wr.close();

            int responseCode = conn.getResponseCode();
            inputStream = conn.getInputStream();
            result = convertInputStreamToString(inputStream);
            Log.d(LOG_TAG, "[queryLastFileDay] the query responseCode "  + responseCode + " result is " + result);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //result
        try {
            JSONObject resultJson = new JSONObject(result);
            String queryType = resultJson.getString(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DATA_TYPE);
            Log.d(LOG_TAG, "[queryLastBackgroundRecordingLogSyncHour] query type " + queryType);
            if (resultJson.has(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_HAS_DOCUMENT)) {
                boolean hasDocument = Boolean.parseBoolean(resultJson.getString(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_HAS_DOCUMENT));
                Log.d(LOG_TAG, "[queryLastBackgroundRecordingLogSyncHour] the background recording has document? " + hasDocument);

                //if the database has no background recording yet, we should submit all background recording
                if (hasDocument) {

                    //submit background recording until the recent hour
                    String lastSyncHourStr = resultJson.getString(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_LAST_SYNC_HOUR_TIME);

                    Log.d(LOG_TAG, "the last sync hour is " + lastSyncHourStr);

                    SimpleDateFormat sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW);
                    try {
                        Date lastSynhHour = sdf.parse(lastSyncHourStr);
                        postBackgroundRecordingDocuments(lastSynhHour.getTime());

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }


                }
                //no document
                else {

                    postBackgroundRecordingDocuments(0);

                }


            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }



    private static HttpClient getHttpClient(){
        if (mHttpClient == null){
            try {
                mHttpClient = new DefaultHttpClient();

                final HttpParams params = mHttpClient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, HTTP_TIMEOUT);
                HttpConnectionParams.setSoTimeout(params, HTTP_TIMEOUT);
                ConnManagerParams.setTimeout(params, HTTP_TIMEOUT);
            }catch (Exception e){
                Log.e(LOG_TAG, e.getMessage()+"");
            }

        }
        return mHttpClient;
    }



    public static String postFile(String address, String filepath) throws IOException{

        Log.d(LOG_TAG, "postFile");

/*
        String result = "";

        try {

            // Client-side HTTP transport library
            HttpClient httpClient = new DefaultHttpClient();

            // using POST method
            HttpPost httpPostRequest = new HttpPost(url);
            File file = new File(filepath);

            Log.d(LOG_TAG, "postFile post file:" + file.getAbsolutePath());

            FileBody bin = new FileBody(file);

            MultipartEntityBuilder multiPartEntityBuilder = MultipartEntityBuilder.create();

            multiPartEntityBuilder.addPart("file", bin);
            multiPartEntityBuilder.addPart("device", new StringBody(GlobalNames.DEVICE_ID));

            httpPostRequest.setEntity(multiPartEntityBuilder.build());

            // Execute POST request to the given URL
            HttpResponse httpResponse = null;
            InputStream inputStream = null;

                httpResponse = httpClient.execute(httpPostRequest);
                inputStream = httpResponse.getEntity().getContent();

                if (inputStream != null) {
                    result = convertInputStreamToString(inputStream);

                    //successfully uploaded. set the last update time
                    setLastLogfileUpdateTime(ContextExtractor.getCurrentTimeInMillis());

                } else
                    result = "Did not work!";

        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }

        return result;
*/
        String response=null;
        InputStream is = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        //create URL and connection
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        File file = new File(filepath);
        String boundary = "==================================";
        int responseCode = -1; // Keeps track of any response codes we might get.

        try {

            Log.d(LOG_TAG, "[queryLastFileDay] connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                Log.d(LOG_TAG, "[queryLastFileDay] [using https]");
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }


            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);

            /* setRequestProperty */
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+ boundary);
//            conn.connect();

            DataOutputStream ds = new DataOutputStream(conn.getOutputStream());

            //parameter
            // Send parameter #1
            ds.writeBytes(twoHyphens + boundary + lineEnd);
            ds.writeBytes("Content-Disposition: form-data; name=\"device\"" + lineEnd + lineEnd);
            ds.writeBytes(GlobalNames.DEVICE_ID + lineEnd);

            //file
            ds.writeBytes(twoHyphens + boundary + lineEnd);
            ds.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + file.getName() +"\"" + lineEnd);
            ds.writeBytes(lineEnd);

            // create a buffer of maximum size
            FileInputStream fStream = new FileInputStream(file);

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1*1024*1024;

            bytesAvailable = fStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...

            bytesRead = fStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0)
            {
                ds.write(buffer, 0, bufferSize);
                bytesAvailable = fStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fStream.read(buffer, 0, bufferSize);
            }


            // send multipart form data necesssary after file data...

            ds.writeBytes(lineEnd);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            /* close streams */
            fStream.close();
            ds.flush();
            ds.close();


            Log.d(LOG_TAG, "postFile post file:" + file.getAbsolutePath());

            // Starts the query

            responseCode = conn.getResponseCode();
            is = conn.getInputStream();
            response = convertInputStreamToString(is);

            Log.d(LOG_TAG, "[postFile] The response is: " + responseCode + " : " + response);

        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return response;
    }


    public static void requestEmailFromServer (JSONObject data) {

        Log.d(LOG_TAG, "[requestEmailFromServer] posting email" + data.toString() );

        String json  = data.toString();

       // Log.d(LOG_TAG, "[requestEmailFromServer] posting email" + json );
        new HttpAsyncPostEmailJSONTask().execute(GlobalNames.WEB_SERVICE_URL_REQUEST_SENDING_EMAIL, json);

    }


    public static String postEmailBodyJSON (String address, String json) {
        InputStream inputStream = null;
        String result = "";

        try {

            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(LOG_TAG, "[queryLastFileDay] connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                Log.d(LOG_TAG, "[queryLastFileDay] [using https]");
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }


            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.connect();

            OutputStreamWriter wr= new OutputStreamWriter(conn.getOutputStream());
            wr.write(json);
            wr.close();

            int responseCode = conn.getResponseCode();
            inputStream = conn.getInputStream();
            result = convertInputStreamToString(inputStream);
            Log.d(LOG_TAG, "[queryLastBackgroundRecordingLogSyncHour] the query result is " + result);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);

        Log.d(LOG_TAG, "[postEmailBodyJSON] posting email\t" + json );

        //set json to StringEntity
        StringEntity se = null;
        try {
            se = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //set httpPost Entity
        httpPost.setEntity(se);
        //Set headers to inform server about the type of the content
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        //Execute POST request to the given URL
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpclient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //receive response as inputStream
        if (httpResponse != null) {
            try {
                inputStream = httpResponse.getEntity().getContent();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 10. convert inputstream to string
        if(inputStream != null)
            try {
                result = convertInputStreamToString(inputStream);

                Log.d(LOG_TAG, "receiving the result from requesting emails " + result);


            } catch (IOException e) {
                e.printStackTrace();
            }
        else
            result = "Did not work!";
*/

        return  result;

    }




    public static String postJSON (String address, String json, String dataType) {

        InputStream inputStream = null;
        String result = "";

        try {

            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(LOG_TAG, "[postJSON] connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                Log.d(LOG_TAG, "[postJSON] [using https]");
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }


            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.connect();

            OutputStreamWriter wr= new OutputStreamWriter(conn.getOutputStream());
            wr.write(json);
            wr.close();

            int responseCode = conn.getResponseCode();
            inputStream = conn.getInputStream();
            result = convertInputStreamToString(inputStream);
            Log.d(LOG_TAG, "[postJSON] the result is " + result);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);

        Log.d(LOG_TAG, "posting " + dataType + "\t" + json );

        //set json to StringEntity
        StringEntity se = null;
        try {
            se = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //set httpPost Entity
        httpPost.setEntity(se);
        //Set headers to inform server about the type of the content
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        //Execute POST request to the given URL
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpclient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //receive response as inputStream
        if (httpResponse != null) {
            try {
                inputStream = httpResponse.getEntity().getContent();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 10. convert inputstream to string
        if(inputStream != null)
            try {
                result = convertInputStreamToString(inputStream);

                //set last update time
                if (dataType.equals(DATA_TYPE_BACKGROUND_RECORDING)){
                    Log.d(LOG_TAG, "receiving the result from inserting document: " + result);
                    Log.d(LOG_TAG, "postJSON updated the last update time of background reocrding " + ContextExtractor.getCurrentTimeInMillis());
                    setLastBackgroundRecordingUpdateTime(ContextExtractor.getCurrentTimeInMillis());
                }

                else if (dataType.equals(DATA_TYPE_SESSION_RECORDING)){
                    Log.d(LOG_TAG, "receiving the result from inserting document: " + result);
                    Log.d(LOG_TAG, "postJSON updated the last update time of session reocrding " +ContextExtractor.getCurrentTimeInMillis() );
                    setLastSessionUpdateTime(ContextExtractor.getCurrentTimeInMillis());

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        else
            result = "Did not work!";
*/

        return  result;

    }


    /** process result **/
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{

        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }


    //use HTTPAsyncTask to perform query from the database
    private static class HttpAsyncQuery extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String url = params[0];
            String queryTarget = params[1];

            if (queryTarget.equals(DATA_TYPE_BACKGROUND_RECORDING)) {
                Log.d(LOG_TAG, "going to query background recording");
                queryLastBackgroundRecordingLogSyncHour(url);
            }
            else if (queryTarget.equals(DATA_TYPE_PHONE_LOG)) {

                Log.d(LOG_TAG, "going to query log");
                queryLastFileDay(url);

            }else if (queryTarget.equals(DATA_TYPE_SESSION_RECORDING)) {

                Log.d(LOG_TAG, "going to query session");
                queryLastSyncSession(url);
                //query session and submit session
            }

            return "result";
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            Log.d(LOG_TAG, "[HttpAsyncQuery] the result is " + result);



        }


    }


    //use HTTPAsyncTask to post data
    private static class HttpAsyncPostJsonTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String result=null;
            String url = params[0];
            String data = params[1];
            String dataType = params[2];

            postJSON(url, data, dataType);

            return result;
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            Log.d(LOG_TAG, "get http post result" + result);

        }


    }


    //use HTTPAsyncTask to post data
    private static class HttpAsyncPostEmailJSONTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String result=null;
            String url = params[0];
            String data = params[1];

            postEmailBodyJSON(url, data);

            return result;
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            Log.d(LOG_TAG, "get http request email result" + result);

        }


    }



    //use HTTPAsyncTask to post file
    private static class HttpAsyncPostFileTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String result=null;
            String url = params[0];
            String filePath = params[1];

            try {
                postFile(url, filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(LOG_TAG, "[onPostExecute] get http post result" + result);

        }

    }



    public static long getLastBackgroundRecordingUpdateTime() {
        return lastBackgroundRecordingUpdateTime;
    }

    public static void setLastBackgroundRecordingUpdateTime(long lastBackgroundRecordingUpdateTime) {
        RemoteDBHelper.lastBackgroundRecordingUpdateTime = lastBackgroundRecordingUpdateTime;
    }

    public static long getLastLogfileUpdateTime() {
        return lastLogfileUpdateTime;
    }

    public static void setLastLogfileUpdateTime(long lastLogfileUpdateTime) {
        RemoteDBHelper.lastLogfileUpdateTime = lastLogfileUpdateTime;
    }

    public static long getLastSessionUpdateTime() {
        return lastSessionUpdateTime;
    }

    public static void setLastSessionUpdateTime(long lastSessionUpdateTime) {
        RemoteDBHelper.lastSessionUpdateTime = lastSessionUpdateTime;
    }
}
