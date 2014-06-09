package com.voting.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/*
 * Implementation of RSA algorithm according to PKCS#1 v1.5
 */
public class RSA {

    private BigInteger d, n;						//secrete exponent and modulus
    private BigInteger m;						//RSA modulus of smb else 
    public static final int KEY_SIZE = 128;				//key size in bytes
    private static final int RESERVED_BYTES = 11;			//reserved bytes for RSA
    public static final int BLOCK_SIZE = KEY_SIZE - RESERVED_BYTES;	//max block size for RSA encryption.
    //digest info consist of bytes wich define hash function used for signature
    public static final byte[] DIGEST_INFO = {
        0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20
    };
    private static MessageDigest MESSAGE_DIGEST;	//hash function

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    private SecureRandom sRand;				//secure random generator for encryption operation

    public RSA(byte[] d, byte[] n, SecureRandom sR) {
        if ((d.length <= KEY_SIZE) && (n.length == KEY_SIZE)) {
            this.n = new BigInteger(1, n);
            this.d = new BigInteger(1, d);
            this.m = null;
            sRand = sR;
        } else {
            this.d = null;
            this.n = null;
            this.m = null;
            sRand = null;
            System.err.println("Input error!!!");
        }
    }

    public RSA(byte[] d, byte[] n, byte[] m, SecureRandom sR) {
        if ((d.length <= KEY_SIZE) && (n.length == KEY_SIZE) && (m.length == KEY_SIZE)) {
            this.n = new BigInteger(1, n);
            this.d = new BigInteger(1, d);
            this.m = new BigInteger(1, m);
            sRand = sR;
        } else {
            this.d = null;
            this.n = null;
            this.m = null;
            sRand = null;
            System.err.println("Input error!!!");
        }
    }

    public void setM(byte[] m) {
        if (m.length == KEY_SIZE) {
            this.m = new BigInteger(1, m);
        } else {
            this.m = null;
            System.err.println("Input error!!!");
        }
    }

    //encryption with smb's keys
    public byte[] encrypt(byte[] text) {
        if (text.length > BLOCK_SIZE) {
            System.err.println("Message too long");
            return null;
        }
        //ps is a padding string of nonzero bytes. min length of ps = 8
        byte[] ps = new byte[KEY_SIZE - text.length - 3];
        boolean flag = true;
        int i;
        //generating nonzero bytes
        while (flag) {
            sRand.nextBytes(ps);
            flag = false;
            for (i = 0; i < ps.length; i++) {
                if (ps[i] == 0) {
                    flag = true;
                    break;
                }
            }
        }
        //em is encrypted message. em = {0x00, 0x02, ps, 0x00, text}; 
        byte[] em = new byte[KEY_SIZE];
        em[0] = 0x00;
        em[1] = 0x02;
        for (i = 2; i < (ps.length + 2); i++) {
            em[i] = ps[i - 2];
        }
        em[i] = 0x00;
        for (int j = 0; j < text.length; j++) {
            i++;
            em[i] = text[j];
        }
        BigInteger result = new BigInteger(1, em);
        //Apply the encryption primitive
        result = result.modPow(RSAGen.e, this.m);
        byte[] c = result.toByteArray();
        if (c.length > KEY_SIZE) {
            c = Arrays.copyOfRange(c, 1, c.length); //cut's zero byte if need
        }
        return c;
    }

    //encrytion with entered keys
    public byte[] keyEncrypt(byte[] text, byte[] modulus) {
        if (modulus.length != KEY_SIZE) {
            System.err.println("Input error");
            return null;
        }
        if (text.length > BLOCK_SIZE) {
            System.err.println("Message too long");
            return null;
        }
        BigInteger mod = new BigInteger(1, modulus);
        //ps is a padding string of nonzero bytes. min length of ps = 8
        byte[] ps = new byte[KEY_SIZE - text.length - 3];
        boolean flag = true;
        int i;
        //generating nonzero bytes
        while (flag) {
            sRand.nextBytes(ps);
            flag = false;
            for (i = 0; i < ps.length; i++) {
                if (ps[i] == 0) {
                    flag = true;
                    break;
                }
            }
        }
        //em is encrypted message. em = {0x00, 0x02, ps, 0x00, text}; 
        byte[] em = new byte[KEY_SIZE];
        em[0] = 0x00;
        em[1] = 0x02;
        for (i = 2; i < (ps.length + 2); i++) {
            em[i] = ps[i - 2];
        }
        em[i] = 0x00;
        for (int j = 0; j < text.length; j++) {
            i++;
            em[i] = text[j];
        }
        //Apply the encryption primitive
        BigInteger result = new BigInteger(1, em);
        result = result.modPow(RSAGen.e, mod);
        byte[] c = result.toByteArray();
        if (c.length > KEY_SIZE) {
            c = Arrays.copyOfRange(c, 1, c.length); //cut's zero byte if need
        }
        return c;
    }

