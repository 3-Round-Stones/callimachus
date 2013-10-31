package org.callimachusproject.sql;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolableDriverConnectionFactory implements
		PoolableObjectFactory<PoolableDriverConnection> {

	private final Logger logger = LoggerFactory
			.getLogger(PoolableDriverConnectionFactory.class);
	private final ConnectionFactory factory;
	private volatile ObjectPool<PoolableDriverConnection> pool = null;

	/**
	 * @param connFactory
	 *            the {@link ConnectionFactory} from which to obtain base
	 *            {@link Connection}s
	 * @param pool
	 *            the {@link ObjectPool} in which to pool those
	 *            {@link Connection}s
	 */
	public PoolableDriverConnectionFactory(ConnectionFactory connFactory) {
		factory = connFactory;
	}

	/**
	 * @param pool
	 *            the {@link ObjectPool} in which to pool those
	 *            {@link Connection}s
	 */
	public synchronized void setPool(ObjectPool<PoolableDriverConnection> pool) {
		if (null != this.pool && this.pool != pool) {
			try {
				this.pool.close();
			} catch (Exception e) {
				logger.warn(e.toString(), e);
			}
		}
		this.pool = pool;
	}

	public PoolableDriverConnection makeObject() throws Exception {
		Connection conn = factory.createConnection();
		if (conn == null) {
			throw new IllegalStateException(
					"Connection factory returned null from createConnection");
		}
		if (conn.isClosed()) {
			throw new SQLException("connection closed");
		}
		return new PoolableDriverConnection(conn, pool);
	}

	public void destroyObject(PoolableDriverConnection obj) throws Exception {
		if (obj instanceof PoolableDriverConnection) {
			((PoolableDriverConnection) obj).reallyClosing();
		}
	}

	public boolean validateObject(PoolableDriverConnection obj) {
		try {
			if (obj.isClosed()) {
				throw new SQLException("connection closed");
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void passivateObject(PoolableDriverConnection conn) throws Exception {
		if (!conn.getAutoCommit() && !conn.isReadOnly()) {
			conn.rollback();
		}
		conn.clearWarnings();
		if (!conn.getAutoCommit()) {
			conn.setAutoCommit(true);
		}
		conn.passivate();
	}

	public void activateObject(PoolableDriverConnection conn) throws Exception {
		conn.activate();
		if (conn.getAutoCommit() != true) {
			conn.setAutoCommit(true);
		}
	}
}
