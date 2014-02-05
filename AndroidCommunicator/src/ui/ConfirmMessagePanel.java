package ui;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.TextArea;
import javax.swing.JButton;

import ui.JavaGUI.ProgramStatus;

import backend.MobileModel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.SocketException;

import net.miginfocom.swing.MigLayout;

public class ConfirmMessagePanel extends JPanel {

	// String constants
	private static final long serialVersionUID = 1L;

	private static final String confirmPrompt = "Are you sure you want to send the following message?";
	private static final String placeholderText = "No message available";
	private static final String noText = "Cancel";
	private static final String yesText = "Confirm";


	// Our panel's components
	private final JavaGUI mainGUI;
	private final JButton yesButton;
	private final JButton noButton;
	private final JLabel lblAreYouSure;
	private final JPanel panel;
	private final MobileModel backingModel;
	private final TextArea customMessage;

	/**
	 * Make our confirmation page.
	 * @param backModel - our backing Model instance
	 * @param backGUI - our GUI class instance
	 */
	public ConfirmMessagePanel(MobileModel backModel, JavaGUI backGUI) {
		// Save our data
		backingModel = backModel;
		mainGUI = backGUI;

		// Set our frame details
		setBounds(100, 100, 500, 350);
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new MigLayout("", "[839px]", "[22px][235px,grow][97px]"));

		// Set up our components
		lblAreYouSure = new JLabel(confirmPrompt);
		lblAreYouSure.setFont(new Font("Lucida Grande", Font.PLAIN, 18));
		add(lblAreYouSure, "cell 0 0,grow,aligny center,alignx center");

		customMessage = new TextArea();
		add(customMessage, "cell 0 1,grow");
		customMessage.setText(placeholderText);
		customMessage.setEditable(false);

		// Set up our button listeners and buttons
		ConfirmButtonListener buttonListener = new ConfirmButtonListener();

		// Set up our panel		
		panel = new JPanel();
		add(panel, "cell 0 2");
		panel.setLayout(new MigLayout("", "[420px][419px]", "[97px]"));
		yesButton = new JButton(yesText);
		yesButton.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		yesButton.addActionListener(buttonListener);
		yesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(yesButton, "cell 0 0,grow,aligny center,alignx center");

		noButton = new JButton(noText);
		noButton.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		noButton.addActionListener(buttonListener);
		noButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(noButton, "cell 1 0,grow,aligny center,alignx center");
		checkRep();
	}

	/**
	 * Representation invariant for ConfirmMessagePanel:
	 * Everything is not null, and
	 * customMessage's text is not empty.
	 */
	private void checkRep() {
		assert mainGUI != null;
		assert yesButton != null;
		assert noButton != null;
		assert lblAreYouSure != null;
		assert panel != null;
		assert backingModel != null;
		assert customMessage != null;
		assert !customMessage.getText().equals("");
	}

	/**
	 * Method to change our panel's content.
	 * @param is911 - a boolean indicating whether this is a 911 request
	 * @param textToSet - a String of the text to display
	 */
	public void setConfirmParameters(String textToSet) {
		lblAreYouSure.setText(confirmPrompt);
		customMessage.setText(textToSet); // setText is thread safe!
		checkRep();
	}

	/**
	 * Our Send/Don't send button listener.
	 * Either sends a message, calls 911,
	 * or goes back to our main screen.
	 */
	private class ConfirmButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {		
			// Get the button that was clicked
			JButton buttonClicked = (JButton) event.getSource();
			String buttonText = buttonClicked.getText();

			if (buttonText.equals(yesText)) { // Confirmed
				// Send the message
				String messageToSend = customMessage.getText();
				// We're not calling 911, just sending a message
				messageToSend = messageToSend.replaceAll("[\n\r]", " ");
				System.out.println("Yes was clicked! We're sending the following message: " + messageToSend);
				try {
					backingModel.sendMessage(messageToSend);
				} catch (SocketException e) {
					mainGUI.onSocketClose();
				}

				// Send us back to main
				mainGUI.showMain(ProgramStatus.MESSAGESENT);
			} else if (buttonText.equals(noText)) { // Cancelled
				mainGUI.showMain(null);
			} // If the button says something else, don't do anything
		}	
	}
}
