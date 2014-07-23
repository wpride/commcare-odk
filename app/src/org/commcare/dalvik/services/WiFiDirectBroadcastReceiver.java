/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.commcare.dalvik.services;

import org.commcare.android.framework.DeviceListFragment;
import org.commcare.android.framework.WiFiDirectManagementFragment;
import org.commcare.android.javarosa.AndroidLogger;
import org.commcare.dalvik.R;
import org.commcare.dalvik.activities.CommCareWiFiDirectActivity;
import org.javarosa.core.services.Logger;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Build;
import android.util.Log;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private Channel channel;
    private WiFiDirectManagementFragment activity;

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
            WiFiDirectManagementFragment activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
    public void onReceive(Context context, Intent intent) {
    	Log.d(CommCareWiFiDirectActivity.TAG, "in on receive ");
        String action = intent.getAction();
        
        Logger.log(CommCareWiFiDirectActivity.TAG, "onReceive of BroadCastReceiver with action: " + action);
        
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            	Log.d(CommCareWiFiDirectActivity.TAG, "BR enabled");
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
            	Log.d(CommCareWiFiDirectActivity.TAG, "BR not enabled");
                activity.setIsWifiP2pEnabled(false);
                activity.resetData();

            }
            Log.d(CommCareWiFiDirectActivity.TAG, "P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
            	
            	activity.onPeersChanged();
            	
            }
            Log.d(CommCareWiFiDirectActivity.TAG, "P2P peers changed2");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            
            activity.onP2PConnectionChanged(networkInfo.isConnected());

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	Log.d(CommCareWiFiDirectActivity.TAG, "in last else with device: " + intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE).toString());
        	
        	
        	activity.onThisDeviceChanged(intent);
        	

        }
    }
}
