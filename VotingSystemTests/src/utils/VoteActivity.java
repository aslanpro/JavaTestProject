package utils;

import junit.framework.Assert;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiSelector;

public class VoteActivity {
	
	public static UiObject voteText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Vote:"));
	}
	
	public static UiObject question(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").index(1));
	}
	
	public static UiObject principalListText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Principal list:"));
	}
	
	public static UiObject exitConferenceButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Exit conference"));
	}
	
	public static UiObject readyToStartButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Ready to start"));
	}
	
	public static UiObject resultsText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Results:"));
	}
	
	public static UiObject yesResult(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").resourceId("com.voting:id/yes_result"));
	}
	
	public static UiObject noResult(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").resourceId("com.voting:id/no_result"));
	}
	
	public static UiObject votedPrincipalsDropdown(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Voted principals:"));
	}
	
	public static UiObject abstainedPrincipalsDropdown(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Abstained principals:"));
	}
	
		public static UiObject okButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("OK"));
	}
	
	public static void verifyQuestionCreated(String question, String principalName, String principalEmail) throws UiObjectNotFoundException{
		Assert.assertTrue("Vote text was not displayed", voteText().exists());
		Assert.assertTrue("Quesiton box was not displayed", question().exists());
		Assert.assertEquals("Invalid question was displayed", question, question().getText());
	    Assert.assertTrue("Principal list text was not displayed", principalListText().exists());
	    verifyPrincipal(principalName, principalEmail);
	    Assert.assertTrue("Exit confenence button was not displayed", 
	    		exitConferenceButton().exists());
	    Assert.assertTrue("Exit conference button was disabled", 
	    		exitConferenceButton().isEnabled());
	    Assert.assertTrue("Ready to start button was not displayed", 
	    		readyToStartButton().exists());
	    Assert.assertTrue("Ready to start button was disabled", 
	    		readyToStartButton().isEnabled());
	}
	
	public static void verifyPrincipal(String name, String email) throws UiObjectNotFoundException{
		String expectedText = name + "; " + email + "; high trust";
		Assert.assertTrue("Principal data were incorrect", 
		    	new UiObject(new UiSelector()
				.className("android.widget.TextView").text(expectedText)).exists());
    }
	
    public static void verifyVoteResult(String question, int yes, int no, String name, String email) throws UiObjectNotFoundException{
    	Assert.assertTrue("Vote text was not displayed", voteText().exists());
		Assert.assertTrue("Quesiton box was not displayed", question().exists());
		Assert.assertEquals("Invalid question was displayed", question, question().getText());
	    Assert.assertTrue("Results text was not displayed", resultsText().exists());
	    Assert.assertEquals("Invalid yes result was displayed", "Yes - " + yes, yesResult().getText());
	    Assert.assertEquals("Invalid no result was displayed", "No - " + no, noResult().getText());
	    Assert.assertTrue("Voted principals dropdown was not displayed", votedPrincipalsDropdown().exists());
	    Assert.assertTrue("Abstained principals dropdown was not displayed", abstainedPrincipalsDropdown().exists());
	    verifyPrincipal(name, email);
    }

}
