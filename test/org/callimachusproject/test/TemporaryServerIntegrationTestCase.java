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
package org.callimachusproject.test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import junit.framework.TestCase;

import org.callimachusproject.repository.CalliRepository;
import org.junit.After;
import org.junit.Before;
import org.openrdf.OpenRDFException;

public abstract class TemporaryServerIntegrationTestCase extends TestCase {
    private static final TemporaryServerFactory factory = TemporaryServerFactory.getInstance();
    private static final Map<Object, TemporaryServer> servers = new HashMap<Object, TemporaryServer>();
	private final TemporaryServer server;

	public TemporaryServerIntegrationTestCase() {
		this(null);
	}

	public TemporaryServerIntegrationTestCase(String name) {
		super(name);
		synchronized (servers) {
			Object key = getUniqueServerKey();
			if (servers.containsKey(key)) {
				server = servers.get(key);
			} else {
				servers.put(key, server = factory.createServer());
			}
		}
	}

	public Object getUniqueServerKey() {
		return getClass();
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		server.resume();
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication(server.getUsername(), server.getPassword()); 
		     }
		 });
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		server.pause();
		Authenticator.setDefault(null);
	}

	public WebResource getHomeFolder() throws IOException, OpenRDFException {
		return getCallimachusUrl("/");
	}

	public WebResource getCallimachusUrl(String path) throws IOException,
			OpenRDFException {
		String origin = server.getOrigin();
		String url = getRepository().getCallimachusUrl(origin, path);
		return new WebResource(url);
	}

	public String getUsername() {
		return server.getUsername();
	}

	public char[] getPassword() {
		return server.getPassword();
	}

	public CalliRepository getRepository() {
		return server.getRepository();
	}

	public <T> T waitForCompile(Callable<T> callable) throws Exception {
		T ret = callable.call();
		Thread.sleep(1000);
		return ret;
	}

}
