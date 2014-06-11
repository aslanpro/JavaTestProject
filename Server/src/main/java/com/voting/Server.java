package com.voting;

import com.voting.Server.ClientThread;
import com.voting.conference.Conference;
import com.voting.conference.Principal;
import com.voting.crypto.AESCipher;
import com.voting.crypto.RSA;
import com.voting.crypto.RSAGen;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.KeyGenerator;
import com.voting.database.DataBaseDriver;
import com.voting.database.DataBaseDriverFactory;
import com.voting.database.User;


public class Server {

    private Map<Integer, Conference> mapConf;
    private int idCount;                        //conference ID; number of conferences
    private Map<Integer, VoteConference> conferences; //Functional conferences
    private ArrayList<ClientThread> servClients;       //All clients
    
    private static int PORT = 7070;     //port for communication
    private boolean keepWaiting;   
    
    private SecureRandom secRand;
    private RSA rsa;
    private static final int RANDOM_SIZE = 28;  //size of random bytes for handshake
    public static final int ID_SIZE = 16;       //size of client id
    private KeyGenerator kGen;                  //AES-key generetor
    
    /*
     * For data base connection:
     */
    private static final String USER = "root";
    private static final String PASWORD = "root";
    private static final String URL = "jdbc:mysql://localhost:3306/keyDB";

	/*
     * For NOSQL database connection:
     */
	private DataBaseDriver db;
    
    /*
     * For email validation:
     */
    private Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    private Matcher matcher;
    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    
    /*
     * Different timeouts:
     */
    private final int CONFIRM_TIMEOUT = 60;     //time for confirm operation in seconds
    private final int REMOVE_TIMEOUT = 3;       //time for bulk remove in seconds
    private final int REKEY_TIMEOUT = 2;        //time for rekeying in seconds
    private final int START_TIMEOUT = 10;       //time for starting voting in seconds
    private final int VOTING_TIMEOUT = 20;      //time for voting in seconds
    
    /*
     * Constants for vote protocol
     */
    private static final int BULLETIN = 33;	//size of bulletin in byte with position-bit
    private static final int VOTE_SIZE = 48;	//all size of vote message in bytes
    private static final int BULL_POS = VOTE_SIZE - BULLETIN - 2; //position from which starts bulletin
    private static final byte HEADER = (byte) 0xFB;
    private static final byte END = (byte) 0xED;

    public Server() {
        try {
            kGen = KeyGenerator.getInstance("AES");
            kGen.init(128);
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("No such algorithm");
        }        
        this.servClients = new ArrayList<ClientThread>();
        conferences = new HashMap<Integer, VoteConference>();
        mapConf = new HashMap<Integer, Conference>();
        this.keepWaiting = false;
        secRand = new SecureRandom();
        try {
            BufferedInputStream input = new BufferedInputStream(new FileInputStream("/home/ubuntu/serverdir/server_key.k"));
            byte[] nKey = new byte[128];
            byte[] dKey;
            try {
                input.read(nKey);
                dKey = new byte[input.available()];
                input.read(dKey);
                input.close();
                rsa = new RSA(dKey, nKey, nKey, this.secRand);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Form input. Generating RSA");
            RSAGen g = new RSAGen();
            rsa = new RSA(g.getPrivateKey(), g.getN(), g.getN(), this.secRand);
            try {
                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream("/home/ubuntu/serverdir/server_key.k"));
                try {
                    output.write(g.getN());
                    output.write(g.getPrivateKey());
                    output.flush();
                    output.close();
                } catch (IOException ex1) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } catch (FileNotFoundException ex1) {
                System.err.println("Form output. File must by created");
            }
        }
		db = DataBaseDriverFactory.getDriver("/home/ubuntu/serverdir/server.conf");
		if (db == null) {
			System.err.println("NoSql database is not configured. Aborting");
			System.exit(0);
		}
    }

    /*
     * Check e-mail by using regular expression
     */
    public boolean validateMail(final String mail) {
        matcher = pattern.matcher(mail);
        return matcher.matches();
    }

