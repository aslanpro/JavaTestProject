package utils;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

public class ConnectToServerActivity {
	
	public static UiObject connectToServerButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Connect to the server"));
	}
	
	public static UiObject noCertificateText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("You don't have a certificate. You must register your key"));
	}
	
	public static UiObject youHaveACertificateText(){
		return new UiObject(new UiSelector()
			.className("android.widget.TextView").text("Congratulations! You have a certificate!"));
	}
	
	public static UiObject refreshButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Refresh"));
	}
	
	public static UiObject createButton(){
		return new UiObject(new UiSelector()
			.className("android.widget.Button").text("Create"));
	}
	
}
