package com.voting.database;

public class User {
	private byte[] id;
	private byte[] p_key;
	private String name;
	private String email;
	private byte[] cert;
	private String trust;

	User() {
	}
	
	public User(byte[] id, byte[] PrK, String name, String email,
			byte[] certificate, String trust) {
		this.id = id;
		this.p_key = PrK;
		this.name = name;
		this.email = email;
		this.cert = certificate;
		this.trust = trust;
	}

	public User(byte[] id, byte[] PrK, String name, String email,
			byte[] certificate) {
		this.id = id;
		this.p_key = PrK;
		this.name = name;
		this.email = email;
		this.cert = certificate;
		this.trust = "high";
	}

	public void setId(byte[] id) {
		this.id = id;
	}

	public void setPrK(byte[] PrK) {
		this.p_key = PrK;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public void setCertificate(byte[] certificate) {
		this.cert = certificate;
	}
	
	public void setTrust(String trust) {
		this.trust = trust;
	}
	
	public byte[] getId() {
		return id;
	}

	public byte[] getPrK() {
		return p_key;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}
	
	public byte[] getCertificate() {
		return cert;
	}
	
	public String getTrust() {
		return trust;
	}
}