    /*
     * Start server
     */
    public void start() {
		if(!db.connect()) {
			System.err.println("NoSql database connection. Aborting");
			System.exit(0);;
		}
        idCount = 0;
        keepWaiting = true;
        try {
            ServerSocket sSocket = new ServerSocket(PORT);
            String s = "Server started at: " + new Date();
            System.out.println(s);
            while (keepWaiting) {
                Socket socket = sSocket.accept();
                if (!keepWaiting) {
                    break;
                }
                ClientThread cT = new ClientThread(socket);
                servClients.add(cT);
                cT.start();
            }
            s = "Server stoped at: " + new Date();
            System.out.println(s);
            sSocket.close();
            int size = servClients.size();
            for (int i = 0; i < size; i++) {
                ClientThread cT = servClients.get(0);
                cT.close();
            }
            mapConf.clear();
            conferences.clear();
        } catch (IOException ex) {
            System.out.println("Error: " + ex);
        }
    }

    /*
     * Stop server by connecting to itself
     */
    public void stop() {
        keepWaiting = false;
        try {
            Socket stopSocket = new Socket("localhost", PORT);
            stopSocket.close();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean getStatus() {
        return this.keepWaiting;
    }

    synchronized private boolean createConf(String question, ClientThread cT) {
        if (question != null) {
            idCount++;
            Conference c = new Conference(question, cT.pr, idCount);
            conferences.put(idCount, new VoteConference(cT));
            mapConf.put(idCount, c);
            cT.confId = idCount;
            conferences.get(idCount).c = c;
            return true;
        }
        return false;
    }
    
    /*
     * Change trust level to worse
     */
    private void banClient(byte[] id) {
		User user = db.getUser(id);
		if(user != null) {
			if (user.getTrust().equals("high")) {
				db.setTrustById("middle", id);
			} else {
				db.setTrustById("ban", id);
			}
		}
        /*Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(URL, USER, PASWORD);
            String query = "SELECT * FROM keytable WHERE id = (?);";
            PreparedStatement pstmt = (PreparedStatement) conn.prepareStatement(query);
            pstmt.setBytes(1, id);
            ResultSet r = (ResultSet) pstmt.executeQuery();
            if (r.first()) {
                //If there is more then one result row
                if (r.next()) {
                    conn.close();
                    System.err.println("Amount of rows more then one!!!");
                }
                r.first();
                if (r.getString("trust").equals("high")) {
                    query = "UPDATE keytable SET trust = 'middle' WHERE id = (?);";
                } else {
                    query = "UPDATE keytable SET trust = 'ban' WHERE id = (?);";
                }
                pstmt = (PreparedStatement) conn.prepareStatement(query);
                pstmt.setBytes(1, id);
                pstmt.executeUpdate();
            }
            conn.close();
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex1) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }*/
    }
    
    private Error joinConf(int id, ClientThread cT) {
        if (!conferences.containsKey(id)) {
            return new Error(Error.JOIN_ERROR);
        }
        synchronized (conferences.get(id).l) {
            System.out.println("Client " + cT.pr.getName() + "catch lock!!");
            VoteConference vC = conferences.get(id);
            synchronized (vC) {
                if ((!vC.isClosed)&&(!vC.ignorList.contains(cT.pr))) {
                    ConfirmMember message = new ConfirmMember(cT.pr.toString());
                    EncryptedMessage eM = new EncryptedMessage();
                    eM.setS0(vC.aes.generateS0());
                    eM.setMessage(vC.aes.encryptMessage(message, eM.getS0()));
                    eM.setConfirm(true);
                    vC.multicast(eM);
                    try {
                        vC.wait(CONFIRM_TIMEOUT * 1000);
                        if (!vC.returnConfirms()) {
                            if (vC.clients.isEmpty()) {
                                return new Error(Error.JOIN_ERROR);
                            }
                            vC.ignorList.add(cT.pr);
                            banClient(cT.pr.getID());
                            return new Error(Error.TRUST_LOWERED);
                        }
                        vC.clients.add(cT);
                        cT.confId = id;
                        mapConf.get(id).addMember(cT.pr);
                        reKeyOperation(id);
                        System.out.println("Client " + cT.pr.getName() + "relae lock!!");
                        return null;
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        return new Error(Error.JOIN_ERROR);
                    }
                }
            }
        }
        return new Error(Error.JOIN_ERROR);
    }
    
    private void reKeyOperation(int id) {
        synchronized (conferences.get(id).l) {
            byte[] key;
            key = kGen.generateKey().getEncoded();
            System.out.println("Conference " + id + ":\n\t Key = " + Arrays.toString(key));
            conferences.get(id).reKey(key);
            try {
                TimeUnit.MILLISECONDS.sleep(REKEY_TIMEOUT * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            conferences.get(id).multicast(new ShowState(conferences.get(id).c));
        }
    }
    
    /*
     * Start voring operation
     */
    private void startOperation(final int id) {
        final VoteConference vC = conferences.get(id);
        if (vC.start) {
            vC.startPoints++;
        } else {
            vC.start = true;
            vC.startPoints = 1;
            final Timer t = new Timer();
            t.schedule(new TimerTask() {

                public void run() {
                    synchronized (vC.l) {
                        if (vC.startPoints == vC.clients.size()) {
                            vC.startVoting();
                            System.out.println("Conference " + id + ":\n\t" + "Voting started");
                        } else {
                            vC.start = false;
                        }
                    }
                    t.cancel();
                }
            }, START_TIMEOUT * 1000);
        }
    }

    private byte[] giveCert(byte[] id, byte[] pKey) {
        byte[] cer = concat(id, pKey);
        return rsa.sign(cer);
    }

    private boolean verifyCert(byte[] id, byte[] pKey, byte[] cert) {
        byte[] unsignedCer = concat(id, pKey);
        return rsa.verify(unsignedCer, cert);
    }
    
    //transform UUID class to byte array
    private byte[] uuidToByte(UUID id) {
        byte[] byteID = new byte[ID_SIZE];
        long temp = id.getMostSignificantBits();
        byteID[0] = (byte) ((temp >> 56) & 0xFF);
        byteID[1] = (byte) ((temp >> 48) & 0xFF);
        byteID[2] = (byte) ((temp >> 40) & 0xFF);
        byteID[3] = (byte) ((temp >> 32) & 0xFF);
        byteID[4] = (byte) ((temp >> 24) & 0xFF);
        byteID[5] = (byte) ((temp >> 16) & 0xFF);
        byteID[6] = (byte) ((temp >> 8) & 0xFF);
        byteID[7] = (byte) (temp & 0xFF);
        temp = id.getLeastSignificantBits();
        byteID[8] = (byte) ((temp >> 56) & 0xFF);
        byteID[9] = (byte) ((temp >> 48) & 0xFF);
        byteID[10] = (byte) ((temp >> 40) & 0xFF);
        byteID[11] = (byte) ((temp >> 32) & 0xFF);
        byteID[12] = (byte) ((temp >> 24) & 0xFF);
        byteID[13] = (byte) ((temp >> 16) & 0xFF);
        byteID[14] = (byte) ((temp >> 8) & 0xFF);
        byteID[15] = (byte) (temp & 0xFF);
        return byteID;
    }
    
    /*
     * Returns collection of available conferences
     */
    private Collection<Conference> getConference() {
        return this.mapConf.values();
    }
    
    /*
     * Function for adding two arrays in one
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

    /*
     * Cheks client information in data base
     */
    private Error acceptClient(ClientThread cT, byte[] id) {
        for (int i = 0; i < servClients.size(); i++) {
            if (servClients.get(i).accepted) {
                if (Arrays.equals(servClients.get(i).pr.getID(), id)) {
                    return new Error(Error.DOUBLE_CONNECTION);
                }
            }
        }
		User user = db.getUser(id);
		if(user != null) {
			if (!user.getTrust().equals("ban")) {
				cT.pr.setName(user.getName());
                cT.pr.setEmail(user.getEmail());
                cT.setKey(user.getPrK());
                cT.pr.setID(id);
                cT.pr.setTrust(user.getTrust().equals("middle"));
                cT.accepted = true;
                System.out.println("Client " + cT.pr.getName() + " connected");
                return null;
			} else {
				System.out.println("Clien " + cT.pr.getName() + " is banned");
                return new Error(Error.BAN_ERROR);
			}
		} else {
			System.err.println("Can't find user in db!!!");
			return new Error(Error.DATABASE_ERROR);
		}
        /*Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(URL, USER, PASWORD);
            String query = "SELECT * FROM keytable WHERE id = (?);";
            PreparedStatement pstmt = (PreparedStatement) conn.prepareStatement(query);
            pstmt.setBytes(1, id);
            ResultSet r = (ResultSet) pstmt.executeQuery();
            if (r.first()) {
                //If there is more then one result row
                if (r.next()) {
                    conn.close();
                    System.err.println("Amount of rows more then one!!!");
                    return new Error(Error.DATABASE_ERROR);
                }
                r.first();
                if (!r.getString("trust").equals("ban")) {
                    cT.pr.setName(r.getString("name"));
                    cT.pr.setEmail(r.getString("email"));
                    cT.setKey(r.getBytes("p_key"));
                    cT.pr.setID(id);
                    cT.pr.setTrust(r.getString("trust").equals("middle"));
                    cT.accepted = true;
                    System.out.println("Client " + cT.pr.getName() + " connected");
                    conn.close();
                    return null;
                } else {
                    conn.close();
                    System.out.println("Clien " + cT.pr.getName() + " is banned");
                    return new Error(Error.BAN_ERROR);
                }
            }
            conn.close();
            System.out.println("Database error");
            return new Error(Error.DATABASE_ERROR);
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex1) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            return new Error(Error.DATABASE_ERROR);
        }*/
    }
    
    /*
     * Registrating client key in data base
     */
    private Information registration(Registration r, byte[] key) {
        if ((r.getName() == null) || (r.getEmail() == null)) {
            return null;
        }
        if (!validateMail(r.getEmail())) {
            return null;
        }
        UUID id = UUID.randomUUID();
        byte[] idBytes = uuidToByte(id);
        byte[] cert;
		if (db.setBanByPrK(key)) {
			cert = giveCert(idBytes, key);
			User user = new User(idBytes, key, r.getName(), r.getEmail(), cert);
			if (db.insertNewUser(user)) {
				System.out.println("Client " + r.getName() + " registrated. Email = " + r.getEmail());
				return new Information(idBytes, key, cert);
			} else {
				return null;
			}
		} else {
			return null;
		}
        /*Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(URL, USER, PASWORD);
            String query = "UPDATE keytable SET trust = 'ban' WHERE p_key = (?);";
            PreparedStatement pstmt = (PreparedStatement) conn.prepareStatement(query);
            pstmt.setBytes(1, key);
            pstmt.executeUpdate();
            query = "INSERT INTO keytable (id, p_key, name, email, cert) VALUES (?, ?, ?, ?, ?)";
            pstmt = (PreparedStatement) conn.prepareStatement(query);
            pstmt.setBytes(1, idBytes);
            pstmt.setBytes(2, key);
            pstmt.setString(3, r.getName());
            pstmt.setString(4, r.getEmail());
            cert = giveCert(idBytes, key);
            pstmt.setBytes(5, cert);
            pstmt.executeUpdate();
            System.out.println("Client " + r.getName() + " registrated. Email = " + r.getEmail());
            conn.close();
            return new Information(idBytes, key, cert);
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex1) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            return null;
        }*/
    }

    /*
     * Small handshake for checking owner of the key
     */
    private boolean handshake(ClientThread ct) {
        byte[] randS = new byte[RANDOM_SIZE];   //Server random bytes for handshake protocol
        byte[] randC;                           //Client random bytes for handshake protocol
        byte[] clientSign;                      //Signature of {randS, randC}
        secRand.nextBytes(randS);
        Information info;
        try {
            ct.output.writeObject(randS);
            info = (Information) ct.input.readObject();
            randC = (byte[]) ct.input.readObject();
            clientSign = (byte[]) ct.input.readObject();
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        byte[] clientKey = info.getKey();
        if (clientKey == null) {
            return false;
        }
        byte[] message = concat(randS, randC);
        boolean verResult = rsa.verifyKey(message, clientSign, clientKey);
        if (!verResult) {
            //Key is not correspond
            return false;
        }
        //Key is corresponded
        byte[] cert = info.getCert();
        byte[] id = info.getID();
        if ((cert != null) && (id != null)) {       //If there is a certificate. Start client acception
            //Check is certificate signed by server
            if (verifyCert(id, clientKey, cert)) {
                //Certificate is signed by server
                Error er = acceptClient(ct, id);
                if (er != null) {
                    ct.send(er);
                    return false;
                }
                return true;
            } else {
                //Certificate is not signed by server
                Error er = new Error(Error.INVALID_SIGNATURE);
                ct.send(er);
                return false;
            }
        } else if ((cert == null) && (id == null)) { //If there is not certificate. Start registration
            Registration reg = new Registration();
            try {
                if (ct.send(reg)) {
                    reg = (Registration) ct.input.readObject();
                    info = registration(reg, clientKey);
                    if (info == null) {
                        Error er = new Error(Error.REGISTRATION_ERROR);
                        ct.send(er);
                        return false;
                    }
                    ct.pr.setID(info.getID());
                    ct.pr.setName(reg.getName());
                    ct.pr.setEmail(reg.getEmail());
                    ct.setKey(clientKey);
                    return ct.send(info);
                } else {
                    return false;
                }
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } else {
            return false;
        }
    }

    /*
     * Class for clients which work in new thread
     */
    class ClientThread extends Thread {

        private Socket threadSocket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        
        private boolean accepted = false;;
        private boolean keepGoing;
        private byte[] keyRSA;
        private Integer confId = null;
        private Principal pr;

        public ClientThread(Socket socket) {
            this.threadSocket = socket;
            try {
                output = new ObjectOutputStream(threadSocket.getOutputStream());
                input = new ObjectInputStream(threadSocket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            pr = new Principal();
        }

        public void run() {
            if (!handshake(this)) {
                Error err = new Error(Error.HANDSHAKE_ERROR);
                send(err);
                close();
                return;
            }
            ShowConf conf = new ShowConf(getConference());
            try {
                output.writeObject(conf);
                keepGoing = true;
                Message m;
                while (keepGoing) {
                    m = (Message) input.readObject();
                    switch (m.getType()) {
                        case Message.SHOW_STATE:
                            int id = ((ShowState) m).getId();
                            if (mapConf.containsKey(id)) {
                                synchronized (conferences.get(id).l) {
                                    output.writeObject(new ShowState(mapConf.get(id)));
                                    /*m = (Message) input.readObject();
                                    switch (m.getType()) {
                                        case Message.SHOW_CONF:
                                            conf = new ShowConf(getConference());
                                            output.writeObject(conf);
                                            break;
                                        case Message.JOIN_CONF:
                                            Error er = joinConf(((JoinConf) m).getId(), this);
                                            if (er != null) {
                                                System.out.println("Joing Error!" + " By " + pr.getName());
                                                serverUI.setLog("Joing Error!" + " By " + pr.getName());
                                                output.writeObject(er);
                                                return;
                                            }
                                            System.out.println("Conference " + confId + ":\n\t" + pr.getName() + " joined");
                                            serverUI.setLog("Conference " + confId + ":\n\t" + pr.getName() + " joined");
                                            keepGoing = false;
                                            break;
                                        default:
                                            System.out.println("Incorrect protocol! " + pr.getName());
                                            serverUI.setLog("Incorrect protocol! " + pr.getName());
                                            return;
                                    }*/
                                }
                            } else {
                                System.out.println("Showing deleted state!");
                                output.writeObject(new ShowState());
                            }
                            break;
                        case Message.CREATE_CONF:
                            if (createConf(((CreateConf) m).getQuestion(), this)) {
                                System.out.println("Conference " + confId+ ":\n\t" + ((CreateConf) m).getQuestion()
                                        + "\n\tcreated by " + pr.getName());
                                reKeyOperation(this.confId);
                            } else {
                                System.out.println("Creating Error!" + " By " + pr.getName());
                                Error er = new Error(Error.CREATE_ERROR);
                                output.writeObject(er);
                                return;
                            }
                            keepGoing = false;
                            break;
                        case Message.JOIN_CONF:
                            Error er = joinConf(((JoinConf) m).getId(), this);
                            if (er != null) {
                                System.out.println("Joing Error!" + " By " + pr.getName());
                                output.writeObject(er);
                                return;
                            }
                            System.out.println("Conference " + confId + ":\n\t" + pr.getName() +" joined");
                            keepGoing = false;
                            break;
                        case Message.SHOW_CONF:
                            conf = new ShowConf(getConference());
                            output.writeObject(conf);
                            break;
                        default:
                            System.out.println("Incorrect protocol! " + pr.getName());
                            return;
                    }
                }
                keepGoing = true;
                while (keepGoing) {
                    m = (Message) input.readObject();
                    switch (m.getType()) {
                        case Message.ENCRYPTED:
                            if (((EncryptedMessage) m).getCorrect()) {
                                m = conferences.get(confId).decryptMessage((EncryptedMessage) m);
                                switch (m.getType()) {
                                    case Message.CONFIRM_MEM:
                                        conferences.get(confId).confirmOperation(((ConfirmMember) m).getConfirm(), this.pr);
                                        break;
                                    case Message.ERROR:
                                        System.out.println(pr + " caused decryption error!");
                                        return;
                                    case Message.VOTE:
                                        conferences.get(confId).tailer((Vote) m, this);
                                        keepGoing = false;
                                        synchronized (conferences.get(confId)) {
                                            conferences.get(confId).wait(VOTING_TIMEOUT * 1000);
                                            output.writeObject(new Message(Message.EXIT_CONF));
                                        }
                                        break;
                                    default:
                                        System.out.println("Incorrect protocol! " + pr.getName());
                                        return;
                                }
                            } else {
                                System.out.println(pr + " caused decryption error!");
                                return;
                            }
                            break;
                        case Message.START_VOTE:
                            startOperation(confId);
                            break;
                        case Message.EXIT_CONF:
                            output.writeObject(m);
                            keepGoing = false;
                            break;
                        case Message.VOTE:
                            keepGoing = false;
                            synchronized (conferences.get(confId)) {
                                conferences.get(confId).wait(VOTING_TIMEOUT * 1000);
                                output.writeObject(new Message(Message.EXIT_CONF));
                            }
                            break;
                        default:
                            System.out.println("Incorrect protocol! " + pr.getName());
                            keepGoing = false;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("I am fall out " + pr);
            } finally {
                close();
                System.out.println("End of connection " + pr.getName());
            }
            System.out.println("Success end of connection" + pr.getName());
        }

        private void close() {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (threadSocket != null) {
                try {
                    this.threadSocket.close();
                } catch (IOException e) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (confId != null) {
                mapConf.get(confId).removeMember(pr);
                if (mapConf.get(confId).getPrincipals().isEmpty()) {
                    mapConf.remove(confId);
                }
                conferences.get(confId).removeBulk(this);
                if (conferences.containsKey(confId)) {
                    if (conferences.get(confId).clients.isEmpty()) {
                        conferences.remove(confId);
                        System.out.println("Conference " + confId+ " deleted");
                    }
                }
            }
            servClients.remove(this);
        }

        public boolean send(Message msg) {
            try {
                this.output.writeObject(msg);
                return true;
            } catch (IOException e) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
                return false;
            }
        }

        public void setKey(byte[] key) {
            this.keyRSA = key;
        }
        
        @Override
        public boolean equals(Object cT) {
            if (cT instanceof ClientThread) {
                return ((ClientThread) cT).pr.equals(this.pr);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + (this.pr != null ? this.pr.hashCode() : 0);
            return hash;
        }

    }
    
    /*
     * Class for synchronized access to conference
     */
    class LockClass {

        public LockClass() {
        }
    }
    
    /*
     * Class for functional Conference
     */
    class VoteConference {

        private ArrayList<ClientThread> clients;
        private ArrayList<Principal> prList;
        private ArrayList<Principal> alreadyVoted;
        private Set<Principal> ignorList;
        
        /*
         * Fields for confirm operation
         */
        private int confirms;
        private boolean confirmMem = false;
        private boolean confirmOperation = false;
        private int done;
        
        /*
         * Fields for voting
         */
        private int startPoints;
        private boolean start = false;
        private boolean isClosed = false;
        private boolean isVoting = false;
        private byte[] voteMask;
        private int result = 0;
        private final Timer voteTime; 
        
        /*
         * Fields for changing key
         */
        private int keychange = 0;
        boolean keychangeflag = false;
        
        private AESCipher aes;
        
        private final LockClass l;
        private Conference c;
        
        public VoteConference(ClientThread cT) {
            clients = new ArrayList<ClientThread>();
            prList = new ArrayList<Principal>();
            ignorList = new HashSet<Principal>();
            clients.add(cT);
            l = new LockClass();
            aes = new AESCipher(secRand);
            voteTime = new Timer();
        }
        
        /*
         * Bulk remove operation.
         */
        public void removeBulk(ClientThread cT) {
            removeClient(cT);
            int state = ++keychange;
            try {
                TimeUnit.MILLISECONDS.sleep(REMOVE_TIMEOUT*1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Thread error!");
            }
            state -= keychange--;
            if ((!clients.isEmpty())&&(!isVoting)) {
                if ((state == 0) || (keychange == 0)) {
                    reKeyOperation(cT.confId);
                    keychange = 0;
                }
            }
        }
        
        synchronized public void removeClient(ClientThread cT) {
            clients.remove(cT);
            if (confirmOperation) {
                done--;
            }
        }
        
        synchronized private void multicast(Message m) {
            if (m.getType() == Message.ENCRYPTED) {
                if (((EncryptedMessage) m).getConfirm()) {
                    prList.clear();
                    confirmOperation = true;
                    done = clients.size();
                    confirms = 0;
                }
            }
            int size = clients.size();
            for (int i = 0; i < size; i++) {
                clients.get(i).send(m);
            }
        }

        synchronized private void reKey(byte[] key) {
            byte[] encrKey;
            KeyInside message = new KeyInside();
            int size = clients.size();
            for (int i = 0; i < size; i++) {
                encrKey = rsa.keyEncrypt(key, clients.get(i).keyRSA);
                message.setKey(encrKey);
                message.setSign(rsa.sign(encrKey));
                clients.get(i).send(message);
            }
            aes.setKey(key);
        }

        private boolean returnConfirms() {
            confirmOperation = false;
            if (confirms > 0) {
                return true;
            } else if (confirms < 0) {
                return false;
            } else {
                return confirmMem;
            }
        }

        synchronized private void confirmOperation(boolean confirm, Principal p) {
            if ((confirmOperation)&&(!prList.contains(p))) {
                prList.add(p);
                if (confirm) {
                    confirms++;
                } else {
                    confirms--;
                }
                if (clients.get(0).pr.equals(p)) {
                    confirmMem = true;
                }
                done--;
                if (done <= 0) {
                    notifyAll();
                    confirmOperation = false;
                }
            }
        }
        
        synchronized private void tailer(Vote v, ClientThread cT) {
            if (isVoting) {
                System.out.println("Voting!!!!");
                if (prList.contains(cT.pr)) {
                    prList.remove(cT.pr);
                    if (checkVote(v, cT)) {
                        int pos = voteMask[BULL_POS] & 0xFF;
                        byte mask = (byte) ((1 << (pos % 8)) & 0xFF);
                        mask &= voteMask[BULL_POS + 1 + (pos / 8)];
                        if (mask != 0) {
                            result++;
                        }
                        alreadyVoted.add(cT.pr);
                    }
                } else {
                    banClient(cT.pr.getID());
                    cT.send(new Error(Error.TRUST_LOWERED));
                    cT.close();
                }
            }
        }
        
        private boolean checkVote(Vote v, ClientThread cT) {
            if ((v.getVote() != null) && (v.getSignature() != null)) {
                if (rsa.verifyKey(v.getVote(), v.getSignature(), cT.keyRSA)) {
                    byte[] vote = rsa.decrypt(v.getVote());
                    System.arraycopy(vote, BULL_POS, voteMask, BULL_POS, BULLETIN);
                    return Arrays.equals(vote, voteMask);
                }
            }
            return false;
        }
        
        synchronized private void startVoting() {
             /*
	     * voteMask = 0xFB || Conference ID || Server Random bytes ||
	     *  	|| 0 [1] || 0 [32] || 0xFB || 0xED
	     *  || - is a concat operation
	     */
            isClosed = true;
            alreadyVoted = new ArrayList<Principal>();      //list of principals, that already voted
            byte[] rand = new byte[Message.RANDBYTES_SIZE];
            secRand.nextBytes(rand);
            voteMask = new byte[VOTE_SIZE];
            Arrays.fill(voteMask, (byte) 0);
            voteMask[0] = HEADER;
            int id = this.c.getId();
            voteMask[1] = (byte) ((id >> 24) & (byte) 0xFF);
            voteMask[2] = (byte) ((id >> 16) & (byte) 0xFF);
            voteMask[3] = (byte) ((id >> 8) & (byte) 0xFF);
            voteMask[4] = (byte) (id & (byte) 0xFF);
            int i;
            for (i = 0; i < rand.length; i++) {
                voteMask[5 + i] = rand[i];
            }
            voteMask[VOTE_SIZE - 2] = HEADER;
            voteMask[VOTE_SIZE - 1] = END;
            multicast(new StartVoting(rand));
            prList.clear();
            for (i = 0; i < clients.size(); i++) {
                prList.add(clients.get(i).pr);
            }
            isVoting = true;
            voteTime.schedule(new VoteThread(this), VOTING_TIMEOUT * 1000);
        }
        
        private Message decryptMessage(EncryptedMessage eM) {
            return aes.decryptMessage(eM.getMessage(), eM.getS0());
        }
        
        private class VoteThread extends TimerTask {
            
            private final VoteConference vC;
            
            public VoteThread(VoteConference vC) {
                this.vC = vC;
            }
            
            @Override
            public void run() {
                System.out.println("Conference " + vC.c.getId() + ":\n\t" + "Voting ended");
                vC.isVoting = false;
                synchronized (vC) {
                    VoteResult vR = new VoteResult(result, alreadyVoted, prList);
                    EncryptedMessage eM = new EncryptedMessage();
                    eM.setS0(aes.generateS0());
                    eM.setMessage(aes.encryptMessage(vR, eM.getS0()));
                    multicast(eM);
                    vC.notifyAll();
                }
                vC.voteTime.cancel();
            }
        }
    }
    
    public static void main(String args[]) {
        Server server = new Server();
        Runtime.getRuntime().addShutdownHook(new ShutDownThread(server));
        server.start();
    }
    
    static class ShutDownThread extends Thread {
        
        private Server server;
        
        public ShutDownThread(Server s) {
            this.server = s;
        }
        
        @Override
        public void run() {
            if (this.server.getStatus()) {
                this.server.stop();
            }
        }
    }
}