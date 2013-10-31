package org.callimachusproject.sql;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.pool.ObjectPool;

/**
 * A delegating connection that, rather than closing the underlying connection,
 * returns itself to an {@link ObjectPool} when closed.
 */
public class PoolableDriverConnection extends PoolableConnection {
	private boolean reallyClosing = false;

	/**
	 * 
	 * @param conn
	 *            my underlying connection
	 * @param pool
	 *            the pool to which I should return when closed
	 */
	public PoolableDriverConnection(Connection conn,
			ObjectPool<PoolableDriverConnection> pool) {
		super(conn, pool);
	}

	/**
	 * Returns me to my pool, unless really closing is set.
	 */
	public synchronized void close() throws SQLException {
		if (reallyClosing) {
			super.reallyClose();
		} else {
			super.close();
		}
	}

	/**
	 * Actually close my underlying {@link Connection} on the next call to
	 * {@link #close()}.
	 */
	public synchronized void reallyClosing() throws SQLException {
		if (super.isClosed()) {
			super.reallyClose();
		} else {
			reallyClosing = true;
		}
	}

	@Override
	protected void activate() {
		super.activate();
	}

	@Override
	protected void passivate() throws SQLException {
		super.passivate();
	}
}
