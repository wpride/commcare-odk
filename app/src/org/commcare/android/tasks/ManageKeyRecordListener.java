/**
 * 
 */
package org.commcare.android.tasks;

import org.commcare.android.tasks.templates.HttpCalloutTask.HttpCalloutOutcomes;

/**
 * @author ctsims
 *
 */
public interface ManageKeyRecordListener<R> {
	
	
	/**
	 * This signals that a login was completed successfully with
	 * a user and data in the sandbox. 
	 */
	public void keysLoginComplete(R r);
	
	/**
	 * This signals that the app is ready to sync the applicable user credentials,
	 * but that no user was logged in with those credentials.
	 */
	public void keysReadyForSync(R r);
	
	/**
	 * This signals any unsuccessful outcome which is passed as an
	 * argument. 
	 * 
	 * @param outcome
	 */
	public void keysDoneOther(R r, HttpCalloutOutcomes outcome);
}
