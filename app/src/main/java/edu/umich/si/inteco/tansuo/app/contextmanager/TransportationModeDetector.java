package edu.umich.si.inteco.tansuo.app.contextmanager;

import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.List;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.data.DataHandler;
import edu.umich.si.inteco.tansuo.app.model.record.ActivityRecord;
import edu.umich.si.inteco.tansuo.app.util.LogManager;
import edu.umich.si.inteco.tansuo.app.util.ScheduleAndSampleManager;

/**
 * Created by Armuro on 7/8/14.
 */
public class TransportationModeDetector {

    private static final int STATE_STATIC = 0;
    private static final int STATE_SUSPECTING_START = 1;
    private static final int STATE_CONFIRMED = 2;
    private static final int STATE_SUSPECTING_STOP = 3;

    private static final float CONFIRM_START_ACTIVITY_THRESHOLD_IN_VEHICLE = (float) 0.6;
    private static final float CONFIRM_START_ACTIVITY_THRESHOLD_ON_FOOT = (float)0.6;
    private static final float CONFIRM_START_ACTIVITY_THRESHOLD_ON_BICYCLE =(float) 0.6;
    private static final float CONFIRM_STOP_ACTIVITY_THRESHOLD_IN_VEHICLE = (float)0.2;
    private static final float CONFIRM_STOP_ACTIVITY_THRESHOLD_ON_FOOT = (float)0.2;
    private static final float CONFIRM_STOP_ACTIVITY_THRESHOLD_ON_BICYCLE =(float) 0.2;

    private static final long WINDOW_LENGTH_START_ACTIVITY_DEFAULT = 20 * GlobalNames.MILLISECONDS_PER_SECOND;
    private static final long WINDOW_LENGTH_STOP_ACTIVITY_DEFAULT = 20 * GlobalNames.MILLISECONDS_PER_SECOND;
    private static final long WINDOW_LENGTH_START_ACTIVITY_IN_VEHICLE = 20 * GlobalNames.MILLISECONDS_PER_SECOND;
    private static final long WINDOW_LENGTH_START_ACTIVITY_ON_FOOT = 20 * GlobalNames.MILLISECONDS_PER_SECOND;
    private static final long WINDOW_LENGTH_START_ACTIVITY_ON_BICYCLE = 20 * GlobalNames.MILLISECONDS_PER_SECOND;
    private static final long WINDOW_LENGTH_STOP_ACTIVITY_IN_VEHICLE = 150 * GlobalNames.MILLISECONDS_PER_SECOND;
    private static final long WINDOW_LENGTH_STOP_ACTIVITY_ON_FOOT = 60 * GlobalNames.MILLISECONDS_PER_SECOND;
    private static final long WINDOW_LENGTH_STOP_ACTIVITY_ON_BICYCLE = 90 * GlobalNames.MILLISECONDS_PER_SECOND;

    private static final int NO_ACTIVITY_TYPE = -1;

    private static int mSuspectedStartActivityType = -1;
    private static int mSuspectedStopActivityType = -1;
    private static int mConfirmedActivityType = -1;
    private static long mSuspectTime = 0;
    private static int mCurrentState = 0;



    private static ArrayList<ActivityRecord> mActivityRecords;
    public static List<String> testList;
    /** Tag for logging. */
    private static final String LOG_TAG = "TransportationModeDetector";

