package org.callimachusproject.sail.keyword;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

import junit.framework.TestCase;

public class SoundexTest extends TestCase {
	private ValueFactory vf = ValueFactoryImpl.getInstance();
	private Soundex function = new Soundex();

	private String soundex(String input) throws Exception {
		return function.evaluate(vf, vf.createLiteral(input)).stringValue();
	}

	public void testAshcraft() throws Exception {
		assertEquals("A261", soundex("Ashcraft"));
	}

	public void testAshcroft() throws Exception {
		assertEquals("A261", soundex("Ashcroft"));
	}

	public void testBase() throws Exception {
		assertEquals("B200", soundex("base"));
	}

	public void testBaseBall() throws Exception {
		assertEquals("B214", soundex("base ball"));
		assertEquals("B214", soundex("base.ball"));
		assertEquals("B214", soundex("base\nball"));
	}

	public void testBaths() throws Exception {
		assertEquals(soundex("bath"), soundex("baths"));
	}

	public void testBees() throws Exception {
		assertEquals(soundex("bee"), soundex("bees"));
	}

	public void testBushes() throws Exception {
		assertEquals(soundex("bush"), soundex("bushes"));
	}

	public void testCacti() throws Exception {
		assertEquals(soundex("cactus"), soundex("cacti"));
	}

	public void testCanto() throws Exception {
		assertEquals(soundex("canto"), soundex("cantos"));
	}

	public void testCats() throws Exception {
		assertEquals(soundex("cat"), soundex("cats"));
	}

	public void testChairs() throws Exception {
		assertEquals(soundex("chair"), soundex("chairs"));
	}

	public void testCherries() throws Exception {
		assertEquals(soundex("cherry"), soundex("cherries"));
	}

	public void testCriteria() throws Exception {
		assertEquals(soundex("criterion"), soundex("criteria"));
	}

	public void testDays() throws Exception {
		assertEquals(soundex("day"), soundex("days"));
	}

	public void testEleonore() throws Exception {
		assertEquals("E456", soundex("Éléonore"));
	}

	public void testFacade() throws Exception {
		assertEquals("F230", soundex("façade"));
	}

	public void testFora() throws Exception {
		assertEquals(soundex("forum"), soundex("fora"));
	}

	public void testFuehrer() throws Exception {
		assertEquals("F660", soundex("Fuehrer"));
	}

	public void testFuhrer() throws Exception {
		assertEquals("F660", soundex("Führer"));
	}

	public void testFungi() throws Exception {
		assertEquals(soundex("fungus"), soundex("fungi"));
	}

	public void testHomos() throws Exception {
		assertEquals(soundex("homo"), soundex("homos"));
	}

	public void testHouses() throws Exception {
		assertEquals(soundex("house"), soundex("houses"));
	}

	public void testIndices() throws Exception {
		assertEquals(soundex("index"), soundex("indices"));
	}

	public void testItches() throws Exception {
		assertEquals(soundex("itch"), soundex("itches"));
	}

	public void testJalapeno() throws Exception {
		assertEquals("J415", soundex("jalapeño"));
	}

	public void testKimonos() throws Exception {
		assertEquals(soundex("kimono"), soundex("kimonos"));
	}

	public void testLadies() throws Exception {
		assertEquals(soundex("lady"), soundex("ladies"));
	}

	public void testLampreys() throws Exception {
		assertEquals(soundex("lamprey"), soundex("lampreys"));
	}

	public void testLeaves() throws Exception {
		assertEquals(soundex("leaf"), soundex("leaves"));
	}

	public void testMatrices() throws Exception {
		assertEquals(soundex("matrix"), soundex("matrices"));
	}

	public void testMonkeys() throws Exception {
		assertEquals(soundex("monkey"), soundex("monkeys"));
	}

	public void testMoths() throws Exception {
		assertEquals(soundex("moth"), soundex("moths"));
	}

	public void testMouths() throws Exception {
		assertEquals(soundex("mouth"), soundex("mouths"));
	}

	public void testNothing() throws Exception {
		assertEquals("_000", soundex(""));
	}

	public void testNumbers() throws Exception {
		assertEquals("F000", soundex("F230"));
	}

	public void testOBrian() throws Exception {
		assertEquals("O165", soundex("O'Brian"));
	}

	public void testPhotos() throws Exception {
		assertEquals(soundex("photo"), soundex("photos"));
	}

	public void testPianos() throws Exception {
		assertEquals(soundex("piano"), soundex("pianos"));
	}

	public void testPluralS() throws Exception {
		assertEquals(soundex("word"), soundex("words"));
	}

	public void testPorticos() throws Exception {
		assertEquals(soundex("portico"), soundex("porticos"));
	}

	public void testProofs() throws Exception {
		assertEquals(soundex("proof"), soundex("proofs"));
	}

	public void testPros() throws Exception {
		assertEquals(soundex("pro"), soundex("pros"));
	}

	public void testQuarto() throws Exception {
		assertEquals(soundex("quarto"), soundex("quartos"));
	}

	public void testRays() throws Exception {
		assertEquals(soundex("ray"), soundex("rays"));
	}

	public void testResume() throws Exception {
		assertEquals("R250", soundex("résumé"));
	}

	public void testRobert() throws Exception {
		assertEquals("R163", soundex("Robert"));
	}

	public void testRoofs() throws Exception {
		assertEquals(soundex("roof"), soundex("roofs"));
	}

	public void testRubin() throws Exception {
		assertEquals("R150", soundex("Rubin"));
	}

	public void testRupert() throws Exception {
		assertEquals("R163", soundex("Rupert"));
	}

	public void testSharks() throws Exception {
		assertEquals(soundex("shark"), soundex("sharks"));
	}

	public void testSingular() throws Exception {
		assertEquals(soundex("words"), soundex("word"));
	}

	public void testTrees() throws Exception {
		assertEquals(soundex("tree"), soundex("trees"));
	}

	public void testZeros() throws Exception {
		assertEquals(soundex("zero"), soundex("zeros"));
	}

	public void testJ() throws Exception {
		assertEquals(soundex("j"), "J000");
	}

}
