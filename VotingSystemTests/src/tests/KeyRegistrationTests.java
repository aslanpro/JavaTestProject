package tests;


import java.io.IOException;

import junit.framework.Assert;
import utils.AppActions;
import utils.ConnectToServerActivity;
import utils.RegisterKeyDialog;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

public class KeyRegistrationTests extends UiAutomatorTestCase{
	
	public void test_noSertificateActivityLooksCorrectly() throws UiObjectNotFoundException, IOException, InterruptedException{
		AppActions.clearVotingSystemApp();
		AppActions.findAndRunVotingSystemApp();
		Assert.assertTrue("You don't have a certificate text wasn't displayed", 
				 ConnectToServerActivity.noCertificateText().exists());
		Assert.assertTrue("Connect to the server button was not clickable", 
				 ConnectToServerActivity.connectToServerButton().isClickable());
		Assert.assertTrue("Create button was not displayed", 
				 ConnectToServerActivity.createButton().exists());
		Assert.assertTrue("Refresh button was not displayed", 
				 ConnectToServerActivity.refreshButton().exists());
		Assert.assertFalse("Create button was clickable", 
				 ConnectToServerActivity.createButton().isEnabled());
		Assert.assertFalse("Refresh button was clicable", 
				 ConnectToServerActivity.refreshButton().isEnabled());
     }
	
	 public void test_registerKeyDialogAppears() throws IOException, InterruptedException, UiObjectNotFoundException{
		 AppActions.clearVotingSystemApp();
		 AppActions.findAndRunVotingSystemApp();
		 ConnectToServerActivity.connectToServerButton().clickAndWaitForNewWindow();
		 Assert.assertTrue("Register key dialog wasn't displayed", 
				 RegisterKeyDialog.regKeyDialog().exists());
		 Assert.assertTrue("Register key dialog title wasn't displayed", 
				 RegisterKeyDialog.regKeyDialogTitle().exists());
		 Assert.assertTrue("Enter your name text wasn't displayed", 
				 RegisterKeyDialog.enterYourNameText().exists());
		 Assert.assertTrue("Name input wasn't displayed", 
				 RegisterKeyDialog.nameInput().exists());
		 Assert.assertTrue("Enter your e-mail text wasn't displayed", 
				 RegisterKeyDialog.enterYourEmailText().exists());
		 Assert.assertTrue("Email input wasn't displayed", 
				 RegisterKeyDialog.emailInput().exists());
		 Assert.assertTrue("Send button wasn't displayed", 
				 RegisterKeyDialog.sendButton().exists());
		 Assert.assertFalse("Send button was enabled", 
				 RegisterKeyDialog.sendButton().isEnabled());
	 }
	
	

}
