package edu.umich.si.inteco.tansuo.app.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.R;
import edu.umich.si.inteco.tansuo.app.activities.AnnotateActivity;
import edu.umich.si.inteco.tansuo.app.model.Task;
import edu.umich.si.inteco.tansuo.app.model.actions.SavingRecordAction;
import edu.umich.si.inteco.tansuo.app.services.CaptureProbeService;
import edu.umich.si.inteco.tansuo.app.util.ActionManager;
import edu.umich.si.inteco.tansuo.app.util.DatabaseNameManager;
import edu.umich.si.inteco.tansuo.app.util.LogManager;
import edu.umich.si.inteco.tansuo.app.util.TaskManager;

/**
 * Created by Armuro on 7/13/14.
 */
public class RecordSectionFragment extends Fragment{

    private static final String LOG_TAG = "RecordSectionFragment";

    private TextView currentRecordingTaskTextView;
    private Chronometer chronometer;
    private Button startButton;
    private Button stopButton;
    private Button labelButton;
    private static SavingRecordAction mLastUserInitiatedSavingRecordingAction;

    @Override
    public void onResume() {
        super.onResume();
/*
            Log.d(LOG_TAG, "the base is " + CaptureProbeService.getCentralChrometer().getBase());

            long t= SystemClock.elapsedRealtime() - CaptureProbeService.getCentralChrometer().getBase();

            int h   = (int)(t/3600000);
            int m = (int)(t - h*3600000)/60000;
            int s= (int)(t - h*3600000- m*60000)/1000 ;
            String hh = h < 10 ? "0"+h: h+"";
            String mm = m < 10 ? "0"+m: m+"";
            String ss = s < 10 ? "0"+s: s+"";
            CaptureProbeService.getCentralChrometer().setText(hh+":"+mm+":"+ss);
*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_record, container, false);

/*
            if (CaptureProbeService.getCentralChrometer()==null)
                CaptureProbeService.setCentralChrometer((Chronometer) rootView.findViewById(R.id.recording_chronometer));
                */

        chronometer = (Chronometer) rootView.findViewById(R.id.recording_chronometer);
        startButton = (Button) rootView.findViewById(R.id.start_recording_Button);
        stopButton = (Button) rootView.findViewById(R.id.stop_recording_Button);
        labelButton = (Button) rootView.findViewById(R.id.add_details_Button);
        currentRecordingTaskTextView = (TextView) rootView.findViewById(R.id.recording_task_textview);


        //the textview is based on the task of which the recording is for. By default there should be a list of task that signs up for using this interface ( the recording function
        //can be used by multiple tasks, and the user would choose which task the current recording is for.
        //For the labeling study, we change the textview based on which condition they are in

        Log.d(LOG_TAG, "back to creatView the base is " + CaptureProbeService.getBaseForChronometer());


        //if the chrometer was running when we leave the fragment, after coming back we should recover it
        if (CaptureProbeService.isCentralChrometerRunning()) {

            long time = SystemClock.elapsedRealtime() -  CaptureProbeService.getBaseForChronometer();
            int h   = (int)(time/3600000);
            int m = (int)(time - h*3600000)/60000;
            int s= (int)(time - h*3600000- m*60000)/1000 ;
            String hh = h < 10 ? "0"+h: h+"";
            String mm = m < 10 ? "0"+m: m+"";
            String ss = s < 10 ? "0"+s: s+"";

            chronometer.setText(hh+":"+mm+":"+ss);
            chronometer.start();
            CaptureProbeService.setCentralChrometerRunning(true);

            startButton.setText("PAUSE");

        }
        else {

            chronometer.setText(CaptureProbeService.getCentralChrometerText());
            //if it is not running, check if it's paused
            if (CaptureProbeService.isCentralChrometerPaused())
                startButton.setText("RESUME");

        }


        //setup component
        chronometer.setOnChronometerTickListener(

                new Chronometer.OnChronometerTickListener(){
                    @Override
                    public void onChronometerTick(Chronometer cArg) {
                        long time = SystemClock.elapsedRealtime() -  CaptureProbeService.getBaseForChronometer();
                        int h   = (int)(time/3600000);
                        int m = (int)(time - h*3600000)/60000;
                        int s= (int)(time - h*3600000- m*60000)/1000 ;
                        String hh = h < 10 ? "0"+h: h+"";
                        String mm = m < 10 ? "0"+m: m+"";
                        String ss = s < 10 ? "0"+s: s+"";
                        cArg.setText(hh+":"+mm+":"+ss);
                    }

                });

        //set the text of recording task
        currentRecordingTaskTextView.setText(GlobalNames.CURRENT_STUDY_CONDITION);

        // clicking on the start button will start or resume the recording
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (startButton.getText().equals("START")){

                    CaptureProbeService.setBaseForChronometer(SystemClock.elapsedRealtime());

                    //start recording and start the stopwatch
                    chronometer.setBase( CaptureProbeService.getBaseForChronometer());
                    chronometer.start();
                    CaptureProbeService.setCentralChrometerRunning(true);
                    CaptureProbeService.setCentralChrometerPaused(false);


                    //Log.d(LOG_TAG, " CaptureProbeService is runnung? " + CaptureProbeService.isCentralChrometerRunning() + " start! the base is " + CaptureProbeService.getBaseForChronometer());

                    //when clicking on the recording action, execute the saveRecordAction
                    SavingRecordAction action = new SavingRecordAction(
                            ActionManager.USER_INITIATED_RECORDING_ACTION_ID,
                            ActionManager.USER_START_RECORDING_ACTION_NAME,
                            ActionManager.ACTION_TYPE_SAVING_RECORD,
                            ActionManager.ACTION_EXECUTION_STYLE_ONETIME, GlobalNames.LABELING_STUDY_ID);

                    //an user-initiated recoring should allow users to annotate in process
                    action.setAllowAnnotationInProcess(true);

                    mLastUserInitiatedSavingRecordingAction = action;

                    //user initiated recording should be a continuous action; if we don't set this, it will just be a one-time action.
                    action.setContinuous(true);

                    //specify which task this recording is for
                    Task task = getTaskFromRecordingTaskView(currentRecordingTaskTextView.getText().toString() );
//                            	Log.d(LOG_TAG, "[participatory sensing] the recording is for task: " + task.getName() + " " + task.getId());

                    if (task!=null)
                        action.setTaskId((int) task.getId());
                    else
                        action.setTaskId(-1);

                    //start the saving record action
                    ActionManager.startAction(action);
                    //Log.d(LOG_TAG, "[participatory sensing] user clicking on the start action, start recording action" + action.getId() + " with session "  + action.getSessionId());

                    //changing the labee of the start button to PAUSE
                    startButton.setText("PAUSE");


                    //Log user action
                    LogManager.log(LogManager.LOG_TYPE_USER_ACTION_LOG,
                            LogManager.LOG_TAG_USER_CLICKING,
                            "User Click:\t" + "startRecording" + "\t" + "RecordingTab");

                    //for testing...check records in the backgroudn recording
                    //RecordingAndAnnotateManager.getRecordsInBackgroundRecording(DatabaseNameManager.RECORD_TABLE_NAME_ACTIVITY);

                }

                //if the user clicks on button labeled "PAUSE", we should pause the watch and the recording.
                else if (startButton.getText().equals("PAUSE")){


                    //stop the watch
                    chronometer.stop();
                    CaptureProbeService.setCentralChrometerRunning(false);
                    CaptureProbeService.setCentralChrometerPaused(true);
                    //rememeber the text of the chrometer
                    CaptureProbeService.setCentralChrometerText(chronometer.getText().toString());

                    //remember the time
                    CaptureProbeService.setTimeWhenStopped(SystemClock.elapsedRealtime());


                    Log.d(LOG_TAG, "pause at time" + SystemClock.elapsedRealtime() + " remember text " + CaptureProbeService.getCentralChrometerText() + " and time " + CaptureProbeService.getTimeWhenStopped());


                    SavingRecordAction action  = (SavingRecordAction) ActionManager.getRunningAction(ActionManager.USER_INITIATED_RECORDING_ACTION_ID);

                    //pause the saving record action
                    if (action!=null)
                        ActionManager.pauseAction(action);

                    //changing the labee of the start button to RESUME
                    startButton.setText("RESUME");

                    //Log user action
                    LogManager.log(LogManager.LOG_TYPE_USER_ACTION_LOG,
                            LogManager.LOG_TAG_USER_CLICKING,
                            "User Click:\t" + "pauseRecording"+ "\t" + "RecordingTab");



                }


                //if the user clicks on button labeled "PAUSE", we should pause the watch and the recording.
                else if (startButton.getText().equals("RESUME")){

                    //new base = original base + (the time of resuming - the time of pausing)
                    CaptureProbeService.setBaseForChronometer( CaptureProbeService.getBaseForChronometer() + (SystemClock.elapsedRealtime() - CaptureProbeService.getTimeWhenStopped()) );
                    //stop the watch
                    chronometer.setBase( CaptureProbeService.getBaseForChronometer());
                    chronometer.start();
                    CaptureProbeService.setCentralChrometerRunning(true);
                    CaptureProbeService.setCentralChrometerPaused(false);

                    Log.d(LOG_TAG, "resume at time" + SystemClock.elapsedRealtime() + " need to add base " + ( SystemClock.elapsedRealtime() - CaptureProbeService.getTimeWhenStopped() )  );


                    Log.d(LOG_TAG, " CaptureProbeService is runnung? " + CaptureProbeService.isCentralChrometerRunning() + " resume the base is " + CaptureProbeService.getBaseForChronometer());


                    SavingRecordAction action  = (SavingRecordAction) ActionManager.getRunningAction(ActionManager.USER_INITIATED_RECORDING_ACTION_ID);

                    //pause the saving record action
                    if (action!=null)
                        ActionManager.resumeAction(action);


                    //changing the labee of the start button to PAUSE
                    startButton.setText("PAUSE");

                    //Log user action
                    LogManager.log(LogManager.LOG_TYPE_USER_ACTION_LOG,
                            LogManager.LOG_TAG_USER_CLICKING,
                            "User Click:\t" + "pauseRecording"+ "\t" + "RecordingTab");

                }

            }
        });



        // clicking on the stop button will start the recording

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //stop recording and stop the stopwatch
                chronometer.stop();
                CaptureProbeService.setCentralChrometerRunning(false);
                CaptureProbeService.setCentralChrometerPaused(false);
                //reset
                chronometer.setText("00:00:00");
                //rememeber the text of the chrometer
                CaptureProbeService.setCentralChrometerText(chronometer.getText().toString());


                /**stop the user-initiated recording action **/

                //find the action from the runningAction List
                SavingRecordAction action  = (SavingRecordAction) ActionManager.getRunningAction(ActionManager.USER_INITIATED_RECORDING_ACTION_ID);

                //stop the recording action
                if (action!=null){
                    ActionManager.stopAction(action);
                    Log.d(LOG_TAG, "[participatory sensing] user clicking on the stop action, stop recording action " + action.getId() + " with session "  + action.getSessionId());
                }

                //changing the labee of the start button back to START
                startButton.setText("START");

                //Log user action
                LogManager.log(LogManager.LOG_TYPE_USER_ACTION_LOG,
                        LogManager.LOG_TAG_USER_CLICKING,
                        "User Click:\t" + "stoptRecording"+ "\t" + "RecordingTab");

            }
        });


        /***When user clicks on Label button they will enter annotations to the recordin that is initiated by themselves*
         * To add annotaiton we need to know which session the recording is. So we need to first fine the aciton that gets the session id**/

        labelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mLastUserInitiatedSavingRecordingAction!=null){

                    Log.d(LOG_TAG, "[participatory sensing] user clicking on the label action, going to annotate " + mLastUserInitiatedSavingRecordingAction.getId() + " with session "  + mLastUserInitiatedSavingRecordingAction.getSessionId());

                    int sessionId = mLastUserInitiatedSavingRecordingAction.getSessionId();

                    startAnnotateActivity(sessionId);

                    //Log user action
                    LogManager.log(LogManager.LOG_TYPE_USER_ACTION_LOG,
                            LogManager.LOG_TAG_USER_CLICKING,
                            "User Click:\t" + "annotateRecording"+ "\t" + "RecordingTab");

                }
            }
        });


        return rootView;



    }


    public void startAnnotateActivity(int sessionId){

        Bundle bundle = new Bundle();
        bundle.putInt(DatabaseNameManager.COL_SESSION_ID, sessionId);

        Intent intent = new Intent(getActivity(), AnnotateActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);


    }

    private static Task getTaskFromRecordingTaskView(String selectedTask) {

        //the selectedTask is its name

        Log.d(LOG_TAG, "the selected Task text is " + selectedTask);

        Task task = TaskManager.getTaskByName(selectedTask);

        if (task!=null){
            return task;
        }
        else
            return null;


    }

}