    public TransportationModeDetector() {



    }
/*
    public static void test() {



        Log.d(LOG_TAG, "there are " + getActivityRecords().size() + " activity records in the test pool");
        for (int i=0 ; i<getActivityRecords().size(); i++) {

            ActivityRecord record = getActivityRecords().get(i);
            examineTransportation(record);

            //do something
        }

    }

*/
    public static int examineTransportation(ActivityRecord record) {

        Log.d (LOG_TAG, "before examineTransportation " + ScheduleAndSampleManager.getTimeString(record.getTimestamp())  + " " + getActivityNameFromType(record.getProbableActivities().get(0).getType()) +":"+  record.getProbableActivities().get(0).getConfidence());

        /*
        Log.d (LOG_TAG, "before examineTransportation the current state is " + getStateName(getCurrentState()) + " confirmedActivity type is " + getActivityNameFromType(getConfirmedActivityType()) +
                " suspectedSTartActivity " + getActivityNameFromType(getSuspectedStartActivityType()) +
                " suspect stop " + getActivityNameFromType(getSuspectedStopActivityType()) );
*/
        //if in the static state, we try to suspect new activity
        if (getCurrentState()==STATE_STATIC) {
            //getLatestActivityRecord();

            //if the detected activity is vehicle, bike or on foot, then we suspect the activity from now
            if (record.getProbableActivities().get(0).getType()== DetectedActivity.ON_BICYCLE ||
                    record.getProbableActivities().get(0).getType()== DetectedActivity.IN_VEHICLE ||
                    record.getProbableActivities().get(0).getType()== DetectedActivity.ON_FOOT ) {

                //set current state to suspect stop
                setCurrentState(STATE_SUSPECTING_START);

                //set suspected Activity type
                setSuspectedStartActivityType(record.getProbableActivities().get(0).getType());

                //set suspect time
                setSuspectTime(record.getTimestamp());

                Log.d (LOG_TAG, " examineTransportation [detected start possible activity] " + getActivityNameFromType(getSuspectedStartActivityType()) + " entering state " + getStateName(getCurrentState()) );

                LogManager.log(LogManager.LOG_TAG_ACTIVITY_RECOGNITION,
                        LogManager.LOG_TAG_PROBE_TRANSPORTATION,
                        "Suspect Start Transportation:\t" +  getActivityNameFromType(getSuspectedStartActivityType())  + "\t" + "state:" + getStateName(getCurrentState()) );
            }



        }
        else if (getCurrentState()==STATE_SUSPECTING_START) {
            boolean isTimeToConfirm = checkTimeElapseOfLatestActivityFromSuspectPoint(record.getTimestamp(), getSuspectTime(), getWindowLengh(getSuspectedStartActivityType(), getCurrentState()) );

            if (isTimeToConfirm) {

                long startTime = getWindowLengh(getSuspectedStartActivityType(), getCurrentState());
                long endTime = record.getTimestamp();
                boolean isNewTransportationModeConfirmed = confirmStartPossibleTransportation(getSuspectedStartActivityType(), getWindowData(startTime, endTime));

                if (isNewTransportationModeConfirmed) {

                    //change the state to Confirmed
                    setCurrentState(STATE_CONFIRMED);
                    //set confirmed activity type
                    setConfirmedActivityType(getSuspectedStartActivityType());
                    //no suspect
                    setSuspectedStartActivityType(NO_ACTIVITY_TYPE);

                    //set the suspect time so that other class can access it.(startTime is when we think the transportation starts)
                    setSuspectTime(startTime);

                    Log.d (LOG_TAG, " examineTransportation [confiremd start activity]  " + getActivityNameFromType(getConfirmedActivityType()) + " entering state " + getStateName(getCurrentState()) );

                    LogManager.log(LogManager.LOG_TAG_ACTIVITY_RECOGNITION,
                            LogManager.LOG_TAG_PROBE_TRANSPORTATION,
                            "Confirm Transportation:\t" +  getActivityNameFromType(getConfirmedActivityType())  + "\t" + "state:" + getStateName(getCurrentState()) );

                    return getConfirmedActivityType();
                }
                //if the suspection is wrong, back to the static state
                else {

                    //change the state to Confirmed
                    setCurrentState(STATE_STATIC);
                    //set confirmed activity type
                    setConfirmedActivityType(NO_ACTIVITY_TYPE);

                    setSuspectTime(0);

                    Log.d (LOG_TAG, " examineTransportation [cancel activity suspection], back to state " + getStateName(getCurrentState()) );

                    LogManager.log(LogManager.LOG_TAG_ACTIVITY_RECOGNITION,
                            LogManager.LOG_TAG_PROBE_TRANSPORTATION,
                            "Cancel Suspection:\t" + "state:" + getStateName(getCurrentState()) );

                    return getConfirmedActivityType();

                }
            }
        }
        //if in the confirmed state, we suspect whether users exit the activity
        else if (getCurrentState()==STATE_CONFIRMED) {
            /** if the detected activity is vehicle, bike or on foot, then we suspect the activity from now**/

            //if the latest activity is not the currently confirmed activity nor tilting nor unkown
            if (record.getProbableActivities().get(0).getType() != getConfirmedActivityType() &&
                    record.getProbableActivities().get(0).getType() != DetectedActivity.TILTING &&
                    record.getProbableActivities().get(0).getType() != DetectedActivity.UNKNOWN) {

                //set current state to suspect stop
                setCurrentState(STATE_SUSPECTING_STOP);
                //set suspected Activity type to the confirmed activity type
                setSuspectedStopActivityType(getConfirmedActivityType());
                //set suspect time
                setSuspectTime(record.getTimestamp());

                Log.d (LOG_TAG, " examineTransportation [detected stop possible activity] " + getActivityNameFromType(getSuspectedStopActivityType()) + " entering state " + getStateName(getCurrentState())  );


                LogManager.log(LogManager.LOG_TAG_ACTIVITY_RECOGNITION,
                        LogManager.LOG_TAG_PROBE_TRANSPORTATION,
                        "Suspect Stop Transportation:\t" +  getActivityNameFromType(getSuspectedStopActivityType())  + "\t" + "state:" + getStateName(getCurrentState()) );
            }
        }
        else if (getCurrentState()==STATE_SUSPECTING_STOP) {

            boolean isTimeToConfirm = checkTimeElapseOfLatestActivityFromSuspectPoint(record.getTimestamp(), getSuspectTime(), getWindowLengh(getSuspectedStopActivityType(), getCurrentState()) );

            if (isTimeToConfirm) {

                long startTime = getWindowLengh(getSuspectedStartActivityType(), getCurrentState());
                long endTime = record.getTimestamp();
                boolean isExitingTransportationMode = confirmStopPossibleTransportation(getSuspectedStopActivityType(), getWindowData(startTime, endTime));

                if (isExitingTransportationMode) {

                    LogManager.log(LogManager.LOG_TAG_ACTIVITY_RECOGNITION,
                            LogManager.LOG_TAG_PROBE_TRANSPORTATION,
                            "Stop Transportation:\t" +  getActivityNameFromType(getSuspectedStopActivityType())  + "\t" + "state:" + getStateName(getCurrentState()) );

                    //back to static
                    setCurrentState(STATE_STATIC);

                    setConfirmedActivityType(NO_ACTIVITY_TYPE);

                    setSuspectedStopActivityType(NO_ACTIVITY_TYPE);

                    //set the suspect time so that other class can access it.(startTime is when we think the transportation starts)
                    setSuspectTime(startTime);

                    Log.d (LOG_TAG, " examineTransportation [stop activity], entering state" + getStateName(getCurrentState()) );


                }

                //not exiting the confirmed activity
                else {
                    //back to static, cancel the suspection
                    setCurrentState(STATE_CONFIRMED);

                    setSuspectedStartActivityType(NO_ACTIVITY_TYPE);

                    Log.d (LOG_TAG, " examineTransportation [still maintain confirmed]" + getActivityNameFromType(getConfirmedActivityType()) + " still in the state " + getStateName(getCurrentState()));

                    LogManager.log(LogManager.LOG_TAG_ACTIVITY_RECOGNITION,
                            LogManager.LOG_TAG_PROBE_TRANSPORTATION,
                            "Cancel Suspection:\t" +  "state:" + getStateName(getCurrentState()) );
                }

                setSuspectTime(0);
            }



            //or directly enter suspecting activity: if the current record is other type of transportation mode
            if (record.getProbableActivities().get(0).getType() != getSuspectedStopActivityType() &&
                    record.getProbableActivities().get(0).getType()!=DetectedActivity.TILTING &&
                    record.getProbableActivities().get(0).getType()!=DetectedActivity.STILL &&
                    record.getProbableActivities().get(0).getType()!=DetectedActivity.UNKNOWN ) {


               // Log.d (LOG_TAG, " examineTransportation by the way, check if we can suspect start activity " + getActivityNameFromType(record.getProbableActivities().get(0).getType()));
                isTimeToConfirm = checkTimeElapseOfLatestActivityFromSuspectPoint(record.getTimestamp(), getSuspectTime(), getWindowLengh(record.getProbableActivities().get(0).getType(), STATE_SUSPECTING_START) );

                if (isTimeToConfirm) {

                    Log.d (LOG_TAG, " examineTransportation yes it's good time to confirm whether we can change the suspection for " + getActivityNameFromType(record.getProbableActivities().get(0).getType()));

                    long startTime = record.getTimestamp() - getWindowLengh(record.getProbableActivities().get(0).getType(), STATE_SUSPECTING_START) ;
                    long endTime = record.getTimestamp();
                    boolean isActuallyStartingAnotherActivity = changeSuspectingTransportation(record.getProbableActivities().get(0).getType(), getWindowData(startTime, endTime));

                    if (isActuallyStartingAnotherActivity) {

                        Log.d (LOG_TAG, " examineTransportation [interrupt suspecting stop activity] " + getActivityNameFromType(getSuspectedStopActivityType()));



                        //back to static
                        setCurrentState(STATE_SUSPECTING_START);

                        //
                       // setConfirmedActivityType(NO_ACTIVITY_TYPE);

                        setSuspectedStopActivityType(NO_ACTIVITY_TYPE);

                        setSuspectedStartActivityType(record.getProbableActivities().get(0).getType());

                        Log.d (LOG_TAG, " examineTransportation [detected start possible activity] " + getActivityNameFromType(getSuspectedStartActivityType()) + " entering state " + getStateName(getCurrentState()) );

                        //start suspecting new activity
                        setSuspectTime(record.getTimestamp());

                        LogManager.log(LogManager.LOG_TAG_ACTIVITY_RECOGNITION,
                                LogManager.LOG_TAG_PROBE_TRANSPORTATION,
                                "Suspect Start Transportation:\t" + getActivityNameFromType(getSuspectedStartActivityType()) + "\t" + "state:" + getStateName(getCurrentState()) );

                    }



                }

            }




        }

        return getConfirmedActivityType();




    }

