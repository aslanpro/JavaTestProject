package utils;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiSelector;

public class RegisterKeyDialog {
	
	public static UiObject regKeyDialog(){
		return new UiObject(new UiSelector()
			.className("android.widget.FrameLayout").index(0));
	}
	
	public static UiObject regKeyDialogTitle(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Reg Dialog"));
	}
	
	public static UiObject enterYourNameText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Enter your name:"));
	}
	
	public static UiObject nameInput(){
		return new UiObject(new UiSelector()
			.className("android.widget.EditText").index(1));
	}
	
	public static UiObject enterYourEmailText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Enter your e-mail:"));
	}
	
	public static UiObject emailInput(){
		return new UiObject(new UiSelector()
			.className("android.widget.EditText").index(3));
	}
	
	public static UiObject sendButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Send"));
	}
	
	public static void fillRegKeyDialog(String name, String email) throws UiObjectNotFoundException, InterruptedException{
		ConnectToServerActivity.connectToServerButton().clickAndWaitForNewWindow();
		String nameForInput = name + "\n";
		nameInput().setText(nameForInput);
		String emailForInput = email + "\n";
	    emailInput().setText(emailForInput);
	    sendButton().click();
	    Thread.sleep(2000);
	}

}
