package com.voting.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.voting.Message;

public class AESCipher {

    /**
     * @param args
     */
    static {
	System.loadLibrary("AES");
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
   
    private byte[] serialization(Message message) {
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
    
    private Message deserialization(byte[] bytes) {
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
	SecureRandom sR = new SecureRandom();
	AESCipher a = new AESCipher(sR);
	byte[] S0 = a.generateS0();
	KeyGenerator kGen;
	try {
	    kGen = KeyGenerator.getInstance("AES");
	    kGen.init(128);
	    SecretKey key = kGen.generateKey();
	    a.setKey(key.getEncoded());
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	}

	int length = 1024 * 1024;
	int iterations = 100;
	byte[] testBytes1 = new byte[length];
	byte[] testBytes2 = new byte[length];
	byte[] testBytes3 = new byte[length];
	double[] time = new double[iterations];
	// int j;
	for (int i = 0; i < iterations; i++) {
	    a.sRand.nextBytes(testBytes1);

	    time[i] = -System.nanoTime();
	    testBytes2 = a.encrypt(testBytes1, S0);

	    testBytes3 = a.decrypt(testBytes2, S0);
	    System.out.println(Arrays.equals(testBytes1, testBytes3));
	    time[i] += System.nanoTime();

	    time[i] /= Math.pow(10, 9);
	}
	double sum = 0;
	for (int i = 0; i < iterations; i++)
	    sum += time[i];
	System.out.println(sum / iterations);
	System.out.println();
    }

    public static native int[] keyExpansion(byte[] key);

    public static native byte[] crypt(byte[] text, int[] r_key, byte[] S0);
}