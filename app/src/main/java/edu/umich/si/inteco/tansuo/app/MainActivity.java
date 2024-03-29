package edu.umich.si.inteco.tansuo.app;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;

import edu.umich.si.inteco.tansuo.app.Fragments.DailyJournalSectionFragment;
import edu.umich.si.inteco.tansuo.app.Fragments.ListRecordingSectionFragment;
import edu.umich.si.inteco.tansuo.app.Fragments.RecordSectionFragment;
import edu.umich.si.inteco.tansuo.app.Fragments.TaskSectionFragment;
import edu.umich.si.inteco.tansuo.app.services.CaptureProbeService;
import edu.umich.si.inteco.tansuo.app.util.ActionManager;
import edu.umich.si.inteco.tansuo.app.util.LogManager;
import edu.umich.si.inteco.tansuo.app.util.RecordingAndAnnotateManager;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

	private static final String LOG_TAG = "MainActivity";

    public static final int PAGE_POSITION_RECORDING = 0;
    public static final int PAGE_POSITION_TASKS = 1;
    public static final int PAGE_POSITION_DAILY_JOURMAL = 2;

    private static String mReviewMode = RecordingAndAnnotateManager.ANNOTATE_REVIEW_RECORDING_ALL;
    private static String mLaunchTab = GlobalNames.MAIN_ACTIVITY_TAB_RECORD;

	//provide fragments for each of the three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter} 
	//derivative, which will keep every loaded fragment in memory
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;


     //display the three primary sections of the app, one at a time
    ViewPager mViewPager;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


        //RemoteDBHelper.syncWithRemoteDatabase();
       // Log.d(LOG_TAG, "[queryLastBackgroundRecordingLogSyncHour] get the synTime is " + lastSynhour);

        /** when the app starts, first obtain the participant ID **/
        TelephonyManager mngr = (TelephonyManager)getSystemService(this.TELEPHONY_SERVICE);
        GlobalNames.DEVICE_ID = mngr.getDeviceId();
        Log.d(LOG_TAG, "[GlobalNames.DEVICE_ID] get the synTime is " + GlobalNames.DEVICE_ID);

        /**start the contextManager service**/
        if (!CaptureProbeService.IsServiceRunning()){
            Log.d(LOG_TAG, "[test service running]  going start the probe service isServiceRunning:" + CaptureProbeService.IsServiceRunning());
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, CaptureProbeService.class);
            startService(intent);
        }


        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        // Specify that the Home/Up button should not be enabled, since there is no hierarchical
        // parent.
        actionBar.setHomeButtonEnabled(false);

        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);

            }
        });




        //we first set the mLaunchtab parameter based on the study condition
        //TODO: Probe should not have conditions, remove this after the labeling study..
        if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.PARTICIPATORY_LABELING_CONDITION)) {
            mLaunchTab = GlobalNames.MAIN_ACTIVITY_TAB_RECORD;
            actionBar.addTab(actionBar.newTab().setText(GlobalNames.MAIN_ACTIVITY_TAB_RECORD).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(GlobalNames.MAIN_ACTIVITY_TAB_RECORDINGS).setTabListener(this));
        }
        else if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.IN_STIU_LABELING_CONDITION)) {
            mLaunchTab = GlobalNames.MAIN_ACTIVITY_TAB_DAILY_REPORT;
        }

        else if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.POST_HOC_LABELING_CONDITION)) {
            mLaunchTab = GlobalNames.MAIN_ACTIVITY_TAB_DAILY_REPORT;
            actionBar.addTab(actionBar.newTab().setText(GlobalNames.MAIN_ACTIVITY_TAB_RECORDINGS).setTabListener(this));
        }

        actionBar.addTab(actionBar.newTab().setText(GlobalNames.MAIN_ACTIVITY_TAB_DAILY_REPORT).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText(GlobalNames.MAIN_ACTIVITY_TAB_TASKS).setTabListener(this));


    }



    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first


        //get bundles and set up environment
        Bundle bundle = getIntent().getExtras();
        if (bundle!=null ) {

            if (bundle.containsKey(ActionManager.ACTION_PROPERTIES_ANNOTATE_REVIEW_RECORDING)) {

                String reviewMode = bundle.getString(ActionManager.ACTION_PROPERTIES_ANNOTATE_REVIEW_RECORDING);
                if (reviewMode!=null){
                    mReviewMode = reviewMode;
                    //showRecordingList(mReviewMode);
                }
            }

            if (bundle.containsKey("launchTab")) {
                mLaunchTab = bundle.getString("launchTab");

            }

        }
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        for (int i=0; i<actionBar.getTabCount(); i++) {
            Log.d(LOG_TAG, "at position " + actionBar.getTabAt(i) + " is tab " +  actionBar.getTabAt(i).getText());
            if (actionBar.getTabAt(i).getText().equals(mLaunchTab)) {
                actionBar.selectTab(actionBar.getTabAt(i));
            }
        }

    }


    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		mViewPager.setCurrentItem(tab.getPosition());

        Log.d(LOG_TAG, "[onTabSelected] the selected page position is " + tab.getPosition());

        //Log user action
        LogManager.log(LogManager.LOG_TYPE_USER_ACTION_LOG,
                LogManager.LOG_TAG_USER_CLICKING,
                "User Click:\t" + "Tab " + tab.getText());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}


    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {

            switch (i) {
                case 0:

                    if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.PARTICIPATORY_LABELING_CONDITION)){
                        RecordSectionFragment recordSectionFragment = new RecordSectionFragment();
                        recordSectionFragment.setRetainInstance(true);
                        return recordSectionFragment;
                    }
                    else if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.POST_HOC_LABELING_CONDITION)) {
                        mReviewMode = RecordingAndAnnotateManager.ANNOTATE_REVIEW_RECORDING_RECENT;
                        ListRecordingSectionFragment listRecordingSectionFragment = new ListRecordingSectionFragment(mReviewMode);
                        listRecordingSectionFragment.setRetainInstance(true);
                        return listRecordingSectionFragment;
                    }

                    else {
                        DailyJournalSectionFragment dailyJournalSectionFragment = new DailyJournalSectionFragment();
                        dailyJournalSectionFragment.setRetainInstance(true);
                        return dailyJournalSectionFragment;
                    }

                    // The first section of the app is the most interesting -- it offers
                    // a launchpad into the other demonstrations in this example application.

                case 1:
                    if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.PARTICIPATORY_LABELING_CONDITION)){
                        mReviewMode = RecordingAndAnnotateManager.ANNOTATE_REVIEW_RECORDING_ALL;
                        ListRecordingSectionFragment listRecordingSectionFragment = new ListRecordingSectionFragment(mReviewMode);
                        listRecordingSectionFragment.setRetainInstance(true);
                        return listRecordingSectionFragment;
                    }
                    else if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.POST_HOC_LABELING_CONDITION)){
                        mReviewMode = RecordingAndAnnotateManager.ANNOTATE_REVIEW_RECORDING_RECENT;
                        DailyJournalSectionFragment dailyJournalSectionFragment = new DailyJournalSectionFragment();
                        dailyJournalSectionFragment.setRetainInstance(true);
                        return dailyJournalSectionFragment;
                    }
                    else {

                        TaskSectionFragment taskSectionFragment = new TaskSectionFragment();
                        taskSectionFragment.setRetainInstance(true);
                        return taskSectionFragment;
                    }

                case 2:
                    if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.PARTICIPATORY_LABELING_CONDITION)){

                        DailyJournalSectionFragment dailyJournalSectionFragment = new DailyJournalSectionFragment();
                        dailyJournalSectionFragment.setRetainInstance(true);
                        return dailyJournalSectionFragment;
                    }
                    else {
                        TaskSectionFragment taskSectionFragment = new TaskSectionFragment();
                        taskSectionFragment.setRetainInstance(true);
                        return taskSectionFragment;
                    }

                case 3:
                    if (GlobalNames.CURRENT_STUDY_CONDITION.equals(GlobalNames.PARTICIPATORY_LABELING_CONDITION)){

                        Log.d(LOG_TAG, "enter task fragment");
                        TaskSectionFragment taskSectionFragment = new TaskSectionFragment();
                        taskSectionFragment.setRetainInstance(true);
                        return taskSectionFragment;
                    }

                default:
                    TaskSectionFragment taskSectionFragment1 = new TaskSectionFragment();
                    taskSectionFragment1.setRetainInstance(true);
                    return taskSectionFragment1;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Section " + (position + 1);
        }
    }



}
