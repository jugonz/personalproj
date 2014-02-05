package edu.mit.jugonz97.gpslocation;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class EndCallListener extends PhoneStateListener {

	// Need to override onCallStateChanged method.
	
	@Override
	public void onCallStateChanged(int currentState, String incomingPhoneNum) {
		if (currentState == TelephonyManager.CALL_STATE_RINGING) {
			Log.i("onCallStateChanged", "Phone ringing w/num:" + incomingPhoneNum);
		} else if (currentState == TelephonyManager.CALL_STATE_OFFHOOK) {
			Log.i("onCallStateChanged", "Phone was hung up!");
		} else if (currentState == TelephonyManager.CALL_STATE_IDLE) {
			Log.i("onCallStateChanged", "Time to restart main Activity!");
		}
	}

}
