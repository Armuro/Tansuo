package edu.umich.si.inteco.tansuo.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.contextmanager.ContextExtractor;
import edu.umich.si.inteco.tansuo.app.model.Log.ProbeLog;

public class LogManager {

    private static final boolean WRITE_ALL_LOG = true;
    public static final String ALL_LOG_FILE_NAME = "All-Log";

	private static final String LOG_TAG = "LogManager";
	
	public static final String LOG_DIRECTORY_PATH = "Logs/";
	
	/**types of log**/
    public static final String LOG_TYPE_SYSTEM_LOG = "System-Log";
    public static final String LOG_TYPE_USER_ACTION_LOG = "User-Action-Log";

	public static final String LOG_TAG_ACTIVITY_RECOGNITION = "AR";
    public static final String LOG_TAG_PROBE_TRANSPORTATION = "PROBETR";
    public static final String LOG_TAG_GEO_FENCE = "GF";

    public static final String LOG_TAG_ACTION_START = "ACTSTART";
    public static final String LOG_TAG_ACTION_STOP = "ACTSTOP";
    public static final String LOG_TAG_ACTION_PAUSE = "ACTPAUSE";
    public static final String LOG_TAG_ACTION_RESUME = "ACTRESUME";
	public static final String LOG_TAG_ACTION_EXECUTION = "ACTEXE";
    public static final String LOG_TAG_ACTION_GENERATION = "ACTGEN";
    public static final String LOG_TAG_ACTION_TRIGGER = "ACTTRG";
	public static final String LOG_TAG_EVENT_DETECTED = "EVTDETCT";
    public static final String LOG_TAG_NOTIFICATION = "NOTI";
    public static final String LOG_TAG_SERVICE = "SERVICE";


    public static final String LOG_TAG_ACTIVITY_START = "ACTISTART";

    /** User action ***/
    public static final String LOG_TAG_USER_SELECTING = "USELECTING";
    public static final String LOG_TAG_USER_CLICKING = "UCLICK";
    public static final String LOG_TAG_USER_TYPING = "UTYPE";
    public static final String LOG_TAG_USER_NOTIFICATION_ATTENDING = "UATTNOTI";
    public static final String LOG_TAG_USER_NOTIFICATION_SELECTING = "USELNOTI";

    //tags for questionnaire
	public static final String LOG_TAG_QUESTIONNAIRE_NOTI_GENERATED = "QUE-NOTI-GEN";
    public static final String LOG_TAG_ANNOTATION_NOTI_GENERATED = "ANO-NOTI-GEN";

    public static final String LOG_TAG_QUESTIONNAIRE_NOTI_ATTENDED = "QUE-NOTI-ATT";
	public static final String LOG_TAG_QUESTIONNAIRE_NOTI_SUBMITTED = "QUE-NOTI-SUB";	
	public static final String LOG_MESSAGE_QUESTIONNAIRE_NOTI_GENERATED = "questionnaire notification generated";
	public static final String LOG_MESSAGE_QUESTIONNAIRE_NOTI_ATTENDED = "questionnaire notification attended";
    public static final String LOG_MESSAGE_ANNOTATION_NOTI_GENERATED = "annotation notification generated";
    public static final String LOG_MESSAGE_ANNOTATION_NOTI_ATTENDED = "annotation notification attended";
	public static final String LOG_MESSAGE_QUESTIONNAIRE_SUBMITTED = "questionnaire submitted";
    public static final String LOG_MESSAGE_EMAIL_QUESTIONNAIRE_NOTI_GENERATED = "email questionnaire notification generated";
    public static final String LOG_MESSAGE_EMAIL_QUESTIONNAIRE_NOTI_ATTENDED = "email questionnaire notification attended";
    public static final String LOG_MESSAGE_EMAIL_QUESTIONNAIRE_NOTI_SUBMITTED = "email questionnaire notification submitted";

	public LogManager(){


		
	}

    public static void log(String type, String tag, String content) {

        ProbeLog systemLog = new ProbeLog(
                type,
                tag,
                ContextExtractor.getCurrentTimeInMillis(),
                ContextExtractor.getCurrentTimeString(),
                //also get the app info
                content + "\t" + ContextExtractor.getCurrentForegroundActivity()
        );

        writeLogToFile(systemLog);

    }

	public static void writeLogToFile(ProbeLog log){
	
		String path= LOG_DIRECTORY_PATH; 
		
		String filename = log.getType() + "-" + getLogFileTimeString(GlobalNames.DATA_FORMAT_TYPE_DAY) + ".txt" ;
		FileHelper.writeStringToFile(path, filename, log.toString());

        //if also write to all logs..(but not activity recognition)
        if (WRITE_ALL_LOG && !log.getTag().equals(LOG_TAG_ACTIVITY_RECOGNITION)){
            filename = ALL_LOG_FILE_NAME + "-" + getLogFileTimeString(GlobalNames.DATA_FORMAT_TYPE_DAY) + ".txt" ;
            FileHelper.writeStringToFile(path, filename, log.toString());
        }

	}

	/**
	 * return the file name of the log
	 * @return
	 */
	public String getFileName(){
		String filename = null;
		return filename;
	}
	
	public String getActivityLogName(){
		return "GoogleActivity-Log";
	}
	
	
	/***
	 * 
	 * 
	 * @return
	 */
	private static String getLogFileTimeString(int data_format_type){		
		//get timzone		 
		TimeZone tz = TimeZone.getDefault();		
		Calendar cal = Calendar.getInstance(tz);
		SimpleDateFormat sdf=null;
		
		if ( data_format_type== GlobalNames.DATA_FORMAT_TYPE_DAY){
			
			sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW_DAY);
		}
		
		else if ( data_format_type== GlobalNames.DATA_FORMAT_TYPE_HOUR){
			
			sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW_HOUR);
		}
		
		else if ( data_format_type== GlobalNames.DATA_FORMAT_TYPE_NOW){
			
			sdf = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW);
		}

		
		return sdf.format(cal.getTime());
	}
	
	
}
