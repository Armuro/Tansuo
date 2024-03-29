package edu.umich.si.inteco.tansuo.app.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.model.ProbeObjectControl.ActionControl;
import edu.umich.si.inteco.tansuo.app.model.Schedule;
import edu.umich.si.inteco.tansuo.app.services.CaptureProbeService;

public class ScheduleAndSampleManager {

	private static final String LOG_TAG = "ScheduleManager";
	
	private static AlarmManager mAlarmManager;
	
	private static Context mContext;
	
	public static int bedStartTime = 23;
	public static int bedEndTime = 6;
    public static int bedMiddleTime = 2;
    public static long time_base = 0;

    public static int pauseProbeService = bedMiddleTime;    //3 AM
    public static long resumeProbeService = bedEndTime;

	
	//this stores all of the request codes of been scheduled alarm
	private static ArrayList<Integer> mAlarmRequestCodes;
	
	public static final String ALARM_REQUEST_CODE = "alarm_request_code";
	
	public static final String SCHEDULE_TYPE_EVENT_CONTINGENT = "event_contingent";
	public static final String SCHEDULE_TYPE_INTERVAL_CONTINGENT = "interval_contingent";
	
	public static final String SCHEDULE_SAMPLE_METHOD_SIMPLE_ONE_TIME = "simple_one_time";
	public static final String SCHEDULE_SAMPLE_METHOD_RANDOM = "random";
	public static final String SCHEDULE_SAMPLE_METHOD_RANDOM_WITH_MINIMUM_INTERVAL = "random_with_minimum_interval";
	public static final String SCHEDULE_SAMPLE_METHOD_FIXED_TIME_OF_DAY = "fixed_time_of_day";
	public static final String SCHEDULE_SAMPLE_METHOD_FIXED_INTERVAL = "fixed_interval";
	
	
	
	public static final String SCHEDULE_PROPERTIES_TYPE = "Type";
	public static final String SCHEDULE_PROPERTIES_SAMPLE_METHOD = "Sample_method";
	public static final String SCHEDULE_PROPERTIES_SAMPLE_DELAY = "Sample_delay";
	public static final String SCHEDULE_PROPERTIES_SAMPLE_INTERVAL = "Sample_interval";
	public static final String SCHEDULE_PROPERTIES_SAMPLE_DURATION = "Sample_duration";
	public static final String SCHEDULE_PROPERTIES_SAMPLE_COUNT = "Sample_count";
	public static final String SCHEDULE_PROPERTIES_SAMPLE_END_AT = "Sample_endAt";
	public static final String SCHEDULE_PROPERTIES_FIXED_TIME_OF_DAY = "Time_of_day";
	public static final String SCHEDULE_PROPERTIES_SAMPLE_MINIMUM_INTERVAL = "Sample_min_interval";
	
	public ScheduleAndSampleManager(Context context){
		
		mContext = context;
		mAlarmManager = (AlarmManager)mContext.getSystemService( mContext.ALARM_SERVICE );
		mAlarmRequestCodes = new ArrayList<Integer>();
		
		//register action alarm receiver to receive action alarms.
		registerAlarmReceivers();

        //time base is for generated request code
		time_base = getCurrentTimeInMillis();
		
	}
	
	
	//register all alarm receivers. 
	public void registerAlarmReceivers(){
		
		//register alarm the receiver for action alarm
		registerActionAlarmReceiver();

        generateRefreshServiceScheduleAlarms();
	}

    /**
     * this function is for scheduling alarms for updating the schedule of action controls everyday
     * The schedule of the updateScheduleAlarm is at the end of bed time
     */
    public void registerUpdateScheduleAlarm() {

        Log.d(LOG_TAG, "[test reschedule][egisterUpdateScheduleAlarm] the alarm receiver  with request code " );

    }

