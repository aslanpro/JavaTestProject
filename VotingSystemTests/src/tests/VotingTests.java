package tests;

import java.io.IOException;

import junit.framework.Assert;
import utils.AppActions;
import utils.ConnectToServerActivity;
import utils.CreateVotingDialog;
import utils.RegisterKeyDialog;
import utils.VoteActivity;
import utils.VotingDialog;

import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

public class VotingTests extends UiAutomatorTestCase{
	
	public void test_VotingDialogOpens() throws IOException, InterruptedException, UiObjectNotFoundException{
		AppActions.clearVotingSystemApp();
		AppActions.findAndRunVotingSystemApp();
		RegisterKeyDialog.fillRegKeyDialog("testname", "testmail@test.com");
		ConnectToServerActivity.verifyHasSertificateState();
		CreateVotingDialog.fillCreateVotingDialog("Test question?");
		Thread.sleep(2000);
		VoteActivity.verifyQuestionCreated("Test question?", "testname", "testmail@test.com");
		
		VoteActivity.readyToStartButton().click();
		Thread.sleep(15000);
		Assert.assertTrue("Voting dialog was not displayed", VotingDialog.votingDialog().exists());
		Assert.assertTrue("Vote dialog title was not displayed", VotingDialog.voteText().exists());
		Assert.assertEquals("Incorrect voting question was displayed",
				"Test question?", VotingDialog.question().getText());
		Assert.assertTrue("Yes option was not displayed", VotingDialog.yesButton().exists());
		Assert.assertTrue("Abstain option was not displayed", VotingDialog.abstainButton().exists());
		Assert.assertTrue("No option was not displayed", VotingDialog.noButton().exists());
		Assert.assertTrue("Voting progress bar was not displayed", VotingDialog.votingProgress().exists());
		
		VotingDialog.yesButton().clickAndWaitForNewWindow();
		Assert.assertTrue("Waiting voting results dialog was not displayed", VotingDialog.waitingForResultsDialog().exists());
		Assert.assertTrue("Waiting for voting results text was not displayed", VotingDialog.waitingForResultsText().exists());
		Assert.assertTrue("Waiting progress bar was not displayed", VotingDialog.waitingProgress().exists());
	}
	
	}
