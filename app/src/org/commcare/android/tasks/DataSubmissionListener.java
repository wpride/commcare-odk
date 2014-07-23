/**
 * 
 */
package org.commcare.android.tasks;

/**
 * @author ctsims
 *
 */
public interface DataSubmissionListener {
	public void beginSubmissionProcess(int totalItems);
	public void startSubmission(int itemNumber, long length);
	public void notifyProgress(int itemNumber, long progress);
	public void endSubmissionProcess();
}
