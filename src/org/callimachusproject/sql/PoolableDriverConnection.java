/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
