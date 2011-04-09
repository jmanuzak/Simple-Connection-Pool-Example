package com.manuzak.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection factory interface for use with the ConnectionPoolImpl.
 * 
 * Separating the factory allows for this solution to be database agnostic.
 * Additionally, it allows for mock/fake database connection implementations to
 * be used for testing.
 */
public interface ConnectionFactory {
	public Connection createConnection() throws SQLException;
}
