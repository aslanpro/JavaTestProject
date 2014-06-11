package utils;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiSelector;

public class VotingDialog {
	
	public static UiObject votingDialog(){
		return new UiObject(new UiSelector()
			.className("android.widget.FrameLayout").index(0));
	}
	
	public static UiObject voteText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").resourceId("android:id/title"));
	}
	
	public static UiObject question(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").resourceId("com.voting:id/vote_question_dilog"));
	}
	
	public static UiObject yesButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Yes"));
	}
	
	public static UiObject abstainButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Abstain"));
	}
	
	public static UiObject noButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("No"));
	}
	
	public static UiObject votingProgress(){
		return new UiObject(new UiSelector()
			.className("android.widget.ProgressBar").index(4));
	}
	
	public static UiObject waitingForResultsDialog(){
		return new UiObject(new UiSelector()
			.className("android.widget.FrameLayout").index(0));
	}
	
	public static UiObject waitingForResultsText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Vaiting for voting results"));
	}
	
	public static UiObject waitingProgress(){
		return new UiObject(new UiSelector()
			.className("android.widget.ProgressBar").resourceId("android:id/progress"));
	}

}