    public static long getWindowLengh (int activityType, int state) {

        if (state==STATE_SUSPECTING_START) {

            switch (activityType) {
                case DetectedActivity.IN_VEHICLE:
                    return WINDOW_LENGTH_START_ACTIVITY_IN_VEHICLE;
                case DetectedActivity.ON_FOOT:
                    return WINDOW_LENGTH_START_ACTIVITY_ON_FOOT;
                case DetectedActivity.ON_BICYCLE:
                    return WINDOW_LENGTH_START_ACTIVITY_ON_BICYCLE;
                default:
                    return WINDOW_LENGTH_START_ACTIVITY_DEFAULT;

            }
        }
        else if (state==STATE_SUSPECTING_STOP) {

            switch (activityType) {
                case DetectedActivity.IN_VEHICLE:
                    return WINDOW_LENGTH_STOP_ACTIVITY_IN_VEHICLE;
                case DetectedActivity.ON_FOOT:
                    return WINDOW_LENGTH_STOP_ACTIVITY_ON_FOOT;
                case DetectedActivity.ON_BICYCLE:
                    return WINDOW_LENGTH_STOP_ACTIVITY_ON_BICYCLE;
                default:
                    return WINDOW_LENGTH_STOP_ACTIVITY_DEFAULT;

            }

        }else {
            return WINDOW_LENGTH_STOP_ACTIVITY_DEFAULT;
        }

    }


