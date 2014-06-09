package com.voting.database.sql;

import com.voting.database.DataBaseDriver;
import com.voting.database.User;

public class SqlConnector implements DataBaseDriver {

	@Override
	public boolean connect() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean insertNewUser(User user) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setBanByPrK(byte[] PrK) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public User getUser(byte[] id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setTrustById(String trust, byte[] id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteUser(byte[] id) {
		// TODO Auto-generated method stub
		return false;
	}

}
