package com.voting;

import com.voting.crypto.RSA;
import com.voting.conference.Conference;
import com.voting.conference.Principal;
import com.voting.crypto.AESCipher;
import java.io.*;
import java.util.*;

public class Message implements Serializable {

    /*
     * Types of messages
     */
    public static final byte REG = 0x0, INFO = 0x1, SHOW_CONF = 0x2, CREATE_CONF = 0x3,
            JOIN_CONF = 0x4, KEY = 0x5, ENCRYPTED = 0x6, CONFIRM_MEM = 0x7, EXIT_CONF = 0x8, VOTE = 0x9,
            START_VOTE = 0x10, SHOW_STATE = 0x11, VOTE_RES = 0x12, ERROR = 0x13, UNKNOWN = 0x14;
    private byte type;
    public static final int ID_SIZE = 16;
    public static final int RANDBYTES_SIZE = 8;

    public Message() {
        type = UNKNOWN;
    }

    public Message(byte type) {
        if ((type >= UNKNOWN) || (type < REG)) {
            this.type = UNKNOWN;
        } else {
            this.type = type;
        }
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        if ((type >= UNKNOWN) || (type < REG)) {
            this.type = UNKNOWN;
        } else {
            this.type = type;
        }
    }
}

class Registration extends Message {

    private String name;
    private String eMail;

    public Registration() {
        super(Message.REG);
        name = null;
        eMail = null;
    }

    public Registration(String name, String eMail) {
        super(Message.REG);
        this.name = name;
        this.eMail = eMail;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return eMail;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String eMail) {
        this.eMail = eMail;
    }

    public void setInfo(String name, String eMail) {
        this.name = name;
        this.eMail = eMail;
    }
}

class Information extends Message {

    private byte[] id;
    private byte[] pKey;
    private byte[] cert;

    public Information() {
        super(Message.INFO);
        this.id = null;
        this.cert = null;
        this.pKey = null;
    }

    public Information(byte[] id, byte[] pKey, byte[] cert) {
        super(Message.INFO);
        this.id = Arrays.copyOfRange(id, 0, ID_SIZE);
        this.pKey = Arrays.copyOfRange(pKey, 0, RSA.KEY_SIZE);
        this.cert = Arrays.copyOfRange(cert, 0, RSA.KEY_SIZE);
    }

    public Information(byte[] pKey) {
        super(Message.INFO);
        this.pKey = Arrays.copyOfRange(pKey, 0, RSA.KEY_SIZE);
        this.cert = null;
        this.id = null;
    }

    public byte[] getID() {
        return this.id;
    }

    public byte[] getKey() {
        return this.pKey;
    }

    public byte[] getCert() {
        return this.cert;
    }

    public void setKey(byte[] pKey) {
        this.pKey = Arrays.copyOfRange(pKey, 0, RSA.KEY_SIZE);
    }

    public void setID(byte[] id) {
        this.id = Arrays.copyOfRange(id, 0, ID_SIZE);
    }

    public void setCert(byte[] cert) {
        this.cert = Arrays.copyOfRange(cert, 0, RSA.KEY_SIZE);
    }
}

class ShowConf extends Message {

    private ArrayList<Conference> conf;

    public ShowConf() {
        super(Message.SHOW_CONF);
        this.conf = null;
    }

    public ShowConf(Collection<Conference> conf) {
        super(Message.SHOW_CONF);
        this.conf = new ArrayList<Conference>(conf);
    }
    
    public void setConf(Collection<Conference> conf) {
        this.conf = new ArrayList<Conference>(conf);
    }

    public ArrayList<Conference> show() {
        return this.conf;
    }    
}

class CreateConf extends Message {

    private String question;

    public CreateConf() {
	super(Message.CREATE_CONF);
	question = null;
    }

    public CreateConf(String question) {
	super(Message.CREATE_CONF);
	this.question = question;
    }

    public String getQuestion() {
	return this.question;
    }

    public void setQuestion(String question) {
	this.question = question;
    }
}

class ShowState extends Message {

    private Conference conference;
    private int confId;

    public ShowState() {
        super(Message.SHOW_STATE);
        conference = null;
    }
    
    public ShowState(int confId) {
        super(Message.SHOW_STATE);
	this.confId = confId;
    }

    public ShowState(Conference conference) {
        super(Message.SHOW_STATE);
        this.conference = new Conference(conference);
        this.confId = conference.getId();
    }

    public Conference getConference() {
        return this.conference;
    }

    public void setConference(Conference conference) {
        this.conference = new Conference(conference);
        this.confId = conference.getId();
    }
    
    public int getId() {
	return confId;
    }
}

class JoinConf extends Message {
    
    private int idConf;
    
    public JoinConf() {
        super(Message.JOIN_CONF);
    }
    
    public JoinConf(int idConf) {
        super(Message.JOIN_CONF);
        this.idConf = idConf;
    }
    
    public int getId() {
        return this.idConf;
    }
    
    public void setId(int idConf) {
        this.idConf = idConf;
    }
}

class EncryptedMessage extends Message {
    
    private byte[] s0;
    private byte[] message;
    private boolean correct = true;
    private boolean confirm = false;
    
    public EncryptedMessage() {
        super(Message.ENCRYPTED);
        s0 = null;
        message = null;
    }
    
    public EncryptedMessage(byte[] s0, byte[] message) {
        super(Message.ENCRYPTED);
        this.s0 = Arrays.copyOf(s0, AESCipher.SIZE);
        this.message = message;
        if (message == null) {
            this.correct = false;
        }
    }
    
    public void setS0(byte[] s0) {
	this.s0 = Arrays.copyOf(s0, AESCipher.SIZE);
    }
    
