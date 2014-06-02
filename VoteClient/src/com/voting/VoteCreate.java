package com.voting;

import java.io.Serializable;
import java.security.SecureRandom;

@SuppressWarnings("serial")
class VoteCreate implements Serializable {
    private SecureRandom sR;
    private KeyInside kI;
    private String question;

    public VoteCreate(SecureRandom sR, KeyInside kI, String question) {
	this.sR = sR;
	this.kI = kI;
	this.question = question;
    }

    public SecureRandom getSecureRandom() {
	return sR;
    }

    public KeyInside getKey() {
	return kI;
    }

    public String getQuestion() {
	return question;
    }
}