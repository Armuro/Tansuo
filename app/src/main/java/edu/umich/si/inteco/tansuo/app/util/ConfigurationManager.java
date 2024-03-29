package edu.umich.si.inteco.tansuo.app.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.contextmanager.EventManager;
import edu.umich.si.inteco.tansuo.app.data.LocalDBHelper;
import edu.umich.si.inteco.tansuo.app.model.Condition;
import edu.umich.si.inteco.tansuo.app.model.Configuration;
import edu.umich.si.inteco.tansuo.app.model.EmailQuestionnaireTemplate;
import edu.umich.si.inteco.tansuo.app.model.Event;
import edu.umich.si.inteco.tansuo.app.model.Notification;
import edu.umich.si.inteco.tansuo.app.model.ProbeObjectControl.ActionControl;
import edu.umich.si.inteco.tansuo.app.model.Question;
import edu.umich.si.inteco.tansuo.app.model.QuestionnaireTemplate;
import edu.umich.si.inteco.tansuo.app.model.actions.Action;
import edu.umich.si.inteco.tansuo.app.model.actions.AnnotateAction;
import edu.umich.si.inteco.tansuo.app.model.actions.AnnotateRecordingAction;
import edu.umich.si.inteco.tansuo.app.model.actions.GenerateEmailQuestionnaireAction;
import edu.umich.si.inteco.tansuo.app.model.actions.GeneratingQuestionnaireAction;
import edu.umich.si.inteco.tansuo.app.model.actions.MonitoringEventAction;
import edu.umich.si.inteco.tansuo.app.model.actions.SavingRecordAction;

public class ConfigurationManager {

	private static final String LOG_TAG = "ConfigurationManager";

	public static final String CONFIGURATION_FILE_NAME = GlobalNames.CONFIGURATION_FILE_NAME_PARTI;

	public static final String CONFIGURATION_PROPERTIES_ID = "Id";
	public static final String CONFIGURATION_PROPERTIES_STUDY = "Study";
	public static final String CONFIGURATION_PROPERTIES_VERSION = "Version";
	public static final String CONFIGURATION_PROPERTIES_NAME = "Name";
	public static final String CONFIGURATION_PROPERTIES_CONTENT = "Content";
	public static final String CONFIGURATION_PROPERTIES_CONFIGURATION = "Configuration";

	public static final String TASK_PROPERTIES_ID = "Id";
	public static final String TASK_PROPERTIES_NAME = "Name";
	public static final String TASK_PROPERTIES_DESCRIPTION = "Description";
	public static final String TASK_PROPERTIES_TIMESTAMP_STRING = "Timestamp_string";
	public static final String TASK_PROPERTIES_CREATED_TIME = "Created_time";
	public static final String TASK_PROPERTIES_START_TIME = "Start_time";
	public static final String TASK_PROPERTIES_END_TIME = "End_time";

	public static final String CONFIGURATION_CATEGORY_ACTION = "Action";
	public static final String CONFIGURATION_CATEGORY_TASK = "Task";
	public static final String CONFIGURATION_CATEGORY_EVENT = "Event";
	public static final String CONFIGURATION_CATEGORY_QUESTIONNAIRE = "Questionnaire";

    public static final String SERVICE_SETTING_STOP_SERVICE_DURING_MIDNIGHT = "StopServiceDuringMidNight";


	private static LocalDBHelper mLocalDBHelper;
	private static Context mContext;
	
	public ConfigurationManager(Context context){		
		
		mContext = context;
		mLocalDBHelper = new LocalDBHelper(mContext, GlobalNames.TEST_DATABASE_NAME);
		loadConfiguration();
	}
	
	
	
