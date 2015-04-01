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
package org.callimachusproject.script;

import info.aduna.io.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.callimachusproject.annotations.script;
import org.openrdf.annotations.Iri;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

public class ConcurrentResponseTest extends TestCase {
	private static final String THING = "http://example.com/thing";

	public static final String REDIRECT_TYPE = "http://example.com/types/Redirect";

	private File dataDir;
	private ObjectRepository repository;
	private ObjectConnection con;

	@Iri(REDIRECT_TYPE)
	public interface Redirect {

		@script("return {status:302,message:'Alternate',headers:{'location':'target','cache-control':[],'content-type':'text/plain'},body:['target']};")
		public HttpResponse redirect();
	}

	public void setUp() throws Exception {
		ObjectRepositoryConfig config = new ObjectRepositoryConfig();
		config.addConcept(Redirect.class);
		dataDir = FileUtil.createTempDir("response");
		if (config.getBlobStore() == null) {
			config.setBlobStore(dataDir.toURI().toString());
		}
		repository = createRepository(dataDir, config);
		initDataset(repository);
		con = repository.getConnection();
	}

	public void testJsonResponse() throws Exception {
		Redirect thing = con.getObject(Redirect.class, THING);
		HttpResponse response = thing.redirect();
		assertEquals(302, response.getStatusLine().getStatusCode());
		assertEquals("Alternate", response.getStatusLine().getReasonPhrase());
		assertEquals(1, response.getHeaders("Location").length);
		assertEquals("target", response.getFirstHeader("Location").getValue());
		assertEquals(0, response.getHeaders("Cache-Control").length);
		assertEquals(1, response.getHeaders("Content-Type").length);
		assertEquals("text/plain", response.getFirstHeader("Content-Type").getValue());
	}

	public void testResponseConcurrent() throws Throwable {
		int n = Runtime.getRuntime().availableProcessors() * 4;
		final CountDownLatch up = new CountDownLatch(1);
		final CountDownLatch down = new CountDownLatch(n);
		final List<Throwable> errors = new ArrayList<Throwable>(n);
		for (int i=0; i<n; i++) {
			new Thread(new Runnable() {
				public void run() {
					try {
						up.await();
						for (int i=0; i<100; i++) {
							testJsonResponse();
						}
					} catch (Throwable e) {
						e.printStackTrace();
						synchronized (errors) {
							errors.add(e);
						}
					} finally {
						down.countDown();
					}
				}
			}).start();
		}
		up.countDown();
		down.await();
		synchronized (errors) {
			if (!errors.isEmpty()) {
				throw errors.get(0);
			}
		}
	}

	public void tearDown() throws Exception {
		con.close();
		repository.shutDown();
		FileUtil.deltree(dataDir);
	}

	private ObjectRepository createRepository(File dataDir, ObjectRepositoryConfig config) throws Exception {
		Sail sail = new MemoryStore();
		Repository delegate = new SailRepository(sail);
		delegate.setDataDir(dataDir);
		delegate.initialize();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(config, delegate);
	}

	private void initDataset(ObjectRepository repository) throws Exception {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI thing = vf.createURI(THING);
			con.add(thing, RDF.TYPE, vf.createURI(REDIRECT_TYPE));
		} finally {
			con.close();
		}
	}

}
