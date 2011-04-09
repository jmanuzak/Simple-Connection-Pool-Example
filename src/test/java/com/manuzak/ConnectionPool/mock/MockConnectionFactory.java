package com.manuzak.ConnectionPool.mock;

import java.sql.Connection;
import java.sql.SQLException;

import com.manuzak.connectionpool.ConnectionFactory;

/*
 * Mock implementation of ConnectionFactory for connection pool testing.
 *
 * 
 */
public class MockConnectionFactory implements ConnectionFactory {

	/*
	 * Maintain a counter with the number of connections created by the factory.
	 * This is helpful for verification during unit testing.
	 */
	private int connectionCounter = 0;

	public int getCount() {
		return this.connectionCounter;
	}

	public Connection createConnection() throws SQLException {
		Connection mockConnection = new MockConnection();
		this.connectionCounter++;
		return mockConnection;
	}
}
