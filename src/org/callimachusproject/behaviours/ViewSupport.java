/*
   Copyright 2009 Zepheira LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.Reader;
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
import org.callimachusproject.stream.SPARQLWriter;
import org.callimachusproject.stream.TriplePatternStore;
import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.transform;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

public abstract class ViewSupport implements Template, RDFObject,
		VersionedObject, FileObject {
	private static final String NS = "http://callimachusproject.org/rdf/2009/framework#";
	private static TermFactory tf = TermFactory.newInstance();

	@Override
	public Reader calliConstruct(String mode, Object target)
			throws Exception {
		String uri = null;
		if (target instanceof RDFObject) {
			uri = ((RDFObject) target).getResource().stringValue();
		}
		XMLEventReader data = calliConstructRDF(mode, null, uri);
		return calliConstructTemplate(mode, null, data);
	}

	/**
	 * Extracts an element from the template (without variables) and populates
	 * the element with the properties of the about resource.
	 */
	@operation("construct")
	@type("application/rdf+xml")
	@cacheControl("no-store")
	@transform("http://callimachusproject.org/rdf/2009/framework#construct-template")
	public XMLEventReader calliConstruct(@parameter("mode") String mode,
			@parameter("element") String element,
			@parameter("about") String about) throws Exception {
		return calliConstructRDF(mode, element, about);
	}

	/**
	 * Extracts an element from the template (without variables).
	 */
	@operation("template")
	@transform("http://callimachusproject.org/rdf/2009/framework#construct-template")
	public XMLEventReader template(@parameter("mode") String mode,
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
	public XMLEventReader options(@parameter("mode") String mode,
			@parameter("element") String element) throws Exception {
		String base = toUri().toASCIIString();
		TriplePatternStore patterns = readPatternStore(mode, element, base);
		TriplePattern pattern = patterns.getFirstTriplePattern();
		RDFEventReader query = constructPossibleTriples(patterns, pattern);
		ObjectConnection con = getObjectConnection();
		RDFEventReader rdf = new RDFStoreReader(toSPARQL(query), patterns, con);
		return new RDFXMLEventReader(new ReducedTripleReader(rdf));
	}

	@operation("query")
	@type("application/sparql-query")
	public byte[] query(@parameter("element") String element, @parameter("about") String about)
			throws XMLStreamException, IOException, TransformerException,
			RDFParseException {
		java.net.URI net = about == null ? toUri() : toUri().resolve(about);
		String uri = net.toASCIIString();
		String query = readPatternStore("view", element, uri).toString();
		return query.getBytes(Charset.forName("UTF-8"));
	}

	protected String toSPARQL(RDFEventReader query) throws RDFParseException,
			IOException {
		return SPARQLWriter.toSPARQL(query);
	}

	protected abstract Reader calliConstructTemplate(String mode,
			String element, XMLEventReader rdf) throws Exception;

	private XMLEventReader calliConstructRDF(String mode, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException, RDFParseException, RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		String uri = about == null ? null : toUri().resolve(about).toASCIIString();
		TriplePatternStore query = readPatternStore(mode, element, uri);
		ObjectConnection con = getObjectConnection();
		RDFEventReader rdf = new RDFStoreReader(toSPARQL(query.openQueryReader()), query, con, uri);
		rdf = new ReducedTripleReader(rdf);
		if (uri != null) {
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

	private String getETag(String uri) throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		VersionedObject target = (VersionedObject) con.getObject(uri);
		return target.revisionTag(0);
	}
}
