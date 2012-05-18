package org.callimachusproject.script;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class ScriptTestCase extends TestCase {
	public static TestSuite suite() throws Exception {
		return new TestSuite();
	}

	public static TestSuite suite(Class<? extends ScriptTestCase> test,
			String file, String relative) throws Exception {
		return suite(test, ScriptTest.class.getResource(file).toURI().resolve(
				relative).toASCIIString());
	}

	public static TestSuite suite(Class<? extends ScriptTestCase> testcase,
			String url) throws Exception {
		ObjectRepository repository = createRepository(getOntology(url));
		TestSuite suite = new TestSuite(testcase.getName());
		ObjectConnection con = repository.getConnection();
		try {
			Class<? extends Object> c = con.getObject(url).getClass();
			for (Method method : c.getMethods()) {
				if (method.getName().startsWith("test")
						&& method.getParameterTypes().length == 0
						&& method.getReturnType().equals(Void.TYPE)) {
					String name = method.getName() + " " + url;
					ScriptTestCase test = testcase.newInstance();
					test.setName(name);
					test.setRepository(repository);
					suite.addTest(test);
				}
			}
		} finally {
			con.close();
		}
		if (suite.countTestCases() == 0) {
			suite.addTest(TestSuite.warning("Individual " + url
					+ " has no public test methods"));
		}
		return suite;
	}

	private static String getOntology(String name) {
		if (name.indexOf('#') >= 0)
			return name.substring(0, name.indexOf('#'));
		return name;
	}

	private static ObjectRepository createRepository(String ontology)
			throws Exception {
		URL url = new URL(ontology);
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepositoryConfig config = ofm.getConfig();
		config.addBehaviour(AssertSupport.class, RDFS.RESOURCE);
		config.addImports(url);
		ObjectRepository repository = ofm.getRepository(config);
		repository.setDelegate(new SailRepository(new MemoryStore()));
		repository.initialize();
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			String base = url.toExternalForm();
			RDFFormat format = RDFFormat.forFileName(url.getFile());
			con.add(url, base, format, vf.createURI(base));
		} finally {
			con.close();
		}
		return repository;
	}

	private ObjectRepository repository;
	private ObjectConnection con;
	private Object self;

	public ScriptTestCase() {
		super();
	}

	public ScriptTestCase(String name) {
		super(name);
	}

	public void setRepository(ObjectRepository repository) {
		this.repository = repository;
	}

	public void setUp() throws Exception {
		if (repository == null) {
			repository = createRepository(getOntology());
		}
		con = repository.getConnection();
		self = con.getObject(getURI());
		try {
			self.getClass().getMethod("setUp").invoke(self);
		} catch (NoSuchMethodException exc) {
			// skip
		}
	}

	public void tearDown() throws Exception {
		try {
			self.getClass().getMethod("tearDown").invoke(self);
		} catch (NoSuchMethodException exc) {
			// skip
		} finally {
			con.close();
		}
	}

	@Override
	protected void runTest() throws Throwable {
		Method runMethod = null;
		try {
			runMethod = self.getClass().getMethod(getMethodName(),
					(Class[]) null);
		} catch (NoSuchMethodException e) {
			fail("Method \"" + getMethodName() + "\" not found");
		}
		if (!Modifier.isPublic(runMethod.getModifiers())) {
			fail("Method \"" + getMethodName() + "\" should be public");
		}

		try {
			runMethod.invoke(self, (Object[]) new Class[0]);
		} catch (InvocationTargetException e) {
			e.fillInStackTrace();
			throw e.getTargetException();
		} catch (IllegalAccessException e) {
			e.fillInStackTrace();
			throw e;
		}

	}

	private String getMethodName() {
		String name = getName();
		return name.substring(0, name.indexOf(' '));
	}

	private String getOntology() {
		String name = getName();
		return name.substring(name.indexOf(' ') + 1, name.indexOf('#'));
	}

	private String getURI() {
		String name = getName();
		return name.substring(name.indexOf(' ') + 1);
	}

}
