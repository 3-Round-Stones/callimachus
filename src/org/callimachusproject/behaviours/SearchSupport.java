/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
  Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
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
package org.callimachusproject.behaviours;

import static org.callimachusproject.stream.SPARQLWriter.toSPARQL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.concepts.Page;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.BuiltInCall;
import org.callimachusproject.rdfa.events.ConditionalOrExpression;
import org.callimachusproject.rdfa.events.Expression;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.callimachusproject.stream.IterableRDFEventReader;
import org.callimachusproject.stream.RDFStoreReader;
import org.callimachusproject.stream.RDFXMLEventReader;
import org.callimachusproject.stream.ReducedTripleReader;
import org.callimachusproject.stream.SPARQLResultReader;
import org.callimachusproject.stream.TriplePatternStore;
import org.callimachusproject.stream.TriplePatternVariableStore;
import org.callimachusproject.traits.SoundexTrait;
import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Implements the construct search method to lookup resources by label prefix
 * and options method to list all possible values.
 * 
 * @author James Leigh
 * 
 */
public abstract class SearchSupport implements Page, SoundexTrait,
		RDFObject, FileObject {
	private static TermFactory tf = TermFactory.newInstance();

	/**
	 * Returns the given element with all known possible children.
	 */
	@query("options")
	@type("application/rdf+xml")
	@header("cache-control:no-store")
	public XMLEventReader options(@query("query") String query,
			@query("element") String element) throws Exception {
		String base = toUri().toASCIIString();
		TriplePatternStore patterns = readPatternStore(query, element, base);
		TriplePattern pattern = patterns.getFirstTriplePattern();
		RDFEventReader q = constructPossibleTriples(patterns, pattern);
		ObjectConnection con = getObjectConnection();

		/* consume UNION form of sparql construct */
		// RDFEventReader rdf = new RDFStoreReader(toSPARQL(q), patterns, con);
		RDFEventReader rdf = new SPARQLResultReader(toSPARQL(q), patterns, con);
		
		return new RDFXMLEventReader(new ReducedTripleReader(rdf));
	}

	/**
	 * Returns an HTML page listing suggested resources for the given element.
	 */
	@query("search")
	@type("application/rdf+xml")
	@header("cache-control:no-validate,max-age=60")
	public XMLEventReader constructSearch(@query("query") String query,
			@query("element") String element, @query("q") String q)
			throws Exception {
		String base = toUri().toASCIIString();
		TriplePatternStore patterns = readPatternStore(query, element, base);
		TriplePattern pattern = patterns.getFirstTriplePattern();
		patterns.consume(filterPrefix(patterns, pattern, q));
		RDFEventReader qry = constructPossibleTriples(patterns, pattern);
		ObjectConnection con = getObjectConnection();
		RDFEventReader rdf = new RDFStoreReader(toSPARQL(qry), patterns, con);
		return new RDFXMLEventReader(new ReducedTripleReader(rdf));
	}

	private TriplePatternStore readPatternStore(String query, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException, RDFParseException {
		String base = toUri().toASCIIString();
		TriplePatternStore qry = new TriplePatternVariableStore(base);
		RDFEventReader reader = openPatternReader(about, query, element);
		try {
			qry.consume(reader);
		} finally {
			reader.close();
		}
		return qry;
	}

	private IterableRDFEventReader filterPrefix(TriplePatternStore patterns,
			TriplePattern pattern, String q) {
		VarOrTerm obj = pattern.getPartner();
		PlainLiteral phone = tf.literal(asSoundex(q));
		String regex = regexStartsWith(q);
		List<RDFEvent> list = new ArrayList<RDFEvent>();
		list.add(new Subject(true, obj));
		list.add(new TriplePattern(obj, tf.iri(SOUNDEX), phone));
		boolean filter = false;
		for (String pred : LABELS) {
			IRI iri = tf.iri(pred);
			for (TriplePattern tp : patterns.getPatternsByPredicate(iri)) {
				if (tp.getAbout().equals(obj)) {
					if (filter) {
						list.add(new ConditionalOrExpression());
					} else {
						filter = true;
					}
					list.add(new BuiltInCall(true, "regex"));
					list.add(new BuiltInCall(true, "str"));
					list.add(new Expression(tp.getObject()));
					list.add(new BuiltInCall(false, "str"));
					list.add(new Expression(tf.literal(regex)));
					list.add(new Expression(tf.literal("i")));
					list.add(new BuiltInCall(false, "regex"));
				}
			}
		}
		list.add(new Subject(false, obj));
		return new IterableRDFEventReader(list);
	}

	public RDFEventReader constructPossibleTriples(TriplePatternStore patterns,
			TriplePattern pattern) {
		VarOrTerm subj = pattern.getPartner();
		return patterns.openQueryBySubject(subj);
	}
}
