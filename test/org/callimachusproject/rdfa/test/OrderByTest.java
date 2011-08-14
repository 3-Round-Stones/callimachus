package org.callimachusproject.rdfa.test;

import static org.callimachusproject.stream.SPARQLWriter.toSPARQL;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Comment;
import org.callimachusproject.rdfa.events.Document;
import org.callimachusproject.rdfa.events.Group;
import org.callimachusproject.rdfa.events.Namespace;
import org.callimachusproject.rdfa.events.Optional;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Select;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.events.Union;
import org.callimachusproject.rdfa.events.Where;
import org.callimachusproject.rdfa.impl.IRIImpl;
import org.callimachusproject.rdfa.impl.VarImpl;
import org.callimachusproject.stream.BlankOrLiteralVar;
import org.callimachusproject.stream.IterableRDFEventReader;
import org.callimachusproject.stream.OrderedSparqlReader;
import org.junit.Assert;
import org.junit.Test;

public class OrderByTest {

	@Test
	public void testOnePattern() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true), new Namespace(
				"", "http://www.w3.org/1999/xhtml"), new Namespace("v",
				"http://www.w3.org/2006/vcard/ns#"), new Select(), new Where(
				true),
				new Subject(true, new IRIImpl("http://example.org/test")),
				new Subject(true, new IRIImpl("http://example.org/jd")),
				new Group(true), new TriplePattern(new IRIImpl(
						"http://example.org/jd"), new IRIImpl(
						"http://www.w3.org/2006/vcard/ns#fn"),
						new BlankOrLiteralVar("_fn"), false), new Group(false),
				new Subject(false, new IRIImpl("http://example.org/jd")),
				new Subject(false, new IRIImpl("http://example.org/test")),
				new Where(false), new Comment(" @origin _fn /1/2/1/1 !"),
				new Document(false));
		assertOrderBy(list, "ORDER BY ?_fn");
	}

	@Test
	public void testOptional() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true), new Namespace(
				"", "http://www.w3.org/1999/xhtml"), new Namespace("rdfs",
				"http://www.w3.org/2000/01/rdf-schema#"), new Namespace("skos",
				"http://www.w3.org/2004/02/skos/core#"), new Select(),
				new Where(true), new Subject(true, new VarImpl("this")),
				new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
						new BlankOrLiteralVar("_topConcept"), false),
				new Subject(true, new BlankOrLiteralVar("_topConcept")),
				new Optional(true), new TriplePattern(new BlankOrLiteralVar(
						"_topConcept"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_topConcept_label"), false),
				new Optional(false), new Subject(false, new BlankOrLiteralVar(
						"_topConcept")), new Group(false), new Subject(false,
						new VarImpl("this")), new Where(false), new Comment(
						" @origin this /1/2"), new Comment(
						" @origin _topConcept /1/2/1"), new Comment(
						" @origin _topConcept_label /1/2/1/1 !"), new Document(
						false));
		assertOrderBy(list, "ORDER BY ?_topConcept ?_topConcept_label");
	}

	@Test
	public void testUnion() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true), new Namespace(
				"", "http://www.w3.org/1999/xhtml"), new Namespace("rdf",
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#"), new Namespace(
				"rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
				new Namespace("skos", "http://www.w3.org/2004/02/skos/core#"),
				new Namespace("calli",
						"http://callimachusproject.org/rdf/2009/framework#"),
				new Select(), new Where(true), new Subject(true, new VarImpl(
						"this")), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
						new VarImpl("type"), false), new Group(false),
				new Subject(true, new VarImpl("create")), new Subject(true,
						new VarImpl("this")), new Union(), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label"), false), new Group(
						false), new Subject(false, new VarImpl("this")),
				new Subject(false, new VarImpl("create")), new Union(),
				new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_comment"), false), new Group(
						false), new Subject(false, new VarImpl("this")),
				new Where(false), new Comment(" @origin this /1/2"),
				new Comment(" @origin type /1/2/1"), new Comment(
						" @origin _label /1/2/2/1/1/1 !"), new Comment(
						" @origin _comment /1/2/3/1 !"), new Document(false));
		assertOrderBy(list, "ORDER BY ?_comment ?_label ?type");
	}

	@Test
	public void testOptionalUnion() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true), new Namespace(
				"", "http://www.w3.org/1999/xhtml"), new Namespace("rdfs",
				"http://www.w3.org/2000/01/rdf-schema#"), new Namespace("skos",
				"http://www.w3.org/2004/02/skos/core#"), new Select(),
				new Where(true), new Subject(true, new VarImpl("this")),
				new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
						new VarImpl("concept"), false), new Subject(true,
						new VarImpl("concept")), new Optional(true), new Group(
						true),
				new TriplePattern(new VarImpl("concept"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#altLabel"),
						new BlankOrLiteralVar("_concept_altLabel"), false),
				new Group(false), new Union(), new Group(true),
				new TriplePattern(new VarImpl("concept"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_concept_label"), false),
				new Group(false), new Optional(false), new Subject(false,
						new VarImpl("concept")), new Group(false), new Subject(
						false, new VarImpl("this")), new Where(false),
				new Comment(" @origin this /1/2"), new Comment(
						" @origin concept /1/2/1"), new Comment(
						" @origin _concept_altLabel /1/2/1/1 skos:altLabel"),
				new Comment(" @origin _concept_label /1/2/1/1 !"),
				new Document(false));
		assertOrderBy(list,
				"ORDER BY ?concept ?_concept_label ?_concept_altLabel");
	}

	public void testUnionOptional() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true), new Namespace(
				"", "http://www.w3.org/1999/xhtml"), new Namespace("rdfs",
				"http://www.w3.org/2000/01/rdf-schema#"), new Namespace("skos",
				"http://www.w3.org/2004/02/skos/core#"), new Select(),
				new Where(true), new Subject(true, new VarImpl("this")),
				new Group(true), new TriplePattern(new VarImpl("this"),
						new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label"), false), new Group(
						false), new Union(), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
						new VarImpl("concept"), false), new Subject(true,
						new VarImpl("concept")), new Optional(true),
				new TriplePattern(new VarImpl("concept"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#prefLabel"),
						new BlankOrLiteralVar("_concept_prefLabel"), false),
				new Optional(false),
				new Subject(false, new VarImpl("concept")), new Group(false),
				new Union(), new Group(true), new TriplePattern(new VarImpl(
						"this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_comment"), false), new Group(
						false), new Subject(false, new VarImpl("this")),
				new Where(false), new Comment(" @origin this /1/2"),
				new Comment(" @origin _label /1/2/1 !"), new Comment(
						" @origin concept /1/2/1/1/1"), new Comment(
						" @origin _concept_prefLabel /1/2/1/1/1 !"),
				new Comment(" @origin _comment /1/2/1/2 !"),
				new Document(false));
		assertOrderBy(list,
				"ORDER BY ?_concept_label ?_concept_altLabel ?concept");
	}

	@Test
	public void testUnionUnionOptional() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true), new Namespace(
				"", "http://www.w3.org/1999/xhtml"), new Namespace("rdfs",
				"http://www.w3.org/2000/01/rdf-schema#"), new Namespace("foaf",
				"http://xmlns.com/foaf/0.1/"), new Namespace("skos",
				"http://www.w3.org/2004/02/skos/core#"), new Select(),
				new Where(true), new Subject(true, new IRIImpl(
						"http://example.org/test")), new Subject(true,
						new VarImpl("this")), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label"), false), new Group(
						false), new Subject(false, new VarImpl("this")),
				new Subject(false, new IRIImpl("http://example.org/test")),
				new Subject(true, new VarImpl("this")), new Union(), new Group(
						true), new TriplePattern(new VarImpl("this"),
						new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
						new BlankOrLiteralVar("_label1"), false), new Group(
						false), new Union(), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://xmlns.com/foaf/0.1/depiction"), new VarImpl(
						"image"), false), new Subject(true,
						new VarImpl("image")), new Optional(true),
				new TriplePattern(new VarImpl("image"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_image_comment"), false),
				new Optional(false), new Subject(false, new VarImpl("image")),
				new Group(false), new Union(), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2000/01/rdf-schema#comment"),
						new BlankOrLiteralVar("_comment"), false), new Group(
						false), new Union(), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
						new VarImpl("top"), false), new Subject(true,
						new VarImpl("top")), new Optional(true),
				new TriplePattern(new VarImpl("top"), new IRIImpl(
						"http://www.w3.org/2004/02/skos/core#prefLabel"),
						new BlankOrLiteralVar("_top_prefLabel"), false),
				new Optional(false), new Subject(false, new VarImpl("top")),
				new Group(false), new Subject(false, new VarImpl("this")),
				new Where(false), new Comment(" @origin this /1/2"),
				new Comment(" @origin _label /1/1/1 !"), new Comment(
						" @origin _label1 /1/2/1 !"), new Comment(
						" @origin image /1/2/2"), new Comment(
						" @origin _image_comment /1/2/2/2 !"), new Comment(
						" @origin _comment /1/2/3 !"), new Comment(
						" @origin top /1/2/4/1"), new Comment(
						" @origin _top_prefLabel /1/2/4/1 !"), new Document(
						false));
		assertOrderBy(
				list,
				"ORDER BY ?top ?_top_prefLabel ?_comment ?image ?_image_comment ?_label1 ?_label");
	}

	@Test
	public void testOPtionalOptional() throws Exception {
		List<RDFEvent> list = Arrays
				.asList(
						new Document(true),
						new Namespace("", "http://www.w3.org/1999/xhtml"),
						new Namespace("eg", "http://example.com#"),
						new Namespace("skos",
								"http://www.w3.org/2004/02/skos/core#"),
						new Namespace("skosxl",
								"http://www.w3.org/2008/05/skos-xl#"),
						new Select(),
						new Where(true),
						new Subject(true,
								new IRIImpl("http://example.org/test")),
						new Subject(true, new VarImpl("this")),
						new Group(true),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
								new IRIImpl("http://example.com#MyClass"),
								false),
						new Optional(true),
						new Group(true),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://example.com#myRel"), new VarImpl(
								"concept"), false),
						new Subject(true, new VarImpl("concept")),
						new TriplePattern(
								new VarImpl("concept"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#inScheme"),
								new IRIImpl(
										"http://example.org/samples/MyScheme1"),
								false),
						new Optional(true),
						new Group(true),
						new TriplePattern(new VarImpl("concept"), new IRIImpl(
								"http://www.w3.org/2008/05/skos-xl#prefLabel"),
								new VarImpl("lit"), false),
						new Subject(true, new VarImpl("lit")),
						new Optional(true),
						new TriplePattern(
								new VarImpl("lit"),
								new IRIImpl(
										"http://www.w3.org/2008/05/skos-xl#literalForm"),
								new BlankOrLiteralVar("_lit_literalForm"),
								false),
						new Optional(false),
						new Subject(false, new VarImpl("lit")),
						new Group(false),
						new Optional(false),
						new Subject(false, new VarImpl("concept")),
						new Group(false),
						new Optional(false),
						new Group(false),
						new Subject(false, new VarImpl("this")),
						new Subject(false, new IRIImpl(
								"http://example.org/test")),
						new Where(false),
						new Comment(" @origin this /1/2/1"),
						new Comment(" @origin concept /1/2/1/2/1/1"),
						new Comment(" @origin lit /1/2/1/2/1/1/2"),
						new Comment(
								" @origin _lit_literalForm /1/2/1/2/1/1/2 skosxl:literalForm"),
						new Document(false));
		assertOrderBy(list, "ORDER BY ?concept ?lit ?_lit_literalForm");
	}

	@Test
	public void testNestedUnion() throws Exception {
		List<RDFEvent> list = Arrays
				.asList(
						new Document(true),
						new Namespace("", "http://www.w3.org/1999/xhtml"),
						new Namespace("rdf",
								"http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
						new Namespace("rdfs",
								"http://www.w3.org/2000/01/rdf-schema#"),
						new Namespace("skos",
								"http://www.w3.org/2004/02/skos/core#"),
						new Namespace("foaf", "http://xmlns.com/foaf/0.1/"),
						new Namespace("calli",
								"http://callimachusproject.org/rdf/2009/framework#"),
						new Select(),
						new Where(true),
						new Subject(true,
								new IRIImpl("http://example.org/test")),
						new Subject(true, new VarImpl("this")),
						new Group(true),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_label"), false),
						new Group(false),
						new Subject(false, new VarImpl("this")),
						new Subject(false, new IRIImpl(
								"http://example.org/test")),
						new Subject(true, new VarImpl("this")),
						new Subject(true, new VarImpl("this")),
						new Union(),
						new Group(true),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_label1"), false),
						new Group(false),
						new Subject(false, new VarImpl("this")),
						new Subject(true, new VarImpl("this")),
						new Union(),
						new Group(true),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#ConceptScheme"),
								false),
						new Optional(true),
						new Group(true),
						new TriplePattern(new VarImpl("this"), new IRIImpl(
								"http://www.w3.org/2000/01/rdf-schema#label"),
								new BlankOrLiteralVar("_label2"), false),
						new Group(false),
						new Union(),
						new Group(true),
						new TriplePattern(
								new VarImpl("this"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#hasTopConcept"),
								new VarImpl("top"), false),
						new Subject(true, new VarImpl("top")),
						new TriplePattern(
								new VarImpl("top"),
								new IRIImpl(
										"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#Concept"),
								false),
						new Optional(true),
						new TriplePattern(
								new VarImpl("top"),
								new IRIImpl(
										"http://www.w3.org/2004/02/skos/core#prefLabel"),
								new BlankOrLiteralVar("_top_prefLabel"), false),
						new Optional(false), new Subject(false, new VarImpl(
								"top")), new Group(false), new Optional(false),
						new Group(false), new Subject(false,
								new VarImpl("this")), new Subject(false,
								new VarImpl("this")), new Where(false),
						new Comment(" @origin this /1/2"), new Comment(
								" @origin _label /1/1/1 !"), new Comment(
								" @origin _label1 /1/2/1/1 !"), new Comment(
								" @origin _label2 /1/2/2/2/1 !"), new Comment(
								" @origin top /1/2/2/4/1"), new Comment(
								" @origin _top_prefLabel /1/2/2/4/1/1 !"),
						new Document(false));
		assertOrderBy(list,
				"ORDER BY ?top ?_top_prefLabel ?_label2 ?_label1 ?_label");
	}

	@Test
	public void testNoVariables() throws Exception {
		List<RDFEvent> list = Arrays.asList(new Document(true), new Namespace(
				"", "http://www.w3.org/1999/xhtml"), new Namespace("xmlns",
				"http://www.w3.org/1999/xhtml"), new Namespace("cc",
				"http://creativecommons.org/ns#"), new Select(),
				new Where(true), new Subject(true, new IRIImpl(
						"http://example.org/test")), new Subject(true,
						new VarImpl("this")), new Group(true),
				new TriplePattern(new VarImpl("this"), new IRIImpl(
						"http://creativecommons.org/ns#license"), new IRIImpl(
						"http://creativecommons.org/licenses/by-nc-nd/2.5/"),
						false), new Optional(true),
				new Subject(true, new IRIImpl(
						"http://creativecommons.org/licenses/by-nc-nd/2.5/")),
				new Subject(false, new IRIImpl(
						"http://creativecommons.org/licenses/by-nc-nd/2.5/")),
				new Optional(false), new Group(false), new Subject(false,
						new VarImpl("this")), new Subject(false, new IRIImpl(
						"http://example.org/test")), new Where(false),
				new Comment(" @origin this /1/2/1/1"), new Document(false));
		RDFEventReader sparql = new IterableRDFEventReader(list);
		String query = toSPARQL(new OrderedSparqlReader(sparql));
		Assert.assertFalse(query.contains("ORDER BY"));
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
