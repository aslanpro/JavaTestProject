package com.voting.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.voting.database.nosql.NoSqlConnector;

public class DataBaseDriverFactory {
	static public DataBaseDriver getDriver(String path) {
		Charset charset = Charset.forName("US-ASCII");

		Path file = FileSystems.getDefault().getPath(path);

		try {
			BufferedReader reader = Files.newBufferedReader(file, charset);
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
			reader.close();
		}

		return null;
	}
}
