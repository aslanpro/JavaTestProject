package utils;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

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

}
