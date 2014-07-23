/**
 * 
 */
package org.commcare.dalvik.activities;

import java.math.BigInteger;
import java.security.MessageDigest;

import org.commcare.android.database.SqlStorage;
import org.commcare.android.database.app.models.UserKeyRecord;
import org.commcare.android.framework.CommCareActivity;
import org.commcare.android.framework.ManagedUi;
import org.commcare.android.framework.UiElement;
import org.commcare.android.javarosa.AndroidLogger;
import org.commcare.android.models.notifications.NotificationMessage;
import org.commcare.android.models.notifications.NotificationMessageFactory;
import org.commcare.android.models.notifications.NotificationMessageFactory.StockMessages;
import org.commcare.android.tasks.DataPullTask;
import org.commcare.android.tasks.ManageKeyRecordListener;
import org.commcare.android.tasks.ManageKeyRecordTask;
import org.commcare.android.tasks.templates.HttpCalloutTask.HttpCalloutOutcomes;
import org.commcare.android.util.DemoUserUtil;
import org.commcare.android.util.SessionUnavailableException;
import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.commcare.dalvik.preferences.CommCarePreferences;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author ctsims
 *
 */
@ManagedUi(R.layout.screen_login)
public class LoginActivity extends CommCareActivity<LoginActivity> {
	
	public static final int MENU_DEMO = Menu.FIRST;
	
	public static String ALREADY_LOGGED_IN = "la_loggedin";
	
	@UiElement(value=R.id.login_button, locale="login.button")
	Button login;
	
	@UiElement(value=R.id.text_username, locale="login.username")
	TextView userLabel;
	@UiElement(value=R.id.text_password, locale="login.password")
	TextView passLabel;
	@UiElement(R.id.screen_login_bad_password)
	TextView errorBox;
	
	@UiElement(R.id.edit_username)
	EditText username;
	
	@UiElement(R.id.edit_password)
	EditText password;
	
	@UiElement(R.id.screen_login_banner_pane)
	View banner;
	
	@UiElement(R.id.str_version)
	TextView versionDisplay;
	
	public static final int TASK_KEY_EXCHANGE = 1;
	
	SqlStorage<UserKeyRecord> storage;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        username.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        
        //Only on the initial creation
        if(savedInstanceState ==null) {
        	String lastUser = CommCareApplication._().getCurrentApp().getAppPreferences().getString(CommCarePreferences.LAST_LOGGED_IN_USER, null);
        	if(lastUser != null) {
        		username.setText(lastUser);
        		password.requestFocus();
        	}
        }
        
        login.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				errorBox.setVisibility(View.GONE);
				//Try logging in locally
				if(tryLocalLogin(false)) {
					return;
				}

