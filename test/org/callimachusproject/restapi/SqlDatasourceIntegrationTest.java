package org.callimachusproject.restapi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.OpenRDFException;

public class SqlDatasourceIntegrationTest extends TemporaryServerIntegrationTestCase {
	private static final String JDBC_URL = "jdbc:derby:memory:testdb;create=true";
	private static final String DRIVER_JAR = "tmp/derby.jar";
	private static final String DRIVER_CLASSNAME = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String SQL = "application/sql";
	private static final String RESULTS_XML = "application/xml";
	private static final String RESULTS_CSV = "text/csv";
	private static final String RESULTS_TAB = "text/tab-separated-values";
	private WebResource datasource;

	@Override
	@Before
	public void setUp() throws Exception {
		File car = findCallimachusEnterpriseWebappCar();
		if (car != null) {
			System.setProperty("com.threeroundstones.config.webapp", car.getAbsolutePath());
		}
		super.setUp();
		datasource = createSqlDatasource(getName() + "-datasource");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (200 == datasource.ref("?table=TESTDATA").headCode()) {
			datasource.ref("?table=TESTDATA").delete();
		}
		datasource.link("describedby").delete();
		super.tearDown();
	}

	private File findCallimachusEnterpriseWebappCar() {
		File dist = new File("dist");
		if (dist.list() != null) {
			for (String file : dist.list()) {
				if (file.startsWith("Callimachus-Enterprise-webapp")
						&& file.endsWith(".car"))
					return new File(dist, file);
			}
		}
		return null;
	}

	private WebResource createSqlDatasource(String slug) throws IOException,
			OpenRDFException {
		String jar = new File(".").toURI().resolve(DRIVER_JAR).toASCIIString();
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("INSERT DATA {\n");
		sb.append("<").append(slug).append(">");
		sb.append(" a calli:SqlDatasource, </callimachus/1.3/types/SqlDatasource>;\n");
		sb.append("rdfs:label \"").append(slug).append("\";\n");
		sb.append("calli:maxActive 1;\n");
		sb.append("calli:maxIdle 1;\n");
		sb.append("calli:maxWait 1;\n");
		sb.append("calli:jdbcUrl \"").append(JDBC_URL).append("\";\n");
		sb.append("calli:driverClassName \"").append(DRIVER_CLASSNAME).append("\";\n");
		sb.append("calli:driverJar <").append(jar).append(">\n");
		sb.append("}");
		return getHomeFolder().link("describedby").create("application/sparql-update", sb.toString().getBytes("UTF-8"));
	}

	@Test
	public void testGetXmlQueryParameter() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		datasource.post(SQL, "insert into TESTDATA values(1, 'hello', 12345)".getBytes());
		String sparql = "select * from TESTDATA";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String results = new String(datasource.ref("?query=" + encoded).get(RESULTS_XML));
		assertTrue(results.contains(">hello<"));
	}

	@Test
	public void testGetCsvQueryParameter() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		datasource.post(SQL, "insert into TESTDATA values(1, 'hello', 12345)".getBytes());
		String sparql = "select * from TESTDATA";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String results = new String(datasource.ref("?query=" + encoded).get(RESULTS_CSV));
		assertTrue(results.contains(",hello,") || results.contains(",\"hello\","));
	}

	@Test
	public void testGetTabQueryParameter() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		datasource.post(SQL, "insert into TESTDATA values(1, 'hello', 12345)".getBytes());
		String sparql = "select * from TESTDATA";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String results = new String(datasource.ref("?query=" + encoded).get(RESULTS_TAB));
		assertTrue(results.contains("hello"));
	}

	@Test
	public void testPostUpdateDirectly() throws Exception {
		String sparql = "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)";
		datasource.post(SQL, sparql.getBytes());
	}

	@Test
	public void testGetXmlTable() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		datasource.post(SQL, "insert into TESTDATA values(1, 'hello', 12345)".getBytes());
		String results = new String(datasource.ref("?table=TESTDATA").get(RESULTS_XML));
		assertTrue(results.contains(">hello<"));
	}

	@Test
	public void testGetCsvTable() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		datasource.post(SQL, "insert into TESTDATA values(1, 'hello', 12345)".getBytes());
		String results = new String(datasource.ref("?table=TESTDATA").get(RESULTS_CSV));
		assertTrue(results.contains(",hello,") || results.contains(",\"hello\","));
	}

	@Test
	public void testGetTabTable() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		datasource.post(SQL, "insert into TESTDATA values(1, 'hello', 12345)".getBytes());
		String results = new String(datasource.ref("?table=TESTDATA").get(RESULTS_TAB));
		assertTrue(results.contains("hello"));
	}

	@Test
	public void testPutXmlTable() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		writer.write("id,foo,bar\r\n");
		writer.write("1,hello,12345\r\n");
		writer.close();
		datasource.ref("?table=TESTDATA").put(RESULTS_CSV, out.toByteArray());
		String results = new String(datasource.ref("?table=TESTDATA").get(RESULTS_XML));
		assertTrue(results.contains(">hello<"));
	}

	@Test
	public void testPostXmlTable() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		datasource.post(SQL, "insert into TESTDATA values(1, 'hello', 12345)".getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		writer.write("id,foo,bar\r\n");
		writer.write("2,world,6789\r\n");
		writer.close();
		datasource.ref("?table=TESTDATA").post(RESULTS_CSV, out.toByteArray());
		String results = new String(datasource.ref("?table=TESTDATA").get(RESULTS_XML));
		assertTrue(results, results.contains(">hello<"));
		assertTrue(results, results.contains(">world<"));
	}

	@Test
	public void testDeleteTable() throws Exception {
		datasource.post(SQL, "create table TESTDATA (id int not null primary key, foo varchar(25), bar int)".getBytes());
		assertEquals(200, datasource.ref("?table=TESTDATA").headCode());
		datasource.ref("?table=TESTDATA").delete();
		assertEquals(404, datasource.ref("?table=TESTDATA").headCode());
	}

}
