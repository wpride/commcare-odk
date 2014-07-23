/**
 * 
 */
package org.commcare.dalvik.activities;

import org.commcare.android.database.global.models.AndroidSharedKeyRecord;
import org.commcare.android.framework.CommCareActivity;
import org.commcare.android.framework.ManagedUi;
import org.commcare.android.framework.UiElement;
import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.javarosa.core.services.storage.StorageFullException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author ctsims
 *
 */
@ManagedUi(R.layout.screen_permission_request)
public class KeyAccessRequestActivity extends CommCareActivity<KeyAccessRequestActivity> {
	
	@UiElement(value = R.id.screen_permission_grant_text_message, locale="app.key.request.message")
	TextView message;
	
	@UiElement(value = R.id.screen_permission_request_button_grant, locale="app.key.request.grant")
	Button grantButton;
	
	@UiElement(value = R.id.screen_permission_request_button_deny, locale="app.key.request.deny")
	Button denyButton;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		grantButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent response = new Intent(getIntent());
				
				AndroidSharedKeyRecord record = AndroidSharedKeyRecord.generateNewSharingKey();
				
				try {
					CommCareApplication._().getGlobalStorage(AndroidSharedKeyRecord.class).write(record);
				} catch (StorageFullException e) {
					setResult(Activity.RESULT_CANCELED);
					finish();
					return;
				}
				
				record.writeResponseToIntent(response);
				
				setResult(Activity.RESULT_OK, response);
				finish();
			}
			
		});
		
		denyButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
			
		});
	}
	
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
}