    public static void unregisterAlarmReceivers() {

        //unregister action alarm receiver
        unregisterActionAlarmReceiver();

        //unregister update schedule alarm receiver
        //unregisterUpdateScheduleAlarmReceiver();
    }

/*
    public static void registerUpdateScheduleAlarmReceiver(){

        Log.d(LOG_TAG, "[test reschedule][registerUpdateScheduleAlarmReceiver] " );


        //register action alarm
        IntentFilter alarm_filter = new IntentFilter();
        alarm_filter.addAction(GlobalNames.UPDATE_SCHEDULE_ALARM);
        alarm_filter.addAction(GlobalNames.STOP_SERVICE_ALARM);
        alarm_filter.addAction(GlobalNames.START_SERVICE_ALARM);

        mContext.registerReceiver(UpdateScheduleAlarmReceiver, alarm_filter);

    }
*/
	
	public static void registerActionAlarmReceiver(){
		//register action alarm
		IntentFilter alarm_filter = new IntentFilter(GlobalNames.ACTION_ALARM);
		mContext.registerReceiver(ActionAlarmReceiver, alarm_filter);
		
	}

	public static void unregisterActionAlarmReceiver(){
		mContext.unregisterReceiver(ActionAlarmReceiver);
	}

    /*
    public static void unregisterUpdateScheduleAlarmReceiver(){
         mContext.unregisterReceiver(UpdateScheduleAlarmReceiver);
    }
*/

    public static long getSamplingAtTime(Schedule schedule) {

        //get information of how
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH)+1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        long time = -1;

        //first get hour and minute of the specified time
        int t_hour  = getHourOfTimeOfDay(schedule.getFixedTimeOfDay());
        int t_minute = getMinuteOfTimeOfDay(schedule.getFixedTimeOfDay());

        //create the specified time in milliseconds today
        Calendar designatedTime = Calendar.getInstance();
        designatedTime.set(year, month-1, day, t_hour, t_minute);

        Log.d(LOG_TAG, "[test fixed time][getSamplingTime] the designatedTime is : " + getTimeString(designatedTime.getTimeInMillis()) );

        ////compare if the designated time has passed (i.e. the curren time  > designated time ), return -1.
        if(cal.after(designatedTime)){
            // day + 1
            designatedTime.roll(Calendar.DAY_OF_MONTH, 1);
				Log.d(LOG_TAG, "[test fixed time][getSamplingTime] the timeOfDay " + schedule.getFixedTimeOfDay() + " has passed, now is already " + hour+ " so we don't scheudle it" );
        }
        //the time has not been passed
        else {
            Log.d(LOG_TAG, "[test fixed time][getSamplingTime] the timeOfDay " + schedule.getFixedTimeOfDay() + " has not passed, we will scheudle the action at " + getTimeString(designatedTime.getTimeInMillis() ) );
            time = designatedTime.getTimeInMillis();
        }


