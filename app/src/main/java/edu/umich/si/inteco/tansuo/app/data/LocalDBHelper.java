package edu.umich.si.inteco.tansuo.app.data;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.MainActivity;
import edu.umich.si.inteco.tansuo.app.contextmanager.ActivityRecognitionService;
import edu.umich.si.inteco.tansuo.app.contextmanager.ContextManager;
import edu.umich.si.inteco.tansuo.app.model.AnnotationSet;
import edu.umich.si.inteco.tansuo.app.model.Configuration;
import edu.umich.si.inteco.tansuo.app.model.Questionnaire;
import edu.umich.si.inteco.tansuo.app.model.Session;
import edu.umich.si.inteco.tansuo.app.model.Task;
import edu.umich.si.inteco.tansuo.app.model.TimeConstraint;
import edu.umich.si.inteco.tansuo.app.model.UserResponse;
import edu.umich.si.inteco.tansuo.app.model.record.ActivityRecord;
import edu.umich.si.inteco.tansuo.app.model.record.AppActivityRecord;
import edu.umich.si.inteco.tansuo.app.model.record.LocationRecord;
import edu.umich.si.inteco.tansuo.app.model.record.Record;
import edu.umich.si.inteco.tansuo.app.model.record.SensorRecord;
import edu.umich.si.inteco.tansuo.app.util.ConditionManager;
import edu.umich.si.inteco.tansuo.app.util.DatabaseNameManager;

public class LocalDBHelper extends SQLiteOpenHelper{

	/** Tag for logging. */
    private static final String LOG_TAG = "LocalDBHelper";
    
    /**parameters for creating a database**/

    private static int DATABASE_VERSION = 1;

    
    /** constants **/
    
    private static String SQL_CMD_CREATE_TABLE = "CREATE TABLE"; 
    
    private static int SQL_TYPE_CREATE_TABLE = 1; 
    
    private static int SQL_TYPE_INSERT_TABLE = 2; 
 
    
    /***Member**/
    