    //dencryption with own keys
    public byte[] decrypt(byte[] text) {
        if (text.length > KEY_SIZE) {
            System.err.println("Message too long");
            return null;
        }
        BigInteger res = new BigInteger(1, text);
        if (res.compareTo(this.n) >= 0) {
            System.err.println("Decryption error! text > modulus");
            return null;
        }
        //Apply the decryption primitive
        res = res.modPow(this.d, this.n);
        byte[] em = res.toByteArray();
        if (em.length >= KEY_SIZE) {
            System.err.println("Decryption error! em[0] != 0x00");
            return null;
        }
        if (em[0] != 0x02) {
            System.err.println("Decryption error! em[1] != 0x02");
            return null;
        }
        int i = 1;
        int j = 0;
        while (em[i] != 0x00) {
            j++;
            i++;
        }
        if (j < 8) {
            System.err.println("Decryption error! PS < 8");
            return null;
        }
        byte[] result = Arrays.copyOfRange(em, i + 1, em.length);
        return result;
    }

    //signature of message by own secrete key
    public byte[] sign(byte[] text) {
        byte[] em = encoding(text);
        if (em == null) {
            return null;
        }
        BigInteger mes = new BigInteger(1, em);
        //Apply the signature primitive
        mes = mes.modPow(this.d, this.n);
        byte[] s = mes.toByteArray();
        byte[] signature = new byte[KEY_SIZE];
        if (s.length > KEY_SIZE) {
            signature = Arrays.copyOfRange(s, 1, s.length); //cut's zero byte if need
        } else if (s.length < KEY_SIZE) {
            int fillSize = KEY_SIZE - s.length;
            for (int j = 0; j < fillSize; j++) {
                signature[j] = 0;
            }
            System.arraycopy(s, 0, signature, fillSize, s.length); //fills zero bytes if need
        } else {
            return s;
        }
        return signature;
    }

    //verification of smb's signature
    public boolean verify(byte[] message, byte[] signature) {
        if (signature.length > KEY_SIZE) {
            System.err.println("Length error!!!");
            return false;
        }
        BigInteger s = new BigInteger(1, signature);
        if (s.compareTo(this.m) >= 0) {
            System.err.println("Signature representative out of range");
            return false;
        }
        //Apply verification primitive
        s = s.modPow(RSAGen.e, this.m);
        //m is encrypted signature
        byte[] m = s.toByteArray();
        if (m.length >= KEY_SIZE) {
            System.err.println("Large signature!");
            return false;
        }
        //em is hash of internal message
        byte[] em = Arrays.copyOfRange(encoding(message), 1, KEY_SIZE);
        return Arrays.equals(m, em);
    }

    //verification of smb's signature
    public boolean verifyKey(byte[] message, byte[] signature, byte[] modulus) {
        if (modulus.length != KEY_SIZE) {
            System.err.println("Input error");
            return false;
        }
        if (signature.length > KEY_SIZE) {
            System.err.println("Length error!!!");
            return false;
        }
        BigInteger mod = new BigInteger(1, modulus);
        BigInteger s = new BigInteger(1, signature);
        if (s.compareTo(mod) >= 0) {
            System.err.println("Signature representative out of range");
            return false;
        }
        //Apply verification primitive
        s = s.modPow(RSAGen.e, mod);
        //m is encrypted signature
        byte[] m = s.toByteArray();
        if (m.length >= KEY_SIZE) {
            System.err.println("Large signature!");
            return false;
        }
        //em is hash of internal message
        byte[] em = Arrays.copyOfRange(encoding(message), 1, KEY_SIZE);
        return Arrays.equals(m, em);
    }

    //hashing message for signature
    private byte[] encoding(byte[] message) {
        MESSAGE_DIGEST.update(message);
        //hash is a hash of message
        byte[] hash = MESSAGE_DIGEST.digest();
        if (hash.length > MESSAGE_DIGEST.getDigestLength()) {
            System.err.println("Digest too long!!!");
            return null;
        }
        //t = {DIGEST_INFO, hash};
        byte[] t = new byte[hash.length + RSA.DIGEST_INFO.length];
        if (t.length > BLOCK_SIZE) {
            System.err.println("Intended encoded message length too short!!!");
            return null;
        }
        int i, j;
        for (i = 0; i < RSA.DIGEST_INFO.length; i++) {
            t[i] = DIGEST_INFO[i];
        }
        for (j = 0; j < hash.length; j++) {
            t[i] = hash[j];
            i++;
        }
        //ps is a padding string which consisting of 0xFF byte
        byte[] ps = new byte[KEY_SIZE - t.length - 3];
        Arrays.fill(ps, (byte) 0xFF);
        //em is encrypted message. em = {0x00, 0x01, ps, 0x00, t};
        byte[] em = new byte[KEY_SIZE];
        em[0] = 0x00;
        em[1] = 0x01;
        for (i = 2; i < (ps.length + 2); i++) {
            em[i] = ps[i - 2];
        }
        em[i] = 0x00;
        for (j = 0; j < t.length; j++) {
            i++;
            em[i] = t[j];
        }
        return em;
    }
}