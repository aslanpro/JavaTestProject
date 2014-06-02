package com.voting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.List;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.voting.conference.Conference;
import com.voting.conference.Principal;
import com.voting.crypto.RSA;
import com.voting.crypto.RSAGen;

public class VoteClientActivity extends Activity {
    
    com.voting.Message mSend;

    private ArrayAdapter<Conference> adapter;
    private TextView hello;
    private ListView list;
    private Button buttonSend;
    private Button buttonCreate;
    private Button buttonShow;
    private ProgressDialog prog_dialog;
    
    private String sName = null;
    private String sMail = null;
    private String sQuest = null;
    private byte[] pKey = new byte[RSA.KEY_SIZE];
    private byte[] id = new byte[com.voting.Message.ID_SIZE];
    private byte[] cert = new byte[RSA.KEY_SIZE];
    private int confID = -1;
    
    private ClientThread cTh;
    private PipedWriter outPipe;
    static final int PORT = 7070;
    static final String IP_ADR = "192.168.1.3"; //10.0.2.2 - local; 192.168.1.2 - router
    static final int RANDOM_SIZE = 28; // size of random bytes for handshake
    
    private RSA rsa;
    private SecureRandom sRandom;
    
    private BroadcastReceiver br;
    static final String BROADCAST = "com.voting.broadcast";
    static final String ANSWER = "com.voting.answer";
    static final String START_ACTIVITY = "com.voting.startactivity";
    
    // Server public key:
    public static final byte[] SERVER_KEY = { 78, -53, -68, -55, -46, 81, 98,
	    38, -81, 113, 43, 4, 26, 8, 117, -51, -68, 34, -55, 55, -27, 18,
	    24, -80, -105, 89, 59, -49, -73, -8, -15, -92, -120, -27, -111,
	    -50, 126, 9, -3, 98, 64, -104, 14, 44, -14, -78, -4, 30, -7, 71,
	    104, -11, 51, -34, -33, 44, 20, 20, 113, 73, 61, 105, 127, 29, -48,
	    -53, 117, 113, -19, 24, 109, 126, 76, 121, 6, -25, 0, 109, 26, 13,
	    -18, -52, 73, 49, -90, 76, 4, 72, 106, -61, 7, -13, -47, -91, -98,
	    41, 92, 4, -67, -61, 55, 6, 15, -114, 105, -84, -92, -16, 57, 111,
	    -5, -60, -123, 2, -53, -80, 48, -22, 66, -111, -109, 26, 22, -41,
	    107, -67, 106, 83 };
    
    static final int DISMISS = 0, SPIN = 1, REG = 2, INFO_RECIVED = 3,
	    SHOW = 4, CREATE = 5, JOIN = 6, NOTE = 7, SHOW_PRI = 8, EXIT = 9, END_OF_CONNECTION = -1;

    private MyHandler handler = new MyHandler(this);

    static class MyHandler extends Handler {
	private final WeakReference<VoteClientActivity> activitys;

	public MyHandler(VoteClientActivity activity) {
	    super();
	    this.activitys = new WeakReference<VoteClientActivity>(activity);
	}