    private Context mContext;
    
    
	public LocalDBHelper(Context context, String dbName) {

		super(context, dbName, null, DATABASE_VERSION);
		
		mContext = context;

        DatabaseManager.initializeInstance(this);

	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {

		//when the DBHelper is initiated, create the necessary tables
		if (db!=null){
			
			////Log.d(LOG_TAG, " the DB " + db.toString() + " has existed");
			
			createStudyTable(db, DatabaseNameManager.STUDY_TABLE_NAME);	
			
			//2. Whenever a study is created, create a TASK Table 
			createTaskTable(db, DatabaseNameManager.TASK_TABLE_NAME);			
			
			//3. ...and a SESSION table, so that we can insert tasks and session into the table
			createSessionTable(db, DatabaseNameManager.SESSION_TABLE_NAME);		
			
			//4 a study will have a configuration, presumably.
			createConfigurationTable(db, DatabaseNameManager.CONFIGURATION_TABLE_NAME);	
			
			//5 create user response table
			createUserResponseTable(db, DatabaseNameManager.USER_RESPONSE_TABLE_NAME);	
			
			createQuestionnaireTable(db, DatabaseNameManager.QUESTIONNAIRE_TABLE_NAME);	

			//4. create necessary record tables 
			createRecordTables(db);
		}


			
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	
		/*
		if (db!=null){
			db.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + SESSION_TABLE_NAME);
			onCreate(db);
		}*/
	}	
    
	
	public void createStudyTable(SQLiteDatabase db, String table_name){
    	
    	String cmd = SQL_CMD_CREATE_TABLE + " " + 
    				table_name + " ( "+
    				DatabaseNameManager.COL_ID + " " + " INTEGER PRIMARY KEY, " +
    				DatabaseNameManager.COL_STUDY_NAME + " TEXT NOT NULL " +
    				");" ;
    	
    	db.execSQL(cmd);

    }

    
    public void createTaskTable(SQLiteDatabase db, String table_name){
    	
    	String cmd = SQL_CMD_CREATE_TABLE + " " + 
    				table_name + " ( "+
    				DatabaseNameManager.COL_ID + " " + " INTEGER PRIMARY KEY, " +
    				DatabaseNameManager.COL_STUDY_ID + " INTEGER NOT NULL, " +
    				DatabaseNameManager.COL_TASK_NAME + " TEXT NOT NULL, " +
    				DatabaseNameManager.COL_TASK_DESCRIPTION + " TEXT NOT NULL, " +
    				DatabaseNameManager.COL_TASK_CREATED_TIME + " INTEGER NOT NULL, " +
    				DatabaseNameManager.COL_START_TIME + " INTEGER NOT NULL, " +
    				DatabaseNameManager.COL_END_TIME + " INTEGER NOT NULL " +
    				");" ;
    	
    	db.execSQL(cmd);
    }
    

    /**
     * 
     * @param db
     * @param table_name
     */
    public void createSessionTable(SQLiteDatabase db, String table_name){
    	
    	String cmd = SQL_CMD_CREATE_TABLE + " " + 
    				table_name + " ( "+
    				DatabaseNameManager.COL_ID + " " + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
    				DatabaseNameManager.COL_TASK_ID + " INTEGER NOT NULL, " +
    				DatabaseNameManager.COL_TIMESTAMP_STRING + " TEXT NOT NULL, " +
    				DatabaseNameManager.COL_SESSION_START_TIME + " INTEGER NOT NULL, " +
    				DatabaseNameManager.COL_SESSION_END_TIME + " INTEGER, " +
                    DatabaseNameManager.COL_SESSION_ANNOTATION_SET + " TEXT " +
    				
    				");" ;
    	
    	db.execSQL(cmd);
    }    
    
    public void createConfigurationTable(SQLiteDatabase db, String table_name){
    	
    	String cmd = SQL_CMD_CREATE_TABLE + " " + 
				table_name + " ( "+
				DatabaseNameManager.COL_ID + " " + " INTEGER PRIMARY KEY, " +
				DatabaseNameManager.COL_STUDY_ID + " INTEGER NOT NULL, " +
				DatabaseNameManager.COL_CONFIGURATION_NAME+ " TEXT, " + 
				DatabaseNameManager.COL_CONFIGURATION_VERSION+ " INTEGER, " +
				DatabaseNameManager.COL_CONFIGURATION_CONTENT+ " TEXT " +
				");" ;
	
		db.execSQL(cmd);

    	
    }
    
    public void createUserResponseTable(SQLiteDatabase db, String table_name){
    	
    	String cmd = SQL_CMD_CREATE_TABLE + " " + 
				table_name + " ( "+
				DatabaseNameManager.COL_ID + " " + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				DatabaseNameManager.COL_STUDY_ID + " INTEGER NOT NULL, " +
				DatabaseNameManager.COL_QUESTIONNAIRE_ID+ " INTEGER NOT NULL, " + 
				DatabaseNameManager.COL_START_TIME+ " INTEGER, " +
				DatabaseNameManager.COL_END_TIME+ " INTEGER, " +
				DatabaseNameManager.COL_CONTENT+ " TEXT " +
				");" ;
	
		db.execSQL(cmd);

    	
    }
    
    
    public void createQuestionnaireTable(SQLiteDatabase db, String table_name){
    	
    	String cmd = SQL_CMD_CREATE_TABLE + " " + 
				table_name + " ( "+
				DatabaseNameManager.COL_ID + " " + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				DatabaseNameManager.COL_STUDY_ID + " INTEGER NOT NULL, " + 
				DatabaseNameManager.COL_QUESTIONNAIRE_GENERATED_TIME+ " INTEGER, " +
				DatabaseNameManager.COL_QUESTIONNAIRE_ATTENDED_TIME+ " INTEGER, " +
				DatabaseNameManager.COL_QUESTIONNAIRE_SUBMITTED_TIME+ " INTEGER, " +
				DatabaseNameManager.COL_QUESTIONNAIRE_RESPONSE + " TEXT " + 
				");" ;
	
		db.execSQL(cmd);

    	
    }
    
    
    
    

    
    
    
    /**
     * 
     * @param db
     * @param table_name
     * @param RecordType
     * @param SensorSource
     */
    public void createRecordTable(SQLiteDatabase db, String table_name, int RecordType, int SensorSource){
    	
    	////Log.d(LOG_TAG, " enter createSessionTable()");
    	
    	String cmd = SQL_CMD_CREATE_TABLE + " " + 
    				table_name + " ( "+
    				DatabaseNameManager.COL_ID + " " + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
    				DatabaseNameManager.COL_SESSION_ID + " INTEGER NOT NULL, " +
    				DatabaseNameManager.COL_TIMESTAMP_STRING+ " TEXT NOT NULL, " + 
    				DatabaseNameManager.COL_TIMESTAMP_LONG+ " INTEGER NOT NULL, " +
    				
					getExtraSQLStatementByRecordType(SQL_TYPE_CREATE_TABLE, RecordType, SensorSource) + 
    				
    				");" ;

    	db.execSQL(cmd);
    	
    }
    
    /***
     * This function create all record tables
     * @param db
     */
    private void createRecordTables (SQLiteDatabase db){
    	
    	//Location
    	createRecordTable (db, DatabaseNameManager.RECORD_TABLE_NAME_LOCATION, ContextManager.CONTEXT_RECORD_TYPE_LOCATION, -1);
    	createRecordTable (db, DatabaseNameManager.RECORD_TABLE_NAME_ACTIVITY, ContextManager.CONTEXT_RECORD_TYPE_ACTIVITY, -1);
        createRecordTable (db, DatabaseNameManager.RECORD_TABLE_NAME_APPLICATION_ACTIVITY, ContextManager.CONTEXT_RECORD_TYPE_APPLICATION_ACTIVITY, -1);
    	createRecordTable (db, DatabaseNameManager.RECORD_TABLE_NAME_ACCELEROMETER, ContextManager.CONTEXT_RECORD_TYPE_SENSOR, ContextManager.SENSOR_SOURCE_PHONE_ACCELEROMETER);

    }
    
    
    /**
     * 
     * Insert data into tables. Since every table has a different scheme, we separate them as different functions
     * 
     * **/
    
    
    public long insertStudyTable(int id, String table_name){
    	
    	SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        //SQLiteDatabase db2 = DatabaseManager.getInstance().openDatabase();
		ContentValues values = new ContentValues();
    	
		values.put(DatabaseNameManager.COL_ID, id);
    	
		
		//get row number after the insertion
		//Log.d(LOG_TAG, " Inserting study " + id);
		
		long rowId=0;
		try{
			rowId = db.insert(table_name, null, values);

		}catch(Exception e){
			e.printStackTrace();
			rowId = -1;
		}

        DatabaseManager.getInstance().closeDatabase();
		
		//Log.d(LOG_TAG, " Inserting successfully! The " + table_name + " table now has " + rowId + " rows.");
		
		return rowId;
    }
    
    /**
     * insert a task into a TaskTable
     * @param task
     * @param table_name
     */
    public long insertTaskTable(Task task, String table_name){
	

    	//TODO: the user should be able to specify the database because each study may have a different database.

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
      //  Log.d(LOG_TAG, "[test instantiate db]  after instanstiate the database" );
		ContentValues values = new ContentValues();

		
		values.put(DatabaseNameManager.COL_ID, task.getId());
		values.put(DatabaseNameManager.COL_STUDY_ID, task.getStudyId());
		values.put(DatabaseNameManager.COL_TASK_NAME, task.getName());		
		values.put(DatabaseNameManager.COL_TASK_DESCRIPTION, task.getDescription());
		values.put(DatabaseNameManager.COL_TASK_CREATED_TIME, getCurrentTimeInMilli());
		values.put(DatabaseNameManager.COL_START_TIME, task.getStartTime());	
		values.put(DatabaseNameManager.COL_END_TIME, task.getEndTime());

        //Log.d(LOG_TAG, "[testInsert][insertTaskTable] db " + db);
		//get row number after the insertion
		//Log.d(LOG_TAG, " trying to insert task " +task.getId() + " : " + task.getName() + " to the task table " + table_name);
		
		long rowId=0;
		try{
			rowId = db.insert(table_name, null, values);

		}catch(Exception e){
			e.printStackTrace();
			rowId = -1;
		}

        DatabaseManager.getInstance().closeDatabase();

		//Log.d(LOG_TAG, " Inserting successfully! The " + table_name + " table now has " + queryTaskCount() + " rows.");
		
		return rowId;
    }
	
    
    /**
     * 
     * @param config
     * @param table_name
     * @return
     */
    public long insertConfigurationTable(Configuration config, String table_name){

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		ContentValues values = new ContentValues();
		
		values.put(DatabaseNameManager.COL_ID, config.getId());
		values.put(DatabaseNameManager.COL_STUDY_ID, config.getStudyId());
		values.put(DatabaseNameManager.COL_CONFIGURATION_NAME, config.getName());
		values.put(DatabaseNameManager.COL_CONFIGURATION_VERSION, config.getVersion());
		values.put(DatabaseNameManager.COL_CONFIGURATION_CONTENT, config.getContent().toString());

       // Log.d(LOG_TAG, "[testInsert][insertTaskTable] db " + db);
       // Log.d(LOG_TAG, "[insertConfigurationTable] db " + db);
		long rowId=0;
		try{
			rowId = db.insert(table_name, null, values);

		}catch(Exception e){
			e.printStackTrace();
			rowId = -1;
		}

        DatabaseManager.getInstance().closeDatabase();
		//Log.d(LOG_TAG, " Inserting configuration successfully! The " + table_name + " table now has " + rowId + " rows.");
		
		return rowId;
		
    }
    
 
    /**
     * insert a response into a ResponseTable
     * @param response
     * @param table_name
     */
    public long insertUserResponseTable(UserResponse response, String table_name){
	
    	//TODO: the user should be able to specify the database because each study may have a different database.

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		ContentValues values = new ContentValues();

		values.put(DatabaseNameManager.COL_STUDY_ID, response.getStudyId());
		values.put(DatabaseNameManager.COL_QUESTIONNAIRE_ID, response.getStudyId());
		values.put(DatabaseNameManager.COL_START_TIME, response.getStartTime());	
		values.put(DatabaseNameManager.COL_END_TIME, response.getEndTime());	
		values.put(DatabaseNameManager.COL_CONTENT, response.getContent().toString());	

		//get row number after the insertion
		Log.d(LOG_TAG, " Inserting User Response " + response.getId() + " response: " + response.getContent()  + " to the task table " + table_name);
		
		long rowId=0;
		try{
			rowId = db.insert(table_name, null, values);
            DatabaseManager.getInstance().closeDatabase();
		}catch(Exception e){
			e.printStackTrace();
			rowId = -1;
		}

		
		//Log.d(LOG_TAG, " Inserting successfully! The " + table_name + " table now has " + rowId + " rows.");
		
		return rowId;
    }
    
    
    public long insertQuestionnaireTable(Questionnaire questionnaire, String table_name){
    	
    	//TODO: the user should be able to specify the database because each study may have a different database.

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		ContentValues values = new ContentValues();

		values.put(DatabaseNameManager.COL_QUESTIONNAIRE_SUBMITTED_TIME, questionnaire.getSubmittedTime());
		values.put(DatabaseNameManager.COL_STUDY_ID, questionnaire.getStudyId());

		//get row number after the insertion
		//Log.d(LOG_TAG, " Inserting questionnaire generated at  " + questionnaire.getSubmittedTime() + " of study  " + questionnaire.getStudyId() + " to the questionnaire table " + table_name);
		
		long rowId=0;
		try{
			rowId = db.insert(table_name, null, values);
            DatabaseManager.getInstance().closeDatabase();
		}catch(Exception e){
			e.printStackTrace();
			rowId = -1;
		}

		
		//Log.d(LOG_TAG, " Inserting successfully! The " + table_name + " table now has " + rowId + " rows.");
		
		return rowId;
    }

    /**
     * Update the endtime of a session. Usually this information is obtained when a recording is finished.
     * @param sessionId
     * @param table_name
     * @param endTime
     */
    public void updateSessionEndTime(int sessionId, String table_name, long endTime ){

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    	ContentValues values = new ContentValues();	
    	
		values.put(DatabaseNameManager.COL_SESSION_END_TIME, endTime);	
    	
		String where = DatabaseNameManager.COL_ID + " = " +  sessionId;

		try{
			db.update(DatabaseNameManager.SESSION_TABLE_NAME, values, where, null);
		//	Log.d(LOG_TAG, " Updating end time " + endTime  + " successfully! ");
            DatabaseManager.getInstance().closeDatabase();
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		ArrayList<String> result = querySessions();
		//Log.d(LOG_TAG, "the session update result is " + result);

    }

    /**
     * Add annotation to a session
     * @param sessionId
     * @param table_name
     * @param annotationSet
     */
    public void updateSessionAnnotation(int sessionId, String table_name, AnnotationSet annotationSet){

       // Log.d(LOG_TAG, "[updateSessionAnnotation] submit annotation set " + annotationSet.toJSONObject().toString());


        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseNameManager.COL_SESSION_ANNOTATION_SET, annotationSet.toJSONObject().toString());

        String where = DatabaseNameManager.COL_ID + " = " +  sessionId;

      //  Log.d(LOG_TAG, "[querySession] the query statement is " +where);

        try{
            db.update(DatabaseNameManager.SESSION_TABLE_NAME, values, where, null);
         //   Log.d(LOG_TAG, " Updating annotation successfully!");
            DatabaseManager.getInstance().closeDatabase();

        }catch(Exception e){
            e.printStackTrace();
        }

        ArrayList<String> result = querySessions();
       // Log.d(LOG_TAG, "the session update result is " + result);

    }
    
    /**
     * insert a session into a TaskTable
     * @param session
     * @param table_name
     */
    public long insertSessionTable(Session session, String table_name){
    	
    	//TODO: the user should be able to specify the database because each study may have a different database.

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		ContentValues values = new ContentValues();

		values.put(DatabaseNameManager.COL_TASK_ID, session.getTaskId());
		values.put(DatabaseNameManager.COL_TIMESTAMP_STRING, getTimeString(session.getStartTime()));	
		values.put(DatabaseNameManager.COL_SESSION_START_TIME, session.getStartTime());	

		
		//get row number after the insertion
		Log.d(LOG_TAG, " Inserting session " + session.getTaskId() + ": Session-" + session.getStartTime() + " to the session table " + table_name);
		
		long rowId=0;

		try{
			rowId = db.insert(table_name, null, values);


		}catch(Exception e){
			e.printStackTrace();
			rowId = -1;
		}

        DatabaseManager.getInstance().closeDatabase();

		return rowId;
    }
    
    
    
    //insert a record into a RecordTable
    public static long insertRecordTable(Record record, String table_name, int session_id){

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();

		ContentValues values = new ContentValues();
		
		values.put(DatabaseNameManager.COL_SESSION_ID, session_id);
		values.put(DatabaseNameManager.COL_TIMESTAMP_STRING, getTimeString(record.getTimestamp()));	
		values.put(DatabaseNameManager.COL_TIMESTAMP_LONG, record.getTimestamp());	
		
		//we need to add values based on the record type, because different types of record have different fields in the database.
		addExtraContentValuesByRecordType(values, record);
		
		//Log.d(LOG_TAG, "[insertRecordTable] Inserting record " + ContextManager.getSensorTypeName(record.getType()) + ": at-" + record.getTimestamp() + " : " + record.getTimeString() + " in session " + session_id );
		//" to the record table " + table_name);
		
		long rowId=0;
		try{
			
			rowId = db.insert(table_name, null, values);

		Log.d(LOG_TAG, "[insertRecordTable] Inserting successfully! The " + table_name + " table now has " + rowId + " rows.");
			
		}catch(Exception e){
			e.printStackTrace();
			rowId = -1;
		}

        DatabaseManager.getInstance().closeDatabase();
		
		return rowId;
    }



    
    public ArrayList<String> queryUnUploadedData(long lastSynTimestamp){
    	
    	ArrayList<String> s = new ArrayList<String>();
    	
    	return s;
    }

    
    
    /**
     * This function will add extra values into the ContentValues, which is later inserted into a corresponding record table, based on the type of the record. 
     * 
     * @param values:  existing values already contain required fields such as session id, timestamp
     * @param record:  the record that is going to be inserted into the record tables.
     * @return
     */
    private static ContentValues addExtraContentValuesByRecordType(ContentValues values, Record record){

    	
    	if (record.getType() == ContextManager.CONTEXT_RECORD_TYPE_LOCATION){
    		
    		LocationRecord locationRecord = (LocationRecord) record;
    		
    		values.put(DatabaseNameManager.COL_LOC_LATITUDE, locationRecord.getLocation().getLatitude());
    		values.put(DatabaseNameManager.COL_LOC_LONGITUDE, locationRecord.getLocation().getLongitude());
			values.put(DatabaseNameManager.COL_LOC_ACCURACY, locationRecord.getLocation().getAccuracy());
			values.put(DatabaseNameManager.COL_LOC_ALTITUDE, locationRecord.getLocation().getAltitude());	
			values.put(DatabaseNameManager.COL_LOC_PROVIDER, locationRecord.getLocation().getProvider());
			values.put(DatabaseNameManager.COL_LOC_BEARING, locationRecord.getLocation().getBearing());
			values.put(DatabaseNameManager.COL_LOC_SPEED, locationRecord.getLocation().getSpeed());

    		
    	}else if(record.getType() == ContextManager.CONTEXT_RECORD_TYPE_ACTIVITY) {
    		
    		ActivityRecord activityRecord = (ActivityRecord) record;
    		
    		List<DetectedActivity> activities = activityRecord.getProbableActivities();
    		
    		//examine whether the Top1, 2, and 3 activity exist. Put the activity into the ContentValues..
    		try{
    			if(activities.get(0)!=null){
        			values.put(DatabaseNameManager.COL_ACTIVITY_1, ActivityRecognitionService.getActivityNameFromType(activities.get(0).getType()));
        			values.put(DatabaseNameManager.COL_ACTIVITY_CONF_1, activities.get(0).getConfidence());

                    //detection time
                    values.put(DatabaseNameManager.COL_ACTIVITY_DETECTION_TIME, activityRecord.getDetectionTime());
        		}
    			if(activities.size() ==2 && activities.get(1)!=null){
    				values.put(DatabaseNameManager.COL_ACTIVITY_2, ActivityRecognitionService.getActivityNameFromType(activities.get(1).getType()));
        			values.put(DatabaseNameManager.COL_ACTIVITY_CONF_2, activities.get(1).getConfidence());
    			}
    			if(activities.size() ==3 && activities.get(2)!=null){
    				values.put(DatabaseNameManager.COL_ACTIVITY_3, ActivityRecognitionService.getActivityNameFromType(activities.get(2).getType()));
        			values.put(DatabaseNameManager.COL_ACTIVITY_CONF_3, activities.get(2).getConfidence());
    			}

    			
    		}catch (Exception e){
    			e.printStackTrace();
    		}

			
		}
        else if(record.getType() == ContextManager.CONTEXT_RECORD_TYPE_APPLICATION_ACTIVITY) {

            AppActivityRecord appActivityRecord = (AppActivityRecord) record;

            //examine whether the Top1, 2, and 3 activity exist. Put the activity into the ContentValues..
            try{
                values.put(DatabaseNameManager.COL_APPLICATION_ACTIVITY, appActivityRecord.getAppActivityName());
                values.put(DatabaseNameManager.COL_APPLICATION_ACTIVITY_PACKAGE, appActivityRecord.getAppPackageName());

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        else if(record.getType() == ContextManager.CONTEXT_RECORD_TYPE_SENSOR){
						
			SensorRecord sensorRecord = (SensorRecord) record;
			
    		try{
    			
    			if(sensorRecord.sensorValues.get(0)!=null){
        			values.put(DatabaseNameManager.COL_SENSOR_VALUE_1, sensorRecord.sensorValues.get(0).toString());
        		}
    			if(sensorRecord.sensorValues.get(1)!=null){
    				values.put(DatabaseNameManager.COL_SENSOR_VALUE_2, sensorRecord.sensorValues.get(1).toString());
    			}
    			if(sensorRecord.sensorValues.get(2)!=null){
    				values.put(DatabaseNameManager.COL_SENSOR_VALUE_3, sensorRecord.sensorValues.get(2).toString());
    			}
    			if(sensorRecord.sensorValues.get(3)!=null){
    				values.put(DatabaseNameManager.COL_SENSOR_VALUE_4, sensorRecord.sensorValues.get(3).toString());
    			}

    			
    		}catch (Exception e){
    			e.printStackTrace();
    		}
   	
		}
    	
    	return values;
    }
    
    
    
    
    
    /**
     * this function generates sql for creating tables, based on the record type.
     * 
     * @param recordType
     * @param sensorSource
     */
    private static String getExtraSQLStatementByRecordType(int sqlType, int recordType, int sensorSource){
    	
    	String sql = "";
    	
    	
    	if (sqlType==SQL_TYPE_CREATE_TABLE){

    		if (recordType == ContextManager.CONTEXT_RECORD_TYPE_LOCATION){    			
    			
    			sql +=    					
    					DatabaseNameManager.COL_LOC_LATITUDE+ " REAL NOT NULL, " +
    					DatabaseNameManager.COL_LOC_LONGITUDE+ " REAL NOT NULL, " +
    					DatabaseNameManager.COL_LOC_ACCURACY + " REAL NOT NULL, " +
    					DatabaseNameManager.COL_LOC_ALTITUDE + " REAL NOT NULL, " +
    					DatabaseNameManager.COL_LOC_PROVIDER + " TEXT NOT NULL, " +
    					DatabaseNameManager.COL_LOC_BEARING + " REAL NOT NULL, " +
    					DatabaseNameManager.COL_LOC_SPEED+ " REAL NOT NULL "; 

    		}   		
    		
    		else if(recordType == ContextManager.CONTEXT_RECORD_TYPE_ACTIVITY) {
    			
    			sql += 
    					DatabaseNameManager.COL_ACTIVITY_1 + " TEXT, " +
    					DatabaseNameManager.COL_ACTIVITY_2 + " TEXT, " +
    					DatabaseNameManager.COL_ACTIVITY_3 + " TEXT, " +
    					DatabaseNameManager.COL_ACTIVITY_CONF_1 + " NUMERIC, " +
    					DatabaseNameManager.COL_ACTIVITY_CONF_2 + " NUMERIC, " +
    					DatabaseNameManager.COL_ACTIVITY_CONF_3 + " NUMERIC, " +
                        DatabaseNameManager.COL_ACTIVITY_DETECTION_TIME + " NUMERIC ";
    					
    			////Log.d(LOG_TAG, " the created SQL statement is " + sql);
    		}

            else if(recordType == ContextManager.CONTEXT_RECORD_TYPE_APPLICATION_ACTIVITY) {

                sql +=
                        DatabaseNameManager.COL_APPLICATION_ACTIVITY + " TEXT, " +
                        DatabaseNameManager.COL_APPLICATION_ACTIVITY_PACKAGE + " TEXT " ;

                ////Log.d(LOG_TAG, " the created SQL statement is " + sql);
            }
    		
    		
    		else if(recordType == ContextManager.CONTEXT_RECORD_TYPE_SENSOR){

    			sql +=
    					DatabaseNameManager.COL_SENSOR_VALUE_1 + " REAL NOT NULL, " +
    					DatabaseNameManager.COL_SENSOR_VALUE_2 + " REAL NOT NULL, " +
    					DatabaseNameManager.COL_SENSOR_VALUE_3 + " REAL NOT NULL, " +
    					DatabaseNameManager.COL_SENSOR_VALUE_4 + " REAL NOT NULL " ;   					
    							
    			////Log.d(LOG_TAG, " the created SQL statement is " + sql);			
    			
    		}
    		
    	}

    	
    	////Log.d(LOG_TAG, " the created SQL statement is " + sql);
    	
    	return sql;
    }


    /*****
     *
     * Remove data
     *
     *
     */


    public static void removeQuestionnaires() {
        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            db.execSQL("delete from "+ DatabaseNameManager.QUESTIONNAIRE_TABLE_NAME);
            DatabaseManager.getInstance().closeDatabase();

        }catch (Exception e){

        }
    }

    public static void removeTasks() {
        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            db.execSQL("delete from "+ DatabaseNameManager.TASK_TABLE_NAME);
            DatabaseManager.getInstance().closeDatabase();

        }catch (Exception e){

        }
    }


    public static void removeConfigurations() {
        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            db.execSQL("delete from "+ DatabaseNameManager.CONFIGURATION_TABLE_NAME);
            DatabaseManager.getInstance().closeDatabase();

        }catch (Exception e){

        }
    }


