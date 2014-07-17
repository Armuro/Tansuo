package edu.umich.si.inteco.tansuo.app;

public class GlobalNames {

	/**for testing**/
	public static boolean isTestingActivity = true;

	public static boolean isBackgroundRecordingEnabled  = true;

    //preferndce name
    public static final String SHARED_PREFERENCE_NAME = "probe sharedpreference";
    public static final String SHARED_PREFERENCE_PROPERTY_DEVICE_ID = "device_id";



	public static final String TEST_DATABASE_NAME = "SensingStudyDatabase";
	public static final String TEST_FIRST_TASK_NAME = "Detecting Using Phone";
	public static final String TEST_SECOND_TASK_NAME = "Second_Task";
	public static final String TEST_THIRD_TASK_NAME = "third_Task";

    /***Main Activity Tab Name**/
    public static final String MAIN_ACTIVITY_TAB_DAILY_REPORT = "Daily Report";
    public static final String MAIN_ACTIVITY_TAB_RECORD = "Record";
    public static final String MAIN_ACTIVITY_TAB_RECORDINGS = "Recordings";
    public static final String MAIN_ACTIVITY_TAB_TASKS = "Profile";


	/**for labeling Study**/
	//1: participatory 
	//2: in situ
	//3: post hoc

	public static final String PARTICIPATORY_LABELING_CONDITION = "Participatory Labeling";
	public static final String IN_STIU_LABELING_CONDITION = "In Situ Labeling";
	public static final String POST_HOC_LABELING_CONDITION = "Post Hoc Labeling";

    public static final String CONFIGURATION_FILE_NAME_POST_HOC = "post_hoc_study.json";
    public static final String CONFIGURATION_FILE_NAME_IN_SITU = "in_situ_study.json";
    public static final String CONFIGURATION_FILE_NAME_PARTI = "participatory_study.json";

    //Web serivce
    public static final String WEB_SERVICE_URL_QUERY_LAST_SYNC_SESSION = "http://inteco.cloudapp.net:5001/querylastsyncsession";
    public static final String WEB_SERVICE_URL_QUERY_LAST_SYNC_file = "http://inteco.cloudapp.net:5001/querylastsyncfile";
    public static final String WEB_SERVICE_URL_QUERY_LAST_SYNC_BACKGROUND_RECORDING = "https://inteco.cloudapp.net:5001/query";
    public static final String WEB_SERVICE_URL_POST_SESSION = "https://inteco.cloudapp.net:5001/postsession";
    public static final String WEB_SERVICE_URL_POST_BACKGROUND_RECORDING = "https://inteco.cloudapp.net:5001/postbackgroundrecording";
    public static final String WEB_SERVICE_URL_REQUEST_SENDING_EMAIL = "https://inteco.cloudapp.net:5001/request_sending_email";


//    public static final String WEB_SERVICE_URL_POST_FILES = "http://inteco.cloudapp.net:5001/";
    public static final String WEB_SERVICE_URL_POST_FILES = "https://inteco.cloudapp.net:5001/postlog";
    public static final int WEB_SERVICE_PORT = 5000;


    public static String CURRENT_STUDY_CONDITION = PARTICIPATORY_LABELING_CONDITION;

	//this is the id for the labling study.
	public static final int LABELING_STUDY_ID = 1;
	
	//if researchers want to record records in the background for the purpose of monitoring, the session number by default is 0
	public static final int BACKGOUND_RECORDING_SESSION_ID= 1;	//the id column in the session table is auto-incremental, so the minimum is 1
	public static final int BACKGOUND_RECORDING_TASK_ID= 0;	//task with id 0 means it's the background recording.
	public static final String BACKGOUND_RECORDING_TASK_NAME = "Background_Recording";
	public static final String BACKGOUND_RECORDING_TASK_DESCRIPTION = "The task is Probe's background recording";
	public static final int BACKGOUND_RECORDING_NO_STUDY_ID = 0;

	/**participant**/
    public static String DEVICE_ID = "NA";
	
	//action alarm
	public static final String ACTION_ALARM = "edu.umich.si.inteco.captureprobe.actionAlarm";
  //  public static final String INTENT_ACTION_CONNECTIVITY_CHANGE = "edu.umich.si.inteco.captureprobe.intent.action.connectivityChange";
    public static final String UPDATE_SCHEDULE_ALARM = "edu.umich.si.inteco.captureprobe.updateScheduleAlarm";
    public static final String STOP_SERVICE_ALARM = "edu.umich.si.inteco.captureprobe.stopServiceAlarm";
    public static final String START_SERVICE_ALARM = "edu.umich.si.inteco.captureprobe.startServiceAlarm";
	
	/**constant**/
	public static final int MILLISECONDS_PER_SECOND = 1000;
	public static final int SECONDS_PER_MINUTE = 60;
	public static final int MINUTES_PER_HOUR = 60;
	public static final int HOURS_PER_DAY = 24;
	public static final int MILLISECONDS_PER_DAY = HOURS_PER_DAY *MINUTES_PER_HOUR*SECONDS_PER_MINUTE*MILLISECONDS_PER_SECOND;
    public static final int MILLISECONDS_PER_HOUR = MINUTES_PER_HOUR*SECONDS_PER_MINUTE*MILLISECONDS_PER_SECOND;
    public static final int MILLISECONDS_PER_MINUTE = SECONDS_PER_MINUTE*MILLISECONDS_PER_SECOND;
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss Z";
	public static final String DATE_FORMAT_NOW_DAY = "yyyy-MM-dd";	
	public static final String DATE_FORMAT_NOW_HOUR = "yyyy-MM-dd HH";
    public static final String DATE_FORMAT_NOW_HOUR_MIN = "yyyy-MM-dd HH:mm";
    public static final String DATE_FORMAT_HOUR_MIN_SECOND = "HH:mm:ss";
    public static final String DATE_FORMAT_FOR_ID = "yyyyMMddHHmmss";
    public static final String DATE_FORMAT_HOUR_MIN = "HH:mm";
    public static final String DATE_FORMAT_HOUR = "HH";
    public static final String DATE_FORMAT_MIN = "mm";
    public static final String DATE_FORMAT_DATE_TEXT = "MMM dd";
    public static final String DATE_FORMAT_DATE_TEXT_HOUR_MIN = "MMM dd HH:mm";
	public static final int DATA_FORMAT_TYPE_NOW=0;
	public static final int DATA_FORMAT_TYPE_DAY=1;
	public static final int DATA_FORMAT_TYPE_HOUR=2;
	public static final String DELIMITER = ";;;";
	public static final String DELIMITER_IN_COLUMN = "::";
	
	/**File Path **/
	
	public static String PACKAGE_DIRECTORY_PATH = "/Android/data/edu.umich.si.inteco.captureprobe/";
    public static String PACKAGE_DIRECTORY_NARRATIVE_PATH = "/Android/data/com.narrative.main/cache/";
	
	
	/**Configurable parameters**/
	
	/*Context Extracotr*/
	public static int CONTEXTEXTRACTOR_SAMPLING_RATE = 1;
	public static double NULL_SENSOR_VALUE = -9999;

	
}
