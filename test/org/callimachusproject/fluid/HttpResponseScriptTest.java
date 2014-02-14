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
package org.callimachusproject.fluid;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.callimachusproject.annotations.script;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class HttpResponseScriptTest extends TestCase {

	@Iri("file:///tmp/test/BooleanTester")
	public interface BooleanTester {
		@script("return { headers:{'content-type':'text/plain'}, body:'hello' }")
		Object returnBodyString();
		@script("return { headers:{'content-type':'text/plain'}, body:['hello'] }")
		Object returnBodyArray();
		@script("return { status:201, statusText:'Created', headers:{'content-type':'text/plain', 'location':'about:blank'}, body:['hello'] }")
		Object returnStatus201();
	}

	private File targetDir;
	private ObjectRepository repository;
	private ObjectConnection con;

	@Before
	public void setUp() throws Exception {
		targetDir = File.createTempFile(getClass().getSimpleName(), "");
		targetDir.delete();
		targetDir.mkdirs();
		ObjectRepositoryFactory orf = new ObjectRepositoryFactory();
		ObjectRepositoryConfig config = orf.getConfig();
		config.addConcept(BooleanTester.class);
		repository = orf.getRepository(config);
		repository.setDelegate(new SailRepository(new MemoryStore()));
		repository.setDataDir(targetDir);
		repository.initialize();
		con = repository.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		con.close();
		repository.shutDown();
		FileUtil.deltree(targetDir);
	}

	@Test
	public void testBodyString() throws Exception {
		BooleanTester tester = con.addDesignation(
				con.getObject("file:///tmp/test"), BooleanTester.class);
		HttpResponse http = asHttpResponse(tester.returnBodyString());
		assertEquals(200, http.getStatusLine().getStatusCode());
		assertEquals("text/plain", http.getFirstHeader("Content-Type")
				.getValue());
		assertEquals("hello", new Scanner(http.getEntity().getContent())
				.useDelimiter("\n").next());
	}

	@Test
	public void testBodyArray() throws Exception {
		BooleanTester tester = con.addDesignation(
				con.getObject("file:///tmp/test"), BooleanTester.class);
		HttpResponse http = asHttpResponse(tester.returnBodyArray());
		assertEquals(200, http.getStatusLine().getStatusCode());
		assertEquals("text/plain", http.getFirstHeader("Content-Type")
				.getValue());
		assertEquals("hello", new Scanner(http.getEntity().getContent())
				.useDelimiter("\n").next());
	}

	@Test
	public void testStatus201() throws Exception {
		BooleanTester tester = con.addDesignation(
				con.getObject("file:///tmp/test"), BooleanTester.class);
		HttpResponse http = asHttpResponse(tester.returnStatus201());
		assertEquals(201, http.getStatusLine().getStatusCode());
		assertEquals("text/plain", http.getFirstHeader("Content-Type")
				.getValue());
		assertEquals("hello", new Scanner(http.getEntity().getContent())
				.useDelimiter("\n").next());
	}

	private HttpResponse asHttpResponse(Object obj) throws IOException,
			FluidException {
		return FluidFactory.getInstance().builder(con).consume(obj, null, Object.class, "message/http").asHttpResponse();
	}

}
