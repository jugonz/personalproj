package backend;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Class to store the data related to a conversation.
 */
public class Conversation {

	// Data stored in a conversation
	private final PlainDocument textAreaModel;
	private final String partner;
	
	// Line and text separators
	private static final String lineSep = MobileModel.getLineSeparator();
	private static final String textSep = MobileModel.getTextSeparator();

	/* Used to detect if a given message is the first
	 * to be inserted in our Conversation
	 */
	private boolean firstMessage = true;

	/* Used to tell if we (the PC) were the origin
	 * of the last message in the conversation, for
	 * better UI handling (don't put line separators
	 * when the sender hasn't changed)
	 */
	private boolean pcOriginOfLastMessage;
	
	/**
	 * Constructor for a Conversation.
	 * @param nameOfPartner - the String name of the partner
	 */
	public Conversation(String nameOfPartner) {
		textAreaModel = new PlainDocument();
		partner = nameOfPartner;
		checkRep();
	}

	/**
	 * Representation invariant for Conversation:
	 * textAreaModel and partner are both not null
	 * Only needs to be run at creation,
	 * as all involved fields are final
	 */
	private void checkRep() {
		assert textAreaModel != null;
		assert partner != null;
	}
	
	/**
	 * Method to add a message to our Conversation.
	 * @param msgToAdd - a ConversationMessage containing
	 * the message's details. If null, we do nothing.
	 */
	public void addMessage(ConversationMessage msgToAdd) {
		if (msgToAdd == null) return; // Fulfill our specification
		
		// Get the data from our new message
		boolean isSentMessage = msgToAdd.isSentMessage();
		String messageContent = msgToAdd.getMessage();
		String timeStamp = msgToAdd.getTimeStamp();
		
		/* Get the current position of text in our Document
		   and build the message */
		int insertPosition = textAreaModel.getLength();
		StringBuilder convoTextBuilder = new StringBuilder();
		
		// Remember to remove the placeholder text
		if (firstMessage) { 
			try {
				textAreaModel.remove(0, insertPosition);
				insertPosition = 0;
				pcOriginOfLastMessage = isSentMessage;
				// Append the name of the person who sent the message
				if (isSentMessage) {
					convoTextBuilder.append("Me:     ");
				} else {
					convoTextBuilder.append(partner).append(":     ");
				}
			} catch (BadLocationException e) {
				/* If our insert position is wrong, we can't do much about it.
			 	   This should really be a runtime exception! */
				throw new RuntimeException(e);
			}
		}
		
		// If we're not the first line, we need to add line and message separators
		if (!firstMessage) {		
			// Separate our message
			convoTextBuilder.append(lineSep);
			
			// This message is from a different person. Add a text separator
			if (pcOriginOfLastMessage != isSentMessage) {
				convoTextBuilder.append(textSep).append(lineSep);
				
				// Next, append the name of the person who sent the message
				if (isSentMessage) {
					convoTextBuilder.append("Me:     ");
				} else {
					convoTextBuilder.append(partner).append(":     ");
				}
				
				pcOriginOfLastMessage = isSentMessage;
			}
		}

		// Append the actual message
		convoTextBuilder.append(messageContent);
		
		// Now, add the time stamp
		convoTextBuilder.append("   ").append(timeStamp);
		
		// Get the actual message and send it
		String convoText = convoTextBuilder.toString();
		try {
			textAreaModel.insertString(insertPosition, convoText, null);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
		
		// Make sure to remember that we've been run
		firstMessage = false;
	}

	/**
	 * Method to get our backing Document model.
	 * @return a PlainDocument holding the actually displayed
	 * (formatted text) content of our Conversation.
	 */
	public PlainDocument getDocument() {
		return textAreaModel;
	}

	/**
	 * Gets the partner of this Conversation.
	 * @return a String of that partner's name
	 */
	public String getPartner() {
		return partner;
	}

	/**
	 * Equality (and as such, hashCode()) of our Conversation
	 * is only related to the partner's name, because we can
	 * only have one conversation with a given partner.
	 * This helps when checking a Set of Conversations to see
	 * if we're already talking to someone.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((partner == null) ? 0 : partner.hashCode());
		return result;
	}

	/**
	 * Equality of our Conversation is only related
	 * to the partner's name, because we can only
	 * have one conversation with a given partner.
	 * This helps when checking a Set of Conversations
	 * to see if we're already talking to someone.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Conversation other = (Conversation) obj;
		if (partner == null) {
			if (other.partner != null)
				return false;
		} else if (!partner.equals(other.partner))
			return false;
		return true;
	}

}