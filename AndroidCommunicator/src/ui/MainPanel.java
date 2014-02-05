package ui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import backend.Conversation;
import backend.MobileModel;
import backend.MobileModel.RequestType;

import javax.swing.JTabbedPane;
import javax.swing.JComboBox;

import ui.JavaGUI.ProgramStatus;

public class MainPanel extends JPanel {

	// String constants, usually messages used in our program
	private static final long serialVersionUID = 1L;
	private static final String call911Text = "Call 311";
	private static final String connectionLostText = "Connection to Android lost! Reconnecting...";
	private static final String currentlyConnectedText = "Connected to Android.";
	private static final String disconnectedText = "Connection terminated.";
	private static final String insertLocationText = "Here is my location:";
	private static final String messageSentText = "Message successfully sent.";
	private static final String newMessageText = "Send a New Message";
	private static final String receiveMessageText = "View Conversations";
	private static final String receiveMessageWelcomeText = "Welcome to Emergency!";
	private static final String sendCustomMsgBText = "Send Custom Message";
	private static final String setRecipientText = "Set Recipient";
	private static final String viewLocationText = "View Location";
	private static final String waitingForConnectionText = "Attempting to connect to Android...";

	// Our main screen's components
	private final JavaGUI mainGUI;
	private final JComboBox recipientMenu;
	private final JButton btnViewLocation;
	private final JButton E911button;
	private final JButton messageB0;
	private final JButton messageB1;
	private final JButton messageB2;
	private final JButton messageB3;
	private final JButton messageB4;
	private final JButton messageB5;
	private final JTabbedPane tabbedPane;
	private final JButton sendCustomMsgButton;
	private final JLabel lblConnectionStatus;
	private final JLabel lblNewMessage;
	private final JLabel lblReceiveMessages;
	private final JLabel lblSetDefaultRecipient;
	private final JScrollPane scrollPane;
	private final JTextArea customMessage;
	private final Map<String, MessageViewer> convoTabs;
	private final MessageViewer messagesViewer;
	private final MobileModel backingModel;

	/**
	 * Our main panel's constructor.
	 * @param backModel - our backing Model instance
	 * @param backGUI - our GUI class instance
	 */
	public MainPanel(MobileModel backModel, JavaGUI backGUI) {
		// Set our components
		backingModel = backModel;
		mainGUI = backGUI;
		convoTabs = new HashMap<String,MessageViewer>();
		Set<Conversation> convoSet = backingModel.getConversations();

		// Set our layout
		setLayout(new MigLayout("", "[350px][19px][210px][210px]", "[][16px][57px][57px][57px][10px][97px][57px][57px][][][29px][97px][][16px]"));

		// Add the tabs for our conversation partners
		for (Conversation partner : convoSet) {
			convoTabs.put(partner.getPartner(), new MessageViewer(partner.getPartner(), partner.getDocument()));
		}

		/* Set up the message buttons.
		/* The message buttons share a common listener, which gets the long
		version of a message and displays a confirmation screen with it before sending. */
		TextMessageButtonListener messageListener = new TextMessageButtonListener();
		messageB1 = new JButton(backingModel.getButtonText(RequestType.MESSAGE, 1));	
		messageB1.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		messageB1.addActionListener(messageListener);
		messageB3 = new JButton(backingModel.getButtonText(RequestType.MESSAGE, 3));
		messageB3.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		messageB3.addActionListener(messageListener);
		messageB5 = new JButton(backingModel.getButtonText(RequestType.MESSAGE, 5));
		messageB5.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		messageB5.addActionListener(messageListener);
		messageB0 = new JButton(backingModel.getButtonText(RequestType.MESSAGE, 0));	
		messageB0.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		messageB0.addActionListener(messageListener);
		messageB2 = new JButton(backingModel.getButtonText(RequestType.MESSAGE, 2));	
		messageB2.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		messageB2.addActionListener(messageListener);
		messageB4 = new JButton(backingModel.getButtonText(RequestType.MESSAGE, 4));
		messageB4.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		messageB4.addActionListener(messageListener);
		add(messageB2, "cell 2 3,alignx center,aligny center,grow");
		add(messageB0, "cell 2 2,alignx center,aligny center,grow");
		add(messageB4, "cell 2 4,alignx center,aligny center,grow");
		add(messageB5, "cell 3 4,alignx center,aligny center,grow");
		add(messageB3, "cell 3 3,alignx center,aligny center,grow");
		add(messageB1, "cell 3 2,alignx center,aligny center,grow");

		btnViewLocation = new JButton(viewLocationText);
		btnViewLocation.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		add(btnViewLocation, "cell 0 12,alignx left,grow");
		btnViewLocation.addActionListener(new LocationButtonListener());

		lblConnectionStatus = new JLabel(waitingForConnectionText);
		lblConnectionStatus.setFont(new Font("Lucida Grande", Font.PLAIN, 15));
		add(lblConnectionStatus, "cell 0 14 2 1,alignx left,aligny top");

		lblReceiveMessages = new JLabel(receiveMessageText);
		lblReceiveMessages.setHorizontalAlignment(SwingConstants.CENTER);
		lblReceiveMessages.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		add(lblReceiveMessages, "cell 0 0 2 1,alignx center,aligny center,grow");

		lblNewMessage = new JLabel(newMessageText);
		lblNewMessage.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		lblNewMessage.setHorizontalAlignment(SwingConstants.CENTER);
		add(lblNewMessage, "cell 2 0 2 1,alignx center,aligny center,grow");

		// Set up our conversation window class
		messagesViewer = new MessageViewer();
		messagesViewer.setConversationText(receiveMessageWelcomeText);

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		//tabbedPane.addTab("Welcome", messagesViewer);
		for (Entry<String, MessageViewer> tabToAdd : convoTabs.entrySet()) {
			tabbedPane.addTab(tabToAdd.getKey(), tabToAdd.getValue());
		}
		add(tabbedPane, "cell 0 1 2 11,grow");

		sendCustomMsgButton = new JButton(sendCustomMsgBText);	
		sendCustomMsgButton.setVerticalAlignment(SwingConstants.CENTER);
		sendCustomMsgButton.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		sendCustomMsgButton.addActionListener(messageListener);

		customMessage = new JTextArea();
		customMessage.setWrapStyleWord(true);
		customMessage.setLineWrap(true);
		scrollPane = new JScrollPane(customMessage);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, "cell 2 6 2 1,grow");
		add(sendCustomMsgButton, "cell 2 7 2 1,alignx center,aligny center,grow");

