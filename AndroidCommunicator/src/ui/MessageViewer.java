package ui;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import javax.swing.JScrollPane;
import javax.swing.text.Document;

import java.awt.Font;

public class MessageViewer extends JPanel {

	// Panel components
	private static final long serialVersionUID = 1L;
	private final JScrollPane scrollPane;
	private final JTextArea conversationArea;
	private final String conversantName;

	/**
	 * No-argument constructor useful for testing.
	 */
	public MessageViewer() {
		this("No Name", null);

	}

	/**
	 * Constructor for the conversation-displaying panel
	 * @param partnerName - a String of the conversation's partner's name
	 * @param textViewerModel - the Document that holds the conversation
	 */
	public MessageViewer(String partnerName, Document textViewerModel) {
		// Set our layout
		setLayout(new MigLayout("", "[grow]", "[grow]"));
		
		// Create our textArea
		if (textViewerModel != null) {
			conversationArea = new JTextArea(textViewerModel);
		} else {
			conversationArea = new JTextArea();
		}
		
		// Set up our scroll pane
		scrollPane = new JScrollPane(conversationArea);
		add(scrollPane, "cell 0 0,grow");
		
		// Set properties of our text area
		conversationArea.setEditable(false);
		conversationArea.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		conversationArea.setWrapStyleWord(true);
		conversationArea.setLineWrap(true);
		conversationArea.setText("No currently received texts from " + partnerName);
		conversantName = partnerName;
		checkRep();
	}
	
	/**
	 * Representation invariant for MessageViewer:
	 * Everything is not null
	 */
	private void checkRep() {
		assert scrollPane != null;
		assert conversationArea != null;
		assert conversantName != null;
	}
	
	/**
	 * Method to set the text area's text.
	 * @param textToSet - a String of the text to set
	 */
	public void setConversationText(String textToSet) {
		conversationArea.setText(textToSet); // setText is thread safe!
	}
	
}