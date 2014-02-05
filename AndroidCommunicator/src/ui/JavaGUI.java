package ui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import backend.MobileModel;

import java.awt.CardLayout;
import java.net.ConnectException;

public class JavaGUI {

	/**
	 * This enum controls the status message about our connection.
	 */
	public enum ProgramStatus {
		LISTENING, CONNECTED, DISCONNECTED, FAILED, MESSAGESENT, CALLING
	}

	// Fixed properties of our UI	
	private static final int screenWidth = 400;
	private static final int screenHeight = 400;
	private static final String mainPanelID = "MAINPANEL";
	private static final String confirmPanelID = "CONFIRMPANEL";
	private static final String emergencyPanelID = "EMERGENCYPANEL";
	private static final String locationPanelID = "LOCPANEL";
	private static final String programTitleText = "SMS +";
	private static final String errorMsgText = String.format("A program error occured" +
			" with the following message:%n");
	private static final String failedConnectionText = String.format("%nConnection failed. Remember, the Android"
			+ " phone must be%nconnected and started before starting " + programTitleText + "."
			+ "%nIf the phone is connected, ports may not have been properly forwarded.");
	private static final String onSocketCloseText = String.format("The connection to the Android phone has failed.%n"
			+ "Messages can no longer be sent or received.");

	// Our model
	private MobileModel backingModel;

	// Layout objects
	private Call911Panel emergencyPanel;
	private CardLayout backingLayout;
	private ConfirmMessagePanel confirmPanel;
	private JFrame mainFrame;
	private JFrame errorFrame;
	private JPanel cardPanel;
	private LocationPanel locationPanel;
	private MainPanel mainPanel;
	private ProgramStatus currentProgramStatus;


	/**
	 * Constructor for our Main GUI class. Initializes all panels and our backing layout.
	 * @throws Exception - if our Model failed to instantiate
	 */
	public JavaGUI() throws Exception {

		// Set up our model, and show an error dialog if it was unsuccessful
		try {
			backingModel = new MobileModel();
		} catch (Exception e) {
			showError(e.getLocalizedMessage(), true);
			throw e;
		}

		// Initiate layout and panels
		backingLayout = new CardLayout();
		cardPanel = new JPanel(backingLayout);

		mainFrame = new JFrame(programTitleText);
		mainPanel = new MainPanel(backingModel, this);
		confirmPanel = new ConfirmMessagePanel(backingModel, this);
		emergencyPanel = new Call911Panel(backingModel, this);
		locationPanel = new LocationPanel(backingModel, this);
		errorFrame = new JFrame(programTitleText);

		// Add our panels to our layout
		cardPanel.add(mainPanel, mainPanelID);
		cardPanel.add(confirmPanel, confirmPanelID);
		cardPanel.add(locationPanel, locationPanelID);
		cardPanel.add(emergencyPanel, emergencyPanelID);

		// Set up our main display
		mainFrame.getContentPane().add(cardPanel);
		mainFrame.setSize(screenWidth, screenHeight);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Allow our error frame to quit the program if it needs to
		errorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// We're not yet connected, set our status to listening for a connection
		currentProgramStatus = ProgramStatus.LISTENING;
		checkRep();
	}

	/**
	 * Representation invariant for JavaGUI:
	 * Everything is not null
	 */
	private void checkRep() {
		assert backingModel != null;
		assert backingLayout != null;
		assert cardPanel != null;
		assert mainFrame != null;
		assert confirmPanel != null;
		assert errorFrame != null;
		assert mainPanel != null;
		assert currentProgramStatus != null;
	}

