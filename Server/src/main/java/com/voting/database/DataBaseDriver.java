package com.voting.database;


public interface DataBaseDriver {
	boolean connect();
	boolean insertNewUser(User user);
	boolean setBanByPrK(byte[] PrK);
	boolean setTrustById(String trust, byte[] id);
	User getUser(byte[] id);
	boolean deleteUser(byte[] id);
}
