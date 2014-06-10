package utils;

import java.io.IOException;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiSelector;

public class AppActions {
	
	public static UiObject votingSystemApp(){
		return new UiObject(new UiSelector()
        	.className("android.widget.TextView").text("Voting system"));
	}
	
	public static void findAndRunVotingSystemApp() throws UiObjectNotFoundException{
		votingSystemApp().clickAndWaitForNewWindow();
	}
	
	public static void clearVotingSystemApp() throws IOException, InterruptedException{
		Runtime runtime = Runtime.getRuntime();
        runtime.exec("pm clear com.voting");
        Thread.sleep(2000);
	}

}
