package edu.umich.si.inteco.tansuo.app.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.activities.ListRecordingActivity;
import edu.umich.si.inteco.tansuo.app.contextmanager.ContextExtractor;
import edu.umich.si.inteco.tansuo.app.contextmanager.ContextManager;
import edu.umich.si.inteco.tansuo.app.data.DataHandler;
import edu.umich.si.inteco.tansuo.app.data.LocalDBHelper;
import edu.umich.si.inteco.tansuo.app.model.Annotation;
import edu.umich.si.inteco.tansuo.app.model.AnnotationSet;
import edu.umich.si.inteco.tansuo.app.model.Session;
import edu.umich.si.inteco.tansuo.app.model.SessionDocument;
import edu.umich.si.inteco.tansuo.app.model.Task;

/**
 * Created by Armuro on 6/17/14.
 */
public class RecordingAndAnnotateManager {


    /**Constant for annotation propoerties**/
    public static final String ANNOTATION_PROPERTIES_ANNOTATION = "Annotation";
    public static final String ANNOTATION_PROPERTIES_ID = "Id";
    public static final String ANNOTATION_PROPERTIES_NAME= "Name";
    public static final String ANNOTATION_PROPERTIES_START_TIME = "Start_time";
    public static final String ANNOTATION_PROPERTIES_END_TIME = "End_time";
    public static final String ANNOTATION_PROPERTIES_IS_ENTIRE_SESSION = "Entire_session";
    public static final String ANNOTATION_PROPERTIES_CONTENT = "Content";
    public static final String ANNOTATION_PROPERTIES_TAG = "Tag";

    public static final int BACKGOUND_RECORDING_SESSION_ID = 1;

    //visualization type
    public static final String ANNOTATION_VISUALIZATION_NONE = "none";
    public static final String ANNOTATION_VISUALIZATION_TYPE_LOCATION = "location";

    public static final String ANNOTATION_REVIEW_RECORDING_FLAG = "review_recording";

    //Annotateion mode (for annotate action)
    public static final String ANNOTATE_MODE_MANUAL = "manual";
    public static final String ANNOTATE_MODE_AUTO = "auto";

    //the recording type of the annotation
    public static final String ANNOTATE_RECORDING_NEW = "new";
    public static final String ANNOTATE_RECORDING_BACKGROUND = "background";

    //review recording
    public static final String ANNOTATE_REVIEW_RECORDING_NONE= "none";
    //review all previous recordings
    public static final String ANNOTATE_REVIEW_RECORDING_ALL = "all";
    //review recent recordings (within 24 hours)
    public static final String ANNOTATE_REVIEW_RECORDING_RECENT = "recent";
    //review the latest recorded recording
    public static final String ANNOTATE_REVIEW_RECORDING_LATEST = "latest";

    /** Tag for logging. */
    private static final String LOG_TAG = "RecordingAndAnnotateManager";

    private static Context mContext;
    private static LocalDBHelper mLocalDBHelper;
    private static ArrayList<Session> mCurRecordingSessions;

    public RecordingAndAnnotateManager(Context context) {
        this.mContext = context;
        mLocalDBHelper = new LocalDBHelper(mContext, GlobalNames.TEST_DATABASE_NAME);
        mCurRecordingSessions = new ArrayList<Session>();

        //add the background recording to the curRecordingSession
        setupBackgroundRecordingEnvironment();
    }

    /**
     *
     * @param annotationSet
     * @param sessionId
     */
    public static void addAnnotationToSession(AnnotationSet annotationSet, int sessionId) {

        //get session by id and then set annotation set
        Log.d(LOG_TAG, "[addAnnotationToSession] going to get session  " + sessionId);

        Session session = getCurRecordingSession(sessionId);

        if (session!=null){
            session.setAnnotationSet(annotationSet);
        }

        //also update the database
        updateAnnotationInDatabase(annotationSet, sessionId);
    }


    public static void setNoficiationIdToSession(int notificationId, int sessionId){

        Session session = getCurRecordingSession(sessionId);

        if (session!=null)
           session.setOngoingNotificationId(notificationId);

    }

    /**
     *
     * @param session
     */
    public static void saveSessionToDataBase(Session session) {
        long rowId = mLocalDBHelper.insertSessionTable(session, DatabaseNameManager.SESSION_TABLE_NAME);
        Log.d(LOG_TAG, "[saveSessionToDataBase] saving session " + session.getId() + " to the database");

    }

