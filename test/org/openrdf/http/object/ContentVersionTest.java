package org.openrdf.http.object;

import java.util.concurrent.CountDownLatch;

import org.openrdf.annotations.Iri;
import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.repository.object.ObjectConnection;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ContentVersionTest extends MetadataServerTestCase {
	private static CountDownLatch hasRead;
	private static CountDownLatch willRespond;

	@Iri("urn:test:MyEntity")
	public interface MyEntity extends VersionedObject {
		@Iri("urn:test:label")
		String getLabel();

		@Iri("urn:test:label")
		void setLabel(String label);

		@method("GET")
		@query("no-store")
		@type("text/plain")
		@header("Cache-Control:no-store")
		String GetNoStoreLabel();

		@method("GET")
		@query("validated")
		@type("text/plain")
		@header("Cache-Control:must-revalidate")
		String GetValidatedLabel();
	}

	public static abstract class MyEntitySupport implements MyEntity {
		public String GetNoStoreLabel() {
			String label = getLabel();
			hasRead.countDown();
			try {
				willRespond.await();
			} catch (InterruptedException e) {
				// stop waiting
			}
			return label;
		}
		public String GetValidatedLabel() {
			String label = getLabel();
			hasRead.countDown();
			try {
				willRespond.await();
			} catch (InterruptedException e) {
				// stop waiting
			}
			return label;
		}
	}

	private WebResource entity;
	private String uri;

	public void setUp() throws Exception {
		config.addConcept(MyEntity.class);
		config.addBehaviour(MyEntitySupport.class);
		super.setUp();
		entity = client.path("/entity");
		uri = entity.getURI().toASCIIString();
		ObjectConnection con = repository.getConnection();
		try {
			MyEntity ref = con.addDesignation(con.getObject(uri),
					MyEntity.class);
			ref.setLabel("original");
		} finally {
			con.close();
		}
	}

	public void testNoStoreContentVersionWhileChanging() throws Exception {
		hasRead = new CountDownLatch(1);
		willRespond = new CountDownLatch(1);
		willRespond.countDown();
		WebResource req = entity.queryParam("no-store", "");
		ClientResponse resp = req.get(ClientResponse.class);
		String version = resp.getHeaders().getFirst("Content-Version");
		server.resetCache();
		String body = resp.getEntity(String.class);
		hasRead = new CountDownLatch(1);
		willRespond = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					hasRead.await();
				} catch (InterruptedException e) {
					// stop waiting
				}
				try {
					ObjectConnection con = repository.getConnection();
					try {
						MyEntity ref = con.getObject(MyEntity.class, uri);
						ref.setLabel("modified");
					} finally {
						con.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				willRespond.countDown();
			}
		}).start();
		resp = req.get(ClientResponse.class);
		assertEquals(body, resp.getEntity(String.class));
		assertEquals(version, resp.getHeaders().getFirst("Content-Version"));
		resp = req.get(ClientResponse.class);
		assertFalse(body.equals(resp.getEntity(String.class)));
		assertFalse(version.equals(resp.getHeaders().getFirst("Content-Version")));
	}

	public void testValidatedContentVersionWhileChanging() throws Exception {
		hasRead = new CountDownLatch(1);
		willRespond = new CountDownLatch(1);
		willRespond.countDown();
		WebResource req = entity.queryParam("validated", "");
		ClientResponse resp = req.get(ClientResponse.class);
		String version = resp.getHeaders().getFirst("Content-Version");
		server.resetCache();
		String body = resp.getEntity(String.class);
		hasRead = new CountDownLatch(1);
		willRespond = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					hasRead.await();
				} catch (InterruptedException e) {
					// stop waiting
				}
				try {
					ObjectConnection con = repository.getConnection();
					try {
						MyEntity ref = con.getObject(MyEntity.class, uri);
						ref.setLabel("modified");
					} finally {
						con.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				willRespond.countDown();
			}
		}).start();
		resp = req.get(ClientResponse.class);
		assertEquals(body, resp.getEntity(String.class));
		assertEquals(version, resp.getHeaders().getFirst("Content-Version"));
		resp = req.get(ClientResponse.class);
		String body2 = resp.getEntity(String.class);
		String version2 = resp.getHeaders().getFirst("Content-Version");
		assertFalse(body, body.equals(body2));
		assertFalse(version, version.equals(version2));
	}

}
