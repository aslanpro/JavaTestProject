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
	
	public static UiObject yesResults(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").index(3));
	}
	
	public static UiObject noResutls(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").index(4));
	}
	
	public static UiObject votedPrinsipalsDropdown(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Voted principals:"));
	}
	
	public static UiObject abstainedPrincipalsDropDown(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Abstained principals:"));
	}
	
	public static UiObject firstPrincipal(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").resourceId("android:id/text1"));
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
	    verifyFirstPrincipal(principalName, principalEmail);
	    Assert.assertTrue("Exit confenence button was not displayed", 
	    		exitConferenceButton().exists());
	    Assert.assertTrue("Exit conference button was disabled", 
	    		exitConferenceButton().isEnabled());
	    Assert.assertTrue("Ready to start button was not displayed", 
	    		readyToStartButton().exists());
	    Assert.assertTrue("Ready to start button was disabled", 
	    		readyToStartButton().isEnabled());
	}
	
	public static void verifyFirstPrincipal(String name, String email) throws UiObjectNotFoundException{
		String expectedText = name + "; " + email + "; high trust";
		Assert.assertEquals("Principal data were incorrect", 
		    	expectedText, firstPrincipal().getText());
     }

}