    public static Session getSessionFromLocalDataBase(int sessionId){

        Session session = null;

        //TODO: implement the method of query session by Id from the database
       String result = mLocalDBHelper.querySession(sessionId).get(0); //there is only one session that will be returned because we query by id

        return session;
    }

    /**
     *
     * @param annotationSet
     * @param sessionId
     */
    public static void updateAnnotationInDatabase(AnnotationSet annotationSet, int sessionId){
        mLocalDBHelper.updateSessionAnnotation(sessionId, DatabaseNameManager.SESSION_TABLE_NAME, annotationSet);
    }

    /**
     *
     * @param sessionId
     * @return
     */
    public static Session getCurRecordingSession(int sessionId) {

     //   Log.d(LOG_TAG, " [getCurRecordingSession] tyring to search session by id" + sessionId);


        for (int i=0; i<mCurRecordingSessions.size(); i++){
     //       Log.d(LOG_TAG, " [getCurRecordingSession] looping to " + i + "th session of which the id is " + mCurRecordingSessions.get(i).getId());

            if (mCurRecordingSessions.get(i).getId()==sessionId){
                return mCurRecordingSessions.get(i);
            }
        }
        return null;
    }

    /**
     *
     * @param sessionId
     * @return
     */
    public static int removeRecordingSession(int sessionId) {

        for (int i=0; i<mCurRecordingSessions.size(); i++){
            if (mCurRecordingSessions.get(i).getId()==sessionId){
                mCurRecordingSessions.remove(i);
                return i;
            }
        }
        return -1;
    }

    public static boolean isSessionPaused(int sessionId) {

        Session session = getCurRecordingSession(sessionId);
        //Log.d(LOG_TAG, " [isSessionPaused] session " + sessionId + " pause is " + session.isPaused());
        return session.isPaused();
    }


    public static void startListRecordingActivity(String reviewMode) {

        if (reviewMode.equals(RecordingAndAnnotateManager.ANNOTATE_REVIEW_RECORDING_NONE)) {
            return ;
        }

        else {
            Bundle bundle = new Bundle();

            //indicate which session
            bundle.putString(ActionManager.ACTION_PROPERTIES_ANNOTATE_REVIEW_RECORDING, reviewMode);
            Intent intent = new Intent(mContext, ListRecordingActivity.class);
            intent.putExtras(bundle);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mContext.startActivity(intent);
        }

    }

    public static void addCurRecordingSession(Session session) {

        //add to the list
        mCurRecordingSessions.add(session);

        //if the session is not background recording, we add it ti the database (there should only be one background recording in the db
        //which has been added when the app first launches
        if (session.getId()!=GlobalNames.BACKGOUND_RECORDING_SESSION_ID)
            saveSessionToDataBase(session);
    }

    public static ArrayList<Session> getCurRecordingSessions() {
        return mCurRecordingSessions;
    }

    public static void setCurRecordingSessions(ArrayList<Session> curRecordingSessions) {
        RecordingAndAnnotateManager.mCurRecordingSessions = curRecordingSessions;
    }

