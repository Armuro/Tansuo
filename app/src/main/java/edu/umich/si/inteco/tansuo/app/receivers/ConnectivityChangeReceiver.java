package edu.umich.si.inteco.tansuo.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import edu.umich.si.inteco.tansuo.app.data.DataHandler;
import edu.umich.si.inteco.tansuo.app.data.RemoteDBHelper;
import edu.umich.si.inteco.tansuo.app.model.Schedule;
import edu.umich.si.inteco.tansuo.app.util.ScheduleAndSampleManager;

/**
 * Created by Armuro on 7/11/14.
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver{

    /** Tag for logging. */
    private static final String LOG_TAG = "ConnectivityChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(LOG_TAG, "[ConnectivityChangeReceiver] connectivity change");

        ConnectivityManager conMngr = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo wifi = conMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (wifi.isConnected()){
            Log.d(LOG_TAG, "[ConnectivityChangeReceiver] connected to wifi");

            RemoteDBHelper.syncWithRemoteDatabase();
        }

    }


}
