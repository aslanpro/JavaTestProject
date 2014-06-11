package com.voting.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.voting.database.nosql.NoSqlConnector;

public class DataBaseDriverFactory {
	static public DataBaseDriver getDriver(String path) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();

			if (line.equals("nosql")) {
				System.out.println("NoSql");
				return new NoSqlConnector();
			}

			if (line.equals("sql")) {
				System.out.println("Sql");
			}

		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		return null;
	}
}
