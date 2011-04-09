package com.manuzak.ConnectionPool;

import java.sql.Connection;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.manuzak.ConnectionPool.mock.MockConnectionFactory;
import com.manuzak.connectionpool.ConnectionPoolImpl;

/**
 * @author Jonathan Manuzak
 * 
 *         Unit Test cases for com.manuzak.connectionpool.ConnectionPoolImpl
 * 
 *         Expectations:
 * 
 *         - The pool will be created with the specified initial size
 * 
 *         - The number of connections in a pool will never exceed the specified
 *         maximum size
 * 
 *         - The resulting connection from a connection request will not be
 *         closed
 * 
 *         - Connection requests will be honored until the pool is full
 * 
 *         - Closed connections will not be returned to the pool after being
 *         released by the client
 */
public class ConnectionPoolTest extends TestCase {

	// Global defaults for pool creation
	private MockConnectionFactory mockConnectionFactory;
	private int initialSize = 5;
	private int maxSize = 10;

	public ConnectionPoolTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(ConnectionPoolTest.class);
	}

	/**
	 * Create an instance of the MockConnectionFactory to inject into the
	 * ConnectionPool for testing.
	 * 
	 * The MockConnectionFactory also provides a getCount() method which
	 * indicates the amount of connections it has created so far. This is
	 * helpful to verify if the connections are being reused or created.
	 */
	public void setUp() {
		mockConnectionFactory = new MockConnectionFactory();
	}

	/**
	 * Simple initialization test with correct parameters. Verifies that the
	 * pool is created with <initialSize> connections available.
	 * 
	 * We can also verify that the ConnectionPool requested the correct amount
	 * of connections by interrogating the mock connection factory.
	 * 
	 * @throws Exception
	 */
	public void testPoolInitialization_Basic() throws Exception {
		// Create pool, populated with <initialSize> connections
		ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(
				mockConnectionFactory, initialSize, maxSize);

		// Validate that the pool is the appropriate size
		assertEquals(connectionPool.getPoolSize(), initialSize);

		// Validate the ConnectionFactory created the right amount of
		// connections
		assertEquals(initialSize, mockConnectionFactory.getCount());
	}

	/**
	 * The ConnectionPool requires a ConnectionFactory to create the database
	 * connections, so it should fail if one is not available.
	 */
	public void testPoolInitialization_NullConnectionFactory() {
		@SuppressWarnings("unused")
		ConnectionPoolImpl connectionPool;
		try {
			// Pass null instead of a connection factory instance
			connectionPool = new ConnectionPoolImpl(null, initialSize, maxSize);
			fail("Connection pool should not have been created");
		} catch (Exception e) {
			// Verify that the ConnectionPoolImpl constructor threw a generic
			// Exception-type throwable.
			assert (e.getClass().equals(java.lang.Exception.class));
		}

	}

	/**
	 * The <initialSize> can never be greater than the <maxSize>, but having two
	 * int parameters next to each other in the constructor method signature can
	 * be a source of errors. We should alert the client that this is the case.
	 */
	public void testPoolInitialization_IncorrectSizeParameters() {
		// Override default sizes for this scenario so that <initialSize> >
		// <maxSize>
		initialSize = 10;
		maxSize = 5;

		@SuppressWarnings("unused")
		ConnectionPoolImpl connectionPool;
		try {
			// Pass an <initialSize> > <maxSize>
			connectionPool = new ConnectionPoolImpl(mockConnectionFactory,
					initialSize, maxSize);
			fail("Connection pool should not have been created");
		} catch (Exception e) {
			// Verify that the ConnectionPoolImpl constructor threw a generic
			// Exception-type throwable.
			assert (e.getClass().equals(java.lang.Exception.class));
		}
	}

	/**
	 * It may be desirable to have an <initialSize> of zero and lazy-load the
	 * connections. However, having a <maxSize> of zero would result in a
	 * non-functional pool. Therefore, it should be explicitly disallowed and
	 * result in an error.
	 */
	public void testPoolInitialization_InvalidSize() {
		// Override default sizes for this scenario
		initialSize = 0;
		maxSize = 0;

		@SuppressWarnings("unused")
		ConnectionPoolImpl connectionPool;
		try {
			// Pass a <maxSize> of 0
			connectionPool = new ConnectionPoolImpl(mockConnectionFactory,
					initialSize, maxSize);
			fail("Connection pool should not have been created");
		} catch (Exception e) {
			// Verify that the ConnectionPoolImpl constructor threw a generic
			// Exception-type throwable.
			assert (e.getClass().equals(java.lang.Exception.class));
		}
	}

	/**
	 * The connection pool needs to be able to grow beyond it's <initialSize> up
	 * to the <maxSize>. However, it should first consume all of the existing
	 * connections before creating more.
	 * 
	 * @throws Exception
	 */
	public void testPoolUsage_Growth() throws Exception {
		// Create pool, populated with <initialSize> connections
		ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(
				mockConnectionFactory, initialSize, maxSize);

		// Use an ArrayList to hold the connections as they are retrieved
		ArrayList<Connection> connections = new ArrayList<Connection>(maxSize);
		for (int i = 0; i < maxSize; i++) {

			// get a connection and add it to the array
			connections.add(connectionPool.getConnection());

			if (i < initialSize) {
				// While consuming the pre-created connections, the
				// ConnectionFactory should not have created more connections
				assertEquals(initialSize, mockConnectionFactory.getCount());
			} else {
				// After the pre-created connections have been used, new
				// connections should be created by the factory on demand.
				assertEquals((i + 1), mockConnectionFactory.getCount());
			}
		}
	}

	/**
	 * The connection pool should always favor re-using connections over
	 * creating new connections. In this case, we consume <initialSize> + 1
	 * connections, release them and then consume <initialSize> + 2 connections,
	 * verifying that only the last connection request results in a new
	 * connection from the factory. This test may seem long or overly complex,
	 * but we need to go through a full cycle of requesting, releasing and
	 * requesting to verify the correct behavior.
	 * 
	 * @throws Exception
	 */
	public void testPoolUsage_ConnectionReuse() throws Exception {
		// Override the defaults to ensure that we maintain control over the
		// pool size in this case in the event that the global defaults are not
		// such that that <maxSize> > <initialSize>.

		initialSize = 5;
		maxSize = 10;

		// Create pool, populated with <initialSize> connections
		ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(
				mockConnectionFactory, initialSize, maxSize);

		// Use an ArrayList to hold the <initialSize> + 1 connections as they
		// are retrieved
		ArrayList<Connection> connections = new ArrayList<Connection>(maxSize);
		for (int i = 0; i < initialSize + 1; i++) {

			// get a connection and add it to the array
			connections.add(connectionPool.getConnection());

			if (i < initialSize) {
				// While consuming the pre-created connections, the
				// ConnectionFactory should not have created more connections
				assertEquals(initialSize, mockConnectionFactory.getCount());
			} else {
				// After the existing connections have been used, new
				// connections should be created by the factory on demand.
				assertEquals((i + 1), mockConnectionFactory.getCount());
			}
		}

		// Release all of the consumed exceptions
		for (Connection con : connections) {
			connectionPool.releaseConnection(con);
		}

		// Empty the connection ArrayList for reuse
		connections.clear();

		// The pool should now contain <initialSize> + 1 free connections
		assertEquals(initialSize + 1, connectionPool.getPoolSize());
		
		int connectionFactoryCounter = mockConnectionFactory.getCount();
		
		// Request <initialSize> + 2 (1 more than before) connections and ensure the connectionFactory is only queried once.
		for (int i = 0; i < initialSize + 2; i++) {

			// get a connection and add it to the array
			connections.add(connectionPool.getConnection());

			if (i < initialSize + 1) {
				// While consuming the existing connections, the
				// ConnectionFactory should not have created more connections
				assertEquals((initialSize + 1), mockConnectionFactory.getCount());
			} else {
				// After the existing connections have been used, new
				// connections should be created by the factory on demand.
				assertEquals((i + 1), mockConnectionFactory.getCount());
			}
		}
		
		//Finally, check that the <connectionFactoryCounter> only incremented by 1
		assertEquals((connectionFactoryCounter + 1), mockConnectionFactory.getCount());
		
	}

	/**
	 * Closed connections should not be added back to the pool of available
	 * connections when they are released.
	 * 
	 * Create a new pool of <initialSize> 1. Acquire the existing connection,
	 * close it, release it and verify that the connection pool has decreased by
	 * 1 (i.e. the connection was destroyed and not added back to the pool).
	 * 
	 * @throws Exception
	 */

	public void testPoolUsage_ClosedConnectionsNotPooled() throws Exception {
		initialSize = 1;
		// Create pool, populated with 1 connection
		ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(
				mockConnectionFactory, initialSize, maxSize);
		assertEquals(connectionPool.getPoolSize(), initialSize);

		// Acquire the pre-created connection, close it and release it.
		Connection con = connectionPool.getConnection();
		con.close();
		connectionPool.releaseConnection(con);

		// The connection pool should now be empty since we closed the only
		// existing connection.
		assertEquals(connectionPool.getPoolSize(), initialSize - 1);

	}

	/**
	 * Create a new pool and get <maxSize> number of connections to consume all
	 * pool resources. Then, request one additional connection. This request
	 * should be met with an exception indicating that all connections are in
	 * use.
	 * 
	 * @throws Exception
	 */
	public void testPoolUsage_ConnectionLimit() throws Exception {

		// Create pool, populated with <initialSize> connections
		ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(
				mockConnectionFactory, initialSize, maxSize);

		// get all (<maxSize>) connections from the pool and add them to a
		// temporary ArrayList
		ArrayList<Connection> connections = new ArrayList<Connection>(maxSize);
		for (int i = 0; i < maxSize; i++) {
			connections.add(connectionPool.getConnection());
		}

		// Ensure that all connections have been created by the factory and
		// consumed by the ArrayList
		assertEquals(mockConnectionFactory.getCount(),
				connectionPool.getPoolSize());
		assertEquals(connections.size(), connectionPool.getPoolSize());

		// Request an additional connection and verify a SQLException is thrown.
		try {
			connectionPool.getConnection();
			fail("Connection should not have been acquired.");
		} catch (Exception e) {
			assert (e.getClass().equals(java.sql.SQLException.class));
		}

	}

	/**
	 * Create a new pool with an initial size, then call closeAllConnections()
	 * and confirm that there are no connections in the pool.
	 * 
	 * @throws Exception
	 */
	public void testPoolClosure_Basic() throws Exception {

		// Create pool, populated with <initialSize> connections
		ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(
				mockConnectionFactory, initialSize, maxSize);
		assertEquals(connectionPool.getPoolSize(), initialSize);

		// Explicitly close all of the connections
		connectionPool.closeAllConnections();
		assertEquals(connectionPool.getPoolSize(), 0);
	}

	/**
	 * Create a new pool with an initial size, explicitly close some connections
	 * without releasing them and ensure the pool is emptied cleanly.
	 * 
	 * @throws Exception
	 */
	public void testPoolClosure_PreClosedConnections() throws Exception {

		// Create pool, populated with <initialSize> connections
		ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(
				mockConnectionFactory, initialSize, maxSize);
		assertEquals(connectionPool.getPoolSize(), initialSize);

		// Close half of the connections without releasing them
		int closeCount = Math.round(initialSize / 2);
		for (int i = 0; i < closeCount; i++) {
			Connection con = connectionPool.getConnection();
			con.close();
		}

		// Explicitly close all of the connections
		connectionPool.closeAllConnections();
		assertEquals(connectionPool.getPoolSize(), 0);
	}

}