    public void setMessage(byte[] message) {
	this.message = message;
	if (message == null) {
            this.correct = false;
        }
    }
    
    public void setConfirm(boolean confirm) { 
        this.confirm = confirm;
    }
    
    public boolean getConfirm() { 
        return this.confirm;
    }
    
    public byte[] getS0() {
        return this.s0;
    }
    
    public byte[] getMessage() {
        return this.message;
    }
    
    public boolean getCorrect() {
	return this.correct;
    }
}

class ConfirmMember extends Message {
    private String info;
    private boolean confirm;
    
    public ConfirmMember() {
        super(Message.CONFIRM_MEM);
        info = null;
    }
    
    public ConfirmMember(String info) {
        super(Message.CONFIRM_MEM);
        this.info = info;
        confirm = false;
    }
    
    public String getInfo() {
        return this.info;
    }
    
    public void setInfo(String info) {
        this.info = info;
    }
    
    public void setConfirm(boolean confirm) {
        this.confirm = confirm;
    }
    
    public boolean getConfirm() {
        return this.confirm;
    }
}

class KeyInside extends Message {

    private byte[] key;
    private byte[] sign;

    public KeyInside() {
        super(Message.KEY);
        key = null;
        sign = null;
    }

    public KeyInside(byte[] key, byte[] sign) {
        super(Message.KEY);
        this.key = Arrays.copyOf(key, key.length);
        this.sign = Arrays.copyOf(sign, sign.length);
    }

    public void setKey(byte[] key) {
        this.key = Arrays.copyOf(key, key.length);
    }
    
    public void setSign(byte[] sign) {
        this.sign = Arrays.copyOf(sign, sign.length);
    }

    public byte[] getKey() {
        return key;
    }
    
    public byte[] getSign() {
        return sign;
    }
}

class StartVoting extends Message {
    
    private byte[] random = new byte[RANDBYTES_SIZE];
    
    public StartVoting(byte[] random) {
        super(Message.START_VOTE);
        System.arraycopy(random, 0, this.random, 0, RANDBYTES_SIZE);
    }
    
    public void setBytes(byte[] random) {
        System.arraycopy(random, 0, this.random, 0, RANDBYTES_SIZE);
    }
    
    public byte[] getBytes() {
        return random;
    }
}

class Vote extends Message {

    private byte[] vote;
    private byte[] signature;
    
    public Vote(byte[] vote, byte[] signature) {
        super(Message.VOTE);
        this.vote = vote;
        this.signature = signature;
    }

    public byte[] getVote() {
        return vote;
    }
    
    public byte[] getSignature() {
        return signature;
    }
}

class VoteResult extends Message {

    private ArrayList<Principal> voted;
    private ArrayList<Principal> notvoted;
    private ArrayList<ArrayList<Map<String, String>>> votingPr;
    static final String prName = "principalName";
    
    private int result;
       
    public VoteResult(int result, ArrayList<Principal> voted, ArrayList<Principal> notvoted) {
        super(VOTE_RES);
        this.result = result;
        this.voted = new ArrayList<Principal>(voted);
        this.notvoted = new ArrayList<Principal>(notvoted);
        votingPr = new ArrayList<ArrayList<Map<String, String>>>();
        ArrayList<Map<String, String>> temp = new ArrayList<Map<String, String>>();
        Map<String, String> m;
	int i;
	for (i = 0; i < voted.size(); i++) {
	    m = new HashMap<String, String>();
	    m.put(prName, voted.get(i).toString());
	    temp.add(m);
	}
	votingPr.add(temp);
	temp = new ArrayList<Map<String, String>>();
	for (i = 0; i < notvoted.size(); i++) {
	    m = new HashMap<String, String>();
	    m.put(prName, notvoted.get(i).toString());
	    temp.add(m);
	}
	votingPr.add(temp);
    }
    
    public int getYes() {
        return result;
    }
    
    public int getNo() {
        return votingPr.get(0).size() - result;
    }
    
    public ArrayList<Principal> getVoted() {
        return voted;
    }
    
    public ArrayList<Principal> getNotVoted() {
        return notvoted;
    }
    
    public ArrayList<ArrayList<Map<String, String>>> getAll() {
	return votingPr;
    }
}

class Error extends Message {

    /*
     * Types of errors
     */
    public static final byte HANDSHAKE_ERROR = 0x0, DATABASE_ERROR = 0x1, REGISTRATION_ERROR = 0x2, DOUBLE_CONNECTION = 0x3,
            BAN_ERROR = 0x4, DECRYPT_ERROR = 0x5, INVALID_SIGNATURE = 0x6, CREATE_ERROR = 0x7, JOIN_ERROR = 0x8, TRUST_LOWERED = 0x9,
            UNKNOWN_ERROR = 0x10, CORRECT = (byte) 0xFF;
    private byte errorType;

    public Error() {
        super(Message.ERROR);
        this.errorType = Error.UNKNOWN_ERROR;
    }

    public Error(byte type) {
        super(Message.ERROR);
        if ((type < Error.HANDSHAKE_ERROR) || (type >= Error.UNKNOWN_ERROR)) {
            this.errorType = Error.UNKNOWN_ERROR;
        } else {
            this.errorType = type;
        }
    }

    public void setErrType(byte type) {
        if ((type < Error.HANDSHAKE_ERROR) || (type >= Error.UNKNOWN_ERROR)) {
            this.errorType = Error.UNKNOWN_ERROR;
        } else {
            this.errorType = type;
        }
    }

    public byte getErrType() {
        return this.errorType;
    }
}