	@Override
	public void handleMessage(Message msg) {
	    VoteClientActivity activity = activitys.get();
	    if (activity != null) {
		switch (msg.what) {
		    case DISMISS:
			activity.prog_dialog.dismiss();
			break;
		    case SPIN:
			activity.prog_dialog = new ProgressDialog(activity);
			activity.prog_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			activity.prog_dialog.setCancelable(false);
			activity.prog_dialog.show();
			activity.prog_dialog.setMessage((String) msg.obj);
			break;
		    case REG:
			activity.registration();
			break;
		    case INFO_RECIVED:
			if (!activity.writeCert()) {
			    activity.showToast("Certificate error! Try to registrate later");
			    activity.cTh.close();
			} else {
			    activity.hello.setText("Congratulations! You have a certificate!");
			}
			break;
		    case SHOW:
			activity.buttonCreate.setEnabled(true);
			activity.buttonShow.setEnabled(true);
			activity.adapter = new ArrayAdapter<Conference>(activity, 
				android.R.layout.simple_list_item_1, ((ShowConf) msg.obj).show());
			activity.list.setAdapter(activity.adapter);
			break;
		    case JOIN:
			if (((ShowState) msg.obj).getConference() != null) {
			    activity.joinConf(((ShowState) msg.obj).getConference());
			} else {
			    activity.showToast("This conference is closed.");
			    try {
				activity.outPipe.write(SHOW);
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}
			break;
		    case NOTE:
			activity.showToast((String) msg.obj);
			break;
		    case END_OF_CONNECTION:
			activity.showToast("End of connection");
			activity.buttonSend.setEnabled(true);
			activity.buttonShow.setEnabled(false);
			activity.buttonCreate.setEnabled(false);
			if (activity.adapter != null) {
			    activity.adapter.clear();
			    activity.list.setAdapter(activity.adapter);
			}
			break;
		    default:
			activity.showToast("Wrong command!!!");
		}
	    }
	}
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	Log.d("State", "On create");
	hello = (TextView) findViewById(R.id.hello);
	list = (ListView) findViewById(R.id.list);
	list.setOnItemClickListener(new OnItemClickListener() {

	    @Override
	    public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
		confID = adapter.getItem(arg2).getId();
		try {
		    outPipe.write(SHOW_PRI);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	});
	buttonSend = (Button) findViewById(R.id.send);
	buttonCreate = (Button) findViewById(R.id.create);
	buttonShow = (Button) findViewById(R.id.show);
	buttonShow.setEnabled(false);
	buttonCreate.setEnabled(false);
	sRandom = new SecureRandom();
	try {
	    FileInputStream fIn = openFileInput("Mycert.cer");
	    BufferedInputStream in = new BufferedInputStream(fIn);
	    in.read(id);
	    in.read(cert);
	    in.close();
	    hello.setText("Congratulations! You have a certificate!");
	} catch (FileNotFoundException e) {
	    hello.setText("You don't have a certificate. You must register your key");
	    id = null;
	    cert = null;
	} catch (IOException e) {
	    e.printStackTrace();
	}
	try {
	    FileInputStream fIn = openFileInput("key.k");
	    BufferedInputStream in = new BufferedInputStream(fIn);
	    in.read(pKey);
	    byte[] dKey = new byte[in.available()];
	    in.read(dKey);
	    in.close();
	    rsa = new RSA(dKey, pKey, SERVER_KEY, sRandom);
	    //in.close();
	} catch (FileNotFoundException e) {
	    PrepareKeys p = new PrepareKeys();
	    p.start();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	buttonSend.setOnClickListener(connectServer);
	buttonCreate.setOnClickListener(createConference);
	buttonShow.setOnClickListener(showConference);
	br = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
		Object o = (Object) intent.getSerializableExtra(ANSWER);
		if (o instanceof com.voting.Message) {
		    mSend = (com.voting.Message) o;
		    try {
			cTh.send(mSend);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
		if (o instanceof Integer) {
		    switch ((Integer) o) {
			case 0:
			    try {
				outPipe.write(0); 	// Informs this activity that VoteUIActivity is build
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			    break;
			case -1:
			    cTh.close();
			    break;
			default:
			    showToast("Wrong Intent!");
		    }
		}
	    }
	};
	IntentFilter intFilt = new IntentFilter(VoteClientActivity.ANSWER);
	LocalBroadcastManager.getInstance(this).registerReceiver(br, intFilt);
    }
    
    private synchronized void joinConf(final Conference conf) {
	final Dialog joinDialog = new Dialog(VoteClientActivity.this);
	joinDialog.setContentView(R.layout.conference);
	joinDialog.setTitle("Joining to conference");
	joinDialog.setCancelable(false);
	joinDialog.show();
	final ListView prList = (ListView) joinDialog.findViewById(R.id.principals);
	MyAdapter<Principal> adapter = new MyAdapter<Principal>(VoteClientActivity.this, 
		android.R.layout.simple_list_item_1, conf.getPrincipals());
	prList.setAdapter(adapter);
	prList.setFocusable(false);
	TextView question = (TextView) joinDialog.findViewById(R.id.question);
	question.setText(conf.getQuestion());
	final Button confirm = (Button) joinDialog.findViewById(R.id.confirm);
	final Button back = (Button) joinDialog.findViewById(R.id.back);
	confirm.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		joinDialog.dismiss();
		try {
		    buttonCreate.setEnabled(false);
		    buttonShow.setEnabled(false);
		    confID = conf.getId();
		    sQuest = conf.getQuestion();
		    outPipe.write(JOIN);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	});
	back.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		joinDialog.dismiss();
		try {
		    outPipe.write(SHOW);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		buttonShow.setEnabled(true);
	    }
	});
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

    private synchronized void createConf() {
	final Dialog createDialog = new Dialog(VoteClientActivity.this);
	createDialog.setContentView(R.layout.create_conf);
	createDialog.setTitle("Creating voting");
	createDialog.setCancelable(true);
	createDialog.show();
	final EditText question = (EditText) createDialog.findViewById(R.id.conf_quest);
	final Button buttonSend = (Button) createDialog.findViewById(R.id.send_conf);
	buttonSend.setEnabled(false);
	question.setOnKeyListener(new View.OnKeyListener() {

	    public boolean onKey(View v, int keyCode, KeyEvent event) {
		if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		    sQuest = question.getText().toString();
		    buttonSend.setEnabled(true);
		    return true;
		}
		return false;
	    }
	});
	buttonSend.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		createDialog.dismiss();
		try {
		    buttonCreate.setEnabled(false);
		    buttonShow.setEnabled(false);
		    outPipe.write(CREATE);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	});
    }

