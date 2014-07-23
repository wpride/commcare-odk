package org.commcare.dalvik.test;

import org.commcare.dalvik.activities.CommCareSetupActivity;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.InstrumentationTestCase;

import com.jayway.android.robotium.solo.Solo;


public class CommCareFullTest1 extends InstrumentationTestCase {

	private Solo solo;

	protected void setUp() throws Exception {
		super.setUp();
		//setUp() is run before a test case is started. 
		//This is where the solo object is created.
		solo = new Solo(getInstrumentation());
	}

	@Override
	public void tearDown() throws Exception {
		//tearDown() is run after a test case has finished. 
		//finishOpenedActivities() will finish all the activitie	s that have been opened during the test execution.
		//solo.finishOpenedActivities();
	}

	/**
	 * A test that searches for Robotium and asserts that Robotium is found.
	 */
	public void testSetupActivity() {
		
		Instrumentation instrumentation = getInstrumentation();
		
		Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(CommCareSetupActivity.class.getName(), null, false);
		
	    Intent intent = new Intent(Intent.ACTION_MAIN);
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    intent.setClassName(instrumentation.getTargetContext(), CommCareSetupActivity.class.getName());
	    instrumentation.startActivitySync(intent);
		
	}
}