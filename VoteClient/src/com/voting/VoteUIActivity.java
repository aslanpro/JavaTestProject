package com.voting;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.voting.conference.Principal;
import com.voting.crypto.AESCipher;
import com.voting.crypto.RSA;

public class VoteUIActivity extends Activity {
    
    private Intent answerIntent = new Intent(VoteClientActivity.ANSWER);
    private BroadcastReceiver br;
    
    private TextView question;
    private ListView principals;
    private ProgressDialog progDialog = null;
    private ProgressBar prBar = null;
    private Button buttonExit;
    private Button buttonStart;
    private Integer confId = null;
    private int time = 0;
    
    private static final int CONFIRM_TIMEOUT = 60;	//timeout for confirm operation
    private static final int START_TIMEOUT = 11;	//timeout for start operation
    private static final int VOTING_TIMEOUT = 20;     //time for voting in seconds
    
    private ConfirmMember confirmMessage;
    
    private boolean endVoting = false;
    
    /*
     * Constants for vote protocol
     */
    private static final int POSITIONS = 256;	//size of bulletin in bits
    private static final int VOTE_SIZE = 48;	//all size of vote message in bytes
    private static final byte HEADER = (byte) 0xFB;
    private static final byte END = (byte) 0xED;
    
    private RSA rsa;
    private AESCipher aes;
    byte[] pKey = new byte[RSA.KEY_SIZE];
    SecureRandom sRandom;
    
    static final int BUTTON = 0, DIALOG = 1, ADD = 2;
    
    private SecondHandler handler = new SecondHandler(this);
    
    static class SecondHandler extends Handler {
	private final WeakReference<VoteUIActivity> activitys;

	public SecondHandler(VoteUIActivity activity) {
	    super();
	    this.activitys = new WeakReference<VoteUIActivity>(activity);
	}

