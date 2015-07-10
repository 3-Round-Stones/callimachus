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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.codec.binary.Hex;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;

public final class SqlTupleResult implements TupleQueryResult {
	private final ValueFactory vf = ValueFactoryImpl.getInstance();
	private final ResultSet rs;
	private final Statement stmt;
	private final Connection conn;
	private final ResultSetMetaData md;
	private final DatatypeFactory df;
	private BindingSet next;

	public SqlTupleResult(ResultSet rs, Statement stmt, Connection conn)
			throws SQLException, DatatypeConfigurationException {
		this.rs = rs;
		this.stmt = stmt;
		this.conn = conn;
		md = rs.getMetaData();
		df = DatatypeFactory.newInstance();
	}

	@Override
	public void close() throws QueryEvaluationException {
		try {
			rs.close();
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			throw new QueryEvaluationException(e.toString(), e);
		}
	}

	@Override
	public List<String> getBindingNames()
			throws QueryEvaluationException {
		try {
			int n = md.getColumnCount();
			List<String> names = new ArrayList<String>(n);
			for (int col = 1; col <= n; col++) {
				names.add(md.getColumnLabel(col));
			}
			return names;
		} catch (SQLException e) {
			throw new QueryEvaluationException(e.toString(), e);
		}
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return next != null || (next = next()) != null;
	}

	@Override
	public synchronized BindingSet next() throws QueryEvaluationException {
		try {
			if (next != null)
				return next;
		} finally {
			next = null;
		}
		try {
			if (!rs.next())
				return null;
			int n = md.getColumnCount();
			MapBindingSet map = new MapBindingSet(n);
			for (int col = 1; col <= n; col++) {
				Value value = value(col);
				if (value != null) {
					map.addBinding(md.getColumnLabel(col), value);
				}
			}
			return map;
		} catch (SQLException e) {
			throw new QueryEvaluationException(e.toString(), e);
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		try {
			rs.deleteRow();
		} catch (SQLException e) {
			throw new QueryEvaluationException(e.toString(), e);
		}
	}

	private Value value(int col) throws SQLException {
		int type = md.getColumnType(col);
		String str = rs.getString(col);
		if (str == null)
			return null;
		switch (type) {
		case java.sql.Types.NULL:
			return null;
		case java.sql.Types.DATALINK:
			return vf.createURI(str);
		case java.sql.Types.BINARY:
		case java.sql.Types.VARBINARY:
		case java.sql.Types.BIT:
		case java.sql.Types.BLOB:
		case java.sql.Types.LONGVARBINARY:
		case java.sql.Types.JAVA_OBJECT:
			return vf.createLiteral(Hex.encodeHexString(rs.getBytes(col)), XMLSchema.HEXBINARY);
		case java.sql.Types.DECIMAL:
		case java.sql.Types.NUMERIC:
			return vf.createLiteral(str, XMLSchema.DECIMAL);
		case java.sql.Types.TINYINT:
		case java.sql.Types.SMALLINT:
		case java.sql.Types.INTEGER:
		case java.sql.Types.BIGINT:
			return vf.createLiteral(str, XMLSchema.INTEGER);
		case java.sql.Types.DOUBLE:
		case java.sql.Types.FLOAT:
		case java.sql.Types.REAL:
			return vf.createLiteral(str, XMLSchema.DOUBLE);
		case java.sql.Types.BOOLEAN:
			return vf.createLiteral(rs.getBoolean(col));
		case java.sql.Types.DATE:
			GregorianCalendar date = new GregorianCalendar();
			date.setTime(rs.getDate(col));
			date.clear(Calendar.AM_PM);
			date.clear(Calendar.HOUR);
			date.clear(Calendar.HOUR_OF_DAY);
			date.clear(Calendar.MINUTE);
			date.clear(Calendar.SECOND);
			date.clear(Calendar.MILLISECOND);
			return vf.createLiteral(df.newXMLGregorianCalendar(date));
		case java.sql.Types.TIME:
			GregorianCalendar time = new GregorianCalendar();
			time.setTime(rs.getTime(col));
			time.clear(Calendar.ERA);
			time.clear(Calendar.YEAR);
			time.clear(Calendar.MONTH);
			time.clear(Calendar.WEEK_OF_YEAR);
			time.clear(Calendar.WEEK_OF_MONTH);
			time.clear(Calendar.DATE);
			time.clear(Calendar.DAY_OF_MONTH);
			time.clear(Calendar.DAY_OF_YEAR);
			time.clear(Calendar.DAY_OF_WEEK);
			time.clear(Calendar.DAY_OF_WEEK_IN_MONTH);
			return vf.createLiteral(df.newXMLGregorianCalendar(time));
		case java.sql.Types.TIMESTAMP:
			return vf.createLiteral(rs.getTimestamp(col));
		case java.sql.Types.SQLXML:
			return vf.createLiteral(str, RDF.XMLLITERAL);
		case java.sql.Types.ARRAY:
		case java.sql.Types.CHAR:
		case java.sql.Types.CLOB:
		case java.sql.Types.DISTINCT:
		case java.sql.Types.LONGNVARCHAR:
		case java.sql.Types.NCHAR:
		case java.sql.Types.NCLOB:
		case java.sql.Types.NVARCHAR:
		case java.sql.Types.OTHER:
		case java.sql.Types.REF:
		case java.sql.Types.ROWID:
		case java.sql.Types.STRUCT:
		case java.sql.Types.VARCHAR:
		default:
			return vf.createLiteral(str);
		}
	}
}
