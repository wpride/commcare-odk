/**
 * 
 */
package org.commcare.dalvik.activities;

import org.commcare.android.adapters.MessageRecordAdapter;
import org.commcare.dalvik.R;
import org.commcare.android.util.SessionUnavailableException;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * @author ctsims
 *
 */
public class MessageLogActivity extends ListActivity {

	LinearLayout header;
	MessageRecordAdapter messages;
	
	boolean isMessages = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(getString(R.string.application_name) + " > " + "Message Logs");
        
        refreshView();
    }

    /**
     * Get form list from database and insert into view.
     */
    private void refreshView() {
    	try {
    		messages = new MessageRecordAdapter(this, this.getContentResolver().query(Uri.parse("content://sms"),new String[] {"_id","address","date","type","read","thread_id"}, "type=?", new String[] {"1"}, "date" + " DESC"));
    		this.setListAdapter(messages);
    	} catch(SessionUnavailableException sue) {
    		//TODO: login and return
    	}
    }

    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
    	String number = (String)messages.getItem(position);
    	Intent i = new Intent(this, CallOutActivity.class);
    	i.putExtra(CallOutActivity.PHONE_NUMBER, number);
    	i.putExtra(CallOutActivity.INCOMING_ACTION, Intent.ACTION_SENDTO);
    	startActivity(i);
    	return;
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//    	switch(requestCode){
//    	case CONFIRM_SELECT:
//    		if(resultCode == RESULT_OK) {
//    	        // create intent for return and store path
//    	        Intent i = new Intent(this.getIntent());
//    	        
//    	        i.putExtras(intent.getExtras());
//    	        setResult(RESULT_OK, i);
//
//    	        finish();
//        		return;
//    		} else {
//    	        Intent i = new Intent(this.getIntent());
//    	        setResult(RESULT_CANCELED, i);
//        		return;
//    		}
//    	default:
//    		super.onActivityResult(requestCode, resultCode, intent);
//    	}
//    }
}
