package edu.mit.jugonz97.gpslocation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * BroadcastReceiver that reports the
 * successful sending of a text message.
 */
public class SMSender extends BroadcastReceiver {
	
	// Our components
	private ArrayAdapter<String> recentItemsAdapter;
	private Handler handler = new Handler();
	private String textSent = "Text message sent.";
	private String genericFailure = "Generic failure occurred" +
			"while sending text message!";
	private String noService = "No service available, text not sent.";
	private String nullPDU = "Null PDU! Cannot continue!";
	private String radioOff = "Phone's radio is off! Please turn it" +
			"on if you wish to send a text message.";
	private TextView statusTV;

	/**
	 * Our SMSender constructor.
	 * @param adapter - an ArrayAdapter to write success/failure into
	 * @param tv - a TextView to write success/failure into
	 */
	public SMSender(ArrayAdapter<String> adapter, TextView tv) {
		recentItemsAdapter = adapter;
		statusTV = tv;
	}
	
	/**
	 * Method to determine what to write based on the
	 * result code of the SMS request.
	 */
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		switch (getResultCode()) {
            case Activity.RESULT_OK:
            	handler.post(new MessagePusher(textSent));
            	ModelOperations.updateList(recentItemsAdapter,
            			textSent);
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
            	handler.post(new MessagePusher(genericFailure));
            	ModelOperations.updateList(recentItemsAdapter,
            			genericFailure);
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
            	handler.post(new MessagePusher(noService));
            	ModelOperations.updateList(recentItemsAdapter,
            			noService);
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
            	handler.post(new MessagePusher(nullPDU));
            	ModelOperations.updateList(recentItemsAdapter,
            			nullPDU);
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
            	handler.post(new MessagePusher(radioOff));
            	ModelOperations.updateList(recentItemsAdapter,
            			radioOff);
                break;
		}
	}
	
	private class MessagePusher implements Runnable {
		private String textToPush;
		
		public MessagePusher(String text) {
			textToPush = text;
		}
		
		@Override
		public void run() {
			statusTV.setText(textToPush);
		}
		
	}
}