    public static void getLatestActivityRecord() {
        //TODO: get data from the database


    }



    public static boolean checkTimeElapseOfLatestActivityFromSuspectPoint( long lastestActivityTime, long suspectTime, long windowLenth) {

        if (lastestActivityTime - suspectTime > windowLenth)
            //wait for long enough
            return true;
        else
            //still need to wait
            return false;
    }

    private static ArrayList<ActivityRecord> getWindowData(long startTime, long endTime) {

        ArrayList<ActivityRecord> windowData = new ArrayList<ActivityRecord>();

        //TODO: get activity records from the database
        windowData = DataHandler.getActivityRecordsBetweenTimes(startTime, endTime);

        /*for testing: get data from the testData

        for (int i=0; i<getActivityRecords().size(); i++) {

            if (getActivityRecords().get(i).getTimestamp() > startTime && getActivityRecords().get(i).getTimestamp() < endTime)
                windowData.add(getActivityRecords().get(i));
        }
        */

        return windowData;
    }

    private static boolean confirmStopPossibleTransportation(int activityType, ArrayList<ActivityRecord> windowData) {

        float threshold = getConfirmStopThreshold(activityType);

        /** check if in the window data the number of the possible activity exceeds the threshold**/

        //get number of targeted data
        int count = 0;
        int inRecentCount = 0;
        for (int i=0; i<windowData.size(); i++) {

            List<DetectedActivity> detectedActivities = windowData.get(i).getProbableActivities();

            //in the recent 6 there are more than 3
            if (i >= windowData.size()-5) {
                if (detectedActivities.get(0).getType()==activityType ) {
                    inRecentCount +=1;
                }
            }

            for (int activityIndex = 0; activityIndex<detectedActivities.size(); activityIndex++) {

                //if probable activities contain the target activity, we count! (not simply see the most probable one)
                if (detectedActivities.get(activityIndex).getType()==activityType ) {
                    count +=1;
                    break;
                }
            }
        }

//        Log.d(LOG_TAG, "[confirmStoptPossibleTransportation] examineTransportation there are only " + count  +  " " + getActivityNameFromType(activityType) + " out of " + windowData.size() + " data ");

        float percentage = (float)count/windowData.size();

        if (windowData.size()!=0) {
            //if the percentage > threshold
            Log.d(LOG_TAG, "[confirmStoptPossibleTransportation] examineTransportation the percentage is  " + percentage + " the recent count is " +inRecentCount);

            if ( threshold >= percentage && inRecentCount <= 2)
                return true;
            else
                return false;

        }
        else
            //if there's no data in the windowdata, we should not confirm the possible activity
            return false;




    }

