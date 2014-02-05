package ui;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;

import backend.MobileModel;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingConstants;

public class LocationPanel extends JPanel {

	// String constants
	private static final long serialVersionUID = 1L;
	private static final String defaultLocationText = "Requesting location...";
	private static final String goBackButtonText = "Go Back";
	private static final String locRevealText = "View Location";

	// Our panel's components
	private final JavaGUI mainGUI;
	private final JButton btnGoBack;
	private final JLabel lblLocationdata;
	private final MobileModel backingModel;
	
	// The string of our current location
	private String locationText = "";
	
	/**
	 * The constructor for our location viewer panel.
	 * @param backGUI - an instance of the backing model
	 */
	public LocationPanel(MobileModel backModel, JavaGUI backGUI) {
		// Save our data
		backingModel = backModel;
		mainGUI = backGUI;
		
		// Set our frame details
		setBounds(100, 100, 500, 350);
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new MigLayout("", "[839px]", "[22px][235px,grow][97px]"));
		
		JLabel lblViewLocation = new JLabel(locRevealText);
		lblViewLocation.setHorizontalAlignment(SwingConstants.CENTER);
		lblViewLocation.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		add(lblViewLocation, "cell 0 0,grow,alignx center,aligny center");
		
		lblLocationdata = new JLabel(defaultLocationText);
		lblLocationdata.setHorizontalAlignment(SwingConstants.CENTER);
		lblLocationdata.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		add(lblLocationdata, "cell 0 1");
		
		btnGoBack = new JButton(goBackButtonText);
		btnGoBack.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		add(btnGoBack, "cell 0 2,grow,alignx center,aligny center");
		btnGoBack.addActionListener(new GoBackListener());
	}
	
	/**
	 * A method to set the currently received location.
	 * @param locationToSet - a String of the location to set
	 */
	public void setLocation(String locationToSet) {
		locationText = locationToSet;
		lblLocationdata.setText(locationText); // setText is thread safe!
	}
	
	/**
	 * Method to get the newest location.
	 * Calls a worker thread to do its work.
	 */
	public void getNewLocation() {
		Thread locationWorker = new Thread(new Runnable() {
			@Override
			public void run() {
				// Get the current estimate of our location
				String newLocation = backingModel.getCurrentLocation();
				while (locationText.equals(newLocation)) {
					try {
						// Wait until an update comes in
						Thread.sleep(10000);
						newLocation = backingModel.getCurrentLocation();
					} catch (InterruptedException e) {
						setLocation(newLocation);
						return;
					}
				}
				setLocation(newLocation);
			}
			
		});
		locationWorker.start();
	}
	
	/**
	 * Listener for the back button.
	 * Goes back to the main screen.
	 */
	private class GoBackListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			mainGUI.showMain(null);
		}
	}
}
