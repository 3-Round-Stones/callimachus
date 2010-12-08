/*
 * Copyright (c) 2009-2010, Zepheira LLC and James Leigh Some rights reserved.
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

import org.callimachusproject.concepts.Template;
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
import org.callimachusproject.stream.TriplePatternStore;
import org.callimachusproject.stream.TriplePatternVariableStore;
import org.callimachusproject.traits.SoundexTrait;
import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.transform;
import org.openrdf.http.object.annotations.type;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Implements the construct search method to lookup resources by label prefix.
 * 
 * @author James Leigh
 * 
 */
public abstract class SearchSupport implements Template, SoundexTrait,
		RDFObject, FileObject {
	private static TermFactory tf = TermFactory.newInstance();

	/**
	 * Returns an HTML page listing suggested resources for the given element.
	 */
	@operation("search")
	@type("application/rdf+xml")
	@cacheControl("must-reevaluate")
	@transform("http://callimachusproject.org/rdf/2009/framework#ConstructTemplate")
	public XMLEventReader constructSearch(@parameter("query") String query,
			@parameter("element") String element, @parameter("q") String q)
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
		RDFEventReader reader = openPatternReader(query, element, about);
		try {
			qry.consume(reader);
		} finally {
			reader.close();
		}
		return qry;
	}

	private IterableRDFEventReader filterPrefix(TriplePatternStore patterns,
			TriplePattern pattern, String q) {
		VarOrTerm obj = pattern.getObject();
		PlainLiteral phone = tf.literal(asSoundex(q));
		String regex = regexStartsWith(q);
		List<RDFEvent> list = new ArrayList<RDFEvent>();
		list.add(new Subject(true, obj));
		list.add(new TriplePattern(obj, tf.iri(SOUNDEX), phone));
		boolean filter = false;
		for (String pred : LABELS) {
			IRI iri = tf.iri(pred);
			for (TriplePattern tp : patterns.getPatternsByPredicate(iri)) {
				if (tp.getSubject().equals(obj)) {
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
}
