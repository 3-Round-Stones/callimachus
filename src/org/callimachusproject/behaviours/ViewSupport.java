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
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.concepts.Template;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.CURIE;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.stream.About;
import org.callimachusproject.stream.PrependTriple;
import org.callimachusproject.stream.RDFStoreReader;
import org.callimachusproject.stream.RDFXMLEventReader;
import org.callimachusproject.stream.ReducedTripleReader;
import org.callimachusproject.stream.TriplePatternStore;
import org.callimachusproject.stream.TriplePatternVariableStore;
import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.transform;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Extracts parts of this template and constructs the RDF needed for this
 * template.
 * 
 * @author James Leigh
 * 
 */
public abstract class ViewSupport implements Template, RDFObject,
		VersionedObject, FileObject {
	private static final String NS = "http://callimachusproject.org/rdf/2009/framework#";
	private static TermFactory tf = TermFactory.newInstance();

	@Override
	public InputStream calliConstruct(Object target)
			throws Exception {
		return calliConstruct(target, null);
	}

	@Override
	public InputStream calliConstruct(Object target, String query)
			throws Exception {
		String uri = null;
		if (target instanceof RDFObject) {
			uri = ((RDFObject) target).getResource().stringValue();
		}
		XMLEventReader data = calliConstructRDF(query, null, uri);
		return calliConstructTemplate(null, query, data);
	}

	/**
	 * Extracts an element from the template (without variables) and populates
	 * the element with the properties of the about resource.
	 */
	@operation("construct")
	@type("application/rdf+xml")
	@cacheControl("no-store")
	@transform("http://callimachusproject.org/rdf/2009/framework#construct-template")
	public XMLEventReader calliConstruct(@parameter("query") String query,
			@parameter("element") String element,
			@parameter("about") String about) throws Exception {
		if (about != null && (element == null || element.equals("/1")))
			throw new BadRequest("Missing element parameter");
		return calliConstructRDF(query, element, about);
	}

	/**
	 * Extracts an element from the template (without variables).
	 */
	@operation("template")
	@transform("http://callimachusproject.org/rdf/2009/framework#construct-template")
	public XMLEventReader template(@parameter("query") String query,
			@parameter("element") String element) throws Exception {
		return null;
	}

	/**
	 * Returns the given element with all known possible children.
	 */
	@operation("options")
	@type("application/rdf+xml")
	@cacheControl("no-store")
	@transform("http://callimachusproject.org/rdf/2009/framework#construct-template")
	public XMLEventReader options(@parameter("query") String query,
			@parameter("element") String element) throws Exception {
		String base = toUri().toASCIIString();
		TriplePatternStore patterns = readPatternStore(query, element, base);
		TriplePattern pattern = patterns.getFirstTriplePattern();
		RDFEventReader q = constructPossibleTriples(patterns, pattern);
		ObjectConnection con = getObjectConnection();
		RDFEventReader rdf = new RDFStoreReader(toSPARQL(q), patterns, con);
		return new RDFXMLEventReader(new ReducedTripleReader(rdf));
	}

	@operation("query")
	@type("application/sparql-query")
	public byte[] query(@parameter("element") String element, @parameter("about") String about)
			throws XMLStreamException, IOException, TransformerException,
			RDFParseException {
		String base = toUri().toASCIIString();
		TriplePatternStore query = new TriplePatternStore(base);
		String uri = about == null ? query.resolve(query.getReference()) : query.resolve(about);
		RDFEventReader reader = readPatterns("view", element, uri);
		try {
			query.consume(reader);
		} finally {
			reader.close();
		}
		return query.toString().getBytes(Charset.forName("UTF-8"));
	}

	protected RDFEventReader readPatterns(String query, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException {
		return openPatternReader(query, element, about);
	}

	protected abstract InputStream calliConstructTemplate(String element,
			String query, XMLEventReader rdf) throws Exception;

	private XMLEventReader calliConstructRDF(String query, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException, RDFParseException, RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		String uri = about == null ? null : toUri().resolve(about).toASCIIString();
		TriplePatternStore qry = readPatternStore(query, element, uri);
		ObjectConnection con = getObjectConnection();
		RDFEventReader rdf = new RDFStoreReader(qry, con, uri);
		rdf = new ReducedTripleReader(rdf);
		if (uri != null && element == null) {
			IRI subj = tf.iri(uri);
			rdf = new About(subj, rdf);
			String label = getETag(uri);
			if (label != null) {
				CURIE etag = tf.curie(NS, "etag", "calli");
				PlainLiteral obj = tf.literal(label);
				rdf = new PrependTriple(new Triple(subj, etag, obj), rdf);
			}
		}
		return new RDFXMLEventReader(rdf);
	}

	private TriplePatternStore readPatternStore(String query, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException, RDFParseException {
		String base = toUri().toASCIIString();
		TriplePatternStore qry = new TriplePatternVariableStore(base);
		RDFEventReader reader = readPatterns(query, element, about);
		try {
			qry.consume(reader);
		} finally {
			reader.close();
		}
		return qry;
	}

	private String getETag(String uri) throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		VersionedObject target = (VersionedObject) con.getObject(uri);
		return target.revisionTag(0);
	}
}
