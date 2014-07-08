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
		sb.append(" a calli:SqlDatasource, </callimachus/1.4/types/SqlDatasource>;\n");
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
	public void testPutCsvTable() throws Exception {
		datasource.post(SQL, "create table \"testdata\" (\"id\" int not null primary key, \"foo\" varchar(25), \"bar\" int)".getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		writer.write("id,foo,bar\r\n");
		writer.write("1,hello,12345\r\n");
		writer.close();
		datasource.ref("?table=testdata").put(RESULTS_CSV, out.toByteArray());
		String results = new String(datasource.ref("?table=testdata").get(RESULTS_XML));
		assertTrue(results.contains(">hello<"));
	}

	@Test
	public void testPostCsvTable() throws Exception {
		datasource.post(SQL, "create table \"testdata\" (\"id\" int not null primary key, \"foo\" varchar(25), \"bar\" int)".getBytes());
		datasource.post(SQL, "insert into \"testdata\" values(1, 'hello', 12345)".getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		writer.write("id,foo,bar\r\n");
		writer.write("2,world,6789\r\n");
		writer.close();
		datasource.ref("?table=testdata").post(RESULTS_CSV, out.toByteArray());
		String results = new String(datasource.ref("?table=testdata").get(RESULTS_XML));
		assertTrue(results, results.contains(">hello<"));
		assertTrue(results, results.contains(">world<"));
	}

	@Test
	public void testDeleteTable() throws Exception {
		datasource.post(SQL, "create table \"testdata\" (\"id\" int not null primary key, \"foo\" varchar(25), \"bar\" int)".getBytes());
		assertEquals(200, datasource.ref("?table=testdata").headCode());
		datasource.ref("?table=testdata").delete();
		assertEquals(404, datasource.ref("?table=testdata").headCode());
	}

	@Test
	public void testProxyInsert() throws Exception {
		datasource.post(SQL, "create table \"testdata\" (\"id\" int not null primary key, \"foo\" varchar(256), \"bar\" int)".getBytes());
		String select = "SELECT \"id\", \"foo\", \"bar\" FROM \"testdata\"";
		String insert = "INSERT INTO \"testdata\" (\"id\", \"foo\", \"bar\") VALUES ({+id}, '{+foo}', {+bar})";
		String copy = datasource + "?query=" + URLEncoder.encode(select, "UTF-8") + "\n" +
				"Accept: text/csv";
		String regex = "\\w+(://|%3A%2F%2F)([:\\w\\.-]|%3A)+([/\\w\\.-]*|%2F|%25)*";
		String post = "[^\\?]+\\?id=\\d+&foo=" + regex + "&bar=\\d+$ " + datasource + "\n" +
				"Content-Type: application/sql\n\n" + insert;
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
		sb.append("PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n");
		sb.append("INSERT DATA {\n");
		sb.append("<").append("testdata").append(">");
		sb.append(" a calli:Purl, </callimachus/1.4/types/Purl>;\n");
		sb.append("rdfs:label \"").append("testdata").append("\";\n");
		sb.append("calli:copy \"\"\"").append(copy.replace("\\", "\\\\")).append("\"\"\";");
		sb.append("calli:post \"\"\"").append(post.replace("\\", "\\\\")).append("\"\"\"");
		sb.append("}");
		WebResource testdata = getHomeFolder().link("describedby").create("application/sparql-update", sb.toString().getBytes("UTF-8"));
		String foo = datasource.toString();
		testdata.ref("?id=1&foo=" + URLEncoder.encode(foo, "UTF-8") + "&bar=1").post();
		assertEquals("id,foo,bar\r\n1," + foo + ",1\r\n", new String(testdata.get("*/*"), "UTF-8"));
		testdata.link("describedby").delete();
	}

}
