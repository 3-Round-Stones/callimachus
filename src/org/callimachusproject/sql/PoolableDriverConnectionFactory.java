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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
	private final String validationQuery;
	private volatile ObjectPool<PoolableDriverConnection> pool = null;

	/**
	 * @param connFactory
	 *            the {@link ConnectionFactory} from which to obtain base
	 *            {@link Connection}s
	 * @param validationQuery
	 *            SQL SELECT query to validate connections
	 */
	public PoolableDriverConnectionFactory(ConnectionFactory connFactory,
			String validationQuery) {
		this.factory = connFactory;
		this.validationQuery = validationQuery;
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

	public boolean validateObject(PoolableDriverConnection conn) {
		try {
			if (conn.isClosed()) {
				throw new SQLException("connection closed");
			}
			if (null == validationQuery)
				return true;
			Statement stmt = conn.createStatement();
			try {
				ResultSet rset = stmt.executeQuery(validationQuery);
				try {
					return rset.next();
				} finally {
					rset.close();
				}
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
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
