package edu.umich.si.inteco.tansuo.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import edu.umich.si.inteco.tansuo.app.GlobalNames;

/**
 * Created by Armuro on 7/16/14.
 */
public class PreferenceHelper {

    private static Context mContext;

    public PreferenceHelper (Context context) {

        this.mContext = context;
    }

    public static void setPreferenceValue (String property, String value) {

        if (getPreference()!=null) {
            SharedPreferences.Editor editor = getPreference().edit();
            editor.putString(property,value);
            editor.commit();
        }

    }

    public static void setPreferenceValue (String property, boolean value) {

        if (getPreference()!=null) {
            SharedPreferences.Editor editor = getPreference().edit();
            editor.putBoolean(property,value);
            editor.commit();
        }

    }

    public static String getPreferenceString (String property, String defaultValue) {

        if (getPreference()!=null) {
            return  getPreference().getString(property, defaultValue);
        }
        else
            return defaultValue;
    }

    public static boolean getPreferenceBoolean (String property, boolean defaultValue) {

        if (getPreference()!=null) {
            return  getPreference().getBoolean(property,defaultValue);
        }
        else
            return defaultValue;
    }

    public static SharedPreferences getPreference() {

        if (mContext!=null) {
            return mContext.getApplicationContext().getSharedPreferences(GlobalNames.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        else {
            return null;
        }

    }

}