        return time;
    }
	
	private static long getSamplingStartTime(long base, Schedule schedule){
		
		return (base + schedule.getSampleDelay()*GlobalNames.MILLISECONDS_PER_SECOND);
		
	}
	
	private static long getSamplingEndTime(long startTime, Schedule schedule){
		
		TimeZone tz = TimeZone.getDefault();		
		Calendar cal = Calendar.getInstance(tz);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1; 
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);

		//Log.d(LOG_TAG, "[getSamplingEndTime] now is  : " + month + "/" + day + " " + hour );
		long endTime = 0;
		
		//1. starTime  + duration = endTime
		if ( schedule.getSampleDuration()!=-1){	
			
			endTime = startTime + schedule.getSampleDuration()*GlobalNames.MILLISECONDS_PER_SECOND;

			//TODO: avoid bed time?
			/*
			if (endTime > getNextBedStartTimeInMillis())
				endTime = getNextBedStartTimeInMillis();
				*/
			
			return endTime;

		}
		
		//2 TimeofDay 
		else if (schedule.getSampleEndAtTimeOfDay()!=null){
			
			//geneerate the time of day of the same day. 
			int t_hour = getHourOfTimeOfDay(schedule.getSampleEndAtTimeOfDay());
			int t_minute =  getMinuteOfTimeOfDay(schedule.getSampleEndAtTimeOfDay());	
			
			//Log.d(LOG_TAG, "[getSamplingEndTime] the timeOfDay is : " + t_hour  + " : " + t_minute );

			
			Calendar designatedEndTime = Calendar.getInstance();
			designatedEndTime.set(year, month-1, day, t_hour, t_minute);

			//Log.d(LOG_TAG, "[getSamplingEndTime] the designatedEndTime is : " + getTimeString(designatedEndTime.getTimeInMillis()) );
			//if the timeOfDay has passed (i.e. the curren time  > designated time ), return -1.
			if(cal.after(designatedEndTime)){	
				
				
				// day + 1
				designatedEndTime.roll(Calendar.DAY_OF_MONTH, 1); 
				/*
				Log.d(LOG_TAG, "[getSamplingEndTime] the timeOfDay " + schedule.getSampleEndAtTimeOfDay() + " has passed, now is already " + hour
						 + " so the rolling it, the new designated time is " + getTimeString(designatedEndTime.getTimeInMillis()) );
					*/	 
			}
			/*
			Log.d(LOG_TAG, "[getSamplingEndTime] comparing bed time " +  getTimeString( getNextBedStartTimeInMillis() ) 
					+ "and end time " + getTimeString( designatedEndTime.getTimeInMillis() ));
					*/
			//if the end time will pass the coming bed time, then the latest end time should the bed Time.	
			if (designatedEndTime.getTimeInMillis() > getNextBedStartTimeInMillis()){
	
				endTime = getNextBedStartTimeInMillis();
				//Log.d(LOG_TAG, "[getSamplingEndTime] the end time is later than the bed time" +  getTimeString(designatedEndTime.getTimeInMillis()) );
						 
			}
			else {
				endTime = designatedEndTime.getTimeInMillis();
				//Log.d(LOG_TAG, "[getSamplingEndTime] the end time is earlier than the bed time" +  getTimeString(designatedEndTime.getTimeInMillis()) );

			}
			
			//Log.d(LOG_TAG, "[getSamplingEndTime] the final end time should be " + getTimeString( endTime));
			
			return endTime;

		}
		else 
			return -1;
		
	}
	
	
	private static int getHourOfTimeOfDay (String TimeOfDay){
		
		return Integer.parseInt(TimeOfDay.split(":")[0] ) ;
	}
	
	private static int getMinuteOfTimeOfDay (String TimeOfDay){

		return Integer.parseInt(TimeOfDay.split(":")[1] );
	}
	
	
	
	
	

	

	
	
	/**
	 * 
	 * @param ac
	 */
	public static void executeSchedule (ActionControl ac){

//		Log.d(LOG_TAG, "[executeSchedule] going to schedule " + ac.getId() +"'s schedule" );
		
		Schedule schedule = ac.getSchedule();
		ArrayList<Long> SampleTimes = generateSampleTimes(schedule);
		//according to the sample times, generate alarms
		generateAlarmsForSamples(SampleTimes, ac);
		
	}
	
	/**
	 * 
	 * @param schedule
	 * @return
	 */
	private static ArrayList<Long> generateSampleTimes(Schedule schedule) {
		
		ArrayList <Long> SampleTimes=null ;
		TimeZone tz = TimeZone.getDefault();		
		Calendar cal = Calendar.getInstance(tz);
		long now = cal.getTimeInMillis();
		/***
		 * We need to get the sample method, sample count, sample star time, and sample end time to know how to schedule
		 * 
		 */
		
		//get properties of the schedule;
		String sample_method = schedule.getSampleMethod();
		//if the schedule has sample count, get the sample count
		int sample_count = schedule.getSampleCount();	


        //if the method is at fixed time of a day
		if (sample_method.equals(SCHEDULE_SAMPLE_METHOD_FIXED_TIME_OF_DAY)) {

            String timeString = schedule.getFixedTimeOfDay();
            //need to schedule at the specified time

            long sampleTime = getSamplingAtTime(schedule);
            Log.d(LOG_TAG, "[test fixed time] [generateSampleTimes] fixed time of day, the schedule will be at " + getTimeString(sampleTime));
            if (sampleTime!=-1){
                SampleTimes = new ArrayList<Long>();
                SampleTimes.add(sampleTime);
            }
            return SampleTimes;

        }
        //if the sample method is simple one time, there's no need to generate a number of sample times
        else if (sample_method.equals(SCHEDULE_SAMPLE_METHOD_SIMPLE_ONE_TIME)){

            //because it's one time, we just add a delay to "now"
			long samplingStartTimeInMilli =  getSamplingStartTime(now ,schedule);
		//	Log.d(LOG_TAG, " [generateSampleTimes] this is simple one time action,  the one time action is at " + getTimeString(samplingStartTimeInMilli) );
			
			// if the startTime is later than the next bedTime, should abort this schedule..
			if (samplingStartTimeInMilli > getNextBedStartTimeInMillis() ){			
				//Log.d(LOG_TAG, "[generateSampleTimes]  the start time " + getTimeString(samplingStartTimeInMilli) + " is too late, the bed time is " +  getTimeString( getNextBedStartTimeInMillis()) );
				return SampleTimes;
			}
						
			//because there's only one time action, just add the startTime into the sampletimes. 
			SampleTimes = new ArrayList<Long>();
			SampleTimes.add(samplingStartTimeInMilli);
			
			return SampleTimes;
			
		}
		
		//if it needs multiple sample times (i.e. not simple one time), then we call sample starTime, endTime, and other parameters, and calculate the sample
		//times
		else {
			/** identify the sampling period **/
			long samplingStartTimeInMilli = getSamplingStartTime(now ,schedule);
			long samplingEndTimeInMilli = getSamplingEndTime(now, schedule);	
			
			// if the startTime is later than the next bedTime, should abort this schedule..
			if (samplingStartTimeInMilli > getNextBedStartTimeInMillis() ){			
			//	Log.d(LOG_TAG, " the start time " + getTimeString(samplingStartTimeInMilli) + " is too late, the bed time is " +  getTimeString( getNextBedStartTimeInMillis()) );
				return SampleTimes;
			}

			//generate the sample times 
			SampleTimes = calculateSampleTimes(sample_count, sample_method, samplingStartTimeInMilli, samplingEndTimeInMilli, schedule);

			return SampleTimes;
			
		}

		
		
	}
	
	
	
	/**
	 * Generate a list of sample times according to the schedule's sampling method, sampling period, and sampling numbers.
	 * @return
	 */
	private static ArrayList<Long> calculateSampleTimes (int sample_number, String method, long startTime, long endTime, Schedule schedule){
		
		ArrayList<Long> times = new ArrayList<Long>();
		long now = getCurrentTimeInMillis();
		Random random = new Random(now);

		//if startTime has passed, now becomes the startTime. This is for cases where the delay might be zero. So the action should immediately
		//happen right after the event. But the processing time may delay the startTime.
		
		
		if (now > startTime) now=startTime;

		//1. pure random
		if (method.equals(ScheduleAndSampleManager.SCHEDULE_SAMPLE_METHOD_RANDOM)){
	
			//start to random
			int sample_period = (int) (endTime - startTime);			
			
			Log.d(LOG_TAG, " random between 0  and " + sample_period);

			//generate a number of random number
			for (int i=0; i<sample_number; i++){

				long time = random.nextInt(sample_period) + startTime;
				times.add(time);
			
				Log.d(LOG_TAG, " the random number is " + time + " : " + getTimeString(time));
			}

		}
		
		//restrictd random: a subsequent time must be at a time when its interval from the previous time is larger than the minimum. e.g. 
		//if the first generated time is 13:47, and the mininum is 1 hour, then the next time must be later than 14:47. 
		//the purpose is to avoid too intensive actions (e.g. prompting questionnaires to a participant)
		
		
		
		else if  (method.equals(SCHEDULE_SAMPLE_METHOD_RANDOM_WITH_MINIMUM_INTERVAL)){
			
			int sample_period;
			long sub_startTime = startTime;
			long sub_endTime  = endTime;			
			long min_interval = schedule.getMinInterval() * GlobalNames.MILLISECONDS_PER_SECOND;
					
			//1. first get the entire sampling period			
			
			//repeatedly find the subsampling period
			for (int i=0; i<sample_number; i++){				

				//2. divide the period by the number of sample
				sample_period = (int) (sub_endTime - sub_startTime);
				
				int sub_sample_period = sample_period/(sample_number-i);
				
				//3. random within the sub sample period
				long time =  random.nextInt(sub_sample_period) + sub_startTime;
				/*
				Log.d(LOG_TAG, " semi sampling: the " + i + " sampling period is from " + getTimeString(sub_startTime) + " to " + getTimeString(sub_endTime) + 
				" divied by " + (sample_number-i) + " each period is " + sub_sample_period + " seconds long, " + " the sampled time within the period is " + 
						getTimeString(time) );
				*/
				//4. save the sampled time
				times.add(time);
				
				//5. the next startime is the previous sample time + min interval. We do this to ensure that the next sampled time is 
				//not too close to the previous sampled time. 
				sub_startTime = time +  min_interval;
				
				//Log.d(LOG_TAG, " semi sampling: the new start time is " + getTimeString(sub_startTime));
				
				//6. if the next start time is later than the overall end time, stop the sampling.
				if (sub_startTime >= sub_endTime)
					break;				
			}
			
			
			
		}
		
		else if (method.equals(SCHEDULE_SAMPLE_METHOD_FIXED_TIME_OF_DAY)){


          //  Log.d(LOG_TAG, "adding sampled time " + getTimeString(next_sample_time));


			
		}
		
		else if(method.equals(SCHEDULE_SAMPLE_METHOD_FIXED_INTERVAL)){
			
			int sample_interval = schedule.getInterval();
			int sample_period = (int) (endTime - startTime);	
			
			//Log.d(LOG_TAG, " [calculateSampleTimes] the fixed sample interval is" + sample_interval + " and the period is " + sample_period);
			
			long next_sample_time = startTime+ sample_interval* GlobalNames.MILLISECONDS_PER_SECOND;
			
			while (next_sample_time < endTime){				
				
				Log.d(LOG_TAG, "adding sampled time " + getTimeString(next_sample_time));
				times.add(next_sample_time);			
				next_sample_time+=sample_interval* GlobalNames.MILLISECONDS_PER_SECOND;
			}
			
		}
		
		return times;
	}


    /**
     * generate the alarm for update schedules( or do stop/start the service)
     *
     */
    private static void generateRefreshServiceScheduleAlarms() {

        //if we need to schedule an alarm to stop the service
        if (PreferenceHelper.getPreferenceBoolean(ConfigurationManager.SERVICE_SETTING_STOP_SERVICE_DURING_MIDNIGHT, false)) {

            //intent for stoping service
            long stopServiceTime  = getNextTimeInMillis(bedMiddleTime);
            int request_code_stopServiceTime =NotificationHelper.generatePendingIntentRequestCode(stopServiceTime);
            Intent stopServiceIntent = new Intent(GlobalNames.STOP_SERVICE_ALARM);
            PendingIntent stopServicePi = PendingIntent.getBroadcast(mContext, request_code_stopServiceTime, stopServiceIntent, 0);
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, stopServiceTime,  stopServicePi);
            Log.d(LOG_TAG, "[test reschedule] [generateRefreshServiceScheduleAlarms] the scheduled stop service is " + getTimeString(stopServiceTime) +" and the next is " + getTimeString(stopServiceTime + GlobalNames.MILLISECONDS_PER_DAY) );


            //intent for starting service
            long startServiceTime  = getNextTimeInMillis(bedEndTime);
            int request_code_startServiceTime =NotificationHelper.generatePendingIntentRequestCode(startServiceTime);
            Intent startServiceIntent = new Intent(GlobalNames.START_SERVICE_ALARM);
            PendingIntent startServicePi = PendingIntent.getBroadcast(mContext, request_code_startServiceTime, startServiceIntent, 0);
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, startServiceTime,  startServicePi);
            Log.d(LOG_TAG, "[test reschedule] [generateRefreshServiceScheduleAlarms] the scheduled start service is " + getTimeString(startServiceTime) +" and the next is " + getTimeString(startServiceTime+ GlobalNames.MILLISECONDS_PER_DAY) );

        }

        else {

            //do not stop the service at midnight
            //get the next schedule update Time (or service start time) and schedule an alarm, the alarm should be scheduled during the bed time
            long updateScheduletime  = getNextTimeInMillis(bedEndTime);

            Intent intent = new Intent(GlobalNames.UPDATE_SCHEDULE_ALARM);
            int request_code_updateTime =NotificationHelper.generatePendingIntentRequestCode(updateScheduletime);
            // Log.d(LOG_TAG, "[test reschedule] [generateRefreshServiceScheduleAlarms] request code: "+ intent.getIntExtra(ALARM_REQUEST_CODE, 0) + " at " + getTimeString(time) + " after this action alarm, now we have stored " + mAlarmRequestCodes.size() + " request codes");

            //the updateschedule is repeated on a daily basis
            PendingIntent pi = PendingIntent.getBroadcast(mContext, request_code_updateTime,intent, 0);
            mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, updateScheduletime, GlobalNames.MILLISECONDS_PER_DAY, pi);
            Log.d(LOG_TAG, "[test reschedule] [generateRefreshServiceScheduleAlarms] the scheduled update time is " + getTimeString(updateScheduletime) +" and the next is " + getTimeString(updateScheduletime + GlobalNames.MILLISECONDS_PER_DAY) );


        }

    }
	
	/**
	 * generate alarms for the schedules
	 * @param times
	 */
	private static void generateAlarmsForSamples (ArrayList<Long> times, ActionControl ac){

        //if there are times to schedule
		if (times!=null) {

            Log.d(LOG_TAG, " [generateAlarmsForSamples] there are  " + times.size() + " alarms that should be scheduled for action control"
                    + ac.getId());

            for (int i=0; i< times.size(); i++){

                long time = times.get(i);
                int request_code =NotificationHelper.generatePendingIntentRequestCode(time);

                //log the time of action control
                LogManager.log(LogManager.LOG_TYPE_SYSTEM_LOG,
                        LogManager.LOG_TAG_ACTION_TRIGGER,
                        "Schedule ActionControl:\t" + ActionManager.getActionControlTypeName(ac.getType()) + "\t" + ac.getAction().getName() + "\t" + getTimeString(time));


                Log.d(LOG_TAG, "[generateAlarmsForSamples] we are going to " + ActionManager.getActionControlTypeName(ac.getType()) + " " + ac.getAction().getName() + " at " + getTimeString(time) +
                " with request code " + request_code);

                //create an alarm for a time
                Intent intent = new Intent(GlobalNames.ACTION_ALARM);
                Bundle bundle = new Bundle();


                //send action control id to the alarm so that the application can retrieve the corresponding action and execute it.
               // Log.d(LOG_TAG, " [generateAlarmsForSamples] putting action control id " + ac.getId() + " to bundles with alarm at" + getTimeString(time));

                //add info to the intent, including triggered object id, and the request code.
                //id
                bundle.putInt(ActionManager.ACTION_PROPERTIES_ID, ac.getId());
                //request code: use the time as its request code (so that it's kind of unique
                bundle.putInt(ALARM_REQUEST_CODE, request_code);

                //store the pendingIntent request code so that we can can cancel them in the future
                mAlarmRequestCodes.add(request_code);


                //add extra
                intent.putExtras(bundle);
                /*
                Log.d(LOG_TAG, " [generateAlarmsForSamples] the stored action control id is " +intent.getIntExtra(ActionManager.ACTION_PROPERTIES_ID, 0) + " request code: "
                        + intent.getIntExtra(ALARM_REQUEST_CODE, 0) + " at " + getTimeString(time) + " after this action alarm, now we have stored " + mAlarmRequestCodes.size() + " request codes");
*/
                PendingIntent pi = PendingIntent.getBroadcast(mContext, request_code,intent, 0);

                mAlarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);
            }

        }


	}
	
	public static int generatePendingIntentRequestCode(long time){
		
		int code = 0;
				 
		if (time-time_base > 1000000000){
			time_base = getCurrentTimeInMillis();
		}
		
		return (int) (time-time_base);
	}
	
	
	/**get the current time in milliseconds**/
	public static long getCurrentTimeInMillis(){		
		//get timzone		
		TimeZone tz = TimeZone.getDefault();		
		Calendar cal = Calendar.getInstance(tz);
		long t = cal.getTimeInMillis();		
		return t;
	}




    public static long getNextTimeInMillis(int targetHour){

        TimeZone tz  = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        long now = cal.getTimeInMillis();

        //get the date of now: the first month is Jan:0
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH)+1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

       // Log.d(LOG_TAG, "[test reschedule ][getNextUpdateScheduleTimeInMillis] now is  : " + getTimeString(now) );

        //use the midnight to schedule updateScheduleAlarm
        int hourOfUpdateScheduleTime  = targetHour;
        int minute = 0;

        //set the update time tomorrow
        Calendar nextTime= Calendar.getInstance();

        //make the update time tomorrow
       // nextTime.roll(Calendar.DAY_OF_MONTH,1);
        day = nextTime.get(Calendar.DAY_OF_MONTH);

        if (targetHour==pauseProbeService){

            //nextTime.setTimeInMillis(getCurrentTimeInMillis());
            nextTime.set(year, month-1, day, hourOfUpdateScheduleTime, 0,0);
            nextTime.roll(Calendar.DAY_OF_MONTH,1);
        }

        else if  (targetHour==resumeProbeService){
            //next day
          //  nextTime.setTimeInMillis(getCurrentTimeInMillis());
            nextTime.set(year, month-1, day, hourOfUpdateScheduleTime, 0,0);
            nextTime.roll(Calendar.DAY_OF_MONTH,1);

        }


    //    Log.d(LOG_TAG, "[test reschedule][getNextUpdateScheduleTimeInMillis] the schedule updateTime  is  : " + getTimeString(nextUpdateTime.getTimeInMillis()) );


        return nextTime.getTimeInMillis();

    }

    public static  long getLastEndOfBedTimeInMillis() {

        long bedTime =0;

        TimeZone tz  = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        long now = cal.getTimeInMillis();

        //get the date of now: the first month is Jan:0
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH)+1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        //get the daily report deliver time
        int HourOfBedEndTime  = ScheduleAndSampleManager.bedEndTime;
        int minute = 0;

        //set bedTime on the same day
        Calendar lastBedEndTime= Calendar.getInstance();
        lastBedEndTime.set(year, month-1, day, HourOfBedEndTime, minute);

        //check whether the bedTime should be on the next day..
        //has passed
        if (cal.before(lastBedEndTime)){

            //day + 1
            cal.roll(Calendar.DAY_OF_MONTH, -1);
            day = cal.get(Calendar.DAY_OF_MONTH);

        }

        //set the "day"
        lastBedEndTime.set(year, month-1, day, HourOfBedEndTime, 0,0);


        return lastBedEndTime.getTimeInMillis();
    }



	public static long getNextBedStartTimeInMillis() {

        long bedTime = 0;

        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        long now = cal.getTimeInMillis();

        //get the date of now: the first month is Jan:0
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        //get the daily report deliver time
        int hourOfBedTime = ScheduleAndSampleManager.bedStartTime;
        int minute = 0;

        //set bedTime on the same day
        Calendar nextBedTime = Calendar.getInstance();
        nextBedTime.set(year, month - 1, day, hourOfBedTime, minute);

        //check whether the bedTime should be on the next day..
        //has passed
        if (cal.after(nextBedTime)) {

            //day + 1
            cal.roll(Calendar.DAY_OF_MONTH, 1);
            day = cal.get(Calendar.DAY_OF_MONTH);

        }

        //set the "day"
        nextBedTime.set(year, month - 1, day, hourOfBedTime, 0, 0);


        return nextBedTime.getTimeInMillis();
    }

	/**convert long to timestring**/

	public static String getTimeString(long time){

		SimpleDateFormat sdf_now = new SimpleDateFormat(GlobalNames.DATE_FORMAT_NOW);
		String currentTimeString = sdf_now.format(time);

		return currentTimeString;
	}

    public static String getTimeString(long time,  SimpleDateFormat sdf){

        String currentTimeString = sdf.format(time);

        return currentTimeString;
    }
	
	
	public static AlarmManager getAlarmManager(){
		return mAlarmManager;
	}
	
	
	public static void cancelAlarmByRequestCode(int requestCode, Intent intent) {
		
		Log.d(LOG_TAG, " [cancelAlarmByRequestCode] now we are canceling the pendingIntent " +  requestCode);
		
		//create pendingIntent for each request code
		PendingIntent pi = PendingIntent.getBroadcast(mContext, requestCode,intent, 0);
		
		//cancel the pendingIntent
		mAlarmManager.cancel(pi);	
		
	}

    public static void registerActionControl(ActionControl ac) {

        if (ac.getSchedule()!=null){
            //schedule the alarm for the actionControl
            Log.d(LOG_TAG, "[test reschedule][registerActionControl] the control " + ac.getId() + " has schedule" );

            ScheduleAndSampleManager.executeSchedule(ac);
        }

    }


    public static void updateScheduledActionControls() {

        Log.d(LOG_TAG, "[test reschedule][updateActionControlsSchedules] going to update all actioncontrol's schedule "  );

        for (int i=0; i<ActionManager.getActionControlList().size(); i++){

            ActionControl ac = ActionManager.getActionControlList().get(i);

            //only reschedule action controls that are scheduled..
            if (ac.getLaunchMethod().equals(ActionManager.ACTION_LAUNCH_STYLE_SCHEDULE)){

                registerActionControl(ac);
            }
        }

    }
	
	
	public static void cancelAllActionAlarms() {
		
		//create action alarm intent
		Intent intent = new Intent(GlobalNames.ACTION_ALARM);	
		
		Log.d(LOG_TAG, "[cancelAllActionAlarms] we are going to cancel " + mAlarmRequestCodes.size() + " pendingIntents");
		
		//iterate all stored request codes
		for (int i=0; i<mAlarmRequestCodes.size(); i++){		
			
			//get the request code
			int requestCode = mAlarmRequestCodes.get(i);
			
			//cancel the action alarm
			cancelAlarmByRequestCode(requestCode, intent);
		}
		
	}


    /**
     * Alarm receiver for update schedule
     */
    static BroadcastReceiver UpdateScheduleAlarmReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(GlobalNames.UPDATE_SCHEDULE_ALARM)){
                Log.d(LOG_TAG, "In UpdateScheduleAlarmReceiver");


                Log.d(LOG_TAG, "[test reschedule][UpdateScheduleAlarmReceiver] the alarm receiver  with request code " + intent.getIntExtra(ALARM_REQUEST_CODE, 0)
                 + "going to update actioncontrol's schedule");

                updateScheduledActionControls();


                //this is old..the first method we came up with. During midnight we rescheule anything that needs to be a new schedule
                //but now we will stop service during midnight and restart in the morning

                /**
                 *  TODO: we should let researchers to define whether they want to stop service during midnight,
                 *  because we may be interested in some events happening midnight (drunk, sending late messages)
                 *  For the labeling study we can just try
                 */

            }else if (intent.getAction().equals(GlobalNames.START_SERVICE_ALARM)) {

                Log.d(LOG_TAG, "[UpdateScheduleAlarmReceiver ] we will start the service ");

                if (CaptureProbeService.IsServiceRunning()) {
                    //if the service is running, we should just update schedules.
                    updateScheduledActionControls();
                }
                else {
                    //otherwise we start the service.
                    Intent sintent = new Intent(context, CaptureProbeService.class);
                    //		            sintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startService(sintent);
                }

            }
            if (intent.getAction().equals(GlobalNames.STOP_SERVICE_ALARM)){

                Log.d(LOG_TAG, "[UpdateScheduleAlarmReceiver ] we will stop the service ");

                Intent stopintent = new Intent(context, CaptureProbeService.class);
                context.stopService(stopintent);

            }


        }
    };



    /**
     * Alarm receiver for Action Alarm
     */
	static BroadcastReceiver ActionAlarmReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
        	if (intent.getAction().equals(GlobalNames.ACTION_ALARM)){
    			Log.d(LOG_TAG, "In ActionAlarmReceiver ");
    			
    			//based on the action type, choose which action to do
    			int acId = intent.getIntExtra(ActionManager.ACTION_PROPERTIES_ID, 0);
    			
    			/**retrieve action according to the action id**/
    			//int acId = bundle.getInt(ActionManager.ACTION_PROPERTIES_ID);

    			Log.d(LOG_TAG, "[ActionAlarmReceiver] the alarm receive receives action execution alarm, need to execute the action control " + acId
    					+ " with request code " + intent.getIntExtra(ALARM_REQUEST_CODE, 0));
    			
    			
    			//pass id to the ActionManager to execute the action control
    			ActionManager.executeActionControl(acId);
    		}	
     
        }
    };
	
}