    private static boolean changeSuspectingTransportation(int activityType, ArrayList<ActivityRecord> windowData) {

        float threshold = getConfirmStartThreshold(activityType);

        /** check if in the window data the number of the possible activity exceeds the threshold**/

        int inRecentCount = 0;

        for (int i=windowData.size()-1; i>=0; i--) {

            List<DetectedActivity> detectedActivities = windowData.get(i).getProbableActivities();

            //in the recent 6 there are more than 3
            if (i >= windowData.size()-3) {
                if (detectedActivities.get(0).getType()==activityType ) {
                    inRecentCount +=1;
                }
            }


        }

        if (windowData.size()!=0) {

            //if the percentage > threshold
            Log.d(LOG_TAG, "[changeSuspectingTransportation] examineTransportation changing transportation recentCount " +inRecentCount + " within " + windowData.size()  + "  data");


            if ( inRecentCount >= 2)
                return true;
            else
                return false;

        }
        else
            //if there's no data in the windowdata, we should not confirm the possible activity
            return false;

    }


    private static boolean confirmStartPossibleTransportation(int activityType, ArrayList<ActivityRecord> windowData) {

        float threshold = getConfirmStartThreshold(activityType);

        /** check if in the window data the number of the possible activity exceeds the threshold**/

        //get number of targeted data
        int count = 0;
        int inRecentCount = 0;

        for (int i=0; i<windowData.size(); i++) {

            List<DetectedActivity> detectedActivities = windowData.get(i).getProbableActivities();

            //in the recent 6 there are more than 3
            if (i >= windowData.size()-5) {
                if (detectedActivities.get(0).getType()==activityType ) {
                    inRecentCount +=1;
                }
            }

            if (detectedActivities.get(0).getType()==activityType ) {
                count +=1;
            }


        }

      //  Log.d(LOG_TAG, "[confirmStartPossibleTransportation] there are " + count  +  " " + getActivityNameFromType(activityType) + " out of " + windowData.size() + " data ");

        if (windowData.size()!=0) {

            float percentage = (float)count/windowData.size();
            //if the percentage > threshold
           Log.d(LOG_TAG, "[confirmStartPossibleTransportation] the percentage is  " + percentage + " recentCount " +inRecentCount);

           if ( threshold <= percentage || inRecentCount >= 2)
               return true;
            else
               return false;

        }
        else
        //if there's no data in the windowdata, we should not confirm the possible activity
            return false;




    }

