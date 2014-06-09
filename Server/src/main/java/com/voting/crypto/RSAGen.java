package com.voting.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/*
 * RSA keys generation algorithm. Size of keys = 1024 bits (128 bytes)
 */
public class RSAGen {

    static final int NUM_DIGITS = 1024;							//keysize
    private BigInteger n, d;									//modulus and secrete exponent
    public static final BigInteger e = new BigInteger("65537");	//public exponent is a prime (2^16+1)

    //constructor provide generation of RSA key pair
    public RSAGen() {
        BigInteger p, q;
        SecureRandom r = new SecureRandom();
        p = new BigInteger(NUM_DIGITS / 2, r).nextProbablePrime();
        q = new BigInteger(NUM_DIGITS / 2, r).nextProbablePrime();
        n = p.multiply(q);
        //generates new key while byte length of key < 128
        while (n.bitLength() <= RSAGen.NUM_DIGITS - 8) {
            p = new BigInteger(NUM_DIGITS / 2, r).nextProbablePrime();
            q = new BigInteger(NUM_DIGITS / 2, r).nextProbablePrime();
            n = p.multiply(q);
        }
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        d = e.modInverse(phi);
    }

    //return private key
    public byte[] getPrivateKey() {
        byte[] result = d.toByteArray();
        if (result.length > (RSAGen.NUM_DIGITS / 8)) {
            result = Arrays.copyOfRange(result, 1, result.length); //cut's zero byte if need
        }
        return result;
    }

    //return modulus
    public byte[] getN() {
        byte[] result = n.toByteArray();
        if (result.length > (RSAGen.NUM_DIGITS / 8)) {
            result = Arrays.copyOfRange(result, 1, result.length); //cut's zero byte if need
        }
        return result;
    }
}