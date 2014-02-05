package backend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class MobileModel {


	/**
	 * INTERNAL VARIABLES AND DATATYPES
	 */


	// Enum to internally control requests for resources
	public enum RequestType {
		MESSAGE, PHONENUMBER
	}

	// Fixed properties of our program
	private static final int listenPORT = 4444;
	private static final String timeFormat = "h:mm a";
	private static final String lineSeparator = System.getProperty("line.separator");
	private static final String textSeparator = "----------";

	// Objects related to connection with Android
	private BufferedReader pcIn;
	private PrintWriter pcOut;
	private Socket clientSocket;

	// Internally used storage of pre-loaded messages and phone numbers
	private final Map<String, String> phoneNumberMap;
	private final Map<String, String> textMessageMap;
	private String[] phoneNames;
	private String[] shortMessages;
	
	// Current location data
	private String currentAddress;

	// Internally used conversation data and outgoing phone number
	private final Set<Conversation> currentConversations;
	private String defaultPhoneNumber;


	/**
	 * CONSTRUCTOR AND REPRESENTATION INVARIANT
	 */


	/**
	 * Constructor for our Model.
	 * Sets up our list of text messages,
	 * and pre-inserted phone numbers and names.
	 * @throws MessageFormatException - if a pre-loaded message/number is improperly set up.
	 * @throws IOException - if the text message/phone number file could not be loaded.
	 */
	public MobileModel() throws MessageFormatException, IOException {
		clientSocket = null;
		textMessageMap = new HashMap<String, String>();
		phoneNumberMap = new HashMap<String, String>();
		currentConversations = new HashSet<Conversation>();
		shortMessages = new String[0];
		phoneNames = new String[0];
		defaultPhoneNumber = "";
		currentAddress = "";
		populateMap(RequestType.PHONENUMBER);
		populateMap(RequestType.MESSAGE);
		setupConversations();
		checkRep();
	}

	/**
	 * Representation invariant for MobileModel:
	 * Everything is not null (except clientSocket,
	 * and our arrays are set in an external method
	 * so they can't be checked here)
	 */
	private void checkRep() {
		assert phoneNumberMap != null;
		assert textMessageMap != null;
		assert phoneNames != null;
		assert shortMessages != null;
		assert currentAddress != null;
		assert currentConversations != null;
		assert defaultPhoneNumber != null;
	}


	/**
	 * SOCKET CONNECTION RELATED METHODS AND OBJECTS
	 */


	/**
	 * Method to set up our connection with the Android device.
	 * Blocks until a connection is established. If a connection already exists, does nothing.
	 * @modifies clientSocket, pcIn and pcOut. This method sets these variables to
	 * the current active connection's socket, a BufferedReader on the input stream,
	 * and a PrintWriter on the outputStream.
	 * @return A boolean indicating success or failure (failure occurs if an exception is thrown).
	 * If a connection already exists, returns true.
	 * @throws IOException - if the Socket creation throws an exception. Needs to be handled by the caller.
	 */
	public boolean setupConnection() throws IOException {
		if (clientSocket == null || !clientSocket.isConnected()) {

			ConnectionSetupRunnable setupRunnable = new ConnectionSetupRunnable();

			Thread toRun = new Thread(setupRunnable);
			toRun.start();		
			try {
				toRun.join();
				listen();
				if (setupRunnable.socketException != null) {
					throw setupRunnable.socketException;
				}
			} catch (InterruptedException e) {
				// May cause some problems, but code should be unreachable
				clientSocket = null;
				pcIn = null;
				pcOut = null;
				return false;
			}
			if (clientSocket == null || !clientSocket.isConnected()) return false; // An exception was thrown
		}

		return true;
	}

	/**
	 * Method used privately to listen for messages from the Android phone.
	 * Starts a new thread to do so, so as not to block the UI.
	 */
	private void listen() {
		InputListener androidListener = new InputListener(pcIn, this);
		Thread androidListenerRunner = new Thread(androidListener);
		androidListenerRunner.start();
	}

	/**
	 * Method called by the InputListener when the socket connection closes.
	 * The next attempted action, upon failure, will pass an exception
	 * up the stack to be dealt with at the highest level (probably UI).
	 */
	public void onConnectionClose() {
		clientSocket = null;
		pcOut = null;
		pcIn = null;
	}

	/**
	 * Message to write a message to the android client.
	 * Used because we want all writes to be thread safe.
	 * We also guard against an inoperable PrintWriter safely.
	 * @param messageToSend - a String containing the message to be sent
	 * @throws SocketException - if the Socket connecting to Android has closed.
	 */
	public synchronized void sendMessage(String messageToSend) throws SocketException {

		/* Safeguard against closed connections:
		 * if pcOut == null, our Socket was closed.
		 * Throw a SocketException to be handled at the top level.
		 */
		try {
			// Make the message
			StringBuilder messageToSendBuilder = new StringBuilder();
			messageToSendBuilder.append("msgsend n").append
			(defaultPhoneNumber).append(" ").append(messageToSend);			
			pcOut.println(messageToSendBuilder.toString()); // Send the message

			// Make a new ConversationMessage holding the details of this message
			SimpleDateFormat formatter = new SimpleDateFormat(MobileModel.getTimeFormat());
			Date rightNow = new Date();
			ConversationMessage sentMessage = new ConversationMessage(true,
					formatter.format(rightNow), messageToSend);
			String partner = getName(defaultPhoneNumber);
			writeConversationMessage(partner, sentMessage); // Update our GUI

		} catch (NullPointerException pcOutMaybeNull) { // Most likely our PrintWriter was null
			if (pcOut == null) {
				// Throw a SocketException to deal with this issue at a higher level
				throw new SocketException("Socket closed");
			} else { // pcOut is not the cause
				throw pcOutMaybeNull;
			}
		}
	}

	/**
	 * Method to write a raw String to the PrintWriter.
	 * No processing is done on this message,
	 * and it is not considered part of a conversation.
	 * @param messageToSend - a String containing the message to be sent
	 * @throws SocketException - if the Socket connecting to Android has closed.
	 */
	public synchronized void sendRawMessage(String messageToSend) throws SocketException {
		try {
			// Safeguard against closed connections
			pcOut.println(messageToSend);
		} catch (NullPointerException pcOutMaybeNull) {
			if (pcOut == null) {
				// Throw a SocketException to deal with this issue at a higher level
				throw new SocketException("Socket closed");
			} else { // pcOut is not the cause
				throw pcOutMaybeNull;
			}
		}
	}

	/**
	 * Runnable that sets up our connection with Android.
	 * Contains a (public) exception variable that is set
	 * when an exception is thrown. The caller should
	 * check to see if this exception is null before proceeding.
	 */
	private class ConnectionSetupRunnable implements Runnable {
		public IOException socketException = null;

		@Override
		public void run() {
			try {
				/* Connect to the Android phone
				(which after port forwarding is on localhost) */
				clientSocket = new Socket("localhost", listenPORT);
				// Now, socket exists, get in reader and out writer
				pcIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				pcOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
			} catch (IOException e) {
				// Set the exception to be read by the caller
				socketException = e;
			}
		}
	};


	/**
	 * LOCATION RELATED METHODS
	 */
	
	
	/**
	 * Method to get the current location.
	 * @return a String of the currently known address
	 */
	public String getCurrentLocation() {
		return currentAddress;
	}
	
	/**
	 * Method to update the current best known location.
	 * @param newAddress - a String of the new best known address.
	 */
	public void updateCurrentLocation(String newAddress) {
		if (newAddress != null) {
			currentAddress = newAddress;
		}
	}
	
	/**
	 * CONVERSATION RELATED METHODS
	 */


	/**
	 * Method run in background to populate our Conversation list.
	 */
	private void setupConversations() {
		for (String name: phoneNames) {
			Conversation toAdd = new Conversation(name);
			currentConversations.add(toAdd);
		}
	}

	/**
	 * Getter for our conversations Set.
	 * @return a Set of our active conversations.
	 * UI uses the Documents of the conversations to draw updates.
	 */
	public Set<Conversation> getConversations() {
		return currentConversations;
	}

	/**
	 * A method to write a message into a conversation. If the conversation
	 * does not already exist, a new one is created and then written into.
	 * @param partner - a String name of the partner that we're talking to
	 * @param message - the String message to write
	 */
	public void writeConversationMessage(String partner, ConversationMessage message) {
		// Take care of invalid inputs
		if (partner == null || message == null) return;

		// Get conversation to write to
		Conversation convoToWriteTo = null;
		System.out.println("writeConversationMessage called with partner: " + partner + "and message:"
				+ message.getMessage());
		// If we already have the conversation, just get it
		if (currentConversations.contains(new Conversation(partner))) {
			// Since we have a small number of conversations, it's no problem to just loop through them
			for (Conversation convo: currentConversations) {
				if (convo.getPartner().equals(partner)) {
					convoToWriteTo = convo;
				}
			}
		}

		if (convoToWriteTo != null) {
			System.out.println("Calling addMessage on convoToWriteTo!");
			convoToWriteTo.addMessage(message); // Write the message, fires data change alert automatically
		}	
	}

	/**
	 * Method to set the phone number currently being used to send messages.
	 * @param numberToSet - the number to set
	 */
	public synchronized void setDefaultPhoneNumber(String numberToSet) {
		defaultPhoneNumber = numberToSet;
	}


	/**
	 * INTERNAL DATA GETTERS AND SETTERS
	 */

	
	/**
	 * Method to return a key array. As this array is mutable,
	 * we return a copy.
	 * @param requestedType - the relevant enum type
	 * @return A copy of our the requested array, a String[].
	 * Guaranteed to not be null. If the requestedType is null, we
	 * return an empty String[].
	 */
	public String[] getArray(RequestType requestedType) {
		String[] unknownArray = new String[0];

		if (requestedType == null) {
			return unknownArray;
		} else if (requestedType.equals(RequestType.MESSAGE)) {
			return (shortMessages != null)?
					shortMessages.clone() : unknownArray;
		} else {
			return (phoneNames != null)?
					phoneNames.clone() : unknownArray;
		}
	}


	/**
	 * Method that gets a button's text safely. Also used for drop-down lists.
	 * @param requestType - the enum value representing what button this is for
	 * @param numButton - the button number that we want
	 * @return the requested text, a String.
	 * If the requested text was not found, we return the empty string.
	 */
	public String getButtonText(RequestType requestedType, int numButton) {
		String toReturn = "";
		if (requestedType == null) return toReturn;

		if (requestedType.equals(RequestType.MESSAGE) &&
				numButton < shortMessages.length) {	
			toReturn = shortMessages[numButton];

		} else if (numButton < phoneNames.length) {
			toReturn = phoneNames[numButton];
		}

		return toReturn;
	}

	/**
	 * Method to return the value of a key. Works for both phone
	 * numbers and long messages of button text.
	 * @param requestedType - enum value of request desired
	 * @param name - key to check
	 * @return a String corresponding to the value found.
	 * If no value was found, or the requestedType was null,
	 * we return the key originally provided.
	 */
	public String getValue(RequestType requestedType, String key) {
		if (requestedType == null) return key;

		if (requestedType.equals(RequestType.MESSAGE)) {
			String longMessage = textMessageMap.get(key);
			return (longMessage != null)?
					longMessage : key;
		} else {
			String phoneNumber = phoneNumberMap.get(key);
			return (phoneNumber != null)?
					phoneNumber : key;
		}

	}

	/**
	 * Method to get the name attached to a phone number.
	 * @param phoneNumber - a String of the phone number
	 * @return a String of the name attached to this phone number.
	 * If multiple names are attached to the same phone number,
	 * the first one found is returned. If no name is found, we return null.
	 */
	public String getName(String phoneNumber) {
		synchronized (phoneNumberMap) { // Don't want results to be inaccurate
			for (Entry<String, String> entry: phoneNumberMap.entrySet()) {
				System.out.println("Key: " + entry.getKey());
				System.out.println("Value: " + entry.getValue());
				if (phoneNumber.equals(entry.getValue())) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	/**
	 * Populates the phone number map to be used in our GUI from a file.
	 * @modifies phoneNumbersMap - adds in numbers of phone numbers
	 * @throws MessageFormatException - if a number (or line) is formatted incorrectly
	 */
	private void populateMap(RequestType requestedType) throws MessageFormatException {
		if (requestedType == null) return;
		BufferedReader fileReader = null;

		try {
			if (requestedType.equals(RequestType.MESSAGE)) {
				fileReader = new BufferedReader(new FileReader("textMessages.txt"));
			} else {
				fileReader = new BufferedReader(new FileReader("phoneNumbers.txt"));
			}
			ArrayList<String> keysToAdd = new ArrayList<String>();

			for (String line = fileReader.readLine(); line != null; line = fileReader.readLine()) {

				/* Split line by short code separator
				In theory, parseArray[0] is the short code
				and parseArray[1] is the long version of a text */
				String[] parseArray = line.split("::"); // Separator used in the file
				if (parseArray.length != 2) { // Need name AND number
					fileReader.close();
					throw new MessageFormatException(String.format("Preloaded code: " + line + " with request type: " 
							+ requestedType.toString() + "%nis too short!"));
				}
				String key = parseArray[0];
				String value = parseArray[1];

				// Need to be able to strip off quotes!
				if (key.length() < 2) {
					fileReader.close();
					throw new MessageFormatException(String.format("Preloaded key: " + key + " with request type: " 
							+ requestedType.toString() + "%nis too short!"));
				} else if (value.length() < 2) {
					fileReader.close();
					throw new MessageFormatException(String.format("Preloaded value: " + value + " with request type: " 
							+ requestedType.toString() + "%nis too short!"));
				}

				// Strip off quotes
				key = key.substring(1, key.length() -2).trim();
				value = value.substring(2, value.length() -1).trim();
				if (requestedType.equals(RequestType.MESSAGE)) {
					textMessageMap.put(key, value);
				} else {
					phoneNumberMap.put(key, value);
				}
				keysToAdd.add(key);
			}	
			
			// Close the fileReader!
			String[] keysToAddArray = new String[keysToAdd.size()];
			keysToAdd.toArray(keysToAddArray);
			if (requestedType.equals(RequestType.MESSAGE)) {
				shortMessages = keysToAddArray;
			} else {
				phoneNames = keysToAddArray;
			}
			fileReader.close();

		} catch (IOException e) {
			throw new MessageFormatException(e.getLocalizedMessage());
		}
	}

	/**
	 * Method to get the format used for time.
	 * Used when printing the time in a MessageViewer.
	 * @return a String of the format
	 */
	public static String getTimeFormat() {
		return timeFormat;
	}

	/**
	 * Method to get the system's line separator.
	 * Needed for cross-platform compatibility.
	 * @return a String holding the separator
	 */
	public static String getLineSeparator() {
		return lineSeparator;
	}

	/**
	 * Method to get the text separator.
	 * @return a String holding the separator
	 */
	public static String getTextSeparator() {
		return textSeparator;
	}

	/**
	 * Use this method to run tests on the Model.
	 * @param args - not used currently
	 */
	public static void main(String[] args) {}

}