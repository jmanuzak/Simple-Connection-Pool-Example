package com.manuzak.connectionpool;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;

/**
 * @author Jonathan Manuzak
 * 
 */
public class ConnectionPoolImpl implements ConnectionPool {
	private int maxConnections;
	private ConnectionFactory connectionFactory;

	private Vector<Connection> freeConnections;
	private Vector<Connection> busyConnections;

	/**
	 * Create a new connection pool and optionally (initialConnections > 0)
	 * populate it with available connections.
	 * 
	 * Requiring the initial and maximum pool size to be set allows for the pool
	 * to be tuned to the particular application.
	 * 
	 * Using an external connection factory instead of managing the database
	 * driver, url, user and password internally allows greater versatility and
	 * simplifies testing of the pool logic.
	 * 
	 * @param factory
	 * @param initialConnections
	 * @param maxConnections
	 * @throws Exception
	 */
	public ConnectionPoolImpl(ConnectionFactory factory,
			int initialConnections, int maxConnections) throws Exception {

		// Perform a sanity check of the initialization parameters
		if (initialConnections > maxConnections || maxConnections < 1
				|| factory == null) {
			throw new InvalidParameterException("Invalid parameters");
		}

		this.connectionFactory = factory;
		this.maxConnections = maxConnections;

		// Create free and busy vectors to hold the created connections
		freeConnections = new Vector<Connection>(initialConnections);
		busyConnections = new Vector<Connection>();

		if (initialConnections > 0)
			createInitialConnections(initialConnections);
	}

	/**
	 * Seed the pool with an initial set of connections from the factory.
	 * 
	 * @param initialConnections
	 * @throws SQLException
	 */
	private void createInitialConnections(int initialConnections)
			throws SQLException {
		for (int i = 0; i < initialConnections; i++) {
			Connection con = connectionFactory.createConnection();
			freeConnections.addElement(con);
		}
	}

	/**
	 * Retrieve or create connections as needed.
	 * 
	 * Always prefer reusing existing connections, but create new connections if necessary.
	 * 
	 * @see com.manuzak.connectionpool.ConnectionPool#getConnection()
	 */
	public synchronized Connection getConnection() throws SQLException {

		if (!freeConnections.isEmpty()) {
			// There are available connections in the pool.

			// Extract the last element from the free connection pool
			int index = freeConnections.size() - 1;
			Connection con = freeConnections.remove(index);

			// If the connection is closed (timeout, DB node failure), get
			// another connection.
			if (con.isClosed()) {
				return getConnection();
			}

			// The connection is available. Add it to the busy vector and return
			// it to the client.
			busyConnections.addElement(con);
			return con;
		}
		else if (getPoolSize() < maxConnections) {
			// There are not available connections, but the pool can accommodate
			// more new connections.

			// Get a new connection from the factory, add it to the busy vector
			// and return it to the client
			Connection con = connectionFactory.createConnection();
			busyConnections.addElement(con);
			return con;
		}
		else {
			// There are no available connections and the maximum pool size has been reached.
			
			throw new SQLException("The maximum connection pool size (" + this.maxConnections + ") has been reached.");
		}
	}

	/*
	 * Add a released connection back to the free pool or dispose of it, as necessary.
	 * 
	 * Always recycle connections if possible.  However, if the released connection is close, do not add it back to the free pool.
	 * 
	 * @see com.manuzak.connectionpool.ConnectionPool#releaseConnection(java.sql.
	 * Connection)
	 */
	public synchronized void releaseConnection(Connection connection) throws SQLException {
		
		// This connection is no longer in use, so regardless of it's state, remove it from the busy pool.
		busyConnections.removeElement(connection);
		
		try {
			if (!connection.isClosed()) {
				// The connection is available for reuse, return it to the free pool.
				freeConnections.addElement(connection);
			}
		} catch (SQLException e) {
			// No need to alert the client.  They explicitly asked to no longer use this connection.
		}
	}

	/**
	 * Get the total (e.g. free and busy connections) size of the pool.
	 * 
	 * @return
	 */
	public synchronized int getPoolSize() {

		int totalSize = freeConnections.size() + busyConnections.size();
		return totalSize;
	}

	/**
	 * Close all connections, regardless of their state.
	 * 
	 * Consumers do not have to call this when they are done with the pool.  However, it's polite to give clients a way to clean up the objects this class has created on their behalf.
	 *
	 */
	public synchronized void closeAllConnections() {
		// Close each connection from the free and busy vectors, then empty the vector objects.
		
		closeConnections(freeConnections);
		freeConnections.clear();

		closeConnections(busyConnections);
		busyConnections.clear();
	}

	/**
	 * Explicitly close connections so the objects can be garbage collected.
	 * 
	 */
	private void closeConnections(Vector<Connection> connections) {
		
		try {
			for (Connection con : connections) {
				// Close each connection, if necessary
				if (!con.isClosed()) {
					con.close();
				}
			}
		} catch (SQLException e) {
			// No need to raise any exceptions as the connections is no longer in use.
		}
	}

}