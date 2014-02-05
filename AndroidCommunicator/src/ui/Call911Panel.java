package ui;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.swing.JPanel;

import backend.MobileModel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

import ui.JavaGUI.ProgramStatus;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.SocketException;
import java.awt.Component;
import javax.swing.Box;
import java.awt.Dimension;

public class Call911Panel extends JPanel {
	
	// Enum to tell the type of our call internally
	private enum CallType {
		POLICE, MEDICAL
	};
	
	// String constants
	private static final long serialVersionUID = 1L;
	private static final String call911MagicInput = "About to call 911...";
	private static final String call911MessageToSendMedical = "call n311";
	private static final String call911MessageToSendPolice = "call n311";
	private static final String call911Prompt = "Are you sure you want to call 311?";
	private static final String cancelBText = "Cancel";
	private static final String medicalBText = "Call Medical";
	private static final String medicalCallSoundPath = "/MemoMedical.wav";
	private static final String policeBText = "Call Police";
	private static final String policeCallSoundPath = "/MemoPolice.wav";

	// Our panel's components
	private final JavaGUI mainGUI;
	private final JButton btnCancel;
	private final JButton btnMedical;
	private final JButton btnPolice;
	private final JLabel lblCalling;
	private final JPanel panel;
	private final MobileModel backingModel;
	private Component rigidArea;
	
	
	/**
	 * Initializer for the panel used to call 911.
	 * @param backModel - an instance of our backing model.
	 * @param backGUI - an instance of our main GUI.
	 */
	public Call911Panel(MobileModel backModel, JavaGUI backGUI) {
		// Set our frame details
		setBounds(100, 100, 500, 350);
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new MigLayout("", "[grow][]", "[][][][grow]"));
		
		lblCalling = new JLabel(call911MagicInput);
		lblCalling.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		add(lblCalling, "cell 0 0");
		
		JLabel lblAreYouSure = new JLabel(call911Prompt);
		lblAreYouSure.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		add(lblAreYouSure, "cell 0 1");
		
		rigidArea = Box.createRigidArea(new Dimension(20, 20));
		add(rigidArea, "cell 0 2");
		
		panel = new JPanel();
		add(panel, "cell 0 3,grow");
		panel.setLayout(new MigLayout("", "[280px][280px][279px]", "[97px]"));
		
		EmergencyButtonListener buttonListener = new EmergencyButtonListener();
		btnMedical = new JButton(medicalBText);
		btnMedical.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		panel.add(btnMedical, "cell 0 0");
		btnMedical.addActionListener(buttonListener);
		
		btnPolice = new JButton(policeBText);
		btnPolice.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		panel.add(btnPolice, "cell 1 0");
		btnPolice.addActionListener(buttonListener);
		
		btnCancel = new JButton(cancelBText);
		btnCancel.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		panel.add(btnCancel, "cell 2 0");
		btnCancel.addActionListener(buttonListener);
		
		mainGUI = backGUI;
		backingModel = backModel;
	}
	
	/**
	 * Method to play a recording for a call.
	 * Assumes the recordings could be found.
	 * @param callType - a CallType enum value indicating
	 * the recording to be played.
	 */
	public void playCallRecording(CallType callType) {
		String filePath = "";
		switch(callType){
		case POLICE:
			filePath = policeCallSoundPath;
			break;
		case MEDICAL:
			filePath = medicalCallSoundPath;
			break;
		default:
			// Unreachable code
			return;
		}	
		Runnable soundRunnable = new soundPlayerRunnable(filePath);
		Thread soundPlayer = new Thread(soundRunnable);
		soundPlayer.start();
	}
	
	/**
	 * The private runnable that plays sound files.
	 * Designed to fail silently.
	 */
	private class soundPlayerRunnable implements Runnable {

		private final String filePath;
		
		public soundPlayerRunnable(String pathProvided) {
			filePath = pathProvided;
		}
		
		@Override
		public void run() {
			Clip recording;
			try {
				recording = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));
				recording.open(AudioSystem.getAudioInputStream(getClass().getResourceAsStream(filePath)));
				recording.start();
			} catch (Exception e) {
				// Fail silently
			}

		}
		
	}
	
	/**
	 * Button listener for our call emergency buttons.
	 */
	private class EmergencyButtonListener implements ActionListener {

		// Call the appropriate service
		@Override
		public void actionPerformed(ActionEvent event) {
			JButton sourceButton = (JButton) event.getSource();
			String buttonText = sourceButton.getText();
			if (buttonText.equals(medicalBText)) { // Are we calling 911?
				System.out.println("Calling medical!");
				try {
					backingModel.sendRawMessage(call911MessageToSendMedical);
					Thread.sleep(6000);
					playCallRecording(CallType.MEDICAL);
				} catch (SocketException e) {
					mainGUI.onSocketClose();
				} catch (InterruptedException e) {
					mainGUI.onSocketClose();
				}
				
				// Go back to main
				mainGUI.showMain(ProgramStatus.CALLING);
			} else if (buttonText.equals(policeBText)) {
				System.out.println("Calling police!");
				try {
					backingModel.sendRawMessage(call911MessageToSendPolice);
					Thread.sleep(6000);
					playCallRecording(CallType.POLICE);
				} catch (SocketException e) {
					mainGUI.onSocketClose();
				} catch (InterruptedException e) {
					mainGUI.onSocketClose();
				}
				// Go back to main
				mainGUI.showMain(ProgramStatus.CALLING);
			} else {
				// It's the cancel button
				mainGUI.showMain(null);
			}
			
		}
		
	}

}
