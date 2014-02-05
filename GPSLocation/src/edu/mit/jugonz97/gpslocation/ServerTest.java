package edu.mit.jugonz97.gpslocation;

import java.util.ArrayList;

import com.google.android.maps.MapActivity;

import edu.mit.jugonz97.gpslocation.PCConnectionService.LocalBinder;
import edu.mit.jugonz97.gpslocation.R;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Our main entry point to our android application.
 * The life cycle methods are not working well with the
 * socket currently, but work well otherwise.
 *
 */
public class ServerTest extends MapActivity {
	// Our components
	private ArrayAdapter<String> recentCommandsAdapter;
	private ArrayList<String> recentCommands = new ArrayList<String>();
	
	/**
	 * Receiver to update our UI if a message has been sent,
	 * a message has been received, or an error has occurred.
	 */
	private BroadcastReceiver updateTextReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent textIntent) {
			// Just get the string, update the textView,
			// and then update our array adapter
			String newText = textIntent.getStringExtra("textToSet");
			if (statusView != null) {
				statusView.setText(newText);
				if (recentCommandsAdapter != null) {
					ModelOperations.updateList(recentCommandsAdapter, newText);
				}
			}	
		}
	};
	
	private boolean boundToServer;
	private ListView recentItems;
	private PCConnectionService androidServerService;
	
	/**
	 * Class to regulate the behavior of our connection
	 * with our Android server Service.
	 */
	private ServiceConnection serverServiceConnection = new ServiceConnection() {
		/**
		 * Method called upon successful binding.
		 */
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// Get the Binder, and through the binder the Service
			Log.d("onServiceConnected", "binding in process");
			LocalBinder binder = (LocalBinder) service;
			androidServerService = binder.getService();
			boundToServer = true;

			// Start the server
			androidServerService.startServer();
		}

		/**
		 * Method called when the server Service we're bound to
		 * unexpectedly quits (or is killed by Android).
		 */
		@Override
		public void onServiceDisconnected(ComponentName name) {
			androidServerService = null;
			boundToServer = false;
			String serviceDisconnectMsg = "Server Service unpexctedly quit.";

			// Update our UI with this message if possible
			if (statusView != null) {
				statusView.setText(serviceDisconnectMsg);
				// Okay to call updateList() with a null
				ModelOperations.updateList(recentCommandsAdapter, serviceDisconnectMsg);
			}
		}
	};
	
	private SMSender SMSCallback;
	public static final String updateTextAction = "edu.mit.jugonz97.androidconduit.UPDATE_TEXT";
	private TextView statusView;

	/**
	 * Run on the creation of our Application.
	 * Treated like a constructor.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);

		Log.i("onCreate", "Running onCreate");
		// Display code
		setContentView(R.layout.activity_server_test);
		statusView = (TextView) findViewById(R.id.statusTextView);

		// Take care of Recent Items List
		recentItems = (ListView) findViewById(R.id.recentCommands);
		recentCommandsAdapter = new ArrayAdapter<String> (this,
				android.R.layout.simple_list_item_1, android.R.id.text1, recentCommands);
		recentItems.setAdapter(recentCommandsAdapter);

		// Code to understand the end of a call (needs overhaul)
		EndCallListener callListener = new EndCallListener();
		TelephonyManager callManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		callManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);

		// Class to update our status when we send text messages
		SMSCallback = new SMSender(recentCommandsAdapter, statusView);

		// Receivers
		registerReceiver(updateTextReceiver, new IntentFilter(updateTextAction));
		registerReceiver(SMSCallback, new IntentFilter("SMS_SENT"));

		// Attempt to bind to our server Service
		Intent serverIntent = new Intent(this, PCConnectionService.class);
		bindService(serverIntent, serverServiceConnection, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Run when Android quits our Activity.
	 * Treated like a destructor.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("onDestroy", "about to run");
		// Unregister receivers & listeners
		unregisterReceiver(updateTextReceiver);
		unregisterReceiver(SMSCallback);

		// Unbind from our server Service
		if (boundToServer) {
			unbindService(serverServiceConnection);
			boundToServer = false;
		}

	}

	// Following two are needed for Google Maps API
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_current_location, menu);
		return true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}