package edu.umich.si.inteco.tansuo.app.model.record;

import edu.umich.si.inteco.tansuo.app.contextmanager.ContextManager;

/**
 * Created by Armuro on 6/18/14.
 */
public class AppActivityRecord extends Record{

    private String mAppPackageName = "defaultPackage";
    private String mAppActivityName = "defaultActivity";;


    public AppActivityRecord(String appPackageName, String appActivityName) {
        this._type = ContextManager.CONTEXT_RECORD_TYPE_APPLICATION_ACTIVITY;
        this.mAppPackageName = appPackageName;
        this.mAppActivityName = appActivityName;
    }

    public String getAppPackageName() {
        return mAppPackageName;
    }

    public void setAppPackageName(String appPackageName) {
        this.mAppPackageName = appPackageName;
    }

    public String getAppActivityName() {
        return mAppActivityName;
    }

    public void setAppActivityName(String appActivityName) {
        this.mAppActivityName = appActivityName;
    }

    @Override
    public String toString() {

        String s = "";

        s+= this.getTimeString() + "\t" +
                this.getTimestamp() +  "\t"	+
                this.getType() +"\t" ;

        s+= "\t" +mAppPackageName +
            "\t" +mAppActivityName;

        s+="\n";

        return s;
    }
}