	/**
	 * Method to quit the program.
	 * Called if unrecoverable errors occur, but
	 * after a dialog box showing the error is presented.
	 * Needs to be run on the Swing EDT, or else may not quit properly.
	 */
	private void quit() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Dispose of all frames, Java does the rest
				if (mainFrame != null) {
					mainFrame.dispose();
				}
				if (errorFrame != null) {
					errorFrame.dispose();
				}
			}
		});
	}

	/**
	 * Method called by a panel when the underlying Socket
	 * connection has closed (due to failure or normal
	 * functionality). Closes the UI.
	 * Needs to be run on the Swing EDT, or else may not quit properly.
	 */
	public void onSocketClose() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				showMain(ProgramStatus.DISCONNECTED);
				showError(onSocketCloseText, true);		
			}

		});
	}

	/**
	 * Method to easily show a pop-up error message. Used if
	 * an exception is thrown in our model constructor or if
	 * there is an exception thrown during connection.
	 * The Option Pane shows the dialog on the Swing EDT.
	 * @param errorMsg - the String error message to be thrown
	 * @param toQuit - a boolean indicating whether to quit
	 * or not after the error message is displayed
	 */
	public void showError(String errorMsg, boolean toQuit) {
		class messageRunnable implements Runnable {
			private final boolean quitParameter;
			private final String messageToShow;

			public messageRunnable(String passedInMsg, boolean toQuit) {
				messageToShow = passedInMsg;
				quitParameter = toQuit;
			}

			@Override
			public void run() {
				JOptionPane.showMessageDialog(errorFrame, errorMsgText + messageToShow,
						programTitleText, JOptionPane.ERROR_MESSAGE);
				if (quitParameter) {
					quit();
				}	
			}

		}
		SwingUtilities.invokeLater(new messageRunnable(errorMsg, toQuit));
	}

	/**
	 * Method to show our main screen. Called by other UI components
	 * after they return to bring the user back.
	 * If an error occurs during the creation of our Model,
	 * we do nothing here; the program shows a dialog box and quits.
	 * The actual showing code must be run on the Swing EDT.
	 * @param newMainStatus - the ConnectionStatus to show on the screen.
	 * If null, we show the last saved ConnectionStatus.
	 */
	public void showMain(ProgramStatus newMainStatus) {
		if (backingLayout == null) return;	
		if (newMainStatus != null) {
			mainPanel.setConnectionStatus(newMainStatus);
		} else {
			// Show what we can if we have no argument
			mainPanel.setConnectionStatus(currentProgramStatus);
		}	
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				backingLayout.show(cardPanel, mainPanelID);
				mainFrame.pack();
				mainFrame.setVisible(true);
			}		
		});
	}

	/**
	 * Method to show our location screen.
	 * The actual showing code must be run on the Swing EDT.
	 */
	public void showLocation() {
		if (backingLayout == null) return;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				backingLayout.show(cardPanel, locationPanelID);
				locationPanel.getNewLocation();
			}
		});
	}
	
	/**
	 * Method to set up a connection with our Android phone.
	 * Sets the connection status to the appropriate value
	 * even if an error occurs.
	 * Upon an error, shows an error dialog box as well.
	 */
	public void startConnection() {
		try {
			backingModel.setupConnection();
			// Set the connection status to Connected, if success
			currentProgramStatus = ProgramStatus.CONNECTED;
			mainPanel.setConnectionStatus(ProgramStatus.CONNECTED);
		} catch (ConnectException e) {
			// We failed, show it in the status bar
			currentProgramStatus = ProgramStatus.FAILED;
			mainPanel.setConnectionStatus(ProgramStatus.FAILED);
			// We have a better error message for this.
			// It is either port forwarding issues, or
			// a complete lack of a connection.
			showError(failedConnectionText, true);
		} catch (Exception e) {
			// We failed, show it in the status bar
			currentProgramStatus = ProgramStatus.FAILED;
			mainPanel.setConnectionStatus(ProgramStatus.FAILED);
			// And show an error dialog
			showError(e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Method to show the Confirmation screen in our UI.
	 * The Swing EDT needs to be the one to actually show the layout.
	 * @param is911 - a boolean indicating whether 911 is about to be called
	 * @param textToConfirm - a String indicating the message to display
	 */
	public void showConfirmScreen(boolean is911, String textToConfirm) {
		confirmPanel.setConfirmParameters(textToConfirm);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				backingLayout.show(cardPanel, confirmPanelID);				
			}
		});
	}
	
	/**
	 * Method to show the screen to call 911.
	 * The Swing EDT needs to be the one to actually show the layout.
	 */
	public void show911Screen() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				backingLayout.show(cardPanel, emergencyPanelID);
			}
		});
	}

	/**
	 * Method to hide our GUI. Must be run on the Swing EDT.
	 */
	public void hideGUI() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mainFrame.setVisible(false);	
			}		
		});
	}

	/**
	 * The main thread of our program.
	 * Ports need to be forwarded via ADB first.
	 * Starts the GUI and makes it visible, and then sits.
	 * @param args - ignored
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// If we throw an exception, just stop execution.
				try {
					JavaGUI UI = new JavaGUI();
					UI.showMain(null);
					UI.startConnection();
				} catch (Exception e) {}
			}
		});
	}
}