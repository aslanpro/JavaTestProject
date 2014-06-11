package tests;

import java.io.IOException;

import junit.framework.Assert;
import utils.AppActions;
import utils.ConnectToServerActivity;
import utils.RegisterKeyDialog;

import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

public class KeyRegistrationTests extends UiAutomatorTestCase{
	
	public void test_NoSertificateActivityLooksCorrectly() throws UiObjectNotFoundException, IOException, InterruptedException{
		AppActions.clearVotingSystemApp();
		AppActions.findAndRunVotingSystemApp();
		ConnectToServerActivity.verifyNoSertificateState();
     }
	
	 public void test_RegisterKeyDialogAppears() throws IOException, InterruptedException, UiObjectNotFoundException{
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
	 
	 public void test_KeyRegistrationWithInvalidDataFails() throws IOException, InterruptedException, UiObjectNotFoundException{
		 AppActions.clearVotingSystemApp();
		 AppActions.findAndRunVotingSystemApp();
		 RegisterKeyDialog.fillRegKeyDialog(" ", " ");
		 ConnectToServerActivity.verifyNoSertificateState();
		 RegisterKeyDialog.fillRegKeyDialog("test", " ");
		 ConnectToServerActivity.verifyNoSertificateState();
		 RegisterKeyDialog.fillRegKeyDialog("test", "test");
		 ConnectToServerActivity.verifyNoSertificateState();
		 RegisterKeyDialog.fillRegKeyDialog("test", "test@");
		 ConnectToServerActivity.verifyNoSertificateState();
		 RegisterKeyDialog.fillRegKeyDialog("test", "test.com");
		 ConnectToServerActivity.verifyNoSertificateState();
		 RegisterKeyDialog.fillRegKeyDialog("test", "test@test");
		 ConnectToServerActivity.verifyNoSertificateState();
      }
	 
	 public void test_KeyRegistrationIsSuccessfull() throws IOException, InterruptedException, UiObjectNotFoundException{
		 AppActions.clearVotingSystemApp();
		 AppActions.findAndRunVotingSystemApp();
		 RegisterKeyDialog.fillRegKeyDialog("testname", "testmail@test.com");
		 ConnectToServerActivity.verifyHasSertificateState();
	 }
	 
}
