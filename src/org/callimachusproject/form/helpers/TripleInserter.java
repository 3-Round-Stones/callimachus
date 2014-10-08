/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
   Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
 * Portions Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.form.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.TermFactory;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;

/**
 * Track what node the triples are about and ensures they match one of the given
 * patterns.
 * 
 * @author James Leigh
 * 
 */
public class TripleInserter implements RDFHandler {
	private static final String LDP_RDF_SOURCE = "http://www.w3.org/ns/ldp#RDFSource";
	private static final String FOAF_PRIMARY_TOPIC = "http://xmlns.com/foaf/0.1/primaryTopic";
	private final RepositoryConnection con;
	private final ValueFactory vf;
	private final TripleVerifier verifier = new TripleVerifier();
	private final RDFHandler inserter;
	private final Set<String> prefixes = new HashSet<String>();
	private final Set<String> namespaces = new HashSet<String>();
	private boolean ignoringDocumentMetadata;
	private String base;
	private URI graph;
	private URI documentURI;
	private URI primaryTopic;

	public TripleInserter(RepositoryConnection con) throws RepositoryException {
		this(new RDFInserter(con), con);
	}

	public TripleInserter(RDFHandler handler, RepositoryConnection con) throws RepositoryException {
		this.con = con;
		this.vf = con.getValueFactory();
		this.inserter = handler;
		RepositoryResult<Namespace> currently = con.getNamespaces();
		try {
			while (currently.hasNext()) {
				Namespace ns = currently.next();
				prefixes.add(ns.getPrefix());
				namespaces.add(ns.getName());
			}
		} finally {
			currently.close();
		}
	}

	public String getBaseURI() {
		return base;
	}

	public void setBaseURI(String base) {
		this.base = base;
	}

	/**
	 * Gets the graph that this enforces upon all statements that
	 * are reported to it.
	 * 
	 * @return A URI identifying the contexts, or <tt>null</tt> if no
	 *         contexts is enforced.
	 */
	public URI getGraph() {
		return graph;
	}

	/**
	 * Enforces the supplied graph upon all statements that are reported.
	 * 
	 * @param contexts
	 *        the contexts to use.
	 */
	public void setGraph(URI graph) {
		this.graph = graph;
	}

	public boolean isIgnoringDocumentMetadata() {
		return ignoringDocumentMetadata;
	}

	public void setIgnoringDocumentMetadata(boolean ignoringDocumentMetadata) {
		this.ignoringDocumentMetadata = ignoringDocumentMetadata;
	}

	public synchronized void parseAndInsert(InputStream in, String type, String base)
			throws IOException, OpenRDFException {
		setBaseURI(base);
		RDFFormat format = RDFFormat.forMIMEType(type);
		RDFParserRegistry registry = RDFParserRegistry.getInstance();
		RDFParser parser = registry.get(format).getParser();
		parser.setValueFactory(con.getValueFactory());
		parser.setRDFHandler(this);
		documentURI = con.getValueFactory().createURI(base);
		parser.parse(in, base);
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (st.getContext() != null
				&& !st.getContext().stringValue().equals(base))
			throw new RDFHandlerException(
					"Only the default graph can be used, not "
							+ st.getContext());
		Statement s = canonicalize(st);
		Resource subj = s.getSubject();
		URI pred = s.getPredicate();
		Value obj = s.getObject();
		if (!ignoringDocumentMetadata && subj.equals(documentURI)
				&& pred.equals(RDF.TYPE) && obj instanceof URI
				&& obj.stringValue().equals(LDP_RDF_SOURCE)) {
			// ignore
		} else if (!ignoringDocumentMetadata && subj.equals(documentURI)
				&& pred.stringValue().equals(FOAF_PRIMARY_TOPIC)
				&& obj instanceof URI) {
			primaryTopic = (URI) obj;
			verifier.addSubject(primaryTopic);
		} else {
			verifier.verify(subj, pred, obj);
			inserter.handleStatement(s);
		}
	}

	public void accept(RDFEventReader reader) throws RDFParseException {
		verifier.accept(reader);
	}

	public void accept(TriplePattern pattern) {
		verifier.accept(pattern);
	}

	public boolean isDisconnectedNodePresent() {
		return verifier.isDisconnectedNodePresent();
	}

	public boolean isAbout(Resource about) {
		return verifier.isAbout(about);
	}

	public boolean isEmpty() {
		return verifier.isEmpty();
	}

	public boolean isSingleton() {
		return verifier.isSingleton();
	}

	public boolean isContainmentTriplePresent() {
		return verifier.isContainmentTriplePresent();
	}

	public URI getSubject() {
		return verifier.getSubject();
	}

	public URI getPrimaryTopic() {
		if (primaryTopic != null)
			return primaryTopic;
		return verifier.getSubject();
	}

	public Set<URI> getAllTypes() {
		return verifier.getAllTypes();
	}

	public Set<URI> getTypes(URI subject) {
		return verifier.getTypes(subject);
	}

	public Set<URI> getPartners() {
		return verifier.getPartners();
	}

	public String toString() {
		return con.toString();
	}

	public void startRDF() throws RDFHandlerException {
		inserter.startRDF();
	}

	public void handleComment(String comment) throws RDFHandlerException {
		inserter.handleComment(comment);
	}

	public void endRDF() throws RDFHandlerException {
		inserter.endRDF();
	}

	public void handleNamespace(String prefix, String name) throws RDFHandlerException {
		if (!prefixes.contains(prefix) && !namespaces.contains(name)) {
			prefixes.add(prefix);
			namespaces.add(name);
			inserter.handleNamespace(prefix, name);
		}
	}

	private Statement canonicalize(Statement st) throws RDFHandlerException {
		Resource subj = canonicalize(st.getSubject());
		URI pred = canonicalize(st.getPredicate());
		Value obj = canonicalize(st.getObject());
		URI graph = getGraph();
		if (graph != null) {
			return new ContextStatementImpl(subj, pred, obj, graph);
		} else {
			return new StatementImpl(subj, pred, obj);
		}
	}

	private <V extends Value> V canonicalize(V value) throws RDFHandlerException {
		try {
			if (value instanceof URI) {
				String uri = value.stringValue();
				String iri = canonicalize(uri);
				if (uri.equals(iri))
					return value;
				return (V) vf.createURI(iri);
			}
		} catch (IllegalArgumentException e) {
			throw new RDFHandlerException(e.toString(), e);
		}
		return value;
	}

	private String canonicalize(String uri) {
		return TermFactory.newInstance(uri).getSystemId();
	}

}