    public static Session getSession (int sessionId) {

        ArrayList<String> res =  mLocalDBHelper.querySession(sessionId);
        Session session = null;

        for (int i=0; i<res.size() ; i++) {

            String sessionStr = res.get(i);
            String[] separated = sessionStr.split(GlobalNames.DELIMITER);

            int id = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_SESSION_ID]);
            int taskId = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_SESSION_TASK_ID]);
            long startTime = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_SESSION_START_TIME]);


            long endTime = 0;
            //the session could be still ongoing..so we need to check where's endTime
            if (!separated[DatabaseNameManager.COL_INDEX_SESSION_END_TIME].equals("null")){
                endTime = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_SESSION_END_TIME]);
            }


            JSONObject annotationSetJSON = null;
            JSONArray annotateionSetJSONArray = null;
            try {
                annotationSetJSON = new JSONObject(separated[DatabaseNameManager.COL_INDEX_SESSION_ANNOTATION_SET]);
                annotateionSetJSONArray = annotationSetJSON.getJSONArray(ANNOTATION_PROPERTIES_ANNOTATION);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, " [getSessions] id " + id + " startTime" + startTime + " end time " + endTime+ " annotateionSetJSONArray " +annotateionSetJSONArray );

            session = new Session(id, startTime, taskId );
            session.setEndTime(endTime);
            //get annotationset
            if (annotateionSetJSONArray!=null){
                AnnotationSet annotationSet =  toAnnorationSet(annotateionSetJSONArray);
                session.setAnnotationSet(annotationSet);
            }


        }
        return session;
    }


    public static ArrayList<String> getRecordsInBackgroundRecording(String tableName){

        //test query the background recording session
        ArrayList<String> res = LocalDBHelper.queryRecordsInSession(tableName,BACKGOUND_RECORDING_SESSION_ID);

        Log.d(LOG_TAG,"[see background recording records]" + res);

        return res;
    }

    //get sessions recorded today
    public  static ArrayList<Session> getRecentSessions() {

        ArrayList<Session> sessions = new ArrayList<Session>();

        //from bed time to now
        //end time is now
        long queryEndTime = ScheduleAndSampleManager.getCurrentTimeInMillis();
        //start time = bedTime
        long queryStartTime = ScheduleAndSampleManager.getLastEndOfBedTimeInMillis();

        Log.d(LOG_TAG, " [getRecentSessions] going to query session between " + ScheduleAndSampleManager.getTimeString(queryStartTime) + " and " + ScheduleAndSampleManager.getTimeString(queryEndTime) );

        ArrayList<String> res =  mLocalDBHelper.querySessionsBetweenTimes(queryStartTime, queryEndTime);

        //we start from 1 instead of 0 because the 1st session is the background recording. We will skip it.
        for (int i=1; i<res.size() ; i++) {

            String sessionStr = res.get(i);
            String[] separated = sessionStr.split(GlobalNames.DELIMITER);

            int id = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_SESSION_ID]);
            int taskId = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_SESSION_TASK_ID]);
            long startTime = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_SESSION_START_TIME]);

            long endTime = 0;
            //the session could be still ongoing..so we need to check where's endTime
            if (!separated[DatabaseNameManager.COL_INDEX_SESSION_END_TIME].equals("null")){
                endTime = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_SESSION_END_TIME]);
            }


            JSONObject annotationSetJSON ;
            JSONArray annotateionSetJSONArray = null;
            try {
                annotationSetJSON = new JSONObject(separated[DatabaseNameManager.COL_INDEX_SESSION_ANNOTATION_SET]);
                annotateionSetJSONArray = annotationSetJSON.getJSONArray(ANNOTATION_PROPERTIES_ANNOTATION);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, " [getRecentSessions] id " + id + " startTime" + startTime + " end time " + endTime);

            Session session = new Session(id, startTime, taskId );
            session.setEndTime(endTime);

            //get annotationset
            if (annotateionSetJSONArray!=null){
                AnnotationSet annotationSet =  toAnnorationSet(annotateionSetJSONArray);
                session.setAnnotationSet(annotationSet);
            }

            sessions.add(session);
        }

        Log.d(LOG_TAG, " [getRecentSessions] tje resuslt is " + res );

        return sessions;

    }

    public static ArrayList<Session> getAllSessions() {

        ArrayList<Session> sessions = new ArrayList<Session>();

        //get all sessions from the local database

        ArrayList<String> res =  mLocalDBHelper.querySessions();


        Log.d(LOG_TAG, " [getAllSessions] tje resuslt is " + res );

        //we start from 1 instead of 0 because the 1st session is the background recording. We will skip it.
        for (int i=1; i<res.size() ; i++) {

            String sessionStr = res.get(i);
            String[] separated = sessionStr.split(GlobalNames.DELIMITER);

            int id = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_SESSION_ID]);
            int taskId = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_SESSION_TASK_ID]);
            long startTime = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_SESSION_START_TIME]);

            long endTime = 0;
            //the session could be still ongoing..so we need to check where's endTime
            if (!separated[DatabaseNameManager.COL_INDEX_SESSION_END_TIME].equals("null")){
                endTime = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_SESSION_END_TIME]);
            }


            JSONObject annotationSetJSON ;
            JSONArray annotateionSetJSONArray = null;
            try {
                annotationSetJSON = new JSONObject(separated[DatabaseNameManager.COL_INDEX_SESSION_ANNOTATION_SET]);
                annotateionSetJSONArray = annotationSetJSON.getJSONArray(ANNOTATION_PROPERTIES_ANNOTATION);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, " [getAllSessions] id " + id + " startTime" + startTime + " end time " + endTime);

            Session session = new Session(id, startTime, taskId );
            session.setEndTime(endTime);

            //get annotationset
            if (annotateionSetJSONArray!=null){
                AnnotationSet annotationSet =  toAnnorationSet(annotateionSetJSONArray);
                session.setAnnotationSet(annotationSet);
            }

            sessions.add(session);
        }

       // Log.d(LOG_TAG, " [getAllSessions] in the end there are " + sessions.size() + " sessions " );


        return sessions;
    }

    public static AnnotationSet toAnnorationSet(JSONArray annotationJSONArray) {

        AnnotationSet annotationSet = new AnnotationSet();
        ArrayList<Annotation> annotations = new ArrayList<Annotation>();

        for (int i=0 ; i<annotationJSONArray.length(); i++){

            JSONObject annotationJSON = null;
            try {
                Annotation annotation = new Annotation();
                annotationJSON = annotationJSONArray.getJSONObject(i);

                String content = annotationJSON.getString(ANNOTATION_PROPERTIES_CONTENT);
                annotation.setContent(content);

                JSONArray tagsJSONArray = annotationJSON.getJSONArray(ANNOTATION_PROPERTIES_TAG);

                for (int j=0; j<tagsJSONArray.length(); j++){

                    String tag = tagsJSONArray.getString(j);
                    annotation.addTag(tag);
                    Log.d(LOG_TAG, "[toAnnorationSet] the content is " + content +  " tag " + tag);
                }

                annotations.add(annotation);

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        annotationSet.setAnnotations(annotations);

        Log.d(LOG_TAG, "[toAnnorationSet] the annotationSet has  " + annotationSet.getAnnotations().size() + " annotations ");
        return annotationSet;

    }




    public static AnnotationSet getAnnotationFromDatabase(int sessionId){

        AnnotationSet annotationSet = null;

        //get result from the database
        ArrayList<String> sessionResults = new ArrayList<String>();

       // sessionResults = mLocalDBHelper.querySessions(sessionId);


        return annotationSet;
    }

    /**
     * This function is just for creating environment for background recording
     * **/

    public void setupBackgroundRecordingEnvironment() {

        //Create a task and a session for background recording

        //if the task and the session for background recording has been setup, then return
        Session session = new Session(ContextExtractor.getCurrentTimeInMillis());
        session.setId(BACKGOUND_RECORDING_SESSION_ID);
        addCurRecordingSession(session);

        Task task = new Task(GlobalNames.BACKGOUND_RECORDING_TASK_ID,
                GlobalNames.BACKGOUND_RECORDING_TASK_NAME,
                GlobalNames.BACKGOUND_RECORDING_TASK_DESCRIPTION,
                GlobalNames.BACKGOUND_RECORDING_NO_STUDY_ID);

        session.setTaskId(task.getId());

        //check database for tasks
        ArrayList<String> res = mLocalDBHelper.queryTasks();
        //if the database is empty, we need to store the background recording session into the database

        //for the labeling study, if there are existing configurations, we remove it and insert new ones...
        //TODO: remove this part after we can update the configuration from the database...
        if (res.size()>0) {
            //remove the configurations..
            mLocalDBHelper.removeTasks();
            res = mLocalDBHelper.queryTasks();
            Log.d(LOG_TAG, "[loadConfiguration] there are " + res.size() + " tasks in the database");

        }
        if (res.size()==0){
            //insert task for background recording into the database
            mLocalDBHelper.insertTaskTable(task, DatabaseNameManager.TASK_TABLE_NAME);

        }

        //if the database is empty, we need to store the background recording session into the database
       // Log.d(LOG_TAG, " [setupBackgroundRecordingEnvironment] checking sessions...now there are " + mLocalDBHelper.querySessionCount() + " sessions");
        if (mLocalDBHelper.querySessionCount()==0){
            //insert session for background recording into the database
            saveSessionToDataBase(session);

        }

    }



    public static ArrayList<String> generateHourKeysForSessionDocument(long startTime, long endTime) {


        //get the hour to see how many sections we have in records
        SimpleDateFormat sdf_hour = new SimpleDateFormat(GlobalNames.DATE_FORMAT_HOUR);

//        2014-05-25 14:00:00 -0700

        ArrayList<String> keys = new ArrayList<String>();

        int startHour = Integer.parseInt(ScheduleAndSampleManager.getTimeString(startTime, sdf_hour));
        int endHour = Integer.parseInt(ScheduleAndSampleManager.getTimeString(endTime,sdf_hour));
        Log.d (LOG_TAG, "[generateHourKeysForSessionDocument] startTime " + startTime + " the hour is " + startHour + " endTime " + endTime +  " the hour is " + endHour);

        //create hour key
        TimeZone tz = TimeZone.getDefault();
        Calendar startCal = Calendar.getInstance(tz);
        startCal.setTimeInMillis(startTime);

        //get the date of now: the first month is Jan:0
        int year = startCal.get(Calendar.YEAR);
        int month = startCal.get(Calendar.MONTH) + 1;
        int day = startCal.get(Calendar.DAY_OF_MONTH);
        int hour = startCal.get(Calendar.HOUR_OF_DAY);

        //no minute. only hour
        startCal.set(year, month-1,day, hour, 0,0);

        //create the first key using startTime (only the hour)
        String firstKey = ScheduleAndSampleManager.getTimeString(startCal.getTimeInMillis());
        keys.add(firstKey);
        Log.d (LOG_TAG, "[generateHourKeysForSessionDocument] the firstkey is" + firstKey);


        Calendar cal = Calendar.getInstance(tz);

        //create new hour keys starting from the next hour.
        for (int i=startHour+1; i<=endHour; i++){

            //no minute. only hour
            cal.set(year, month-1, day, i, 0,0);
            String newKey = ScheduleAndSampleManager.getTimeString(cal.getTimeInMillis());
            keys.add(newKey);
            Log.d (LOG_TAG, "[generateHourKeysForSessionDocument] the new key is " + newKey );
        }



        return keys;
    }


    /**
     * "records":
     [
     {
     "timestamp_hour": "2014-05-25 14:00:00 -0700",
     "location":
     {
     "45": {
     "1": {"lat":42.121, "lng":118.12}, "4": {"lat":42.121, "lng":118.12}

     },
     "46": {

     "2": {"lat":42.121, "lng":118.12}, "5": {"lat":42.121, "lng":118.12}
     }
     },
     "activity":{
     "45": {
     "1": {"activity":"in_vehicle", "confidence":44}, "4": {"activity":"in_vehicle", "confidence":54}

     },
     "46": {

     "23": {"activity":"on_foot", "confidence":64}, "54": {"activity":"still", "confidence":54}
     }
     }
     }

     ]

     /*/

    /**based on the last sync hour, we determine what sessions to post **/
    public static  ArrayList<JSONObject> getSessionecordingDocuments(long lastSyncHourTime) {

        ArrayList<JSONObject> documents = new ArrayList<JSONObject>();

        //find all sessions of which the startTime is later than the lastSynchourtime

        ArrayList<Session> sessions = getAllSessions();

        for (int i=0; i<sessions.size(); i++) {

            if (sessions.get(i).getStartTime() > lastSyncHourTime) {

                Log.d(LOG_TAG, "[getSessionecordingDocuments] the session " + sessions.get(i).getId() + " should be posted");

                JSONObject document = getSessionDocument((int)sessions.get(i).getId());

                documents.add(document);

            }
         }

        return documents;

    }

    public static ArrayList<JSONObject> getBackgroundRecordingDocuments(long lastSyncHourTime) {

        ArrayList<JSONObject> documents = new ArrayList<JSONObject>();

        //time range is from the last SyncHour to the most recent complete hour
        long now = ContextExtractor.getCurrentTimeInMillis();

        long startTime =0;
        long endTime  =0;
        //there's no backgrounding documents existing in the server
        if (lastSyncHourTime==0) {

            startTime = getSession(BACKGOUND_RECORDING_SESSION_ID).getStartTime();
            //produce hour time

            TimeZone tz = TimeZone.getDefault();
            Calendar startCal = Calendar.getInstance(tz);
            startCal.setTimeInMillis(startTime);

            //get the date of now: the first month is Jan:0
            int year = startCal.get(Calendar.YEAR);
            int month = startCal.get(Calendar.MONTH) + 1;
            int day = startCal.get(Calendar.DAY_OF_MONTH);
            int hour = startCal.get(Calendar.HOUR_OF_DAY);


            startCal.set(year, month-1,day, hour, 0,0);

            startTime = startCal.getTimeInMillis();
            endTime = startTime + GlobalNames.MILLISECONDS_PER_HOUR;
            Log.d (LOG_TAG, "[getBackgroundRecordingDocuments] no backgorund recording yet, startTime " + ScheduleAndSampleManager.getTimeString(startTime) + " - " + ScheduleAndSampleManager.getTimeString(endTime) );

        }
        //there are specific lastSynchourTime
        else {
            //if the last sync hour in the database is 9, we start from 10, because 9 indicates that 9:00-10:00 has been stored. So we start from 10-11
            startTime = lastSyncHourTime+ GlobalNames.MILLISECONDS_PER_HOUR;
            endTime = startTime + GlobalNames.MILLISECONDS_PER_HOUR;
            Log.d (LOG_TAG, "[getBackgroundRecordingDocuments] get lastSynchour, startTime " + ScheduleAndSampleManager.getTimeString(startTime) + " - " + ScheduleAndSampleManager.getTimeString(endTime) );
        }


        //for that many hours, we generate each hour to generate Background recoridng document
        //getLogDocument(long startHourTime, long endHourTime)

        while (endTime <now) {

            JSONObject document= getBackgroundRecordingDocument(startTime, endTime);
            documents.add(document);
            startTime = endTime;
            endTime += GlobalNames.MILLISECONDS_PER_HOUR;
        }

        return documents;

    }


     public static JSONObject getBackgroundRecordingDocument(long startTime, long endTime) {

         Log.d (LOG_TAG, "[getBackgroundRecordingDocument] going to get background recording from " + ScheduleAndSampleManager.getTimeString(startTime) + " to " + ScheduleAndSampleManager.getTimeString(endTime));

         //we will generate Background recording jSON basedon the 1st hour and the lasthour
         JSONObject document  = new JSONObject();

         //get data from the database

         SimpleDateFormat sdf_now = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW);

         JSONObject hourJSON = new JSONObject();

         try {

             SimpleDateFormat sdf_id = new SimpleDateFormat(GlobalNames.DATE_FORMAT_FOR_ID);
             document.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_ID, GlobalNames.DEVICE_ID +"-"+ScheduleAndSampleManager.getTimeString(startTime, sdf_id) );
             document.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);
             document.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_TIMESTAMP_HOUR, ScheduleAndSampleManager.getTimeString(startTime));
         } catch (JSONException e) {
             e.printStackTrace();
         }

         for (int i=0; i<ContextManager.RECORD_TYPE_LIST.size(); i++) {

             //get data from the database
             ArrayList<String> res = DataHandler.getDataBySession(RecordingAndAnnotateManager.BACKGOUND_RECORDING_SESSION_ID,
                     ContextManager.RECORD_TYPE_LIST.get(i), startTime, endTime);

             //so far we're not sure what "minute" key will be used later. So we just create sixty JSONObject. Later we will only add the JSONObjects that are not null
             //to the final JSONObject
             JSONObject[] minuteJSONArray = new JSONObject[60];
             /** make it a jSON object **/

             //this JSON will add non-null minuteJSONObject to it
             JSONObject recordTypeJSON = new JSONObject();

             //result for a recordType
             for (int j = 0; j < res.size(); j++) {

                 //each record
                 String recordStr = res.get(j);
                 String[] separated = recordStr.split(GlobalNames.DELIMITER);
                 //Log.d (LOG_TAG, "[getBackgroundRecordingDocument] recordStr" + recordStr );
                 /** based on the time of record assign to the right key **/

                 long timestamp = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_RECORD_TIMESTAMP_LONG]);

                 SimpleDateFormat sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_HOUR_MIN_SECOND);
                 String timeStr = ScheduleAndSampleManager.getTimeString(timestamp, sdf);
                 String[] timeparts = timeStr.split(":");

                 //get minute and second
                 String hour = timeparts[0];
                 String min = timeparts[1];
                 String second = timeparts[2];


                 //when we know which minute this record should be located in, we check if the corresponding JSONObject in minuteJSONArray has been initialized.
                 //if not, we initialize it.
                 if (minuteJSONArray[(Integer.parseInt(min))] == null) {
                     minuteJSONArray[(Integer.parseInt(min))] = new JSONObject();
                 }

                 //create "second" key
                 JSONObject secondRecordJSON = createSecondRecordJSONByRecordType(separated, ContextManager.RECORD_TYPE_LIST.get(i));

                 //add the secondJSON to the minuteJSON
                 try {
                     minuteJSONArray[(Integer.parseInt(min))].put(second, secondRecordJSON);
                   //  Log.d (LOG_TAG, "[getBackgroundRecordingDocument] hour "+ hour  + " min " + min + " second " + second + " the secondJSON " + secondRecordJSON);

                 } catch (JSONException e) {
                     e.printStackTrace();
                 }

             }

             //add all minuteJSON to the recordTypeJSON
             for(int minuteJSONIndex = 0; minuteJSONIndex < minuteJSONArray.length; minuteJSONIndex++) {

                 if (minuteJSONArray[minuteJSONIndex]!=null ) {

                     try {
                         recordTypeJSON.put( (minuteJSONIndex)+"" , minuteJSONArray[minuteJSONIndex]);
                     } catch (JSONException e) {
                         e.printStackTrace();
                     }
                 }
             }

             //HourJSOn add recordTypeJSON
             try {
                 hourJSON.put(ContextManager.getSensorTypeName(ContextManager.RECORD_TYPE_LIST.get(i)), recordTypeJSON );
                // Log.d (LOG_TAG, "getBackgroundRecordingDocument the hourJSON is " + hourJSON);


                 document.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_RECORDS, hourJSON);


             } catch (JSONException e) {
                 e.printStackTrace();
             }

         }

         Log.d (LOG_TAG, "getBackgroundRecordingDocument document " + document);
         return document;
     }

    public static JSONObject getSessionDocument(int sessionId) {

        SessionDocument sessionDocument = new SessionDocument(sessionId);

        //get meta data of the session
        Session session = getSession(sessionId);

        JSONObject sessionJSON = new JSONObject();
        JSONArray allRecordJSON = new JSONArray();

        long sessionStartTime = session.getStartTime();
        long sessionEndTime = session.getEndTime();

        //we get a list of keys for hour sections

        ArrayList<String> hourKeys = generateHourKeysForSessionDocument(sessionStartTime, sessionEndTime);

        //Log.d (LOG_TAG, "[getSessionDocument] the hour keys are " + hourKeys );

        SimpleDateFormat sdf_now = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW);
        long startTime = 0, endTime = 0;

       //the highest level structure is hour
       for (int indexOfhourKeys = 0; indexOfhourKeys<hourKeys.size(); indexOfhourKeys++){

           JSONObject hourJSON = new JSONObject();

           try {
               //put the hour into timestamp_hour
               hourJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_TIMESTAMP_HOUR, hourKeys.get(indexOfhourKeys));
               //generate start and end time to get records in the session
               Date startTimeDate = sdf_now.parse(hourKeys.get(indexOfhourKeys));
               startTime = startTimeDate.getTime();
               endTime = startTime + GlobalNames.MILLISECONDS_PER_HOUR;
           } catch (JSONException e) {
               e.printStackTrace();
           } catch (ParseException e) {
               e.printStackTrace();
           }


           //for each hour we get session data in that hour
           for (int i=0; i<session.getRecordTypes().size(); i++) {

//               Log.d (LOG_TAG, "[getSessionDocument] the" + i + " record type is ======================" + ContextManager.getSensorTypeName(session.getRecordTypes().get(i)) + "==============================");
//               Log.d (LOG_TAG, "[getSessionDocument] get session " + sessionId + "'s records between" +  ScheduleAndSampleManager.getTimeString(startTime) + " - " +   ScheduleAndSampleManager.getTimeString(endTime)  );

               //get result of the record type in the session given a startTime and an endTime of each hour
               ArrayList<String> res = DataHandler.getDataBySession(sessionId,session.getRecordTypes().get(i),startTime, endTime );

               //so far we're not sure what "minute" key will be used later. So we just create sixty JSONObject. Later we will only add the JSONObjects that are not null
               //to the final JSONObject
               JSONObject[] minuteJSONArray= new JSONObject[60];
               /** make it a jSON object **/

                //this JSON will add non-null minuteJSONObject to it
               JSONObject recordTypeJSON = new JSONObject();

               //result for a recordType
               for (int j=0; j<res.size(); j++){

                   //each record
                   String recordStr = res.get(j);
                   String[] separated = recordStr.split(GlobalNames.DELIMITER);
                  // Log.d (LOG_TAG, "[getSessionDocument] recordStr" + recordStr );
                   /** based on the time of record assign to the right key **/

                   long timestamp = Long.parseLong(separated[DatabaseNameManager.COL_INDEX_RECORD_TIMESTAMP_LONG]);

                   SimpleDateFormat sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_HOUR_MIN_SECOND);
                   String timeStr = ScheduleAndSampleManager.getTimeString(timestamp,sdf);
                   String[] timeparts = timeStr.split(":");

                   //get minute and second
                   String hour = timeparts[0];
                   String min = timeparts[1];
                   String second = timeparts[2];



                   //when we know which minute this record should be located in, we check if the corresponding JSONObject in minuteJSONArray has been initialized.
                   //if not, we initialize it.
                   if ( minuteJSONArray[(Integer.parseInt(min)) ]==null){
                       minuteJSONArray[(Integer.parseInt(min))]= new JSONObject();
                   }

                   //create "second" key
                   JSONObject secondRecordJSON = createSecondRecordJSONByRecordType(separated, session.getRecordTypes().get(i));

                   //add the secondJSON to the minuteJSON
                   try {
                       minuteJSONArray[(Integer.parseInt(min))].put(second, secondRecordJSON);
                    //   Log.d (LOG_TAG, "[getSessionDocument] hour "+ hour  + " min " + min + " second " + second + " the secondJSON " + secondRecordJSON);

                   } catch (JSONException e) {
                       e.printStackTrace();
                   }



               }

               //add all minuteJSON to the recordTypeJSON
               for(int minuteJSONIndex = 0; minuteJSONIndex < minuteJSONArray.length; minuteJSONIndex++) {

                    if (minuteJSONArray[minuteJSONIndex]!=null ) {

                        try {
                            recordTypeJSON.put( (minuteJSONIndex)+"" , minuteJSONArray[minuteJSONIndex]);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
               }

              // Log.d (LOG_TAG, "[getSessionDocument] RecordTypeJSON " + recordTypeJSON);


               //HourJSOn add recordTypeJSON
               try {
                   hourJSON.put(ContextManager.getSensorTypeName(session.getRecordTypes().get(i)), recordTypeJSON );

               } catch (JSONException e) {
                   e.printStackTrace();
               }


               //Log.d (LOG_TAG, "[getSessionDocument] AllRecordTypeJSON " + allRecordJSON);
           }//end of recordtype

           startTime = sessionStartTime;
           endTime = startTime + GlobalNames.MILLISECONDS_PER_HOUR;

           //add the records in that hour to the sessionJSOn
           allRecordJSON.put(hourJSON);

       }

        //complete the rest proprerties of the session
        try {
            SimpleDateFormat sdf_id = new SimpleDateFormat(GlobalNames.DATE_FORMAT_FOR_ID);
            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_ID, GlobalNames.DEVICE_ID +"-"+ScheduleAndSampleManager.getTimeString(startTime, sdf_id) );
            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_START_TIME, ScheduleAndSampleManager.getTimeString(sessionStartTime));
            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_END_TIME, ScheduleAndSampleManager.getTimeString(sessionEndTime));
            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_RECORDS, allRecordJSON);
            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_ANNOTATIONSET, session.getAnnotationsSet().toJSONObject());
            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_SESSION_ID, getSessionDocumentId(sessionId) );
            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_DEVICE_ID, GlobalNames.DEVICE_ID);

            JSONObject taskJSON = new JSONObject();

            Task task = TaskManager.getTask(session.getTaskId());
            if (task!=null) {
                taskJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_ID, task.getId());
                taskJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_NAME, task.getName());
            }

            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_TASK, taskJSON);



