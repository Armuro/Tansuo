package edu.umich.si.inteco.tansuo.app.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import edu.umich.si.inteco.tansuo.app.util.QuestionnaireManager;

public class testActivity extends Activity {

	private static final String LOG_TAG = "testActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(LOG_TAG, "enter onCreate of QuestionnaireActivitym" );

		
		Bundle bundle = getIntent().getExtras();
		int questionnaire_id = bundle.getInt(QuestionnaireManager.QUESTIONNAIRE_PROPERTIES_ID); 
		Log.d(LOG_TAG, "enter onCreate of QuestionnaireActivitym, the quesitonnaire id is " + questionnaire_id );

		
	}
}
