package backend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

/**
 * Class to test our Conversation class and its behavior.
 */
public class ConversationTest {

	@Test // Tests equality to null
	public void nullTest() {
		Conversation convo1 = new Conversation("I'm not null");
		assertFalse(convo1.equals(null));
	}
	
	@Test // Tests reflexive property of equals()
	public void reflexiveTest() { 
		Conversation convo1 = new Conversation("Just a Test");
		assertTrue(convo1.equals(convo1));
	}
	
	@Test // Tests symmetric property of equals() initially
	public void symmetricInitiallyTest() {
		Conversation convo1 = new Conversation("Artemis Fowl");
		Conversation convo2 = new Conversation("Artemis Fowl");
		assertTrue(convo1.equals(convo2));
		assertTrue(convo2.equals(convo1));
	}
	
	@Test // Tests symmetric property of equals() after adding a message
	public void symmetricAfterMsgTest() {
		Conversation convo1 = new Conversation("John Major");
		Conversation convo2 = new Conversation("John Major");
		convo1.addMessage(new ConversationMessage(true, "x", "Blah"));
		assertTrue(convo1.equals(convo2));
		assertTrue(convo2.equals(convo1));
	}
	
	@Test // Tests transitive property of equals() initially
	public void transitiveInitiallyTest() {
		Conversation convo1 = new Conversation("Just a Girl");
		Conversation convo2 = new Conversation("Just a Girl");
		Conversation convo3 = new Conversation("Just a Girl");
		assertTrue(convo1.equals(convo2));
		assertTrue(convo2.equals(convo3));
		assertTrue(convo1.equals(convo3));
	}
	
	@Test // Tests transitive property of equals() after adding a message
	public void transitiveAfterMsgTest() {
		Conversation convo1 = new Conversation("Steve Jobs");
		Conversation convo2 = new Conversation("Steve Jobs");
		Conversation convo3 = new Conversation("Steve Jobs");
		convo1.addMessage(new ConversationMessage(true, "xx", "How's it going?"));
		convo3.addMessage(new ConversationMessage(false, "yy", "I'm alright!"));
		assertTrue(convo1.equals(convo2));
		assertTrue(convo2.equals(convo3));
		assertTrue(convo1.equals(convo3));
	}
	
	@Test // Tests HashSet equality; which is important for actual use
	public void setContainmentTest() {
		Conversation convo1 = new Conversation("Tony Blair");
		Conversation convo2 = new Conversation("Tony Blair");
		convo1.addMessage(new ConversationMessage(true, "x", "Blah"));
		HashSet<Conversation> testSet = new HashSet<Conversation>();
		testSet.add(convo1);
		assertTrue(testSet.contains(convo1));
		assertTrue(testSet.contains(convo2));
	}
}