//            sessionJSON.put(DatabaseNameManager.MONGO_DB_DOCUMENT_PROPERTIES_TASK, )

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d (LOG_TAG, "[getSessionDocument] sessionJSON " + sessionJSON);



        return sessionJSON;
    }


    private static String getSessionDocumentId(int sessionId) {

        return GlobalNames.DEVICE_ID + "-" + sessionId;

    }

    private static JSONObject createSecondRecordJSONByRecordType(String[] separated, int recordType) {

        JSONObject recordJSON = new JSONObject();

        switch(recordType){

            case ContextManager.CONTEXT_RECORD_TYPE_LOCATION:

                double lat = Double.parseDouble(separated[DatabaseNameManager.COL_INDEX_RECORD_LOC_LATITUDE_]);
                double lng = Double.parseDouble(separated[DatabaseNameManager.COL_INDEX_RECORD_LOC_LONGITUDE]);
                double accuracy = Double.parseDouble(separated[DatabaseNameManager.COL_INDEX_RECORD_LOC_ACCURACY]);

                try {
                    recordJSON.put(ContextManager.RECORD_SHORTNAME_LOCATION_LATITUDE, lat);
                    recordJSON.put(ContextManager.RECORD_SHORTNAME_LOCATION_LONGITUDE, lng);
                    recordJSON.put(ContextManager.RECORD_SHORTNAME_LOCATION_ACCURACY, accuracy);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case ContextManager.CONTEXT_RECORD_TYPE_ACTIVITY:

                String activity = separated[DatabaseNameManager.COL_INDEX_RECORD_ACTIVITY_LABEL_1];
                int confidence  = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_RECORD_ACTIVITY_CONFIDENCE_1]);
                try {
                    recordJSON.put(ContextManager.RECORD_SHORTNAME_ACTIVITY_ACTIVITY, activity);
                    recordJSON.put(ContextManager.RECORD_SHORTNAME_ACTIVITY_CONFIDENCE, confidence);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case ContextManager.CONTEXT_RECORD_TYPE_SENSOR:
                break;
            case ContextManager.CONTEXT_RECORD_TYPE_GEOFENCE:
                break;
            case ContextManager.CONTEXT_RECORD_TYPE_APPLICATION_ACTIVITY:
                break;
        }


        return recordJSON;


    }


}


