package com.manuzak.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 
 * Basic connection pool interface to use as a starting point.
 * 
 */
public interface ConnectionPool {

	Connection getConnection() throws SQLException;

	void releaseConnection(Connection con) throws SQLException;
}
