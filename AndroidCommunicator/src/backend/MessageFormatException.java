package backend;

/**
 * Exception class to uniquely identify formatting problems
 * with either pre-loaded text messages or pre-loaded
 * names and phone numbers.
 */
public class MessageFormatException extends Exception {
	private static final long serialVersionUID = 1L;

	public MessageFormatException() {
		super();
	}
	
	public MessageFormatException(String msg) {
        super(msg);
    }
}