    /*****
     * 
     * Query Tables
     * 
     * 
     */



    
    //query configuration table 
    public static ArrayList<String> queryConfigurations(){
    	
    	ArrayList<String> rows = new ArrayList<String>();
    	
    	try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    		String sql = "SELECT *"  +" FROM " + DatabaseNameManager.CONFIGURATION_TABLE_NAME ;
    		
    		Cursor cursor = db.rawQuery(sql, null);   		
    		int columnCount = cursor.getColumnCount();
			while(cursor.moveToNext()){
				String curRow = "";
				for (int i=0; i<columnCount; i++){
					curRow += cursor.getString(i)+ GlobalNames.DELIMITER;		
				}
				rows.add(curRow);
			}
			cursor.close();
            DatabaseManager.getInstance().closeDatabase();
    		
    	}catch (Exception e){
    		
    	}
    	
    	return rows;

    }

    
    //query task table
    public static ArrayList<String> queryTasks (){
    	
    	ArrayList<String> rows = new ArrayList<String>();
    	
    	try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    		String sql = "SELECT *"  +" FROM " + DatabaseNameManager.TASK_TABLE_NAME ;
    		
    		Cursor cursor = db.rawQuery(sql, null);   		
    		int columnCount = cursor.getColumnCount();
			while(cursor.moveToNext()){
				String curRow = "";
				for (int i=0; i<columnCount; i++){
					curRow += cursor.getString(i)+ GlobalNames.DELIMITER;		
				}
				rows.add(curRow);
			}
			cursor.close();

    		
    	}catch (Exception e){
    		
    	}

        DatabaseManager.getInstance().closeDatabase();
    	return rows;
    	
    }


    public static  ArrayList<String> querySessionsBetweenTimes(long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + DatabaseNameManager.SESSION_TABLE_NAME +
                    " where " + DatabaseNameManager.COL_SESSION_START_TIME + " > " + startTime + " and " +
                                DatabaseNameManager.COL_SESSION_START_TIME + " < " + endTime +
                                " order by " + DatabaseNameManager.COL_SESSION_START_TIME;

           // Log.d(LOG_TAG, "[querySessionsBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ GlobalNames.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();


        }catch (Exception e){

        }

        DatabaseManager.getInstance().closeDatabase();
        return rows;

    }
 
    //query task table
    public static ArrayList<String> querySessions (){
    	
    	ArrayList<String> rows = new ArrayList<String>();
    	
    	try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    		String sql = "SELECT *"  +" FROM " + DatabaseNameManager.SESSION_TABLE_NAME ;
    		
    		Cursor cursor = db.rawQuery(sql, null);   		
    		int columnCount = cursor.getColumnCount();
			while(cursor.moveToNext()){
				String curRow = "";
				for (int i=0; i<columnCount; i++){
					curRow += cursor.getString(i)+ GlobalNames.DELIMITER;		
				}
				rows.add(curRow);
			}
			cursor.close();

    		
    	}catch (Exception e){
    		
    	}

        DatabaseManager.getInstance().closeDatabase();
    	return rows;
    	
    }

    public static ArrayList<String> querySession(int sessionId){

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + DatabaseNameManager.SESSION_TABLE_NAME +
                    //condition with session id
                    " where " + DatabaseNameManager.COL_ID + " = " + sessionId + "";

           // Log.d(LOG_TAG, "[querySession] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ GlobalNames.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();


        }catch (Exception e){

        }

        DatabaseManager.getInstance().closeDatabase();
        return rows;


    }
    
    //get the number of existing session 
    public static long queryTaskCount (){
    	
    	long count = 0;
    	
    	try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    		String sql = "SELECT * "  +" FROM " + DatabaseNameManager.TASK_TABLE_NAME ;
    		Cursor cursor = db.rawQuery(sql, null);  
    		count = cursor.getCount();
    		
			cursor.close();


    	}catch (Exception e){
    		
    	}
        DatabaseManager.getInstance().closeDatabase();
    	return count;
    	
    }
    
    //get the number of existing session 
    public static long querySessionCount (){
    	
    	long count = 0;
    	
    	try{
            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    		String sql = "SELECT * "  +" FROM " + DatabaseNameManager.SESSION_TABLE_NAME ;
    		Cursor cursor = db.rawQuery(sql, null);  
    		count = cursor.getCount();
    		
			cursor.close();

    		
    	}catch (Exception e){
    		
    	}
        DatabaseManager.getInstance().closeDatabase();
    	return count;
    	
    }
    
    
    
    private static String generateTimeConstraintSQL(ArrayList<TimeConstraint> timeconstraints){
    	
    	String sql="";
    	float recency = -1;
    	long timestamp = -1;

    	if (timeconstraints.size()==0){
    		return null;
    	}
    	
    	for (int i = 0; i<timeconstraints.size(); i++){
    		
    		TimeConstraint tc = timeconstraints.get(i);
    		
    		if (tc.getType().equals(ConditionManager.CONDITION_TIME_CONSTRAINT_RECENCY) ){
    			recency = tc.getInterval();
    		}
    		else if (tc.getType().equals(ConditionManager.CONDITION_TIME_CONSTRAINT_EXACTTIME)){
    			timestamp = tc.getExactTime();
    		}
    		
    	}

		
		//add time into SQL
		if (recency!=-1){
			
			long targetTime =  getCurrentTimeInMilli() - (long)(recency*GlobalNames.MILLISECONDS_PER_SECOND) ;
			
			sql += DatabaseNameManager.COL_TIMESTAMP_LONG + " > "  + targetTime; 
			
		}
		else if (timestamp!=-1){

			//TODO: exact time
			
		}

    	
    	return sql;
    	
    }


    public static ArrayList<String> queryLastRecord(String table_name, int sessionId) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where " + DatabaseNameManager.COL_SESSION_ID + " = " + sessionId +
                    " order by " + DatabaseNameManager.COL_ID + " DESC LIMIT 1";
                    ;


            Log.d(LOG_TAG, "[queryLastRecord] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ GlobalNames.DELIMITER;
                }
                Log.d(LOG_TAG, "[queryLastRecord] get result row " +curRow);

                rows.add(curRow);
            }
            cursor.close();


        }catch (Exception e){

        }

        DatabaseManager.getInstance().closeDatabase();
        return rows;
    }

    public static ArrayList<String> queryRecordsInSession(String table_name, int sessionId, long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where " + DatabaseNameManager.COL_SESSION_ID + " = " + sessionId + " and " +
                    DatabaseNameManager.COL_TIMESTAMP_LONG + " > " + startTime + " and " +
                    DatabaseNameManager.COL_TIMESTAMP_LONG + " < " + endTime  +
                    " order by " + DatabaseNameManager.COL_TIMESTAMP_LONG;


            //Log.d(LOG_TAG, "[queryRecordsInSession] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ GlobalNames.DELIMITER;
                }
                Log.d(LOG_TAG, "[queryRecordsInSession] get result row " +curRow);

                rows.add(curRow);
            }
            cursor.close();


        }catch (Exception e){

        }

        DatabaseManager.getInstance().closeDatabase();
        return rows;


    }

    public static ArrayList<String> queryRecordsInSession(String table_name, int sessionId) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where " + DatabaseNameManager.COL_SESSION_ID + " = " + sessionId +
                    " order by " + DatabaseNameManager.COL_TIMESTAMP_LONG;


            Log.d(LOG_TAG, "[queryRecordsInSession] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ GlobalNames.DELIMITER;
                }
                Log.d(LOG_TAG, "[queryRecordsInSession] get result row " +curRow);

                rows.add(curRow);
            }
            cursor.close();


        }catch (Exception e){

        }

        DatabaseManager.getInstance().closeDatabase();
        return rows;


    }

    public static ArrayList<String> queryWithoutColumn(String table_name, int sessionId, ArrayList<TimeConstraint> timeconstraints){
		
    	ArrayList<String> rows = new ArrayList<String>();
    	
    	try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    		String sql = "SELECT *"  +" FROM " + table_name + " where " +
                    DatabaseNameManager.COL_SESSION_ID + " = " + sessionId + " and " ;

    		//generate timeconstraint sql
    		String timeconstraintSQL = generateTimeConstraintSQL (timeconstraints);
    		
    		//if there's no column to specify value, timeconstrain is the first column. So we put the Where only when there's a constraint to 
    		//put in the sql
    		if (timeconstraintSQL!=null){
    			sql += timeconstraintSQL;
    		}

    		
    		//Log.d(LOG_TAG, "[queryWithoutColumn] the query statement is " +sql);
    		
    		//execute the query
    		Cursor cursor = db.rawQuery(sql, null);   		
    		int columnCount = cursor.getColumnCount();
			while(cursor.moveToNext()){
				String curRow = "";
				for (int i=0; i<columnCount; i++){
					curRow += cursor.getString(i)+ GlobalNames.DELIMITER;		
				}
				//Log.d(LOG_TAG, "[queryWithoutColumn] get result row " +curRow);
	    		
				rows.add(curRow);
			}
			cursor.close();

    		
    	}catch (Exception e){
    		
    	}

        DatabaseManager.getInstance().closeDatabase();
    	return rows;
    	
    	
    }
    
    
    public static ArrayList<String> queryFromSingleColumn(String table_name, int sessionId, String column, String relation, String value, ArrayList<TimeConstraint> timeconstraints){
    	
    	////Log.d(LOG_TAG, "entering  queryFromSingleRecordTableByValue" );			
    	
    	ArrayList<String> rows = new ArrayList<String>();
    	
    	
    	/**although we will check time constraint in the eventMonitor, it's a good idea to reduce the number of data by adding time constraints in the query
    	 * Duration is a more complex calculation, because we need to find the first row (if any) and then check whether we have same data lasting for that duration.
    	 * However, it's relatively easy to set time constraint using recency and exact timestamp**/
    	
    	

    	try{

            SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
    		String sql = "SELECT *"  +" FROM " + table_name + " where " +
                    DatabaseNameManager.COL_SESSION_ID + " = " + sessionId + " and ";

    		
    		//generate sql statement based on the value
    		if (relation.equals(ConditionManager.CONDITION_RELATIONSHIP_STRING_EQUAL_TO) ){
    			 sql += column + " = '" + value + "'";
    		}
    		
    		//generate timeconstraint sql
    		String timeconstraintSQL = generateTimeConstraintSQL (timeconstraints);
    		if (timeconstraintSQL!=null){
    			sql += " and " + timeconstraintSQL;
    		}

    		
    		//Log.d(LOG_TAG, "[queryFromSingleColumn] the query statement is " +sql);
    		
    		//execute the query
    		Cursor cursor = db.rawQuery(sql, null);   		
    		int columnCount = cursor.getColumnCount();
			while(cursor.moveToNext()){
				String curRow = "";
				for (int i=0; i<columnCount; i++){
					curRow += cursor.getString(i)+ GlobalNames.DELIMITER;		
				}
				rows.add(curRow);
			}
			cursor.close();

    		
    	}catch (Exception e){
    		
    	}
        DatabaseManager.getInstance().closeDatabase();
    	return rows;
    }
    
    /**query result from multiple columns and values**/
    public static ArrayList<String> queryFromMultipleColumns(String table_name, int sessionid, ArrayList<String> columns, ArrayList<String> relations, ArrayList<String> values, ArrayList<TimeConstraint> timeconstraints){
    	
   
    	ArrayList<String> rows = new ArrayList<String>();
    	
    	return rows;
    }
    
    
	/***
	 * 
	 * 
	 * *Utility Functions**
	 * 
	 * 
	 * 
	 * ***/
	/**get the current time in milliseconds**/
	private static long getCurrentTimeInMilli(){		
		//get timzone		
		TimeZone tz = TimeZone.getDefault();		
		Calendar cal = Calendar.getInstance(tz);
		long t = cal.getTimeInMillis();		

		return t;
	}
	
	/**Generate a formated time string (in the format of "yyyy-MM-dd HH:mm:ss" **/
	private static String getTimeString(long time) {
		
		SimpleDateFormat sdf_now = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW);
		String timeString = sdf_now.format(time);
		
		return timeString;
	}

	

}
