package org.callimachusproject.sql;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverConnectionFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

public class DriverConnectionPoolManager {
	private final HashMap<String, ObjectPool<PoolableDriverConnection>> pools = new HashMap<>();

	public synchronized void registerDriver(String name, Driver driver,
			String url, Properties info, GenericObjectPool.Config config,
			String validationQuery) throws SQLException {
		ConnectionFactory factory;
		PoolableDriverConnectionFactory poolable;
		ObjectPool<PoolableDriverConnection> pool;
		if (pools.containsKey(name)) {
			deregisterDriver(name);
		}
		factory = new DriverConnectionFactory(driver, url, info);
		poolable = new PoolableDriverConnectionFactory(factory, validationQuery);
		pool = new GenericObjectPool<PoolableDriverConnection>(poolable, config);
		poolable.setPool(pool);
		pools.put(name, pool);
	}

	public synchronized void deregisterDriver(String name) throws SQLException {
		ObjectPool<PoolableDriverConnection> pool = pools.get(name);
		if (pool != null) {
			pools.remove(name);
			try {
				pool.close();
			} catch (Exception e) {
				throw (SQLException) new SQLException("Error closing pool "
						+ name).initCause(e);
			}
		}
	}

	public PoolableDriverConnection getConnection(String name)
			throws SQLException {
		ObjectPool<PoolableDriverConnection> pool = getConnectionPool(name);
		if (null == pool) {
			return null;
		} else {
			return borrow(pool);
		}
	}

	private synchronized ObjectPool<PoolableDriverConnection> getConnectionPool(
			String name) throws SQLException {
		return pools.get(name);
	}

	private PoolableDriverConnection borrow(
			ObjectPool<PoolableDriverConnection> pool) throws SQLException {
		try {
			return pool.borrowObject();
		} catch (SQLException e) {
			throw e;
		} catch (NoSuchElementException e) {
			throw (SQLException) new SQLException(
					"Cannot get a connection, pool error: " + e.getMessage())
					.initCause(e);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw (SQLException) new SQLException(
					"Cannot get a connection, general error: " + e.getMessage())
					.initCause(e);
		}
	}
}
