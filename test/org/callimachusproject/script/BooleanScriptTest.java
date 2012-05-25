package org.callimachusproject.script;

import info.aduna.io.FileUtil;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import junit.framework.TestCase;

import org.callimachusproject.annotations.script;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class BooleanScriptTest extends TestCase {

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER,
			ElementType.ANNOTATION_TYPE, ElementType.PACKAGE })
	public @interface equivalentClass {
		@Iri("http://www.w3.org/2002/07/owl#equivalentClass")
		String[] value();
	}

	@Iri("urn:test:BooleanTester")
	public interface BooleanTester {
		@script("return false;")
		@Iri("urn:test:returnBooleanFalse")
		boolean returnBooleanFalse();

		@script("return true;")
		@Iri("urn:test:returnBooleanTrue")
		boolean returnBooleanTrue();

		@script("if (proceed()) return true; return false;")
		@equivalentClass("urn:test:returnBooleanFalse")
		@Iri("returnEquivBooleanFalse")
		boolean returnEquivBooleanFalse();

		@script("if (proceed()) return true; return false;")
		@equivalentClass("urn:test:returnBooleanTrue")
		@Iri("returnEquivBooleanTrue")
		boolean returnEquivBooleanTrue();

		@script("if (arg) return true; return false")
		boolean returnBooleanArg(@Iri("urn:test:arg") boolean arg);

		@script("if (java.lang.Boolean.valueOf(true)) return true; return false")
		boolean returnBooleanObjectTrue();

		@script("if (java.lang.Boolean.valueOf(false)) return true; return false")
		boolean returnBooleanObjectFalse();
	}

	private File targetDir;
	private ObjectRepository repository;
	private ObjectConnection con;

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

	public void tearDown() throws Exception {
		con.close();
		repository.shutDown();
		FileUtil.deltree(targetDir);
	}

	public void testBooleanReturnType() throws Exception {
		BooleanTester tester = con.addDesignation(con.getObject("urn:test:test"), BooleanTester.class);
		assertTrue(tester.returnBooleanTrue());
		assertFalse(tester.returnBooleanFalse());
		assertTrue(tester.returnEquivBooleanTrue());
		assertFalse(tester.returnEquivBooleanFalse());
	}

	public void testBooleanArgument() throws Exception {
		BooleanTester tester = con.addDesignation(con.getObject("urn:test:test"), BooleanTester.class);
		assertTrue(tester.returnBooleanArg(true));
		assertFalse(tester.returnBooleanArg(false));
	}

	public void testBooleanObject() throws Exception {
		BooleanTester tester = con.addDesignation(con.getObject("urn:test:test"), BooleanTester.class);
		assertTrue(tester.returnBooleanObjectTrue());
		assertFalse(tester.returnBooleanObjectFalse());
	}

}
