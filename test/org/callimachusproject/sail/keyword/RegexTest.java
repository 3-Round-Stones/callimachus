package org.callimachusproject.sail.keyword;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;

public class RegexTest extends TestCase {
	private ValueFactory vf = ValueFactoryImpl.getInstance();
	private Regex function = new Regex();

	private boolean contains(String haystack, String needle) throws ValueExprEvaluationException {
		String regex = function.evaluate(vf, vf.createLiteral(needle)).stringValue();
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(haystack);
		return m.find();
	}

	private boolean matches(String singular, String plural) throws ValueExprEvaluationException {
		return contains(singular, plural) && contains(plural, singular);
	}

	public void testBaths() throws Exception {
		assertTrue(matches("bath", "baths"));
	}

	public void testBees() throws Exception {
		assertTrue(matches("bee", "bees"));
	}

	public void testBushes() throws Exception {
		assertTrue(matches("bush", "bushes"));
	}

	public void testCacti() throws Exception {
		assertTrue(matches("cactus", "cacti"));
	}

	public void testCanto() throws Exception {
		assertTrue(matches("canto", "cantos"));
	}

	public void testCase() throws Exception {
		assertTrue(matches("word", "WORD"));
	}

	public void testCats() throws Exception {
		assertTrue(matches("cat", "cats"));
	}

	public void testChairs() throws Exception {
		assertTrue(matches("chair", "chairs"));
	}

	public void testCherries() throws Exception {
		assertTrue(matches("cherry", "cherries"));
	}

	public void testCriteria() throws Exception {
		assertTrue(matches("criterion", "criteria"));
	}

	public void testDays() throws Exception {
		assertTrue(matches("day", "days"));
	}

	public void testExact() throws Exception {
		assertTrue(matches("word", "word"));
	}

	public void testFacade() throws Exception {
		assertTrue(matches("façade", "facade"));
	}

	public void testFora() throws Exception {
		assertTrue(matches("forum", "fora"));
	}

	public void testFuhrer() throws Exception {
		assertTrue(matches("Fuhrer", "Führer"));
	}

	public void testFungi() throws Exception {
		assertTrue(matches("fungus", "fungi"));
	}

	public void testHomos() throws Exception {
		assertTrue(matches("homo", "homos"));
	}

	public void testHouses() throws Exception {
		assertTrue(matches("house", "houses"));
	}

	public void testIndices() throws Exception {
		assertTrue(matches("index", "indices"));
	}

	public void testItches() throws Exception {
		assertTrue(matches("itch", "itches"));
	}

	public void testJalapeno() throws Exception {
		assertTrue(matches("jalapeño", "jalapeno"));
	}

	public void testKimonos() throws Exception {
		assertTrue(matches("kimono", "kimonos"));
	}

	public void testLadies() throws Exception {
		assertTrue(matches("lady", "ladies"));
	}

	public void testLampreys() throws Exception {
		assertTrue(matches("lamprey", "lampreys"));
	}

	public void testMatrices() throws Exception {
		assertTrue(matches("matrix", "matrices"));
	}

	public void testMember() throws Exception {
		assertTrue(contains("hello world", "world"));
		assertTrue(contains("hello world", "hello"));
		assertTrue(contains("hello world", "hello world"));
	}

	public void testMonkeys() throws Exception {
		assertTrue(matches("monkey", "monkeys"));
	}

	public void testMoths() throws Exception {
		assertTrue(matches("moth", "moths"));
	}

	public void testMouths() throws Exception {
		assertTrue(matches("mouth", "mouths"));
	}

	public void testOBrian() throws Exception {
		assertTrue(matches("O`Brian", "O'Brian"));
	}

	public void testPhotos() throws Exception {
		assertTrue(matches("photo", "photos"));
	}

	public void testPianos() throws Exception {
		assertTrue(matches("piano", "pianos"));
	}

	public void testPluralS() throws Exception {
		assertTrue(matches("word", "words"));
	}

	public void testPorticos() throws Exception {
		assertTrue(matches("portico", "porticos"));
	}

	public void testProofs() throws Exception {
		assertTrue(matches("proof", "proofs"));
	}

	public void testPros() throws Exception {
		assertTrue(matches("pro", "pros"));
	}

	public void testQuarto() throws Exception {
		assertTrue(matches("quarto", "quartos"));
	}

	public void testRays() throws Exception {
		assertTrue(matches("ray", "rays"));
	}

	public void testResume() throws Exception {
		assertTrue(matches("résumé", "resume"));
	}

	public void testRoofs() throws Exception {
		assertTrue(matches("roof", "roofs"));
	}

	public void testSharks() throws Exception {
		assertTrue(matches("shark", "sharks"));
	}

	public void testSimilar() throws Exception {
		assertFalse(contains("hello world", "word"));
	}

	public void testSingular() throws Exception {
		assertTrue(matches("words", "word"));
	}

	public void testTrees() throws Exception {
		assertTrue(matches("tree", "trees"));
	}

	public void testZeros() throws Exception {
		assertTrue(matches("zero", "zeros"));
	}

	public void testMitchell() throws Exception {
		assertFalse(contains("Mitchell Corporation", "W. O. Mitchell"));
	}

	public void testNothing() throws Exception {
		assertFalse(contains("some thing", "nothing"));
	}

	public void testJ() throws Exception {
		assertTrue(contains("James", "j"));
	}
}
