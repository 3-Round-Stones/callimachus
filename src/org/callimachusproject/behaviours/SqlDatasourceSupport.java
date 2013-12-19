package org.callimachusproject.behaviours;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.concepts.SqlDatasource;
import org.callimachusproject.io.CharsetDetector;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.NotFound;
import org.callimachusproject.sql.DriverConnectionPoolManager;
import org.callimachusproject.sql.PoolableDriverConnection;
import org.callimachusproject.sql.SqlTupleResult;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SqlDatasourceSupport implements SqlDatasource,
		CalliObject {
	private static final Pattern HOST_POST_REGEX = Pattern
			.compile("([\\w\\-\\.]+):(\\d+)");
	private static final DriverConnectionPoolManager manager = new DriverConnectionPoolManager();
	private static final Map<String, List<Driver>> drivers = new HashMap<>();
	private static final int BATCH_SIZE = 1000;

	private final Logger logger = LoggerFactory
			.getLogger(SqlDatasourceSupport.class);

	public void reset() throws SQLException {
		String name = this.getResource().stringValue();
		synchronized (manager) {
			manager.deregisterDriver(name);
			deregisterLoadedDrivers(name);
		}
	}

	public TupleQueryResult selectTable(String tablename) throws SQLException,
			IOException, OpenRDFException, DatatypeConfigurationException {
		Connection conn = null;
		TupleQueryResult results = null;
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			verifyTableExists(tablename, conn);
			results = evaluateSql("SELECT * FROM \"" + tablename + "\"", conn);
		} finally {
			if (results == null) {
				if (conn != null) {
					conn.close();
				}
			}
		}
		return results;
	}

	public void dropTable(String tablename) throws SQLException,
			OpenRDFException, IOException {
		Connection conn = getConnection();
		try {
			conn.setAutoCommit(false);
			verifyTableExists(tablename, conn);
			executeSql("DROP TABLE \"" + tablename + "\"", conn);
			conn.commit();
		} finally {
			conn.close();
		}
	}

	public void clearAndLoadTable(TupleQueryResult rows, String tablename)
			throws SQLException, OpenRDFException, IOException {
		Connection conn = getConnection();
		try {
			conn.setAutoCommit(false);
			verifyTableExists(tablename, conn);
			conn.createStatement().execute("DELETE FROM \"" + tablename + "\"");
			loadIntoTable(rows, tablename, conn);
			conn.commit();
		} finally {
			conn.close();
		}
	}

	public void loadIntoTable(TupleQueryResult rows, String tablename)
			throws SQLException, OpenRDFException, IOException {
		Connection conn = getConnection();
		try {
			conn.setAutoCommit(false);
			verifyTableExists(tablename, conn);
			loadIntoTable(rows, tablename, conn);
			conn.commit();
		} finally {
			conn.close();
		}
	}

	public void executeSql(byte[] content, String contentType)
			throws IOException, SQLException, OpenRDFException {
		Connection conn = getConnection();
		try {
			executeSql(parse(content, contentType), conn);
		} finally {
			conn.close();
		}
	}

	public TupleQueryResult evaluateSql(String sql) throws SQLException,
			IOException, OpenRDFException, DatatypeConfigurationException {
		Connection conn = null;
		TupleQueryResult results = null;
		try {
			conn = getConnection();
			results = evaluateSql(sql, conn);
		} finally {
			if (results == null) {
				if (conn != null) {
					conn.close();
				}
			}
		}
		return results;
	}

	private TupleQueryResult evaluateSql(String sql, Connection conn)
			throws SQLException, IOException, DatatypeConfigurationException {
		Statement stmt = null;
		ResultSet rs = null;
		TupleQueryResult results = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			results = asTupleQueryResult(rs, stmt, conn);
		} finally {
			if (results == null) {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			}
		}
		return results;
	}

	private void executeSql(String sql, Connection conn) throws SQLException {
		conn.createStatement().execute(sql);
		logger.info(sql);
	}

	private TupleQueryResult asTupleQueryResult(final ResultSet rs,
			final Statement stmt, final Connection conn) throws IOException,
			SQLException, DatatypeConfigurationException {
		return new SqlTupleResult(rs, stmt, conn);
	}

	private String parse(byte[] content, String contentType) throws IOException {
		Charset charset = getCharset(content, contentType);
		return new String(content, charset);
	}

	private Charset getCharset(byte[] content, String contentType)
			throws IOException {
		ContentType type = ContentType.parse(contentType);
		Charset charset = type.getCharset();
		if (charset != null)
			return charset;
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		try {
			return new CharsetDetector().detect(in);
		} finally {
			in.close();
		}
	}

	private Connection getConnection() throws SQLException, OpenRDFException,
			IOException {
		String name = this.getResource().stringValue();
		PoolableDriverConnection conn = manager.getConnection(name);
		if (conn != null)
			return conn;
		synchronized (manager) {
			conn = manager.getConnection(name);
			if (conn != null)
				return conn;
			registerConnectionDriver(name, manager);
			return manager.getConnection(name);
		}
	}

	private void registerConnectionDriver(String name,
			DriverConnectionPoolManager manager) throws SQLException,
			OpenRDFException, IOException {
		Exception cause = null;
		String url = this.getCalliJdbcUrl();
		List<String> classnames = new ArrayList<>(this.getCalliDriverClassName());
		ClassLoader cl = createClassLoader();
		for (String classname : classnames) {
			try {
				Object d = Class.forName(classname, true, cl).newInstance();
				if (!(d instanceof Driver)) {
					logger.error("{} is not a java.sql.Driver class", classname);
				}
				Driver driver = (Driver) d;
				if (driver.getClass().getClassLoader() == cl) {
					deregisterDriverOnReset(name, driver);
				}
				if (driver.acceptsURL(url)) {
					Config config = new Config();
					config.testOnBorrow = true;
					config.maxActive = intOrNeg(this.getCalliMaxActive());
					config.maxIdle = intOrNeg(this.getCalliMaxIdle());
					config.maxWait = intOrNeg(this.getCalliMaxWait());
					Properties props = new Properties();
					Credentials cred = getCredential(url);
					if (cred != null) {
						props.setProperty("user", cred.getUserPrincipal()
								.getName());
						props.setProperty("password", cred.getPassword());
					}
					verifyDriver(url, driver, props);
					manager.registerDriver(name, driver, url, props, config,
							this.getCalliValidationQuery());
					return;
				}
			} catch (ClassNotFoundException e) {
				cause = e;
				logger.error("{} is not found in {}", classname,
						this.getCalliDriverJar());
			} catch (InstantiationException e) {
				cause = e;
				logger.error("Could not instaniate {}", classname);
			} catch (IllegalAccessException e) {
				cause = e;
				logger.error("Could not access {}", classname);
			} catch (Exception e) {
				cause = e;
				logger.error("Could not load driver {}", classname);
			}
		}
		reset();
		if (cause instanceof SQLException)
			throw (SQLException) cause;
		if (cause != null)
			throw new SQLException("Could not load driver "
					+ classnames, cause);
		throw new SQLException("Could not load driver "
				+ classnames);
	}

	private Credentials getCredential(String url) throws OpenRDFException,
			IOException {
		Matcher m = HOST_POST_REGEX.matcher(url);
		if (!m.find())
			return null;
		String host = m.group(1);
		int port = Integer.parseInt(m.group(2));
		String uri = this.getResource().stringValue();
		DetachedRealm realm = this.getCalliRepository().getRealm(uri);
		CredentialsProvider creds = realm.getCredentialsProvider();
		return creds.getCredentials(new AuthScope(host, port));
	}

	private ClassLoader createClassLoader() {
		List<URL> urls = new ArrayList<URL>();
		for (RDFObject jar : this.getCalliDriverJar()) {
			try {
				urls.add(new URL(jar.getResource().stringValue()));
			} catch (MalformedURLException e) {
				logger.warn(e.toString(), e);
			}
		}
		return URLClassLoader.newInstance(urls.toArray(new URL[urls.size()]));
	}

	private void deregisterDriverOnReset(String name, Driver driver) {
		synchronized (drivers) {
			if (!drivers.containsKey(name)) {
				drivers.put(name, new ArrayList<Driver>());
			}
			drivers.get(name).add(driver);
		}
	}

	private void deregisterLoadedDrivers(String name) throws SQLException {
		synchronized (drivers) {
			if (drivers.containsKey(name)) {
				for (Driver driver : drivers.remove(name)) {
					DriverManager.deregisterDriver(driver);
				}
			}
		}
	}

	private void verifyDriver(String url, Driver driver, Properties props)
			throws SQLException {
		driver.connect(url, props).close();
	}

	private int intOrNeg(Number i) {
		if (i == null)
			return -1;
		return i.intValue();
	}

	private void verifyTableExists(String tablename, Connection conn)
			throws SQLException {
		ResultSet tables = conn.getMetaData().getTables(null, null, tablename,
				null);
		try {
			if (!tables.next())
				throw new NotFound("Table " + tablename + " does not exist");
		} finally {
			tables.close();
		}
	}

	private void loadIntoTable(TupleQueryResult rows, String tablename,
			Connection conn) throws QueryEvaluationException, SQLException {
		List<String> columns = rows.getBindingNames();
		Map<String, Integer> columnTypes = getColumnTypes(tablename, conn);
		PreparedStatement insert = prepareInsert(columns, tablename, conn);
		try {
			for (int count = 1; rows.hasNext(); count++) {
				BindingSet row = rows.next();
				for (int i = 0, n = columns.size(); i < n; i++) {
					String column = columns.get(i);
					Integer type = columnTypes.get(column);
					Value value = row.getValue(column);
					int col = i + 1;
					setValue(insert, col, value, type);
				}
				insert.addBatch();
				if (count % BATCH_SIZE == 0) {
					insert.executeBatch();
				}
			}
			insert.executeBatch();
		} finally {
			insert.close();
		}
	}

	private PreparedStatement prepareInsert(List<String> columns,
			String tablename, Connection conn) throws SQLException,
			QueryEvaluationException {
		Set<String> columnNames = getColumnTypes(tablename, conn).keySet();
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO \"").append(tablename);
		sb.append("\" (\"");
		for (String name : columns) {
			if (!columnNames.contains(name))
				throw new BadRequest("Table " + tablename
						+ " does not have column " + name);
			sb.append(name);
			sb.append("\",\"");
		}
		sb.setLength(sb.length() - 2);
		sb.append(") VALUES (");
		for (int i = 0, n = columns.size(); i < n; i++) {
			sb.append("?").append(",");
		}
		sb.setLength(sb.length() - 1);
		sb.append(")");
		return conn.prepareStatement(sb.toString());
	}

	private Map<String, Integer> getColumnTypes(String tablename,
			Connection conn) throws SQLException {
		Map<String, Integer> columnNames = new LinkedHashMap<>();
		ResultSet columns = conn.getMetaData().getColumns(null, null,
				tablename, null);
		try {
			while (columns.next()) {
				columnNames.put(columns.getString(4), columns.getInt(5));
			}
		} finally {
			columns.close();
		}
		return columnNames;
	}

	private void setValue(PreparedStatement insert, int col, Value value,
			Integer type) throws SQLException {
		if (value == null) {
			insert.setNull(col, type);
		} else if (value instanceof Literal) {
			Literal lit = (Literal) value;
			URI datatype = lit.getDatatype();
			if (datatype == null) {
				insert.setString(col, value.stringValue());
			} else if (XMLDatatypeUtil.isCalendarDatatype(datatype)) {
				GregorianCalendar cal = lit.calendarValue()
						.toGregorianCalendar();
				insert.setDate(col, new java.sql.Date(cal.getTimeInMillis()),
						cal);
			} else {
				insert.setString(col, value.stringValue());
			}
		} else {
			insert.setString(col, value.stringValue());
		}
	}
}
