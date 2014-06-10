package tests;

import java.io.IOException;

import junit.framework.Assert;
import utils.AppActions;
import utils.ConnectToServerActivity;
import utils.CreateVotingDialog;
import utils.RegisterKeyDialog;
import utils.VoteActivity;

import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

public class VotingCreationTests extends UiAutomatorTestCase{
	
	public void test_CreateVotingDialogOpens() throws IOException, InterruptedException, UiObjectNotFoundException{
		AppActions.clearVotingSystemApp();
		AppActions.findAndRunVotingSystemApp();
		RegisterKeyDialog.fillRegKeyDialog("tesname", "testmail@test.com");
		ConnectToServerActivity.verifyHasSertificateState();
		ConnectToServerActivity.createButton().clickAndWaitForNewWindow();
		Assert.assertTrue("Create voting dialog was not displayed", 
				CreateVotingDialog.createVotingDialog().exists());
		Assert.assertTrue("Creating voting text was not displayed", 
				CreateVotingDialog.creatingVotingText().exists());
		Assert.assertTrue("Type question text was not displayed", 
				CreateVotingDialog.typeQuestionText().exists());
		Assert.assertTrue("Question input was not displayed",
				CreateVotingDialog.questionInput().exists());
		Assert.assertTrue("Send button was not displayed", 
				CreateVotingDialog.sendButton().exists());
		Assert.assertFalse("Send button was enabled", 
				CreateVotingDialog.sendButton().isEnabled());
    }
	
	public void test_QuestionCreatedSuccessfully() throws IOException, InterruptedException, UiObjectNotFoundException{
		AppActions.clearVotingSystemApp();
		AppActions.findAndRunVotingSystemApp();
		RegisterKeyDialog.fillRegKeyDialog("testname", "testmail@test.com");
		ConnectToServerActivity.verifyHasSertificateState();
		CreateVotingDialog.fillCreateVotingDialog("Test question?");
		Thread.sleep(2000);
		VoteActivity.verifyQuestionCreated("Test question?", "testname", "testmail@test.com");
	}

}
