package org.callimachusproject.script;

import java.io.File;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class MethodTest extends CodeGenTestCase {

	private static final String NS = "urn:test:candy#";

	public void testCandy() throws Exception {
		addRdfSource("rdfs-schema.rdf");
		addRdfSource("candy-ontology.owl");
		File jar = createJar("candy.jar");
		assertTrue(jar.isFile());
		assertEquals(5, countClasses(jar, "candy", ".java"));
		assertEquals(5, countClasses(jar, "candy", ".class"));
	}

	public void testCandyJar() throws Exception {
		addRdfSource("candy-ontology.owl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepository repo = ofm.getRepository(converter);
		repo.setDelegate(new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		ObjectConnection manager = repo.getConnection();
		ClassLoader cl = manager.getObjectFactory().getClassLoader();
		Class<?> Candy = Class.forName("candy.Candy", true, cl);
		Class<?> Person = Class.forName("candy.Person", true, cl);
		Class<?> John = Class.forName("candy.IsJohn", true, cl);
		ObjectFactory of = manager.getObjectFactory();
		Object candy = manager.addDesignation(of.createObject(), Candy);
		Object person = manager.addDesignation(of.createObject(), Person);
		ValueFactory vf = manager.getValueFactory();
		Object john = manager.addDesignation(of.createObject(vf.createURI(NS, "john")), Person);
		Object jane = manager.addDesignation(of.createObject(vf.createURI(NS, "jane")), Person);

		assertEquals(NS + "john", john.getClass().getMethod("candyGetURI").invoke(john).toString());

		Candy.getMethod("setCandyGood", boolean.class).invoke(candy, true);
		assertEquals(Boolean.TRUE, Person.getMethod("candyTaste", Candy).invoke(person, candy));

		Candy.getMethod("setCandyGood", boolean.class).invoke(candy, false);
		assertEquals(Boolean.FALSE, Person.getMethod("candyTaste", Candy).invoke(person, candy));

		John.getMethod("setCandyGoodDay", boolean.class).invoke(john, true);
		assertEquals(Boolean.TRUE, Person.getMethod("candyTaste", Candy).invoke(john, candy));
		assertEquals(Boolean.FALSE, Person.getMethod("candyTaste", Candy).invoke(jane, candy));

		John.getMethod("setCandyGoodDay", boolean.class).invoke(john, false);
		assertEquals(Boolean.FALSE, Person.getMethod("candyTaste", Candy).invoke(john, candy));
		assertEquals(Boolean.TRUE, Person.getMethod("candyTaste", Candy).invoke(jane, candy));

		Candy.getMethod("setCandyGood", boolean.class).invoke(candy, true);
		Candy.getMethod("setCandyEaten", boolean.class).invoke(candy, false);
		assertEquals(Boolean.TRUE, Person.getMethod("candyEat", Candy).invoke(person, candy));
		assertEquals(Boolean.TRUE, Candy.getMethod("isCandyEaten").invoke(candy));

		Candy.getMethod("setCandyGood", boolean.class).invoke(candy, false);
		Candy.getMethod("setCandyEaten", boolean.class).invoke(candy, false);
		Person.getMethod("candyEat", Candy).invoke(person, candy);
		assertEquals(Boolean.TRUE, Candy.getMethod("isCandyEaten").invoke(candy));
		assertEquals(Boolean.TRUE, Candy.getMethod("isCandyGood").invoke(candy));

		assertEquals(Boolean.TRUE, Person.getMethod("sourLick", Candy).invoke(person, candy));

		manager.close();
		repo.shutDown();
	}
}
