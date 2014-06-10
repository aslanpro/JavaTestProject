package utils;

import junit.framework.Assert;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiSelector;

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
	
	public static void verifyNoSertificateState() throws UiObjectNotFoundException{
		Assert.assertTrue("You don't have a certificate text wasn't displayed", 
				 ConnectToServerActivity.noCertificateText().exists());
		Assert.assertTrue("Connect to the server button was not enabled", 
				 ConnectToServerActivity.connectToServerButton().isEnabled());
		Assert.assertTrue("Create button was not displayed", 
				 ConnectToServerActivity.createButton().exists());
		Assert.assertTrue("Refresh button was not displayed", 
				 ConnectToServerActivity.refreshButton().exists());
		Assert.assertFalse("Create button was enabled", 
				 ConnectToServerActivity.createButton().isEnabled());
		Assert.assertFalse("Refresh button was enabled", 
				 ConnectToServerActivity.refreshButton().isEnabled());
	}
	
	public static void verifyHasSertificateState() throws UiObjectNotFoundException{
		Assert.assertTrue("You have a certificate text wasn't displayed", 
				 ConnectToServerActivity.youHaveACertificateText().exists());
		Assert.assertFalse("Connect to the server button was enabled", 
				 ConnectToServerActivity.connectToServerButton().isEnabled());
		Assert.assertTrue("Create button was not displayed", 
				 ConnectToServerActivity.createButton().exists());
		Assert.assertTrue("Refresh button was not displayed", 
				 ConnectToServerActivity.refreshButton().exists());
		Assert.assertTrue("Create button was not enabled", 
				 ConnectToServerActivity.createButton().isEnabled());
		Assert.assertTrue("Refresh button was not enabled", 
				 ConnectToServerActivity.refreshButton().isEnabled());
	}
	
}
