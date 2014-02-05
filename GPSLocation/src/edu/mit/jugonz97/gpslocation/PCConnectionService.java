package edu.mit.jugonz97.gpslocation;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PCConnectionService extends Service {
	public static final int serverPort = 4444;

	// Our components
	private BroadcastReceiver SMSReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent smsIntent) {
			// Call our internal method below
			receivedBroadcast(smsIntent);
		}
	};
	private ConnectionHandler androidServer;
	private Geocoder geocoder;
	private final LocalBinder serviceBinder = new LocalBinder();
	private final LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location newLoc) {
			if (outWriter != null) {
				Log.i("locationListener", "running");
				String address = getAddress(newLoc); // Get the address			
				// Send the message!
				StringBuilder toPrintWriterBuilder = new StringBuilder();
				toPrintWriterBuilder.append("locdata ").append(address);
				outWriter.println(toPrintWriterBuilder.toString());	
			}
		}

		// Not interested in provider events
		@Override
		public void onProviderDisabled(String arg0) {}
		@Override
		public void onProviderEnabled(String arg0) {}
		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}	
	};
	private LocationManager locationManager;
	private PendingIntent sentPI;
	private PrintWriter outWriter;
	private static final String logTag = "PCConnectionService";
	private static final String SMSaction = "android.provider.Telephony.SMS_RECEIVED";
	private static final String SMSReceiverTag = "SMSReceiver";

	@Override
	public void onCreate() {
		// Register our receivers
		sentPI = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
		registerReceiver(SMSReceiver, new IntentFilter(SMSaction));
		
		// Start listening for our location
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
				1500, 0, locationListener);
		
		// Set up our Geocoder
		geocoder = new Geocoder(this, Locale.getDefault());
		
		// Finally, set up our server AsyncTask
		androidServer = new ConnectionHandler();
	}


	/**
	 * Our onBind(Intent).
	 * Give ourselves to the calling activity.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(logTag, "Received bound request!");
		return serviceBinder;
	}

	/**
	 * Our onUnbind(Intent).
	 * Called when all clients have disconnected.
	 * In theory, onDestroy() should be called next,
	 * but we should cancel our server thread just in case.
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(logTag, "onUnbind running");
		// Need to cancel our server thread
		androidServer.cancel(true);
		// Don't call onRebind(), which we haven't implemented
		return false;
	}
	
	/**
	 * Binder class to return ourselves.
	 * Called by onBind(). Allows binding client
	 * to call public methods on this Service.
	 */
	public class LocalBinder extends Binder {
		PCConnectionService getService() {
			return PCConnectionService.this;
		}
	}

	/**
	 * Public method to start our server. Does not start
	 * our server if it is currently or has finished running.
	 * If the server could not be started (which should not happen),
	 * publishes to our UI a descriptive error message.
	 */
	public void startServer() {
		if (!serverIsRunning()) {
			androidServer.execute();
			Log.d(logTag, "Server thread started");
		} else {
			// Publish an error message
			// Make our intent, and put in our String
			Intent showErrorIntent = new Intent();
			showErrorIntent.setAction(ServerTest.updateTextAction);
			showErrorIntent.putExtra("textToSet", "Server could not be started." +
					" Currently server is: " + androidServer.getStatus().toString());
			// Send the broadcast
			Log.d(logTag, "Server could not be started." +
					" Currently server is: " + androidServer.getStatus().toString());
			sendBroadcast(showErrorIntent);
		}
	}

	/**
	 * Private method to tell if our server is running.
	 * @return a boolean indicating whether our server is
	 * currently running. To more easily handle server shutdown,
	 * this method returns true if our server has finished running.
	 */
	private boolean serverIsRunning() {
		// Get the status
		Status androidServerStatus = androidServer.getStatus();
		boolean toReturn = false;

		// Set our boolean and return it
		switch(androidServerStatus) {
		case PENDING:
			toReturn = false;
			break;
		case RUNNING:
			toReturn = true;
		case FINISHED:
			toReturn = true;
			break;
		default:
			// Should be impossible
			toReturn = false;
			break;
		}
		return toReturn;
	}

	/**
	 * AsyncTask to run our action connection with the PC.
	 * Handles input internally. For output, sets the PrintWriter in our
	 * PCConnectionService to allow it to write messages out.
	 * Upon termination, invalidates that PrintWriter reference.
	 * Also updates UI with received input (and error messages).
	 */
	public class ConnectionHandler extends AsyncTask<Void, String, Boolean> {
		// Our components
		private BufferedReader in;
		private ServerSocket servSock;
		private Socket clientSock;

		/*
		 * Our regular expression patterns.
		 * Number is group 1 (in contentP).
		 * Message (if any, else whitespace) is group 2 (in contentP).
		 * Identifier is group 1 (in messageP).
		 * Content is group 5 (in messageP).
		 */
		private final Pattern contentP = Pattern.compile("n([0-9]+)[\\s]*(.*)");
		private final Pattern callP = Pattern.compile("(call)");
		private final Pattern sendmsgP = Pattern.compile("(msgsend)");
		private final Pattern recvmsgP = Pattern.compile("(msgrecv)");
		private final Pattern identifierP = Pattern.compile(callP.pattern() + "|" + sendmsgP.pattern() + "|" + recvmsgP.pattern());
		private final Pattern messageP = Pattern.compile("(" + identifierP.pattern() + ")[\\s](" + contentP.pattern() + ")");
		private final String quitString = "quit";

		/**
		 * Called by the UI thread when we want to update our status.
		 * Updates a TextView, and puts a new entry into the
		 * recent messages array through a BroadcastReceiver.
		 */
		@Override
		protected void onProgressUpdate(String... progress) {
			// Make our intent, and put in our String
			Intent updateTextIntent = new Intent();
			updateTextIntent.setAction(ServerTest.updateTextAction);
			updateTextIntent.putExtra("textToSet", progress[0]);

			// Send the broadcast
			sendBroadcast(updateTextIntent);
		}

		/**
		 * Method to set up our socket connection
		 * and handle incoming messages.
		 */
		@Override
		protected Boolean doInBackground(Void... params) {
			Log.d(logTag, "AsyncTask doInBackground starting");
			publishProgress("Listening on localhost port: " + serverPort + "...");

			try {
				// Get our sockets
				servSock = new ServerSocket(serverPort);
				clientSock = servSock.accept();

				// Now, we're connected, set up I/O
				publishProgress("Connected to PC.");
				in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
				// Stored in Service, not AsyncTask
				outWriter = new PrintWriter(new OutputStreamWriter(clientSock.getOutputStream()), true);

				// Read incoming messages until we get a quit message
				for (String line = in.readLine(); (line != null && !line.equals(quitString)); line = in.readLine()) {
					publishProgress("Received input: " + line);
					handleMessage(line);
				}	
				clientSock.close();

				return Boolean.TRUE;
			} catch (IOException e) {
				publishProgress("I/O Exception in PCConnectionService.");
			}
			return Boolean.FALSE;
		}

		/**
		 * The reader of all incoming messages.
		 * Starts activities, sends messages, and alerts us
		 * about messages.
		 * @param message - a String message to parse
		 */
		public void handleMessage(String message) {
			Matcher messageMatcher = messageP.matcher(message);

			Log.d(logTag, "message: " + message);
			if (messageMatcher.matches()) {
				// Get the identifier and content
				String identifier = messageMatcher.group(1);
				String content = messageMatcher.group(5);
				Matcher contentMatcher = contentP.matcher(content);

				// If we have no content, just log and return.
				if (!contentMatcher.matches()) {
					Log.d(logTag, "ContentMatcher failed with input:" + content);
					return;
				}

				// Else, get our details
				String number = contentMatcher.group(1);
				Matcher idMatcher = identifierP.matcher(identifier);
				Matcher callM = callP.matcher(identifier);
				Matcher sendmsgM = sendmsgP.matcher(identifier);

				if (callM.matches()) {
					// We have a winner, make a call to that number
					Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
					callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(callIntent);

				} else if (sendmsgM.matches()) {
					// Have the number, now get the message
					String messageContent = contentMatcher.group(2);
					SmsManager smsManage = SmsManager.getDefault();

					// Split up the message so we can send it even if it is too long
					ArrayList<String> dividedMessage = smsManage.divideMessage(messageContent);
					ArrayList<PendingIntent> sentPIList = new ArrayList<PendingIntent>();
					sentPIList.add(sentPI);

					// Actually send the message
					smsManage.sendMultipartTextMessage(number, null, dividedMessage, sentPIList, null);

				} else if (!idMatcher.matches()) {
					Log.d(logTag, "Identifier:" + identifier + " wasn't recognized.");
				} else {
					Log.d(logTag, "Identifier unknown error");
				}
			} else {
				Log.d(logTag, "Message received:" + message + " didn't match!");
			}
		}

		/**
		 * Method to tell our calling Activity about our completion.
		 */
		@Override
		protected void onPostExecute(Boolean quitSuccessful) {
			// Update the UI based on connection close results
			Intent updateTextIntent = new Intent();
			updateTextIntent.setAction(ServerTest.updateTextAction);

			// If our PC connection closed properly...
			if (quitSuccessful) {			// quitSuccessful cannot be null
				updateTextIntent.putExtra("textToSet", "Closed connection to PC.");
			} else {
				updateTextIntent.putExtra("textToSet", "Connection to PC failed.");
			}

			// Send the broadcast
			sendBroadcast(updateTextIntent);
		}

		/**
		 * Method run if we've been called to terminate.
		 * Called by our Service's onDestroy().
		 */
		@Override
		protected void onCancelled() {
			Log.d(logTag, "cancelling AsyncTask");

			// Ignore errors, as we already made a command to stop
			if (clientSock != null)  {
				try {
					if (outWriter != null) {
						// If possible, tell our PC that we're quitting
						outWriter.println(quitString);
					}
					clientSock.close();
				} catch (IOException e) {}
			}
			outWriter = null; // Don't leak our PrintWriter reference

			if (servSock != null) {
				try {
					servSock.close();
				} catch (IOException e) {}
			}
		}
	}	

	/**
	 * Method to get a human-readable address String from a Location.
	 * @param locationToTranscribe - a Location instance
	 * @return a String of our current address
	 */
	private String getAddress(Location locationToTranscribe) {
		// Get our current latitude and longitude
		double latitude = locationToTranscribe.getLatitude();
		double longitude = locationToTranscribe.getLongitude();

		try {
			// Get the most likely address
			List<Address> transcribedAddress = geocoder.getFromLocation(latitude, longitude, 1);
			if (transcribedAddress != null && transcribedAddress.size() > 0) {
				Address toReturnAddress = transcribedAddress.get(0);

				// Now, build the human-readable string
				StringBuilder stringBuilderAddress = new StringBuilder();
				for (int i=0; i < toReturnAddress.getMaxAddressLineIndex(); ++i) {
					stringBuilderAddress.append(toReturnAddress.getAddressLine(i));
					if (i + 1 < toReturnAddress.getMaxAddressLineIndex()) {
						stringBuilderAddress.append(", ");
					}
				}

				return stringBuilderAddress.toString();
			} else {
				return "No address found.";
			}
		} catch (IOException e) {
			return "Address lookup failed.";
		}
	}
	
	/**
	 * Method called by our BroadcastReceiver
	 * to send incoming SMS messages to our PC.
	 * @param smsIntent - the Intent holding the incoming messages
	 */
	private void receivedBroadcast(Intent smsIntent) {
		// Only act if we get the proper Intent
		Log.i(SMSReceiverTag, "I was run!");

		if (smsIntent.getAction().equals(SMSaction)) {
			Bundle intentBundle = smsIntent.getExtras();

			if (intentBundle != null) {
				Object[] rawMessages = (Object[]) intentBundle.get("pdus");
				SmsMessage[] smsMessages = new SmsMessage[rawMessages.length];

				for (int i=0; i < rawMessages.length; ++i) {
					smsMessages[i] = SmsMessage.createFromPdu((byte[]) rawMessages[i]);
				}

				// Now, smsMessages has our incoming text messages
				Log.i(SMSReceiverTag, "Received the following messages:");

				for (int j=0; j < smsMessages.length; ++j) {
					String messageBody = smsMessages[j].getMessageBody();
					Log.i(SMSReceiverTag, messageBody);
					
					String fromAddress = smsMessages[j].getOriginatingAddress();
					Log.i(SMSReceiverTag, "fromAddress: " + fromAddress);
					
					if (messageBody != null) {
						StringBuilder outMessage = new StringBuilder(128);
						outMessage.append("msgrecv n").append(fromAddress).append(" ").append(messageBody);
						
						// Update the UI to reflect that we've gotten a message
						Intent updateTextIntent = new Intent();
						updateTextIntent.setAction(ServerTest.updateTextAction);
						updateTextIntent.putExtra("textToSet", outMessage.toString());

						// Send the broadcast
						sendBroadcast(updateTextIntent);
						
						if (outWriter != null) {
							Log.i(SMSReceiverTag, "writing to outputWriter now!");
							outWriter.println(outMessage.toString());
							Log.i(SMSReceiverTag, "sent to PC: " + outMessage.toString());
						} else {
							Log.i(SMSReceiverTag, "outputWriter was null!");
						}
					}
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		Log.d(logTag, "onDestroy running");
		// Unregister receivers
		unregisterReceiver(SMSReceiver);
		
		// Stop listening for location updates
		locationManager.removeUpdates(locationListener);
		
		// Need to cancel our server thread
		androidServer.cancel(true);
	}

}
