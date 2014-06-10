package utils;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiSelector;

public class CreateVotingDialog {
	
	public static UiObject createVotingDialog(){
		return new UiObject(new UiSelector()
			.className("android.widget.FrameLayout").index(0));
	}
	
	public static UiObject creatingVotingText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Creating voting"));
	}
	
	public static UiObject typeQuestionText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Type question of your voting:"));
	}
	
	public static UiObject questionInput(){
		return new UiObject(new UiSelector()
			.className("android.widget.EditText").index(1));
	}
	
	public static UiObject sendButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Send"));
	}
	
	public static void fillCreateVotingDialog(String question) throws UiObjectNotFoundException, InterruptedException{
		ConnectToServerActivity.createButton().clickAndWaitForNewWindow();
		String questionForInput = question + "\n";
		questionInput().setText(questionForInput);
		sendButton().click();
		Thread.sleep(2000);
	}
	
}
