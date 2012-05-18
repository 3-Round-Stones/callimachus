package org.callimachusproject.script;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class ScriptExceptionTest extends CodeGenTestCase {

	private ObjectRepository repository;
	private ObjectConnection con;

	public void setUp() throws Exception {
		super.setUp();
		addRdfSource("exceptions.ttl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		repository = ofm.getRepository(converter);
		repository.setDelegate(new SailRepository(new MemoryStore()));
		repository.setDataDir(targetDir);
		repository.initialize();
		con = repository.getConnection();
		for (URL imp : converter.getImports()) {
			ValueFactory vf = con.getValueFactory();
			String graph = imp.toExternalForm();
			con.add(imp, graph, RDFFormat.TURTLE, vf.createURI(graph));
		}
	}

	public void tearDown() throws Exception {
		con.close();
		repository.shutDown();
		super.tearDown();
	}

	public void testException() throws Throwable {
		Object object = con.getObject("urn:test:object");
		try {
			try {
				object.getClass().getMethod("throwRepositoryException").invoke(
						object);
				fail("No exception");
			} catch (InvocationTargetException ite) {
				throw ite.getCause();
			}
		} catch (RepositoryException exc) {
			assertEquals("exception message", exc.getMessage());
		} catch (BehaviourException be) {
			try {
				throw be.getCause();
			} catch (RepositoryException exc) {
				assertEquals("exception message", exc.getMessage());
			}
		}
	}

	public void testObjectStoreException() throws Throwable {
		Object object = con.getObject("urn:test:object");
		try {
			try {
				object.getClass().getMethod("throwObjectStoreException").invoke(
						object);
				fail("No exception");
			} catch (InvocationTargetException ite) {
				throw ite.getCause();
			}
		} catch (ObjectStoreException exc) {
			assertEquals("runtime message", exc.getMessage());
		}
	}

	public void testMalformedQueryException() throws Throwable {
		Object object = con.getObject("urn:test:object");
		try {
			try {
				object.getClass().getMethod("callMalformedQueryException").invoke(
						object);
				fail("No exception");
			} catch (InvocationTargetException ite) {
				throw ite.getCause();
			}
		} catch (MalformedQueryException exc) {
			// pass
		} catch (BehaviourException be) {
			try {
				throw be.getCause();
			} catch (MalformedQueryException exc) {
				// pass
			}
		}
	}

	public void testClassCastException() throws Throwable {
		Object object = con.getObject("urn:test:object");
		try {
			try {
				object.getClass().getMethod("callClassCastException").invoke(
						object);
				fail("No exception");
			} catch (InvocationTargetException ite) {
				throw ite.getCause();
			}
		} catch (ClassCastException exc) {
			// pass
		}
	}
}