    private void registration() {
	final Dialog regDialog = new Dialog(VoteClientActivity.this);
	regDialog.setContentView(R.layout.reg_info);
	regDialog.setTitle("Reg Dialog");
	regDialog.setCancelable(false);
	regDialog.show();
	final EditText name = (EditText) regDialog.findViewById(R.id.reg_name);
	final EditText mail = (EditText) regDialog.findViewById(R.id.reg_mail);
	final Button buttonReg = (Button) regDialog.findViewById(R.id.send_reg);
	buttonReg.setEnabled(false);
	mail.setOnKeyListener(new View.OnKeyListener() {

	    public boolean onKey(View v, int keyCode, KeyEvent event) {
		if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		    sMail = mail.getText().toString();
		    buttonReg.setEnabled(true);
		    return true;
		}
		return false;
	    }
	});
	name.setOnKeyListener(new View.OnKeyListener() {

	    public boolean onKey(View v, int keyCode, KeyEvent event) {
		if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		    sName = name.getText().toString();
		    return true;
		}
		return false;
	    }
	});
	buttonReg.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		if (sName == null) {
		    sName = name.getText().toString();
		}
		regDialog.dismiss();
		try {
		    outPipe.write(0);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	});
    }

    private boolean writeCert() {
	byte[] unsigned = concat(id, pKey);
	if ((unsigned != null) && (cert != null)) {
	    if (rsa.verify(unsigned, cert)) {
		try {
		    FileOutputStream fOut = openFileOutput("Mycert.cer", MODE_PRIVATE);
		    BufferedOutputStream out = new BufferedOutputStream(fOut);
		    out.write(id);
		    out.write(cert);
		    out.flush();
		    out.close();
		} catch (Exception e1) {
		    e1.printStackTrace();
		    showToast("IO error!");
		    return false;
		}
		return true;
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }

    private void showToast(String message) {
	Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /*
     * Concatenate two byte arrays
     */
    private byte[] concat(byte[] first, byte[] second) {
	if ((first == null) || (second == null)) {
	    return null;
	}
	byte[] result = new byte[first.length + second.length];
	System.arraycopy(first, 0, result, 0, first.length);
	System.arraycopy(second, 0, result, first.length, second.length);
	return result;
    }

    @Override
    public void onStart() {
	super.onStart();
	Log.d("State", "On start 1");
    }

    @Override
    public void onPause() {
	super.onPause();
	Log.d("State", "On pause 1");
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
	super.onSaveInstanceState(savedInstanceState);
	Log.i("State", "On save!! 1");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
	super.onRestoreInstanceState(savedInstanceState);
	Log.i("State", "On restore! 1");
    }

    @Override
    public void onStop() {
	super.onStop();
	Log.d("State", "On stop 1");
    }

    @Override
    public void onRestart() {
	super.onRestart();
	Log.d("State", "On restart 1 ");
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	Log.d("State", "On destroy 1");	
	LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
	Log.i("ACT-1", "EXIT!");
	if (cTh != null) {
	    if (cTh.keepGoing) {
		try {
		    outPipe.write(EXIT);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    private class PrepareKeys extends Thread {

	private RSAGen g;
	
	public PrepareKeys() {
	    g = null;
	}

	public void run() {
	    handler.sendMessage(Message.obtain(handler, SPIN, "Generating keys..."));
	    g = new RSAGen();
	    rsa = new RSA(g.getPrivateKey(), g.getN(), SERVER_KEY, sRandom);
	    pKey = g.getN();
	    try {
		FileOutputStream fOut = openFileOutput("key.k", MODE_PRIVATE);
		BufferedOutputStream out = new BufferedOutputStream(fOut);
		out.write(g.getN());
		out.write(g.getPrivateKey());
		out.flush();
		out.close();
	    } catch (Exception e1) {
		e1.printStackTrace();
		handler.sendMessage(Message.obtain(handler, NOTE, "IO error!"));
	    } finally {
		handler.sendEmptyMessage(DISMISS);
	    }
	}
    }
    
    Button.OnClickListener connectServer = new Button.OnClickListener() {

	@Override
	public void onClick(View arg0) {
	    cTh = new ClientThread();
	    cTh.start();
	    buttonSend.setEnabled(false);
	}
    };
    
    Button.OnClickListener createConference = new Button.OnClickListener() {

	@Override
	public void onClick(View arg0) {
	    createConf();
	}
    };
    
    Button.OnClickListener showConference = new Button.OnClickListener() {

	@Override
	public void onClick(View v) {
	    try {
		outPipe.write(SHOW);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    };

    private class ClientThread extends Thread {

	private Socket socket;
	private ObjectOutputStream output = null;
	private ObjectInputStream input = null;
	private PipedReader inPipe;
	private boolean keepGoing;
	//private boolean thisActivity = true;
	private Intent intent;

	public ClientThread() {
	    try {
		outPipe = new PipedWriter();
		inPipe = new PipedReader(outPipe);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}

	@Override
	public void run() {
	    try {
		socket = new Socket(IP_ADR, PORT);
		input = new ObjectInputStream(socket.getInputStream());
		output = new ObjectOutputStream(socket.getOutputStream());
		ShowConf conf = handshake();
		if (conf != null) {
		    handler.sendMessage(Message.obtain(handler, SHOW, conf));
		    keepGoing = true;
		    com.voting.Message m;
		    com.voting.Message answer;
		    int act;
		    while (keepGoing) {
			act = inPipe.read();
			switch (act) {
			    case SHOW_PRI:
				m = new ShowState(confID);
				output.writeObject(m);
				handler.sendMessage(Message.obtain(handler, SPIN, "Waiting for join..."));
				answer = (com.voting.Message) input.readObject();
				handler.sendEmptyMessage(DISMISS);
				if (answer.getType() == com.voting.Message.SHOW_STATE) {
				    handler.sendMessage(Message.obtain(handler, JOIN, (ShowState) answer));
				} else {
				    throw new ServerCorruptedException();
				}
				break;
			    case JOIN:
				m = new JoinConf(confID);
				output.writeObject(m);
				handler.sendMessage(Message.obtain(handler, SPIN, "Waiting for confirmation..."));
				answer = (com.voting.Message) input.readObject();
				handler.sendEmptyMessage(DISMISS);
				switch (answer.getType()) {
				    case com.voting.Message.ERROR:
					handler.sendMessage(Message.obtain(handler, NOTE, 
						indicateError((Error) answer)));
					return;
				    case com.voting.Message.KEY:
					if (!rsa.verify(((KeyInside) answer).getKey(), 
						((KeyInside) answer).getSign())) {
					    throw new ServerCorruptedException();
					}
					intent = new Intent(VoteClientActivity.this, VoteUIActivity.class);
					intent.putExtra(START_ACTIVITY, new VoteCreate(sRandom, 
						(KeyInside) answer, sQuest));
					startActivity(intent);
					keepGoing = false;
					break;
				    default:
					throw new ServerCorruptedException();
				}
				break;
			    case CREATE:
				m = new CreateConf(sQuest);
				output.writeObject(m);
				answer = (com.voting.Message) input.readObject();
				switch (answer.getType()) {
				    case com.voting.Message.ERROR:
					handler.sendMessage(Message.obtain(handler, NOTE, 
						indicateError((Error) answer)));
					return;
				    case com.voting.Message.KEY:
					if (!rsa.verify(((KeyInside) answer).getKey(), 
						((KeyInside) answer).getSign())) {
					    throw new ServerCorruptedException();
					}
					intent = new Intent(VoteClientActivity.this, VoteUIActivity.class);
					intent.putExtra(START_ACTIVITY, new VoteCreate(sRandom, 
						(KeyInside) answer, sQuest));
					startActivity(intent);
					keepGoing = false;
					break;
				    default:
					throw new ServerCorruptedException();
				}
				break;
			    case SHOW:
				m = new com.voting.Message(com.voting.Message.SHOW_CONF);
				output.writeObject(m);
				conf = (ShowConf) input.readObject();
				handler.sendMessage(Message.obtain(handler, SHOW, conf));
				break;
			    case EXIT:
				return;
			    default:
				throw new ServerCorruptedException();
			}
		    }
		    inPipe.read();
		    intent = new Intent(BROADCAST);
		    keepGoing = true;
		    while (keepGoing) {
			m = (com.voting.Message) input.readObject();
			switch (m.getType()) {
			    case com.voting.Message.KEY:
				sendBroadcastMessage(m);
				break;
			    case com.voting.Message.ENCRYPTED:
				sendBroadcastMessage(m);
				break;
			    case com.voting.Message.SHOW_STATE:
				sendBroadcastMessage(m);
				break;
			    case com.voting.Message.EXIT_CONF:
				keepGoing = false;
				intent = null;
				break;
			    case com.voting.Message.START_VOTE:
				sendBroadcastMessage(m);
				break;
			    case com.voting.Message.ERROR:
				sendBroadcastMessage(m);
				break;
			    default:
				throw new ServerCorruptedException();    
			}
		    }
		}
	    } catch (ServerCorruptedException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
		handler.sendMessage(Message.obtain(handler, NOTE, "Connection error!"));
	    } catch (ClassNotFoundException e) {
		e.printStackTrace();
	    } finally {
		close();
		handler.sendEmptyMessage(END_OF_CONNECTION);
	    }
	}
	
	private void sendBroadcastMessage(com.voting.Message m) {
	    intent.putExtra(BROADCAST, m);
	    LocalBroadcastManager.getInstance(VoteClientActivity.this).sendBroadcast(intent);
	}
	
	private void send(com.voting.Message m) throws IOException, ServerCorruptedException {
	    if (m.getType() == com.voting.Message.ERROR) {
		throw new ServerCorruptedException();
	    }
	    output.writeObject(m);
	}
	
	private void close() {
	    if (input != null) {
		try {
		    input.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	    
	    if (output != null) {
		try {
		    output.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	    if (socket != null) {
		try {
		    socket.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }

	    if (inPipe != null) {
		try {
		    inPipe.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }

	    if (outPipe != null) {
		try {
		    outPipe.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}

	private String indicateError(Error e) {
	    switch (e.getErrType()) {
		case Error.BAN_ERROR:
		    return "Your certificate is baned";
		case Error.DATABASE_ERROR:
		    return "Database error";
		case Error.DECRYPT_ERROR:
		    return "Decrypt error";
		case Error.HANDSHAKE_ERROR:
		    return "Handshake error";
		case Error.INVALID_SIGNATURE:
		    return "Signature is not valid";
		case Error.DOUBLE_CONNECTION:
		    return "Double connection to the server";
		case Error.REGISTRATION_ERROR:
		    return "Registration error";
		case Error.CREATE_ERROR:
		    return "Create error!";
		case Error.JOIN_ERROR:
		    return "Join error!";
		case Error.TRUST_LOWERED:
		    return "You trust level was lowered";
		case Error.UNKNOWN_ERROR:
		    return "Unknown error";
		default:
		    return "Unknown error";
	    }
	}

	private ShowConf handshake() throws OptionalDataException,
		ClassNotFoundException, IOException, ServerCorruptedException {
	    byte[] randS;
	    byte[] randC = new byte[RANDOM_SIZE];
	    randS = (byte[]) input.readObject();
	    com.voting.Information info;
	    if ((id != null) || (cert != null)) {
		info = new Information(id, pKey, cert);
	    } else {
		info = new Information(pKey);
	    }
	    output.writeObject(info);
	    sRandom.nextBytes(randC);
	    output.writeObject(randC);
	    byte[] sign = concat(randS, randC);
	    output.writeObject(rsa.sign(sign));
	    com.voting.Message m = (com.voting.Message) input.readObject();
	    switch (m.getType()) {
		case com.voting.Message.REG:
		    handler.sendEmptyMessage(REG);
		    inPipe.read();
		    Registration r = new Registration(sName, sMail);
		    output.writeObject(r);
		    m = (com.voting.Message) input.readObject();
		    switch (m.getType()) {
			case com.voting.Message.INFO:
			    id = ((Information) m).getID();
			    cert = ((Information) m).getCert();
			    handler.sendEmptyMessage(INFO_RECIVED);
			    break;
			case com.voting.Message.ERROR:
			    handler.sendMessage(Message.obtain(handler, NOTE, indicateError((Error) m)));
			    return null;
			default:
			    throw new ServerCorruptedException();
		    }
		    com.voting.Message mes = (com.voting.Message) input.readObject();
		    if (mes.getType() == com.voting.Message.SHOW_CONF) {
			return ((ShowConf) mes);
		    }
		    throw new ServerCorruptedException();
		case com.voting.Message.ERROR:
		    handler.sendMessage(Message.obtain(handler, NOTE, indicateError((Error) m)));
		    return null;
		case com.voting.Message.SHOW_CONF:
		    return (ShowConf) m;
		default:
		    throw new ServerCorruptedException();
	    }
	}
	
	@SuppressWarnings("serial")
	private class ServerCorruptedException extends IOException {
	    public ServerCorruptedException() {
		super();
		handler.sendMessage(Message.obtain(handler, NOTE, "Server corrupted!"));
	    }
	}
    }
}