	@Override
	public void handleMessage(Message msg) {
	    VoteUIActivity activity = activitys.get();
	    if (activity != null) {
		switch (msg.what) {
		    case BUTTON:
			activity.buttonStart.setEnabled(true);
			break;
		    case DIALOG:
			activity.progDialog = new ProgressDialog(activity);
			activity.progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			activity.progDialog.setCancelable(false);
			activity.progDialog.setMessage("Vaiting for voting results");
			activity.progDialog.setMax(VOTING_TIMEOUT);
			activity.progDialog.show();
			break;
		    case ADD: 
			if (activity.progDialog != null) {
			    activity.progDialog.setProgress(activity.time);
			}
			if (activity.prBar != null) {
			    activity.prBar.setProgress(activity.time);
			}
			if (activity.time >= VOTING_TIMEOUT) {
			    activity.progDialog.dismiss();
			}
			break;
		    default:
			activity.showToast("Wrong command!!!");
		}
	    }
	}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.vote);
	Intent intent = getIntent();
	VoteCreate vC = (VoteCreate) intent.getSerializableExtra(VoteClientActivity.START_ACTIVITY);
	sRandom = vC.getSecureRandom();
	aes = new AESCipher(sRandom);
	question = (TextView) findViewById(R.id.vote_question);
	question.setText(vC.getQuestion());
	principals = (ListView) findViewById(R.id.vote_principals);
	try {
	    FileInputStream fIn = openFileInput("key.k");
	    BufferedInputStream in = new BufferedInputStream(fIn);
	    in.read(pKey);
	    byte[] dKey = new byte[in.available()];
	    in.read(dKey);
	    in.close();
	    rsa = new RSA(dKey, pKey, VoteClientActivity.SERVER_KEY, sRandom);
	    in.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	keyAccept(vC.getKey());
	br = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
		com.voting.Message message = (com.voting.Message) intent.getSerializableExtra(VoteClientActivity.BROADCAST);
		analyzeMessage(message);
	    }
	};
	IntentFilter intFilt = new IntentFilter(VoteClientActivity.BROADCAST);
	LocalBroadcastManager.getInstance(this).registerReceiver(br, intFilt);
	buttonExit = (Button) findViewById(R.id.exit);
	buttonStart = (Button) findViewById(R.id.ready);
	buttonExit.setOnClickListener(exitButton);
	buttonStart.setOnClickListener(startButton);
	sendInteger(0);
    }
    
    Button.OnClickListener exitButton = new Button.OnClickListener() {

	@Override
	public void onClick(View arg0) {
	    Log.i("Conference", "EXIT!");
	    sendMessage(new com.voting.Message(com.voting.Message.EXIT_CONF));
	    finish();
	}
    };
    
    Button.OnClickListener startButton = new Button.OnClickListener() {

	@Override
	public void onClick(View arg0) {
	    Log.i("Conference", "Start!");
	    sendMessage(new com.voting.Message(com.voting.Message.START_VOTE));
	    buttonStart.setEnabled(false);
	    final Timer t = new Timer();
	    t.schedule(new TimerTask() {
		public void run() {
		    handler.sendEmptyMessage(BUTTON);
		    t.cancel();
		}
	    }, START_TIMEOUT * 1000);
	}
    };
    
    synchronized private void analyzeMessage(com.voting.Message message) {
	switch (message.getType()) {
	    case com.voting.Message.KEY:
		if (!keyAccept((KeyInside) message)) {
		    sendInteger(-1);
		    finish();
		}
		break;
	    case com.voting.Message.ENCRYPTED:
		if (((EncryptedMessage) message).getCorrect()) {
		    com.voting.Message m = decryptMessage((EncryptedMessage) message);
		    switch (m.getType()) {
			case com.voting.Message.CONFIRM_MEM:
			    acceptMember((ConfirmMember) m);
			    break;
			case com.voting.Message.ERROR:
			    showToast("Decryption Error!");
			    sendInteger(-1);
			    finish();
			    break;
			case com.voting.Message.VOTE_RES:
			    time = VOTING_TIMEOUT - 1;
			    endVoting = true;
			    showResults((VoteResult) m);
			    break;
			default:
			    Log.i("On Default", "Yes!!!");
			    sendInteger(-1);
			    finish();
		    }
		    break;
		} else {
		    showToast("Incorrect encryption");
		    sendInteger(-1);
		    finish();
		}
		break;
	    case com.voting.Message.SHOW_STATE:
		if (confId == null) {
		    confId = ((ShowState) message).getConference().getId();
		}
		MyAdapter<Principal> adapter = new MyAdapter<Principal>(VoteUIActivity.this, 
			android.R.layout.simple_list_item_1, ((ShowState) message).getConference().getPrincipals());
		principals.setFocusable(false);
		principals.setAdapter(adapter);
		break;
	    case com.voting.Message.START_VOTE:
		startVoting((StartVoting) message);
		break;
	    case com.voting.Message.ERROR:
		showToast("Voting error! Trust level was lowered!");
		sendInteger(-1);
		finish();
		break;
	    default:
		Log.i("Form default!!!", "WTF!!!");
		Log.i("WTF = ", Byte.toString(message.getType()));
		sendInteger(-1);
		Log.i("Form default!!!", "FINISH!!!");
		finish();
	}
    }
    
    private void showToast(String message) {
	Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void sendMessage(com.voting.Message m) {
	answerIntent.putExtra(VoteClientActivity.ANSWER, m);
	LocalBroadcastManager.getInstance(VoteUIActivity.this).sendBroadcast(answerIntent);
    }
    
    private void sendInteger(int i) {
	answerIntent.putExtra(VoteClientActivity.ANSWER, Integer.valueOf(i));
	LocalBroadcastManager.getInstance(VoteUIActivity.this).sendBroadcast(answerIntent);
    }
    
    synchronized private com.voting.Message decryptMessage(EncryptedMessage message) {
	return aes.decryptMessage(message.getMessage(), message.getS0());
    }
    
    synchronized private EncryptedMessage encryptMessage(com.voting.Message message) {
	EncryptedMessage eM = new EncryptedMessage();
	eM.setS0(aes.generateS0());
	eM.setMessage(aes.encryptMessage(message, eM.getS0()));
	return eM;
    }
    
    synchronized private boolean keyAccept(KeyInside key) {
	if (!rsa.verify(key.getKey(), key.getSign())) {
	    return false;
	}
	byte[] keyAES;
	keyAES = rsa.decrypt(key.getKey());
	aes.setKey(keyAES);
	return true;
    }
    
    synchronized private void acceptMember(ConfirmMember member) {
	final Dialog confirmDialog = new Dialog(this);
	confirmDialog.setContentView(R.layout.confirm_member);
	confirmDialog.setTitle("Confirm member");
	confirmDialog.setCancelable(false);
	TextView principal = (TextView) confirmDialog.findViewById(R.id.principal);
	principal.setText(member.getInfo());
	final Button confirm = (Button) confirmDialog.findViewById(R.id.confirm_member);
	final Button decline = (Button) confirmDialog.findViewById(R.id.decline_member);
	String s = new String(member.getInfo());
	confirmMessage = new ConfirmMember(s);
	confirm.setOnClickListener(new View.OnClickListener() {
	    
	    @Override
	    public void onClick(View v) {
		confirmDialog.dismiss();
		confirmMessage.setConfirm(true);
		sendMessage(encryptMessage(confirmMessage));
	    }
	});
	decline.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		confirmDialog.dismiss();
		confirmMessage.setConfirm(false);
		sendMessage(encryptMessage(confirmMessage));
	    }
	});
	confirmDialog.show();
	final Timer t = new Timer();
	t.schedule(new TimerTask() {
	    public void run() {
		confirmDialog.dismiss();
		t.cancel();
	    }
	}, CONFIRM_TIMEOUT * 1000);
    }

    private synchronized void startVoting(final StartVoting start) {
	final Dialog voteDialog = new Dialog(this);
	voteDialog.setContentView(R.layout.vote_dialog);
	voteDialog.setTitle("Vote");
	voteDialog.setCancelable(false);
	TextView question = (TextView) voteDialog.findViewById(R.id.vote_question_dilog);
	prBar = (ProgressBar) voteDialog.findViewById(R.id.progressBar1);
	prBar.setMax(VOTING_TIMEOUT);
	question.setText(this.question.getText());
	final Button yes = (Button) voteDialog.findViewById(R.id.yes);
	final Button no = (Button) voteDialog.findViewById(R.id.no);
	final Button abstain = (Button) voteDialog.findViewById(R.id.abstain);
	yes.setOnClickListener(new View.OnClickListener() {
	    
	    @Override
	    public void onClick(View v) {
		voteDialog.dismiss();
		prBar = null;
		handler.sendEmptyMessage(DIALOG);
		
		SendThread sT = new SendThread(true, start.getBytes());
		sT.start();
		
	    }
	});
	no.setOnClickListener(new View.OnClickListener() {
	    
	    @Override
	    public void onClick(View v) {
		voteDialog.dismiss();
		prBar = null;
		handler.sendEmptyMessage(DIALOG);
		SendThread sT = new SendThread(false, start.getBytes());
		sT.start();
		
	    }
	});
	abstain.setOnClickListener(new View.OnClickListener() {
	    
	    @Override
	    public void onClick(View v) {
		voteDialog.dismiss();
		prBar = null;
		handler.sendEmptyMessage(DIALOG);
		sendMessage(new Vote(null, null));
	    }
	});
	voteDialog.show();
	ProgressThread pT = new ProgressThread();
	pT.start();
	final Timer t = new Timer();
	t.schedule(new TimerTask() {
	    public void run() {
		voteDialog.dismiss();
		t.cancel();
	    }
	}, VOTING_TIMEOUT * 1000);
    }
    
    private synchronized void showResults(VoteResult vR) {
	setContentView(R.layout.vote_result);
	setTitle("Voting results!");
	TextView question = (TextView) findViewById(R.id.result_question);
	TextView yesRes = (TextView) findViewById(R.id.yes_result);
	yesRes.append(" - " + vR.getYes());
	TextView noRes = (TextView) findViewById(R.id.no_result);
	noRes.append(" - " + vR.getNo());
	question.setText(this.question.getText());
	final Button ok = (Button) findViewById(R.id.ok);
	ExpandableListView list = (ExpandableListView) findViewById(R.id.exp_list);
	MyExpandableAdapter adapter = new MyExpandableAdapter(this, vR.getAll());
	list.setAdapter(adapter);
	list.setFocusable(false);
	ok.setOnClickListener(new View.OnClickListener() {
	    
	    @Override
	    public void onClick(View v) {
		finish();
	    }
	});
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i("Conference", "EXIT!");
            if (!endVoting) {
        	sendMessage(new com.voting.Message(com.voting.Message.EXIT_CONF));
            }
	    finish();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
	Log.i("State", "On destroy 2");
    }

    class MyAdapter<T> extends ArrayAdapter<T> {
	public MyAdapter(Context context, int textViewResourceId, List<T> objects) {
	    super(context, textViewResourceId, objects);
	}

	@Override
	public boolean isEnabled(int position) {
	    return false;
	}
    }
    
    static class MyExpandableAdapter extends SimpleExpandableListAdapter {
	
	static final ArrayList<Map<String, String>> groupData;
	static final String[] groupFrom = new String[] {"groupName"};
	static int[] groupTo = new int[] {android.R.id.text1};
	static String[] childFrom = new String[] {VoteResult.prName};
        static int childTo[] = new int[] {android.R.id.text1};
	
	static {
	    groupData = new ArrayList<Map<String, String>>();
	    Map<String, String> m = new HashMap<String, String>();
	    m.put("groupName", "Voted principals:");
	    groupData.add(m);
	    m = new HashMap<String, String>();
	    m.put("groupName", "Abstained principals:");
	    groupData.add(m);
	};

	public MyExpandableAdapter(Context context,
		List<? extends List<? extends Map<String, ?>>> childData) {
	    super(context, groupData,
		    android.R.layout.simple_expandable_list_item_1, groupFrom,
		    groupTo, childData, android.R.layout.simple_list_item_1,
		    childFrom, childTo);
	}
	
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
	    return false;
	}
    }
    
    class SendThread extends Thread {
	
	boolean vote;
	byte[] random;
	
	public SendThread(boolean vote, byte[] random) {
	    this.random = random;
	    this.vote = vote;
	}
	
	@Override
	public void run() {
	    /*
	     * voteMessage = 0xFB || Conference ID || Server Random bytes ||
	     *  	|| Position of vote bit || Vote bytes || 0xFB || 0xED
	     *  || - is a concat operation
	     */
	    byte[] voteMessage = new byte[VOTE_SIZE];
	    voteMessage[0] = HEADER;
	    voteMessage[1] = (byte) ((confId.intValue() >> 24) & (byte) 0xFF);
	    voteMessage[2] = (byte) ((confId.intValue() >> 16) & (byte) 0xFF);
	    voteMessage[3] = (byte) ((confId.intValue() >> 8) & (byte) 0xFF);
	    voteMessage[4] = (byte) (confId.intValue() & (byte) 0xFF);
	    int i, j;
	    for (i = 0; i < random.length; i++) {
		voteMessage[5 + i] = random[i];
	    }
	    i += 5;
	    int pos = sRandom.nextInt(POSITIONS);
	    byte mask = (byte) ((1 << (pos % 8)) & 0xFF);
	    voteMessage[i++] = (byte) (pos & 0xFF);
	    byte[] bulletin = new byte[POSITIONS/8];
	    sRandom.nextBytes(bulletin);
	    if (vote) {
		bulletin[pos / 8] |= mask; 
	    } else {
		bulletin[pos / 8] &= (mask ^ 0xFF);
	    }
	    for (j = 0; j < bulletin.length; j++) {
		voteMessage[i + j] = bulletin[j];
	    }
	    voteMessage[i + j] = HEADER;
	    voteMessage[i + j + 1] = END;
	    byte[] encrypt = rsa.encrypt(voteMessage);
	    Vote v = new Vote(encrypt, rsa.sign(encrypt));
	    sendMessage(encryptMessage(v));
	}
    }
    
    private class ProgressThread extends Thread {
       
	@Override
        public void run() {
            while (time <= VOTING_TIMEOUT) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                }
                time++;
                handler.sendEmptyMessage(ADD);
            }
        }
    }
}