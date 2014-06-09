package com.voting.crypto;

import com.voting.Message;
import com.voting.conference.Conference;
import com.voting.conference.Principal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class AESCipher {

    /**
     * @param args
     */
    static {
        System.load("/usr/lib/libAES.so");
    }
    private SecureRandom sRand;
    private byte[] key = new byte[SIZE];
    private int[] roundKey;
    public static final int SIZE = 16;

    public AESCipher(SecureRandom sRand) {
        this.sRand = sRand;
    }

    public AESCipher(byte[] key, SecureRandom sRand) {
	this.sRand = sRand;
	System.arraycopy(key, 0, this.key, 0, SIZE);
	this.roundKey = AESCipher.keyExpansion(this.key);
    }

    public void setKey(byte[] key) {
	System.arraycopy(key, 0, this.key, 0, SIZE);
	this.roundKey = AESCipher.keyExpansion(this.key);
    }

    public byte[] generateS0() {
        byte[] result = new byte[SIZE];
        this.sRand.nextBytes(result);
        return result;
    }

    private byte[] encrypt(byte[] oText, byte[] s0) {
	byte[] s0Copy = new byte[SIZE];
	System.arraycopy(s0, 0, s0Copy, 0, SIZE);
        return crypt(oText, this.roundKey, s0Copy);
    }
    
    private byte[] decrypt(byte[] cText, byte[] s0) {
	byte[] s0Copy = new byte[SIZE];
	System.arraycopy(s0, 0, s0Copy, 0, SIZE);
        return crypt(cText, this.roundKey, s0Copy);
    }
    
    public byte[] encryptMessage(Message message, byte[] s0) {
        byte[] mes = serialization(message);
        return encrypt(mes, s0);
    }

    public Message decryptMessage(byte[] cipher, byte[] s0) {
        byte[] mes = decrypt(cipher, s0);
        return deserialization(mes);
    }
   
    private byte[] serialization (Message message) {
        byte[] bytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.close();
            bytes = bos.toByteArray();
        } catch (Exception ex) {
            return null;
        }
        return bytes;
    }
    
    private Message deserialization (byte[] bytes) {
        Message message;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(in);
            message = (Message) ois.readObject();
            ois.close();
        } catch (Exception ex) {
            return new Message(Message.ERROR);
        }
        return message;
    }
    
    public static void main(String[] args) {
        Map<Integer, Conference> map = new HashMap<Integer, Conference>();
        ArrayList<Conference> array;
        byte[] id = new byte[SIZE];
        Principal p1 = new Principal("P1", "P1", id, false);
        Principal p2 = new Principal("P1", "P1", id, false);
        Principal p3 = new Principal("P1", "P1", id, false);
        Conference c1 = new Conference("C1", p1, 1);
        Conference c2 = new Conference("C1", p2, 2);
        map.put(0, c1);
        map.put(1, c2);
        array = new ArrayList<Conference>(map.values());
        for (int i=0; i<array.size(); i++) {
            for (int j=0; j<array.get(i).getPrincipals().size(); j++) {
                System.out.println(array.get(i).getPrincipals().get(j));
            }
        }
        System.out.println();
        map.get(0).addMember(p3);
        for (int i=0; i<array.size(); i++) {
            for (int j=0; j<array.get(i).getPrincipals().size(); j++) {
                System.out.println(array.get(i).getPrincipals().get(j));
            }
        }
        System.out.println();
	SecureRandom sR = new SecureRandom();
	AESCipher a = new AESCipher(sR);
	byte[] S0 = a.generateS0();
	KeyGenerator kGen;
        Message cM, eM, cM2;
	try {
	    kGen = KeyGenerator.getInstance("AES");
	    kGen.init(128);
	    SecretKey key = kGen.generateKey();
	    a.setKey(key.getEncoded());
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	}

	int length = 100;
	int iterations = 100000;
	byte[] testBytes1;// = new byte[length];
	byte[] testBytes2 = new byte[length];
	byte[] testBytes3 = new byte[length];
	double[] time = new double[iterations];
	// int j;
	for (int i = 0; i < iterations; i++) {
	    //a.sRand.nextBytes(testBytes1);
            cM = new Message(Message.ENCRYPTED);
            S0 = a.generateS0();

	    //time[i] = -System.nanoTime();
	    testBytes1 = a.encryptMessage(cM, S0);

	    cM2 = a.decryptMessage(testBytes1, S0);
	    System.out.println("iteration " + i + "; " + Byte.toString(cM.getType())+ " and " + Byte.toString(cM2.getType()));
	    //time[i] += System.nanoTime();

	    //time[i] /= Math.pow(10, 9);
	}
	/*double sum = 0;
	for (int i = 0; i < iterations; i++)
	    sum += time[i];
	System.out.println(sum / iterations);*/
	System.out.println();
    }

    public static native int[] keyExpansion(byte[] key);

    public static native byte[] crypt(byte[] text, int[] r_key, byte[] S0);
}