				startOta();
			}
        });
        
        versionDisplay.setText(CommCareApplication._().getCurrentVersionString());
        
        
        final View activityRootView = findViewById(R.id.screen_login_main);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            	int hideAll = LoginActivity.this.getResources().getInteger(R.integer.login_screen_hide_all_cuttoff);
            	int hideBanner = LoginActivity.this.getResources().getInteger(R.integer.login_screen_hide_banner_cuttoff);
                int height = activityRootView.getHeight();
                
                if(height < hideAll) {
                	versionDisplay.setVisibility(View.GONE);
                	banner.setVisibility(View.GONE);
                } else if(height < hideBanner) {
                	versionDisplay.setVisibility(View.VISIBLE);
                	banner.setVisibility(View.GONE);
                }  else {
                	versionDisplay.setVisibility(View.VISIBLE);
                	banner.setVisibility(View.VISIBLE);
                }
             }
        });
    }
    
	public String getActivityTitle() {
		//TODO: "Login"?
		return null;
	}

	private void startOta() {
		
		//We should go digest auth this user on the server and see whether to pull them
		//down.
		SharedPreferences prefs = CommCareApplication._().getCurrentApp().getAppPreferences();
		
		// TODO Auto-generated method stub
		//TODO: we don't actually always want to do this. We need to have an alternate route where we log in locally and sync 
		//(with unsent form submissions) more centrally.
		
		DataPullTask<LoginActivity> dataPuller = new DataPullTask<LoginActivity>(getUsername(), 
                 password.getText().toString(),
                 prefs.getString("ota-restore-url",LoginActivity.this.getString(R.string.ota_restore_url)),
                 prefs.getString("key_server",LoginActivity.this.getString(R.string.key_server)),
                 LoginActivity.this) {

					@Override
					protected void deliverResult( LoginActivity receiver, Integer result) {
						switch(result) {
						case DataPullTask.AUTH_FAILED:
							receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Auth_BadCredentials, new String[3], NOTIFICATION_MESSAGE_LOGIN), false);
							break;
						case DataPullTask.BAD_DATA:
							receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Remote_BadRestore, new String[3], NOTIFICATION_MESSAGE_LOGIN));
							break;
						case DataPullTask.DOWNLOAD_SUCCESS:
							if(!tryLocalLogin(true)) {
								receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Auth_CredentialMismatch, new String[3], NOTIFICATION_MESSAGE_LOGIN));
							} else {
								break;
							}
						case DataPullTask.UNREACHABLE_HOST:
							receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Remote_NoNetwork, new String[3], NOTIFICATION_MESSAGE_LOGIN), true);
							break;
						case DataPullTask.CONNECTION_TIMEOUT:
							receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Remote_Timeout, new String[3], NOTIFICATION_MESSAGE_LOGIN), true);
							break;
						case DataPullTask.SERVER_ERROR:
							receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Remote_ServerError, new String[3], NOTIFICATION_MESSAGE_LOGIN), true);
							break;
						case DataPullTask.UNKNOWN_FAILURE:
							receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Restore_Unknown, new String[3], NOTIFICATION_MESSAGE_LOGIN), true);
							break;
						}

					}

					@Override
					protected void deliverUpdate( LoginActivity receiver, Integer... update) {
						if(update[0] == DataPullTask.PROGRESS_STARTED) {
							receiver.updateProgress(DataPullTask.DATA_PULL_TASK_ID, Localization.get("sync.progress.purge"));
						} else if(update[0] == DataPullTask.PROGRESS_CLEANED) {
							receiver.updateProgress(DataPullTask.DATA_PULL_TASK_ID, Localization.get("sync.progress.authing"));
						} else if(update[0] == DataPullTask.PROGRESS_AUTHED) {
							receiver.updateProgress(DataPullTask.DATA_PULL_TASK_ID, Localization.get("sync.progress.downloading"));
						} else if(update[0] == DataPullTask.PROGRESS_RECOVERY_NEEDED) {
							receiver.updateProgress(DataPullTask.DATA_PULL_TASK_ID, Localization.get("sync.recover.needed"));
						} else if(update[0] == DataPullTask.PROGRESS_RECOVERY_STARTED) {
							receiver.updateProgress(DataPullTask.DATA_PULL_TASK_ID, Localization.get("sync.recover.started"));
						}
						
					}

					@Override
					protected void deliverError( LoginActivity receiver, Exception e) {
						receiver.raiseMessage(NotificationMessageFactory.message(StockMessages.Restore_Unknown, new String[3], NOTIFICATION_MESSAGE_LOGIN), true);
					}
			
		};
		
		dataPuller.connect(this);
		dataPuller.execute();
	}

	/*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        try {
        	//TODO: there is a weird circumstance where we're logging in somewhere else and this gets locked.
        if(CommCareApplication._().getSession().isLoggedIn() && CommCareApplication._().getSession().getLoggedInUser() != null) {
    		Intent i = new Intent();
    		i.putExtra(ALREADY_LOGGED_IN, true);
            setResult(RESULT_OK, i);
            
            CommCareApplication._().clearNotifications(NOTIFICATION_MESSAGE_LOGIN);
    		finish();
    		return;
        }
        }catch(SessionUnavailableException sue) {
        	//Nothing, we're logging in here anyway
        }
        
        refreshView();
    }
    
    private void refreshView() {
    }
    
    private String getUsername() {
    	return username.getText().toString().toLowerCase().trim();
    }
    
    private boolean tryLocalLogin(final boolean warnMultipleAccounts) {
    	//TODO: check username/password for emptiness
    	return tryLocalLogin(getUsername(), password.getText().toString(), warnMultipleAccounts);
    }
    	
    private boolean tryLocalLogin(final String username, String password, final boolean warnMultipleAccounts) {
    	try{
    		
    		//TODO: We don't actually even use this anymore other than for hte local login count, which
    		//seems super silly.
	    	UserKeyRecord matchingRecord = null;
	    	int count = 0;
	    	for(UserKeyRecord record : storage()) {
	    		if(!record.getUsername().equals(username)) {
	    			continue;
	    		}
	    		count++;
	    		String hash = record.getPasswordHash();
	    		if(hash.contains("$")) {
	        		String alg = "sha1";
	        		String salt = hash.split("\\$")[1];
	        		String check = hash.split("\\$")[2];
	        		MessageDigest md = MessageDigest.getInstance("SHA-1");
	        		BigInteger number = new BigInteger(1, md.digest((salt+password).getBytes()));
	        		String hashed = number.toString(16);
	        			
	        		while(hashed.length() < check.length()) {
	        			hashed = "0" + hashed;
	        		}
	        		
	        		if(hash.equals(alg + "$" + salt + "$" + hashed)) {
	        			matchingRecord = record;
	        		}
	        	}
	    	}
	    	
	    	final boolean triggerTooManyUsers = count > 1 && warnMultipleAccounts;
	    	
	    	ManageKeyRecordTask<LoginActivity> task = new ManageKeyRecordTask<LoginActivity>(this, TASK_KEY_EXCHANGE, username, password, CommCareApplication._().getCurrentApp(), new ManageKeyRecordListener<LoginActivity>() {

				@Override
				public void keysLoginComplete(LoginActivity r) {
					if(triggerTooManyUsers) {
						//We've successfully pulled down new user data. 
						//Should see if the user already has a sandbox and let them know that their old data doesn't transition
						raiseMessage(NotificationMessageFactory.message(StockMessages.Auth_RemoteCredentialsChanged, new String[3]), true);
						Logger.log(AndroidLogger.TYPE_USER, "User " + username + " has logged in for the first time with a new password. They may have unsent data in their other sandbox");
					}
					done();
				}

				@Override
				public void keysReadyForSync(LoginActivity r) {
					//TODO: we only wanna do this on the _first_ try. Not subsequent ones (IE: On return from startOta)
					startOta();
				}

				@Override
				public void keysDoneOther(LoginActivity r, HttpCalloutOutcomes outcome) {
					switch(outcome) {
					case AuthFailed:
						Logger.log(AndroidLogger.TYPE_USER, "auth failed");
						r.raiseMessage(NotificationMessageFactory.message(StockMessages.Auth_BadCredentials, new String[3], NOTIFICATION_MESSAGE_LOGIN), false);
						break;
					case BadResponse:
						Logger.log(AndroidLogger.TYPE_USER, "bad response");
						r.raiseMessage(NotificationMessageFactory.message(StockMessages.Remote_BadRestore, new String[3], NOTIFICATION_MESSAGE_LOGIN), true);
						break;
					case NetworkFailure:
						Logger.log(AndroidLogger.TYPE_USER, "bad network");
						r.raiseMessage(NotificationMessageFactory.message(StockMessages.Remote_NoNetwork, new String[3], NOTIFICATION_MESSAGE_LOGIN), false);
						break;
					case BadCertificate:
						Logger.log(AndroidLogger.TYPE_USER, "bad certificate");
						r.raiseMessage(NotificationMessageFactory.message(StockMessages.BadSSLCertificate, new String[3], NOTIFICATION_MESSAGE_LOGIN), false);
						break;
					case UnkownError:
						Logger.log(AndroidLogger.TYPE_USER, "unknown");
						r.raiseMessage(NotificationMessageFactory.message(StockMessages.Restore_Unknown, new String[3], NOTIFICATION_MESSAGE_LOGIN), true);
						break;
					}
				}
				
			}) {
				@Override
				protected void deliverUpdate(LoginActivity receiver, String... update) {
					receiver.updateProgress(this.getTaskId(), update[0]);
				}
			};
			
			task.connect(this);
			task.execute();
			
			return true;
    	}catch (Exception e) {
    		e.printStackTrace();
    		return false;
    	}
    }
    
    private void done() {
		Intent i = new Intent();
        setResult(RESULT_OK, i);
     
        CommCareApplication._().clearNotifications(NOTIFICATION_MESSAGE_LOGIN);
		finish();
    }
    
    private SqlStorage<UserKeyRecord> storage() throws SessionUnavailableException{
    	if(storage == null) {
    		storage=  CommCareApplication._().getAppStorage(UserKeyRecord.class);
    	}
    	return storage;
    }

	public void finished(int status) {
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_DEMO, 0, Localization.get("login.menu.demo")).setIcon(android.R.drawable.ic_menu_preferences);
        return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean otherResult = super.onOptionsItemSelected(item);
		switch(item.getItemId()) {
		case MENU_DEMO:
			//Make sure we have a demo user
			DemoUserUtil.checkOrCreateDemoUser(this, CommCareApplication._().getCurrentApp());
			
			//Now try to log in as the demo user
			tryLocalLogin(DemoUserUtil.DEMO_USER, DemoUserUtil.DEMO_USER, false);
			
			return true;
		default:
			return otherResult;
		}
	}

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case TASK_KEY_EXCHANGE:
        	ProgressDialog progress = new ProgressDialog(this);
        	//Add cancelling
//            DialogInterface.OnClickListener loadingButtonListener =
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                            //If it is null, this is tricky to recover from? 
//                            if(dataPuller != null) {
//                            	dataPuller.cancel(true);
//                            }
//                        }
//                    };
        	progress.setTitle(Localization.get("key.manage.title"));
        	progress.setMessage(Localization.get("key.manage.start"));
        	progress.setIndeterminate(true);
        	progress.setCancelable(false);
            return progress;
        	
            case DataPullTask.DATA_PULL_TASK_ID:
            	ProgressDialog mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //If it is null, this is tricky to recover from? 
                                	LoginActivity.this.cancelCurrentTask();
                                }
                            };
                mProgressDialog.setTitle(Localization.get("sync.progress.title"));
                mProgressDialog.setMessage(Localization.get("sync.progress.starting"));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(Localization.get("option.cancel"), loadingButtonListener);
                return mProgressDialog;
        }
        return null;
    }

	private void raiseMessage(NotificationMessage message) {
    	raiseMessage(message, true);
    }
    
    private void raiseMessage(NotificationMessage message, boolean showTop) {
    	String toastText = message.getTitle();
    	if(showTop) {
    		CommCareApplication._().reportNotificationMessage(message);
    		toastText = Localization.get("notification.for.details.wrapper", new String[] {toastText});
    	}
    	
    	//either way
		errorBox.setVisibility(View.VISIBLE);
		errorBox.setText(toastText);
    	
		Toast.makeText(this,toastText, Toast.LENGTH_LONG).show();
    }
    
    public final static String NOTIFICATION_MESSAGE_LOGIN = "login_message";
}
