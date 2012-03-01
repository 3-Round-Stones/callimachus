package org.callimachusproject.rdfa.test;

import static org.callimachusproject.engine.helpers.SPARQLWriter.toSPARQL;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.Comment;
import org.callimachusproject.engine.events.Document;
import org.callimachusproject.engine.events.Group;
import org.callimachusproject.engine.events.Namespace;
import org.callimachusproject.engine.events.Optional;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Select;
import org.callimachusproject.engine.events.Subject;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.Union;
import org.callimachusproject.engine.events.Where;
import org.callimachusproject.engine.helpers.BlankOrLiteralVar;
import org.callimachusproject.engine.helpers.IterableRDFEventReader;
import org.callimachusproject.engine.helpers.OrderedSparqlReader;
import org.callimachusproject.engine.impl.IRIImpl;
import org.callimachusproject.engine.impl.VarImpl;
import org.junit.Assert;
import org.junit.Test;

public class OrderByTest {

	@Test
	public void testOnePattern() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true, null),
				new Namespace("", "http://www.w3.org/1999/xhtml", null),
				new Namespace("v", "http://www.w3.org/2006/vcard/ns#", null),
				new Select(null), new Where(true, null), new Subject(true,
						new IRIImpl("http://example.org/test"), null),
				new Subject(true, new IRIImpl("http://example.org/jd"), null),
				new Group(true, null), new TriplePattern(new IRIImpl(
						"http://example.org/jd"), new IRIImpl(
						"http://www.w3.org/2006/vcard/ns#fn"),
						new BlankOrLiteralVar("_fn"), false, null), new Group(
						false, null), new Subject(false, new IRIImpl(
						"http://example.org/jd"), null), new Subject(false,
						new IRIImpl("http://example.org/test"), null),
				new Where(false, null), new Comment(" @origin _fn /1/2/1/1 !",
						null), new Document(false, null));
		assertOrderBy(list, "ORDER BY ?_fn");
	}

	@Test
	public void testOptional() throws Exception {
		List<RDFEvent> list = Arrays
				.asList(new Document(true, null),
						new Namespace("", "http://www.w3.org/1999/xhtml", null),
						new Namespace("rdfs",
								"http://www.w3.org/2000/01/rdf-schema#", null),
						new Namespace("skos",
								"http://www.w3.org/2004/02/skos/core#", null),
						new Select(null),
						new Where(true, null),
						new Subject(true, new VarImpl("this"), null),
						new Group(true, null),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
								new BlankOrLiteralVar("_topConcept"), false,
								null),
						new Subject(true, new BlankOrLiteralVar("_topConcept"),
								null),
						new Optional(true, null),
						new TriplePattern(
								new BlankOrLiteralVar("_topConcept"),
								new IRIImpl(
										"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_topConcept_label"),
								false, null), new Optional(false, null),
						new Subject(false,
								new BlankOrLiteralVar("_topConcept"), null),
						new Group(false, null), new Subject(false, new VarImpl(
								"this"), null), new Where(false, null),
						new Comment(" @origin this /1/2", null), new Comment(
								" @origin _topConcept /1/2/1", null),
						new Comment(" @origin _topConcept_label /1/2/1/1 !",
								null), new Document(false, null));
		assertOrderBy(list, "ORDER BY ?this ?_topConcept ?_topConcept_label");
	}

	@Test
	public void testUnion() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true, null),
				new Namespace("", "http://www.w3.org/1999/xhtml", null),
				new Namespace("rdf",
						"http://www.w3.org/1999/02/22-rdf-syntax-ns#", null),
				new Namespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#",
						null), new Namespace("skos",
						"http://www.w3.org/2004/02/skos/core#", null),
				new Namespace("calli",
						"http://callimachusproject.org/rdf/2009/framework#",
						null), new Select(null), new Where(true, null),
				new Subject(true, new VarImpl("this"), null), new Group(true,
						null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
						new VarImpl("type"), false, null), new Group(false,
						null), new Subject(true, new VarImpl("create"), null),
				new Subject(true, new VarImpl("this"), null), new Union(null),
				new Group(true, null), new TriplePattern(new VarImpl("this"),
						new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label"), false, null),
				new Group(false, null), new Subject(false, new VarImpl("this"),
						null), new Subject(false, new VarImpl("create"), null),
				new Union(null), new Group(true, null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_comment"), false, null),
				new Group(false, null), new Subject(false, new VarImpl("this"),
						null), new Where(false, null), new Comment(
						" @origin this /1/2", null), new Comment(
						" @origin type /1/2/1", null), new Comment(
						" @origin _label /1/2/2/1/1/1 !", null), new Comment(
						" @origin _comment /1/2/3/1 !", null), new Document(
						false, null));
		assertOrderBy(list, "ORDER BY ?_comment ?_label ?this ?type");
	}

	@Test
	public void testOptionalUnion() throws Exception {
		List<RDFEvent> list = Arrays
				.asList(new Document(true, null),
						new Namespace("", "http://www.w3.org/1999/xhtml", null),
						new Namespace("rdfs",
								"http://www.w3.org/2000/01/rdf-schema#", null),
						new Namespace("skos",
								"http://www.w3.org/2004/02/skos/core#", null),
						new Select(null),
						new Where(true, null),
						new Subject(true, new VarImpl("this"), null),
						new Group(true, null),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
								new VarImpl("concept"), false, null),
						new Subject(true, new VarImpl("concept"), null),
						new Optional(true, null),
						new Group(true, null),
						new TriplePattern(
								new VarImpl("concept"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#altLabel"),
								new BlankOrLiteralVar("_concept_altLabel"),
								false, null),
						new Group(false, null),
						new Union(null),
						new Group(true, null),
						new TriplePattern(new VarImpl("concept"), new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_concept_label"), false,
								null),
						new Group(false, null),
						new Optional(false, null),
						new Subject(false, new VarImpl("concept"), null),
						new Group(false, null),
						new Subject(false, new VarImpl("this"), null),
						new Where(false, null),
						new Comment(" @origin this /1/2", null),
						new Comment(" @origin concept /1/2/1", null),
						new Comment(
								" @origin _concept_altLabel /1/2/1/1 skos:altLabel",
								null), new Comment(
								" @origin _concept_label /1/2/1/1 !", null),
						new Document(false, null));
		assertOrderBy(list,
				"ORDER BY ?this ?concept ?_concept_label ?_concept_altLabel");
	}

	public void testUnionOptional() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true, null),
				new Namespace("", "http://www.w3.org/1999/xhtml", null),
				new Namespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#",
						null), new Namespace("skos",
						"http://www.w3.org/2004/02/skos/core#", null),
				new Select(null), new Where(true, null), new Subject(true,
						new VarImpl("this"), null), new Group(true, null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label"), false, null),
				new Group(false, null), new Union(null), new Group(true, null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
						new VarImpl("concept"), false, null), new Subject(true,
						new VarImpl("concept"), null),
				new Optional(true, null), new TriplePattern(new VarImpl(
						"concept"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#prefLabel"),
						new BlankOrLiteralVar("_concept_prefLabel"), false,
						null), new Optional(false, null), new Subject(false,
						new VarImpl("concept"), null), new Group(false, null),
				new Union(null), new Group(true, null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_comment"), false, null),
				new Group(false, null), new Subject(false, new VarImpl("this"),
						null), new Where(false, null), new Comment(
						" @origin this /1/2", null), new Comment(
						" @origin _label /1/2/1 !", null), new Comment(
						" @origin concept /1/2/1/1/1", null), new Comment(
						" @origin _concept_prefLabel /1/2/1/1/1 !", null),
				new Comment(" @origin _comment /1/2/1/2 !", null),
				new Document(false, null));
		assertOrderBy(list,
				"ORDER BY ?_concept_label ?_concept_altLabel ?concept");
	}

	@Test
	public void testUnionUnionOptional() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true, null),
				new Namespace("", "http://www.w3.org/1999/xhtml", null),
				new Namespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#",
						null), new Namespace("foaf",
						"http://xmlns.com/foaf/0.1/", null), new Namespace(
						"skos", "http://www.w3.org/2004/02/skos/core#", null),
				new Select(null), new Where(true, null), new Subject(true,
						new IRIImpl("http://example.org/test"), null),
				new Subject(true, new VarImpl("this"), null), new Group(true,
						null), new TriplePattern(new VarImpl("this"),
						new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label"), false, null),
				new Group(false, null), new Subject(false, new VarImpl("this"),
						null), new Subject(false, new IRIImpl(
						"http://example.org/test"), null), new Subject(true,
						new VarImpl("this"), null), new Union(null), new Group(
						true, null), new TriplePattern(new VarImpl("this"),
						new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label1"), false, null),
				new Group(false, null), new Union(null), new Group(true, null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://xmlns.com/foaf/0.1/depiction"), new VarImpl(
						"image"), false, null), new Subject(true, new VarImpl(
						"image"), null), new Optional(true, null),
				new TriplePattern(new VarImpl("image"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_image_comment"), false, null),
				new Optional(false, null), new Subject(false, new VarImpl(
						"image"), null), new Group(false, null),
				new Union(null), new Group(true, null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_comment"), false, null),
				new Group(false, null), new Union(null), new Group(true, null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
						new VarImpl("top"), false, null), new Subject(true,
						new VarImpl("top"), null), new Optional(true, null),
				new TriplePattern(new VarImpl("top"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#prefLabel"),
						new BlankOrLiteralVar("_top_prefLabel"), false, null),
				new Optional(false, null), new Subject(false,
						new VarImpl("top"), null), new Group(false, null),
				new Subject(false, new VarImpl("this"), null), new Where(false,
						null), new Comment(" @origin this /1/2", null),
				new Comment(" @origin _label /1/1/1 !", null), new Comment(
						" @origin _label1 /1/2/1 !", null), new Comment(
						" @origin image /1/2/2", null), new Comment(
						" @origin _image_comment /1/2/2/2 !", null),
				new Comment(" @origin _comment /1/2/3 !", null), new Comment(
						" @origin top /1/2/4/1", null), new Comment(
						" @origin _top_prefLabel /1/2/4/1 !", null),
				new Document(false, null));
		assertOrderBy(
				list,
				"ORDER BY ?top ?_top_prefLabel ?_comment ?image ?_image_comment ?_label1 ?this ?_label");
	}

	@Test
	public void testOPtionalOptional() throws Exception {
		List<RDFEvent> list = Arrays
				.asList(new Document(true, null),
						new Namespace("", "http://www.w3.org/1999/xhtml", null),
						new Namespace("eg", "http://example.com#", null),
						new Namespace("skos",
								"http://www.w3.org/2004/02/skos/core#", null),
						new Namespace("skosxl",
								"http://www.w3.org/2008/05/skos-xl#", null),
						new Select(null),
						new Where(true, null),
						new Subject(true,
								new IRIImpl("http://example.org/test"), null),
						new Subject(true, new VarImpl("this"), null),
						new Group(true, null),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
								new IRIImpl("http://example.com#MyClass"),
								false, null),
						new Optional(true, null),
						new Group(true, null),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://example.com#myRel"), new VarImpl(
								"concept"), false, null),
						new Subject(true, new VarImpl("concept"), null),
						new TriplePattern(
								new VarImpl("concept"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#inScheme"),
								new IRIImpl(
										"http://example.org/samples/MyScheme1"),
								false, null),
						new Optional(true, null),
						new Group(true, null),
						new TriplePattern(new VarImpl("concept"), new IRIImpl(
								"http://www.w3.org/2008/05/skos-xl#prefLabel"),
								new VarImpl("lit"), false, null),
						new Subject(true, new VarImpl("lit"), null),
						new Optional(true, null),
						new TriplePattern(
								new VarImpl("lit"),
								new IRIImpl(
										"http://www.w3.org/2008/05/skos-xl#literalForm"),
								new BlankOrLiteralVar("_lit_literalForm"),
								false, null),
						new Optional(false, null),
						new Subject(false, new VarImpl("lit"), null),
						new Group(false, null),
						new Optional(false, null),
						new Subject(false, new VarImpl("concept"), null),
						new Group(false, null),
						new Optional(false, null),
						new Group(false, null),
						new Subject(false, new VarImpl("this"), null),
						new Subject(false, new IRIImpl(
								"http://example.org/test"), null),
						new Where(false, null),
						new Comment(" @origin this /1/2/1", null),
						new Comment(" @origin concept /1/2/1/2/1/1", null),
						new Comment(" @origin lit /1/2/1/2/1/1/2", null),
						new Comment(
								" @origin _lit_literalForm /1/2/1/2/1/1/2 skosxl:literalForm",
								null), new Document(false, null));
		assertOrderBy(list, "ORDER BY ?this ?concept ?lit ?_lit_literalForm");
	}

	@Test
	public void testNestedUnion() throws Exception {
		List<RDFEvent> list = Arrays
				.asList(new Document(true, null),
						new Namespace("", "http://www.w3.org/1999/xhtml", null),
						new Namespace("rdf",
								"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
								null),
						new Namespace("rdfs",
								"http://www.w3.org/2000/01/rdf-schema#", null),
						new Namespace("skos",
								"http://www.w3.org/2004/02/skos/core#", null),
						new Namespace("foaf", "http://xmlns.com/foaf/0.1/",
								null),
						new Namespace(
								"calli",
								"http://callimachusproject.org/rdf/2009/framework#",
								null),
						new Select(null),
						new Where(true, null),
						new Subject(true,
								new IRIImpl("http://example.org/test"), null),
						new Subject(true, new VarImpl("this"), null),
						new Group(true, null),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_label"), false, null),
						new Group(false, null),
						new Subject(false, new VarImpl("this"), null),
						new Subject(false, new IRIImpl(
								"http://example.org/test"), null),
						new Subject(true, new VarImpl("this"), null),
						new Subject(true, new VarImpl("this"), null),
						new Union(null),
						new Group(true, null),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_label1"), false, null),
						new Group(false, null),
						new Subject(false, new VarImpl("this"), null),
						new Subject(true, new VarImpl("this"), null),
						new Union(null),
						new Group(true, null),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#ConceptScheme"),
								false, null),
						new Optional(true, null),
						new Group(true, null),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_label2"), false, null),
						new Group(false, null),
						new Union(null),
						new Group(true, null),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
								new VarImpl("top"), false, null),
						new Subject(true, new VarImpl("top"), null),
						new TriplePattern(
								new VarImpl("top"),
								new IRIImpl(
										"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#Concept"),
								false, null),
						new Optional(true, null),
						new TriplePattern(
								new VarImpl("top"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#prefLabel"),
								new BlankOrLiteralVar("_top_prefLabel"), false,
								null), new Optional(false, null), new Subject(
								false, new VarImpl("top"), null), new Group(
								false, null), new Optional(false, null),
						new Group(false, null), new Subject(false, new VarImpl(
								"this"), null), new Subject(false, new VarImpl(
								"this"), null), new Where(false, null),
						new Comment(" @origin this /1/2", null), new Comment(
								" @origin _label /1/1/1 !", null), new Comment(
								" @origin _label1 /1/2/1/1 !", null),
						new Comment(" @origin _label2 /1/2/2/2/1 !", null),
						new Comment(" @origin top /1/2/2/4/1", null),
						new Comment(" @origin _top_prefLabel /1/2/2/4/1/1 !",
								null), new Document(false, null));
		assertOrderBy(list,
				"ORDER BY ?top ?_top_prefLabel ?_label2 ?_label1 ?this ?_label");
	}

	@Test
	public void testNoVariables() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true, null),
				new Namespace("", "http://www.w3.org/1999/xhtml", null),
				new Namespace("xmlns", "http://www.w3.org/1999/xhtml", null),
				new Namespace("cc", "http://creativecommons.org/ns#", null),
				new Select(null), new Where(true, null), new Subject(true,
						new IRIImpl("http://example.org/test"), null),
				new Subject(true, new VarImpl("this"), null), new Group(true,
						null),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://creativecommons.org/ns#license"), new IRIImpl(
						"http://creativecommons.org/licenses/by-nc-nd/2.5/"),
						false, null), new Optional(true, null),
				new Subject(true, new IRIImpl(
						"http://creativecommons.org/licenses/by-nc-nd/2.5/"),
						null), new Subject(false, new IRIImpl(
						"http://creativecommons.org/licenses/by-nc-nd/2.5/"),
						null), new Optional(false, null),
				new Group(false, null), new Subject(false, new VarImpl("this"),
						null), new Subject(false, new IRIImpl(
						"http://example.org/test"), null), new Where(false,
						null), new Comment(" @origin this /1/2/1/1", null),
				new Document(false, null));
		assertOrderBy(list, "ORDER BY ?this");
	}

	private void assertOrderBy(List<RDFEvent> list, String string)
			throws RDFParseException, IOException {
		RDFEventReader sparql = new IterableRDFEventReader(list);
		String query = toSPARQL(new OrderedSparqlReader(sparql));
		Assert.assertTrue(query.contains("ORDER BY"));
		String orderBy = query.substring(query.indexOf("ORDER BY"));
		orderBy = orderBy.substring(0, orderBy.indexOf("\n"));
		Assert.assertEquals(string, orderBy);
	}
}