	/**
	 * When the app is back to active, the app loads configurations from the database
	 */
	public void loadConfiguration() {
		
		Log.d(LOG_TAG, "[loadConfiguration]");
		
		//connect to the DB and load configuration from the DB
		
		ArrayList<String> res = new ArrayList<String>();		
	
		/** 1. first try to load configurations from the database **/
		res = mLocalDBHelper.queryConfigurations();
		Log.d(LOG_TAG, "[loadConfiguration] there are " + res.size() + " configurations in the database");
		/*
		for (int i=0; i<res.size() ; i++){
			
			String cline = res.get(i);			
			String [] separated = cline.split(GlobalNames.DELIMITER);
			//Log.d(LOG_TAG, "[loadConfiguration] the first configuration from the database has " + separated.length + " attributes the content is " + cline);
			
			int id = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_CONFIGURATION_ID]);
			int study_id = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_CONFIGURATION_STUDY_ID]);
			int version = Integer.parseInt(separated[DatabaseNameManager.COL_INDEX_CONFIGURATION_VERSION]);
			String name = separated[DatabaseNameManager.COL_INDEX_CONFIGURATION_NAME];
			String content_str = separated[DatabaseNameManager.COL_INDEX_CONFIGURATION_CONTENT];	
			
			JSONObject content=null;
			try {
				content = new JSONObject(content_str);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//create configuration
			Configuration config = new Configuration (id, study_id, version, name, content) ;

			//load the content of the configuration
			loadConfigurationContent (config);
			
		}
		*/




		/*** 2 if there is no data in the DB, then load file **/
		
		
		
		if (res.size()==0){

			//load files
            String filename = "";

            if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.PARTICIPATORY_LABELING_CONDITION)){

                filename = GlobalNames.CONFIGURATION_FILE_NAME_PARTI;
            }else if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.IN_STIU_LABELING_CONDITION)) {
                filename = GlobalNames.CONFIGURATION_FILE_NAME_IN_SITU;
            }else if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.POST_HOC_LABELING_CONDITION) ){
                filename = GlobalNames.CONFIGURATION_FILE_NAME_POST_HOC;
            }

            Log.d(LOG_TAG, "[loadConfiguration] no configuration in the database, load file.." + filename);
            String study_str = new FileHelper(mContext).loadFileFromAsset(filename);
			
			//load events and conditions
			try {
		
				JSONArray studyJSONArray = new JSONArray(study_str);
				
				for (int i=0; i< studyJSONArray.length(); i++){
					
					JSONObject studyJSON = studyJSONArray.getJSONObject(i);							
					
					//get the properties of the current study
					int study_id = studyJSON.getInt(CONFIGURATION_PROPERTIES_ID);
					String study_name = studyJSON.getString(CONFIGURATION_PROPERTIES_NAME);
					Log.d(LOG_TAG, "[loadConfiguration]  Now reading the study " + study_id + " : " + study_name);
					
					
					//now get configuration JSON
					JSONObject configJSON = studyJSON.getJSONObject(CONFIGURATION_PROPERTIES_CONFIGURATION);
					
					//get properties of the config
					int id = configJSON.getInt(CONFIGURATION_PROPERTIES_ID);
					int version = configJSON.getInt(CONFIGURATION_PROPERTIES_VERSION);
					String name = configJSON.getString(CONFIGURATION_PROPERTIES_NAME);					
					JSONObject content = configJSON.getJSONObject(CONFIGURATION_PROPERTIES_CONTENT);			
					Configuration config = new Configuration (id, study_id, version, name, content) ;
					
					//Load the content of the configuration
					loadConfigurationContent (config);
					
					//store the configuration into the database
				//	Log.d(LOG_TAG, "[loadConfiguration]  After creating the configuration object, inser the configuration into the database");
					
					mLocalDBHelper.insertConfigurationTable(config, DatabaseNameManager.CONFIGURATION_TABLE_NAME);
	
				}
				
			
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}				
		}
		
		
		
	}
	
	
	/**
	 * The Configuration has a source in JSON format. The function takse a JSON and configurate the app
	 */
	public void loadConfigurationContent(Configuration config) {
		
		//source is in JSON format
		JSONObject content = config.getContent();
		
		Log.d(LOG_TAG, "[loadConfigurationContent]  load the configuration content of study " + config.getStudyId());

        //load configuration settings
        try {
            if (content.has(ConfigurationManager.SERVICE_SETTING_STOP_SERVICE_DURING_MIDNIGHT)){
                boolean stopServiceDuringMidNight = content.getBoolean(ConfigurationManager.SERVICE_SETTING_STOP_SERVICE_DURING_MIDNIGHT);
                PreferenceHelper.setPreferenceValue(ConfigurationManager.SERVICE_SETTING_STOP_SERVICE_DURING_MIDNIGHT, stopServiceDuringMidNight);
               
                //write into the preference

            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


		/** load events **/
		try {
			if (content.has(ConfigurationManager.CONFIGURATION_CATEGORY_EVENT)){
                JSONArray eventsJSON = content.getJSONArray(ConfigurationManager.CONFIGURATION_CATEGORY_EVENT);
                loadEventsFromJSON (eventsJSON, config.getStudyId());
            }
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		/** load actions **/
		try {
            if (content.has(ConfigurationManager.CONFIGURATION_CATEGORY_ACTION)){
                JSONArray actionsJSON = content.getJSONArray(ConfigurationManager.CONFIGURATION_CATEGORY_ACTION);
                loadActionsFromJSON (actionsJSON, config.getStudyId());
            }

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/** load questionnaires**/
		try {

            if (content.has(ConfigurationManager.CONFIGURATION_CATEGORY_QUESTIONNAIRE)){
                JSONArray questionnairesJSON = content.getJSONArray(ConfigurationManager.CONFIGURATION_CATEGORY_QUESTIONNAIRE);
                loadQuestionnairesFromJSON (questionnairesJSON, config.getStudyId());
            }

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * 
	 * @param eventsJSON
	 */
	public static void loadEventsFromJSON (JSONArray eventsJSON, int study_id){	

		Log.d(LOG_TAG, "[loadEventsFromJSON] load the event content of study " + study_id);
		
		
		for (int i = 0; i < eventsJSON.length(); i++){
			
			Event event = null;
			JSONObject eventJSON = null;
			
			try {
				eventJSON = eventsJSON.getJSONObject(i);
				
				int id= eventJSON.getInt("Id");
				String name = eventJSON.getString("Name");
				String description = eventJSON.getString("Description");

				//creat event object
				event = new Event(id, name, study_id);
				
				//add the conditionJSON to the event
				if (eventJSON.has("Condition")){
					
					JSONArray conditionJSONArray = eventJSON.getJSONArray("Condition");	
					event.setConditionJSON(conditionJSONArray);
					
					//get the list of condition from each event
					ArrayList<Condition> conditions = loadConditionsFromJSON(conditionJSONArray);
					
					Log.d(LOG_TAG, "[ In loadEventsFromJSON] setting conditionJSONArray: " + conditionJSONArray);
					//set the condition arraylist to the event.
					event.setConditionSet(conditions);
				}

				
			} catch (JSONException e1) {

				e1.printStackTrace();
			}
			
			/** after creating the event object, add event to eventList, and to the databasse..**/
			//add to the list
			EventManager.addEvent(event);
			Log.d(LOG_TAG, "[loadEventsFromJSON] adding the events into the EventManager, now it has " + EventManager.getEventList().size() + " events");
		}//end of reading eventJSONArray
		
		
	
	}
	
	
	/**
	 *
     */
	public static void loadActionsFromJSON(JSONArray actionJSONArray, int study_id) {

		Log.d(LOG_TAG, " [loadActionsFromJSON] there are "  +   actionJSONArray.length() + " actions" );
		
		//load details for each action
		for (int i = 0; i < actionJSONArray.length(); i++){
			
			JSONObject actionJSON = null;
			JSONObject controlJSON = null;				
			
			Action action=null;
			
			try {
				
				//get JSON for action and schedule within the action
				actionJSON = actionJSONArray.getJSONObject(i);

				/** 1. first create action and schedule object based on the required field, then set propoerties based on the schedule type**/
				
				//get action required fields
				int action_id= actionJSON.getInt(ActionManager.ACTION_PROPERTIES_ID);
				String type = actionJSON.getString(ActionManager.ACTION_PROPERTIES_TYPE);
				String execution_style = actionJSON.getString(ActionManager.ACTION_PROPERTIES_EXECUTION_STYLE);
				controlJSON = actionJSON.getJSONObject(ActionManager.ACTION_PROPERTIES_CONTROL);
				String name = actionJSON.getString(ActionManager.ACTION_PROPERTIES_NAME);
				
				Log.d(LOG_TAG, "[loadActionsFromJSON] examine action" + " action: " + action_id + " , for type " + type
						+ " execution style " + execution_style );
				
				
				action = new Action (action_id, name, type, execution_style, study_id);

				/** 2 if the action is to generate a quesitonnaire add questionnaire id **/					
				if (type.equals(ActionManager.ACTION_TYPE_QUESTIONNAIRE)){						
					
					int questionnaire_id = actionJSON.getInt(ActionManager.ACTION_PROPERTIES_QUESTIONNAIRE_ID);
					GeneratingQuestionnaireAction a = new GeneratingQuestionnaireAction (action_id, name, type,execution_style, study_id);
					a.setQuestionnaireId(questionnaire_id);
					action = a; 
					//Log.d(LOG_TAG, " the aciton" + action.getId() + " questionnaire id:  " + a.getQuestionnaireId());
					
				}
                else if (type.equals(ActionManager.ACTION_TYPE_EMAIL_QUESTIONNAIRE)){

                    int questionnaire_id = actionJSON.getInt(ActionManager.ACTION_PROPERTIES_QUESTIONNAIRE_ID);
                    GenerateEmailQuestionnaireAction a = new GenerateEmailQuestionnaireAction (action_id, name, type,execution_style, study_id);
                    a.setQuestionnaireId(questionnaire_id);
                    action = a;

                    Log.d(LOG_TAG, "[loadActionsFromJSON] examine action" + " action: " + action_id + " , for type " + type
                            + " execution style " + execution_style );

                }
				
				/** 3. if the action is to monitor events add monitored event ids.**/
				else if (type.equals(ActionManager.ACTION_TYPE_MONITORING_EVENTS)){						
					
					String monitor_event_ids = actionJSON.getString(ActionManager.ACTION_PROPERTIES_MONITORING_EVENTS);
					String [] ids = monitor_event_ids.split(",");
					
					MonitoringEventAction a = new MonitoringEventAction (action_id, name, type, execution_style, study_id);

					
					for (int j=0; j<ids.length; j++){
						int id = Integer.parseInt(ids[j]);
						a.addMonitoredEvent(id);
						Log.d(LOG_TAG, " [loadActionsFromJSON] the aciton" + action.getId() + " monitors event:  "  +  id);

					}
					
					action  = a;
					
					
				}

                //if the action is to save record
                else if (type.equals(ActionManager.ACTION_TYPE_SAVING_RECORD)) {

                    SavingRecordAction a = new SavingRecordAction(action_id,name, type,execution_style, study_id );
                    action = a;
                }

                //just annotate without recording
                else if (type.equals(ActionManager.ACTION_TYPE_ANNOTATE)) {

                    JSONObject annotateJSON = actionJSON.getJSONObject(ActionManager.ACTION_PROPERTIES_ANNOTATE);

                    String mode = annotateJSON.getString(ActionManager.ACTION_PROPERTIES_ANNOTATE_MODE);
                    String vizType = annotateJSON.getString(ActionManager.ACTION_PROPERTIES_VIZUALIZATION_TYPE);
                    String reviewRecordingMode = annotateJSON.getString(ActionManager.ACTION_PROPERTIES_ANNOTATE_REVIEW_RECORDING);

                    AnnotateAction a = new AnnotateAction (action_id, name, type, execution_style, study_id, mode, vizType, reviewRecordingMode);
                    action = a;
                }

                //if the action is to annotate recording
                else if (type.equals(ActionManager.ACTION_TYPE_ANNOTATE_AND_RECORD)) {

                    JSONObject annotateJSON = actionJSON.getJSONObject(ActionManager.ACTION_PROPERTIES_ANNOTATE);

                    String mode = annotateJSON.getString(ActionManager.ACTION_PROPERTIES_ANNOTATE_MODE);
                    String recordingType = annotateJSON.getString(ActionManager.ACTION_PROPERTIES_ANNOTATE_RECORDING_TYPE);
                    String vizType = annotateJSON.getString(ActionManager.ACTION_PROPERTIES_VIZUALIZATION_TYPE);
                    boolean allowAnnotateInProcess = annotateJSON.getBoolean(ActionManager.ACTION_PROPERTIES_ANNOTATE_ALLOW_ANNOTATE_IN_PROCESS);
                    String reviewRecordingMode = annotateJSON.getString(ActionManager.ACTION_PROPERTIES_ANNOTATE_REVIEW_RECORDING);
                    boolean recordingStartByUser = annotateJSON.getBoolean(ActionManager.ACTION_PROPERTIES_RECORDING_STARTED_BY_USER);

                    AnnotateRecordingAction annotateRecordingAction =
                            new AnnotateRecordingAction(
                                    action_id,
                                    name,
                                    type,
                                    execution_style,
                                    study_id,
                                    mode,
                                    recordingType,
                                    vizType,
                                    allowAnnotateInProcess,
                                    reviewRecordingMode,
                                    recordingStartByUser);

                    Log.d(LOG_TAG, "[loadActionsFromJSON] the action is annotateRecording, mode: "
                            + mode + " recordType: " + recordingType + " vizType " + vizType + " allowannotate " + allowAnnotateInProcess + " review recording: " + reviewRecordingMode
                             + " recording start by user " + recordingStartByUser
                            );


                    action  = annotateRecordingAction;

                }



				/**4. examine whether the action is contiuous or not**/
				if (actionJSON.has(ActionManager.ACTION_PROPERTIES_CONTINUITY)){
					
					JSONObject continuityJSON = actionJSON.getJSONObject(ActionManager.ACTION_PROPERTIES_CONTINUITY);
					Log.d(LOG_TAG, "[loadActionsFromJSON] the continuityJSON:  " + continuityJSON.toString());
					
					float rate= (float) continuityJSON.getDouble(ActionManager.ACTION_CONTINUITY_PROPERTIES_RATE);
					int duration = continuityJSON.getInt(ActionManager.ACTION_CONTINUITY_PROPERTIES_DURATION);
					
					action.setActionDuration(duration);
					action.setActionRate(rate);
					action.setContinuous(true);
					Log.d(LOG_TAG, "[loadActionsFromJSON] the action " + action.getId() + " is continuous " + action.isContinuous() + " rate: "
							+ action.getActionRate()  +" duration  " + action.getActionDuration());
					
					
					
				}else {
					action.setContinuous(false);
					Log.d(LOG_TAG, "[loadActionsFromJSON] the action " + action.getId() + " is not continuous " + action.isContinuous() );
				}
				
			
				
				/**5. check whether there are notification of the action **/			
				if (actionJSON.has(ActionManager.ACTION_PROPERTIES_NOTIFICATION)){

                    //notification is an array
                    JSONArray notiJSONArray = actionJSON.getJSONArray(ActionManager.ACTION_PROPERTIES_NOTIFICATION);

                    for (int j=0; j<notiJSONArray.length(); j++){

                        JSONObject notiJSONObject  = notiJSONArray.getJSONObject(j);

                        String notiType = notiJSONObject.getString(ActionManager.ACTION_PROPERTIES_NOTIFICATION_TYPE);
                        String notiLaunch = notiJSONObject.getString(ActionManager.ACTION_PROPERTIES_NOTIFICATION_LAUNCH);
                        String notiTitle = notiJSONObject.getString(ActionManager.ACTION_PROPERTIES_NOTIFICATION_TITLE);
                        String notiMessage = notiJSONObject.getString(ActionManager.ACTION_PROPERTIES_NOTIFICATION_MESSAGE);

                        Notification notification = new Notification(notiLaunch, notiType, notiTitle, notiMessage);

                        action.addNotification(notification);

                        if ( action.getNotifications().size()>=1)
                            Log.d(LOG_TAG, "[loadActionsFromJSON and notification] the action notification title: "+
                                action.getNotifications().get( action.getNotifications().size()-1 ).getTitle() +
                                action.getNotifications().get( action.getNotifications().size()-1 ).getMessage()
                            );

                    }


				}				
				
				/** 6 load controls to actions**/					
				loadActionControlsFromJSON (controlJSON, action);
		

			}catch (JSONException e1) {

				e1.printStackTrace();
			}

			
			if (action!=null){					
				//add action into the list
				ActionManager.getActionList().add(action);
			}
		}//for each action
	}
	
	
	/**
	 * 
	 * @param conditionJSONArray
	 * @return
	 */
	public static ArrayList<Condition> loadConditionsFromJSON(JSONArray conditionJSONArray) {
		
		
		 ArrayList<Condition> conditions = new  ArrayList<Condition>();
		
		try {
				
			Log.d(LOG_TAG, "[loadConditionsFromJSON] the conditions of the current event is:  " +conditionJSONArray.toString());	
			
			for (int j = 0; j < conditionJSONArray.length(); j++){
				
				JSONObject conditionJSON = conditionJSONArray.getJSONObject(j);
				
				String conditionType = conditionJSON.getString(ConditionManager.CONDITION_PROPERTIES_TYPE);
				String conditionRelationship = conditionJSON.getString(ConditionManager.CONDITION_PROPERTIES_RELATIONSHIP);
				
				Condition condition;
				//if the target value is String 
				if (conditionRelationship.equals(ConditionManager.CONDITION_RELATIONSHIP_STRING_EQUAL_TO) || 
						conditionRelationship.equals(ConditionManager.CONDITION_RELATIONSHIP_STRING_NOT_EQUAL_TO) ||
                        conditionRelationship.equals(ConditionManager.CONDITION_RELATIONSHIP_IS) ){

					String targetValue = conditionJSON.getString(ConditionManager.CONDITION_PROPERTIES_TARGETVALUE);
					//create condition object
					condition = new Condition(
							conditionType, 
							conditionRelationship, 
							targetValue);
				}

				//else is not String
				else{
					float targetValue = (float) conditionJSON.getDouble(ConditionManager.CONDITION_PROPERTIES_TARGETVALUE);
					//create condition object
					condition = new Condition(
							conditionType, 
							conditionRelationship, 
							targetValue);	
				}
					
			
				
				Log.d(LOG_TAG, "[loadConditionsFromJSON] get condition from the file:" + condition.getType() + " , " + condition.getRelationship());
				
				
				//add addition fields to the condition 
				if (conditionType.equals(ConditionManager.CONDITION_TYPE_DISTANCE_TO)){
					
					double lat = conditionJSON.getDouble(ConditionManager.CONDITION_PROPERTIES_LATITUDE);
					double lng = conditionJSON.getDouble(ConditionManager.CONDITION_PROPERTIES_LONGITUDE);
					condition.setLatLng(lat, lng);
					/*
					Log.d(LOG_TAG, "[loadConditionsFromJSON] condition of " + condition.getType() + 
							" lat: " + condition.getLatLng().latitude + 
							" lng: " + condition.getLatLng().longitude);
					*/
					
					
				}
				
				
				//get constraint from the condition
				if (conditionJSON.has("Constraint")){
					
					try {
						
						JSONArray constraintJSONArray = conditionJSON.getJSONArray("Constraint");									
						
						
							for (int k = 0; k < constraintJSONArray.length(); k++){
								
								JSONObject constraintJSON = constraintJSONArray.getJSONObject(k);
								
								String constraintType = constraintJSON.getString("Type");										
								String constraintRelationship = constraintJSON.getString("Relationship");
								float constraintValue = Float.parseFloat(constraintJSON.getString("TargetValue"))  ;
								
								//Log.d(LOG_TAG, "get constraint from the file:" + constraintType + " , " + constraintRelationship + " , " + constraintValue);
	
								condition.addTimeConstraint(
										constraintType,
										constraintValue, 
										constraintRelationship);								
							}
							
						}catch (JSONException e2) {

							e2.printStackTrace();
						}
						
					}
					
					conditions.add(condition);
					

				}//end of reading conditionJSONArray
						

		}catch (JSONException e) {
			
			e.printStackTrace();
		}
		
		Log.d(LOG_TAG, " the current event has " + conditions.size() + " condition");				
		return conditions;
		
		
	}



	
	/***
	 * read controlJSON and add control objects to the action
	 * @param controlJSON
	 * @param action
	 */
	public static void loadActionControlsFromJSON (JSONObject controlJSON, Action action) {

		//if the action control is to start an action. Most action controls belong to this type. 
		if (controlJSON.has(ActionManager.ACTION_PROPERTIES_START)){
			
			JSONArray startJSONArray = null;
			try {
				startJSONArray = controlJSON.getJSONArray(ActionManager.ACTION_PROPERTIES_START);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (startJSONArray!=null){
				
				for (int i = 0; i < startJSONArray.length(); i++){
					
					JSONObject startJSONObject=null;
					try {
						startJSONObject = startJSONArray.getJSONObject(i);
						//instantiate an Action Control with type "Start"
						//set id based on the number of existing action contorl
						int id = ActionManager.getActionControlList().size()+1;
						
						//create an ActionControl object 
						ActionControl ac = new ActionControl (id, startJSONObject, ActionManager.ACTION_CONTROL_TYPE_START, action);
						
						//add the ActionControl object to the list
						ActionManager.getActionControlList().add(ac);

						
						Log.d(LOG_TAG, "[loadActionControlsFromJSON]  the start acitonControl id is " + ac.getId() + " connects to action " + ac.getAction().getId() + " " + ac.getAction().getName() +
								" and has schedule : " + ac.getSchedule());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
			
		}
		//if the action control is to stop an action
		if (controlJSON.has(ActionManager.ACTION_PROPERTIES_STOP)) {
			
			JSONArray stopJSONArray;
			try {
				stopJSONArray = controlJSON.getJSONArray(ActionManager.ACTION_PROPERTIES_STOP);
				
				for (int i = 0; i < stopJSONArray.length(); i++){
					
					JSONObject stopJSONObject = stopJSONArray.getJSONObject(i);
					//instantiate an Action Control with type "Stop"
					int id = ActionManager.getActionControlList().size()+1;
					ActionControl ac = new ActionControl (id, stopJSONObject, ActionManager.ACTION_CONTROL_TYPE_STOP, action);
					ActionManager.getActionControlList().add(ac);
					
					Log.d(LOG_TAG, "[loadActionControlsFromJSON]  the stop acitonControl id is " + ac.getId() + " connects to action " + ac.getAction().getId() + 
							" and has schedule : " + ac.getSchedule());
					
					
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//if an action control is to pause an action 
		if (controlJSON.has(ActionManager.ACTION_PROPERTIES_PAUSE)) {
			
			JSONArray pauseJSONArray;		
			
			try {
				pauseJSONArray = controlJSON.getJSONArray(ActionManager.ACTION_PROPERTIES_PAUSE);
				
				Log.d(LOG_TAG, "[loadActionControlsFromJSON] found pause JSON " +  pauseJSONArray);
				
				
				for (int i = 0; i < pauseJSONArray.length(); i++){
					
					JSONObject pauseJSONObject = pauseJSONArray.getJSONObject(i);
					//instantiate an Action Control with type "Pause"
					int id = ActionManager.getActionControlList().size()+1;
					ActionControl ac = new ActionControl (id, pauseJSONObject, ActionManager.ACTION_CONTROL_TYPE_PAUSE, action);
					ActionManager.getActionControlList().add(ac);
					
					Log.d(LOG_TAG, "[loadActionControlsFromJSON]  the pause acitonControl id is " + ac.getId() + " connects to action " + ac.getAction().getId() + 
							" and has schedule : " + ac.getSchedule());
					
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//if the action control is to resume an action 
		if (controlJSON.has(ActionManager.ACTION_PROPERTIES_RESUME)) {
			
			JSONArray resumeJSONArray;
			try {
				resumeJSONArray = controlJSON.getJSONArray(ActionManager.ACTION_PROPERTIES_RESUME);
				
				Log.d(LOG_TAG, "[loadActionControlsFromJSON] found resume JSON " +  resumeJSONArray);
				
				
				for (int i = 0; i < resumeJSONArray.length(); i++){
					
					JSONObject resumeJSONObject = resumeJSONArray.getJSONObject(i);
					//instantiate an Action Control with type "Resume"
					int id = ActionManager.getActionControlList().size()+1;
					ActionControl ac = new ActionControl (id, resumeJSONObject, ActionManager.ACTION_CONTROL_TYPE_RESUME, action);			
					ActionManager.getActionControlList().add(ac);
					
					Log.d(LOG_TAG, "[loadActionControlsFromJSON]  the resume acitonControl id is " + ac.getId() + " connects to action " + ac.getAction().getId() + 
							" and has schedule : " + ac.getSchedule());
					
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//if the action control is to cancel an action
		if (controlJSON.has(ActionManager.ACTION_PROPERTIES_CANCEL)) {
			
			JSONArray cancelJSONArray;
			try {
				cancelJSONArray = controlJSON.getJSONArray(ActionManager.ACTION_PROPERTIES_CANCEL);
				
				for (int i = 0; i < cancelJSONArray.length(); i++){
					
					JSONObject cancelJSONObject = cancelJSONArray.getJSONObject(i);
					//instantiate an Action Control with type "Cancel"
					int id = ActionManager.getActionControlList().size()+1;
					ActionControl ac = new ActionControl (id, cancelJSONObject, ActionManager.ACTION_CONTROL_TYPE_CANCEL, action);
					ActionManager.getActionControlList().add(ac);
					
					Log.d(LOG_TAG, "[loadActionControlsFromJSON]  the acitonControl id is " + ac.getId() + " connects to action " + ac.getAction().getId() + 
							" and has schedule : " + ac.getSchedule());
					
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Log.d(LOG_TAG, "[loadActionControlsFromJSON] after reading action " + action.getId() + " the service has " + ActionManager.getActionControlList().size() + " actioncontrols");
		
	}
	

	

	/**
	 * Load Questions from JSONArray
	 * @param questionnJSONArray
	 * @return
	 */
	private static ArrayList<Question> loadQuestionsFromJSON(JSONArray questionnJSONArray){
		
		
		Log.d(LOG_TAG, "the questionJSONArray is " + questionnJSONArray.toString());
		
		ArrayList<Question> questions = new ArrayList<Question>();
		
		for (int i = 0; i < questionnJSONArray.length(); i++){
			
			JSONObject questionJSON=null;
			Question question = null;

			try {
				
				questionJSON = questionnJSONArray.getJSONObject(i);
				
				
				int index = questionJSON.getInt(QuestionnaireManager.QUESTION_PROPERTIES_INDEX);
				String type = questionJSON.getString(QuestionnaireManager.QUESTION_PROPERTIES_TYPE);
				String question_text = questionJSON.getString(QuestionnaireManager.QUESTION_PROPERTIES_QUESTION_TEXT);
					
				
				question = new Question(index, question_text, type);
				
				Log.d (LOG_TAG, "[loadQuestionsFromJSON]  the question object is " + question.getIndex() + " text: " + question.getText() + " type " + question.getType());
					
				//get options (e.g. multi choice
				if (questionJSON.has(QuestionnaireManager.QUESTION_PROPERTIES_OPTION)){
					
					JSONArray optionJSONArray=null;		
					ArrayList<String> options = new ArrayList<String>();
					
					optionJSONArray = questionJSON.getJSONArray(QuestionnaireManager.QUESTION_PROPERTIES_OPTION);
					
					Log.d (LOG_TAG, "[loadQuestionsFromJSON] the question also has "  + optionJSONArray.length() + " options"); 
							
					for (int j=0; j<optionJSONArray.length(); j++){
						
						JSONObject optionJSON = optionJSONArray.getJSONObject(j);
						
						String option_text = optionJSON.getString(QuestionnaireManager.QUESTION_PROPERTIES_OPTION_TEXT);
						options.add(option_text);
						
						Log.d (LOG_TAG, " option " + j + " : "  + option_text); 
						
					}
					
					question.setOptions(options);

				}

				//other fields of the question...
				if (questionJSON.has(QuestionnaireManager.QUESTION_PROPERTIES_HAS_OTHER_FIELD)){

					question.setHasOtherField(questionJSON.getBoolean(QuestionnaireManager.QUESTION_PROPERTIES_HAS_OTHER_FIELD));
					
				}

                //the questions has dynamic content that needs to be extracted from the database
                if (questionJSON.has(QuestionnaireManager.QUESTION_PROPERTIES_DATA)){

                    question.setDataJSON(questionJSON.getJSONObject(QuestionnaireManager.QUESTION_PROPERTIES_DATA));

                }
				
				
				
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			questions.add(question);
			
		}
			
		
		
		
		
		
		return questions;
				
	}
	
	
	/**
	 * Load Questionnaire from JSON file
	 * @param study_id
	 */
	private static void loadQuestionnairesFromJSON(JSONArray questionnaireJSONArray, int study_id){
				
		Log.d(LOG_TAG, "loadQuestionnaireFromJSON there are "  +   questionnaireJSONArray.length() + " questionnaires" );

		//load details for each action
		for (int i = 0; i < questionnaireJSONArray.length(); i++){
			
			JSONObject questionnaireJSON;
			JSONArray questionsJSONArray;
			
			QuestionnaireTemplate questionnaireTemplate;
			ArrayList<Question> questions = new ArrayList<Question> ();
			
			try {
				
				questionnaireJSON = questionnaireJSONArray.getJSONObject(i);
				
				int id = questionnaireJSON.getInt(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_ID);
				String title = questionnaireJSON.getString(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_TITLE);
                String type = questionnaireJSON.getString(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_TYPE);
				questionsJSONArray = questionnaireJSON.getJSONArray(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_QUESTIONS);

                questionnaireTemplate = new QuestionnaireTemplate(id, title, study_id, type);

				if (questionnaireJSON.has(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_DESCRIPTION)){
					String description = questionnaireJSON.getString(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_DESCRIPTION);
					questionnaireTemplate.setDescription(description);					
				}

                //if the questionnaire is through email
                if (type.equals(QuestionnaireManager.QUESTIONNAIRE_TYPE_EMAIL)) {

                    EmailQuestionnaireTemplate template = new EmailQuestionnaireTemplate (id, title, study_id, type);

                    //the questionnaire shoud has "Email" field
                    JSONObject emailJSON = questionnaireJSON.getJSONObject(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_EMAIL);
                    JSONArray recipientsJSONArray = emailJSON.getJSONArray(QuestionnaireManager.QUESTIONNAIRE_EMAIL_PROPERTIES_RECIPIENTS);
                    String subject = emailJSON.getString(QuestionnaireManager.QUESTIONNAIRE_EMAIL_PROPERTIES_SUBJECT);

                    //get recipients from the JSONArray
                    ArrayList<String> recipients = new ArrayList<String>();
                    for (int j=0; j<recipientsJSONArray.length(); j++){
                        String recipient = recipientsJSONArray.getString(j);
                        recipients.add(recipient);
                    }

                    //convert arraylist to String[]
                    String [] re = new String [recipients.size()];
                    recipients.toArray(re);


                    //add information to the template
                    template.setSubject(subject);
                    template.setRecipients(re);

                    //referenc the template back to this emailTemplate
                    questionnaireTemplate = template;
                }


				//read Questions..
				questions = loadQuestionsFromJSON(questionsJSONArray);
				
				//add questions to the questionnaire
				questionnaireTemplate.setQuestions(questions);
				QuestionnaireManager.addQuestionnaireTemplate(questionnaireTemplate);

				
			} catch (JSONException e1) {

			}
			
			
		}

	}


	
}