		lblSetDefaultRecipient = new JLabel(setRecipientText);
		lblSetDefaultRecipient.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		lblSetDefaultRecipient.setHorizontalAlignment(SwingConstants.CENTER);
		add(lblSetDefaultRecipient, "cell 2 8,alignx left");

		recipientMenu = new JComboBox();
		for (int i = 0; backingModel.getButtonText(RequestType.PHONENUMBER, i) != ""; i++) {
			recipientMenu.addItem(backingModel.getButtonText(RequestType.PHONENUMBER, i));
		}

		try { 
			// Set the menu to the first position
			recipientMenu.setSelectedIndex(0);

			// Now, actually set the default phone number to the first position
			String nameClicked = (String) recipientMenu.getSelectedItem();
			String phoneNumber = backingModel.getValue(RequestType.PHONENUMBER, nameClicked);
			backingModel.setDefaultPhoneNumber(phoneNumber);	
		} catch (IllegalArgumentException e) {
			/* Thrown if we have no items in our menu.
			   In this case, set the menu to nothing. */
			recipientMenu.setSelectedIndex(-1);
		}
		recipientMenu.addActionListener(new PulldownMenuListener());
		add(recipientMenu, "cell 3 8,grow");

		E911button = new JButton(call911Text);
		E911button.setFont(new Font("Lucida Grande", Font.PLAIN, 24));
		E911button.addActionListener(new E911ButtonListener());
		add(E911button, "cell 2 10 3 3,alignx left,grow");
		checkRep();
	}

	/**
	 * Representation invariant for MainPanel:
	 * Everything is not null
	 */
	private void checkRep() {
		assert mainGUI != null;
		assert recipientMenu != null;
		assert E911button != null;
		assert messageB0 != null;
		assert messageB1 != null;
		assert messageB2 != null;
		assert messageB3 != null;
		assert messageB4 != null;
		assert messageB5 != null;
		assert tabbedPane != null;
		assert sendCustomMsgButton != null;
		assert lblConnectionStatus != null;
		assert lblNewMessage != null;
		assert lblReceiveMessages != null;
		assert lblSetDefaultRecipient != null;
		assert scrollPane != null;
		assert customMessage != null;
		assert convoTabs != null;
		assert messagesViewer != null;
		assert backingModel != null;
	}

	/**
	 * Method to set the current connection status in our main view.
	 * @param newStatus - the ProgramStatus to set.
	 * If null, this method does nothing.
	 */
	public void setConnectionStatus(ProgramStatus newStatus) {
		if (newStatus == null) return;

		switch(newStatus) {
		case LISTENING:
			lblConnectionStatus.setText(waitingForConnectionText);
			break;
		case CONNECTED:
			lblConnectionStatus.setText(currentlyConnectedText);
			break;
		case FAILED:
			lblConnectionStatus.setText(connectionLostText);
			break;
		case DISCONNECTED:
			lblConnectionStatus.setText(disconnectedText);
			break;
		case MESSAGESENT:
			lblConnectionStatus.setText(messageSentText);
			break;
		default: // Impossible to reach
			lblConnectionStatus.setText(waitingForConnectionText);
			break;
		}
	}

	/**
	 * Listener to respond to changes in the respondent menu.
	 */
	private class PulldownMenuListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			JComboBox dropDownList = (JComboBox) event.getSource();
			String nameClicked = (String) dropDownList.getSelectedItem();
			String phoneNumber = backingModel.getValue(RequestType.PHONENUMBER, nameClicked);
			backingModel.setDefaultPhoneNumber(phoneNumber);
			// Set the currently in-view conversation tab to be our newly selected name
			MessageViewer tabToSet = convoTabs.get(nameClicked);
			if (tabToSet != null) {
				tabbedPane.setSelectedComponent(tabToSet);
			}

		}

	}

	/**
	 * Listener for the short message buttons.
	 */
	private class TextMessageButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent event) {
			// Get the button that was clicked
			JButton buttonClicked = (JButton) event.getSource();
			String shortText = buttonClicked.getText();
			String longText = backingModel.getValue(RequestType.MESSAGE, shortText);

			if (shortText.equals(sendCustomMsgBText)) {
				// Get text from text box
				longText = customMessage.getText();
			}
			if (longText.equals(insertLocationText)) {
				StringBuilder newLongText = new StringBuilder();
				newLongText.append(longText).append(" ").append(backingModel.getCurrentLocation());
				longText = newLongText.toString();
			}
			// Now need to load the confirmation screen.
			mainGUI.showConfirmScreen(false, longText);
		}	
	}

	/**
	 * Listener for the view location button.
	 */
	private class LocationButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// Go to the location panel
			mainGUI.showLocation();
		}	
	}

	/**
	 * Listener for the call 911 button.
	 */
	private class E911ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent event) {
			// No parameters needed
			mainGUI.show911Screen();
		}
	}
}