    private static float getConfirmStopThreshold(int activityType) {

        //TODO: different activity has different threshold

        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return CONFIRM_STOP_ACTIVITY_THRESHOLD_IN_VEHICLE;
            case DetectedActivity.ON_FOOT:
                return CONFIRM_STOP_ACTIVITY_THRESHOLD_ON_FOOT;
            case DetectedActivity.ON_BICYCLE:
                return CONFIRM_STOP_ACTIVITY_THRESHOLD_ON_BICYCLE;
            default:
                return (float) 0.5;

        }
    }

    private static float getConfirmStartThreshold(int activityType) {

        //TODO: different activity has different threshold

        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return CONFIRM_START_ACTIVITY_THRESHOLD_IN_VEHICLE;
            case DetectedActivity.ON_FOOT:
                return CONFIRM_START_ACTIVITY_THRESHOLD_ON_FOOT;
            case DetectedActivity.ON_BICYCLE:
                return CONFIRM_START_ACTIVITY_THRESHOLD_ON_BICYCLE;
            default:
                return (float) 0.5;

        }
    }

    public static void setCurrentState(int state) {
        mCurrentState = state;
    }

    public static int getCurrentState() {
        return mCurrentState;
    }

    public static long getSuspectTime() {
        return mSuspectTime;
    }

    public static void setSuspectTime(long suspectTime) {
        TransportationModeDetector.mSuspectTime = suspectTime;
    }

    public static int getSuspectedStartActivityType() {
        return mSuspectedStartActivityType;
    }

    public static void setSuspectedStartActivityType(int suspectedStartActivityType) {
        TransportationModeDetector.mSuspectedStartActivityType = suspectedStartActivityType;
    }

    public static int getSuspectedStopActivityType() {
        return mSuspectedStopActivityType;
    }

    public static void setSuspectedStopActivityType(int suspectedStopActivityType) {
        TransportationModeDetector.mSuspectedStopActivityType = suspectedStopActivityType;
    }

    public static int getConfirmedActivityType() {
        return mConfirmedActivityType;
    }

    public static void setConfirmedActivityType(int confirmedActivityType) {
        TransportationModeDetector.mConfirmedActivityType = confirmedActivityType;
    }

    public static ArrayList<ActivityRecord> getActivityRecords() {

        if (mActivityRecords==null){
            mActivityRecords = new ArrayList<ActivityRecord>();
        }
        return mActivityRecords;

    }

    public static void addActivityRecord(ActivityRecord record) {
        getActivityRecords().add(record);
    }

    public static String getStateName(int state) {

        switch(state) {
            
            case STATE_CONFIRMED:
                return "Confirmed";
            case STATE_SUSPECTING_STOP:
                return "Suspect Stop";
            case STATE_SUSPECTING_START:
                return "Suspect Start";
            case STATE_STATIC:
                return "Static";
            default:
                return "NA";
        }
    }
    
    /**
     * Map detected activity types to strings
     */
    public static String getActivityNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
            case NO_ACTIVITY_TYPE:
                return "NA";
        }
        return "unknown";
    }


    public static int getActivityTypeFromName(String activityName) {

        if (activityName.equals("in_vehicle")) {
            return DetectedActivity.IN_VEHICLE;
        }else if(activityName.equals("on_bicycle")) {
            return DetectedActivity.ON_BICYCLE;
        }else if(activityName.equals("on_foot")) {
            return DetectedActivity.ON_FOOT;
        }else if(activityName.equals("still")) {
            return DetectedActivity.STILL;
        }else if(activityName.equals("unknown")) {
            return DetectedActivity.UNKNOWN ;
        }else if(activityName.equals("tilting")) {
            return DetectedActivity.TILTING;
        }else if (activityName.equals("NA")) {
            return NO_ACTIVITY_TYPE;
        }
        else
            return DetectedActivity.UNKNOWN;
    }





}
