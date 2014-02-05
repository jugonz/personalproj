package backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputListener implements Runnable {
	/* Regex Patterns for parsing incoming messages
	   ID (which is always msgrecv, for now) is group 1
	   Phone number is group 4
	   Content of message is group 5
	   
	   For location data:
	   String of location is group 2 */
	private static Pattern contentP = Pattern.compile("n(\\+?)([0-9]+)[\\s]+(.*)");
	private static Pattern messageP = Pattern.compile("(msgrecv)[\\s](" + contentP.pattern() + ")");	
	private static Pattern locationP = Pattern.compile("(locdata)[\\s](.*)");

	// Components used to read/use messages
	private final BufferedReader in;
	private final MobileModel backingModel;

	/**
	 * A Runnable that when run, listens on the Android
	 * input stream and processing incoming messages.
	 * @param inReader - a BufferedReader connected to the Android device.
	 * @param handedInModel - an instance of our backend (MobileModel)
	 */
	public InputListener(BufferedReader inReader, MobileModel handedInModel) {
		in = inReader;
		backingModel = handedInModel;
		checkRep();
	}

	/**
	 * Representation invariant for InputListener:
	 * backingModel is not null
	 */
	private void checkRep() {
		assert backingModel != null;
	}
	
	/**
	 * Our run method, which listens on the input stream
	 * and passes received messages off to handleMessage()
	 * to process and act on.
	 * If an exception is thrown, we abort after setting
	 * our (publicly accessible) exception variable.
	 */
	@Override
	public void run() {
		/* If connection failed, we are passed null.
		 * Ignore it, because we're quitting anyway. */
		if (in == null) return;
		
		try {
			System.out.println("Waiting for input from Android!!!");
			// Listen in on input and block until we receive some
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				
				// Get the entire message, which may be separated by line breaks
				StringBuilder message = new StringBuilder();
				message.append(line);
				
				while (in.ready()) {
					// Replace line breaks with spaces
					message.append(" ").append(in.readLine());
				}
				
				System.out.println("Computer received text:" + message);
				handleMessage(message.toString()); // Hand it off
			}
			System.out.println("InputListener quit.");
		} catch (IOException e) {
			/* Do nothing, as an exception will cause us to run
			   onConnectionClose(), which will close the socket anyway */
		}
		
		// Clean up after ourselves after we're done executing
		backingModel.onConnectionClose();
	}

	/**
	 * Method to handle a passed in message.
	 * @param message a String version of the message
	 * Messages that do not fit our grammar are ignored.
	 */
	private void handleMessage(String message) {
		Matcher messageMatcher = messageP.matcher(message);
		Matcher locationDataMatcher = locationP.matcher(message);
		if (messageMatcher.matches()) {
			System.out.println("Our android input message matched!");
			// Get the phone number, and message content, and get the name
			String phoneNumber = messageMatcher.group(4);
			System.out.println("phoneNumber: " + phoneNumber);
			String messageContent = messageMatcher.group(5);
			System.out.println("messageContent: " + messageContent);
			String name = backingModel.getName(phoneNumber);
			System.out.println("name: " + name);

			// Now construct and write the message
			System.out.println("About to write a message to the screen, name is: " + name);
			SimpleDateFormat formatter = new SimpleDateFormat(MobileModel.getTimeFormat());
			Date rightNow = new Date();
			ConversationMessage msgToWrite = new ConversationMessage(false,
					formatter.format(rightNow), messageContent);
			backingModel.writeConversationMessage(name, msgToWrite);

		} else if (locationDataMatcher.matches()) {
			System.out.println("We have location data!");
			// Send the location data to the model
			String newAddress = locationDataMatcher.group(2);
			System.out.println("Our new address is: " + newAddress);
			backingModel.updateCurrentLocation(newAddress);
		}
	}
}