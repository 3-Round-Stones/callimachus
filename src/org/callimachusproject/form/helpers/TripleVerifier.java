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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.GraphNodePath;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.Term;
import org.callimachusproject.server.exceptions.BadRequest;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.rio.RDFHandlerException;

/**
 * Track what node the triples are about and ensures they match one of the given
 * patterns.
 * 
 * @author James Leigh
 * 
 */
public final class TripleVerifier implements Cloneable {
	private static final String NOT_IN_EDIT_TEMPLATE = "http://callimachusproject.org/callimachus-for-web-developers#Edit_template";
	private static final String LDP = "http://www.w3.org/ns/ldp#";
	private static final String LDP_CONTAINS = LDP + "contains";
	private final AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
	private final Set<URI> subjects;
	private final Set<URI> partners;
	private final Map<Resource, Set<Statement>> connected;
	private final Map<Resource, Set<Statement>> disconnected;
	private final Set<URI> allTypes;
	private final Map<Resource, Set<URI>> types;
	private final Set<URI> ldpURIs;
	private boolean empty = true;
	private Set<TriplePattern> patterns;

	public TripleVerifier() {
		subjects = new HashSet<URI>();
		partners = new HashSet<URI>();
		connected = new HashMap<Resource, Set<Statement>>();
		disconnected = new HashMap<Resource, Set<Statement>>();
		allTypes = new LinkedHashSet<URI>();
		types = new HashMap<Resource, Set<URI>>();
		ldpURIs = new HashSet<>();
		empty = true;
	}

	@Override
	public TripleVerifier clone() {
		return new TripleVerifier(this);
	}

	private TripleVerifier(TripleVerifier cloned) {
		subjects = new HashSet<URI>(cloned.subjects);
		partners = new HashSet<URI>(cloned.partners);
		connected = new HashMap<Resource, Set<Statement>>(cloned.connected);
		disconnected = new HashMap<Resource, Set<Statement>>(cloned.disconnected);
		allTypes = new LinkedHashSet<URI>(cloned.allTypes);
		types = new HashMap<Resource, Set<URI>>(cloned.types);
		ldpURIs = new HashSet<>(cloned.ldpURIs);
		empty = cloned.empty;
		patterns = cloned.patterns;
	}

	public String toString() {
		return subjects.toString();
	}

	public void accept(RDFEventReader reader) throws RDFParseException {
		if (patterns == null) {
			patterns = new LinkedHashSet<TriplePattern>();
		}
		try {
			while (reader.hasNext()) {
				RDFEvent next = reader.next();
				if (next.isTriplePattern()) {
					TriplePattern tp = next.asTriplePattern();
					GraphNodePath pred = tp.getProperty();
					if (!tp.isInverse()) {
						if (pred.isIRI()) {
							accept(tp);
						}
					}
				}
			}
		} finally {
			reader.close();
		}
	}

	public void accept(GraphQueryResult reader) throws RDFParseException,
			QueryEvaluationException {
		if (patterns == null) {
			patterns = new LinkedHashSet<TriplePattern>();
		}
		try {
			while (reader.hasNext()) {
				Statement st = reader.next();
				Resource subj = st.getSubject();
				URI pred = st.getPredicate();
				Value obj = st.getObject();
				accept(new Triple((Node) asTerm(subj), (IRI) asTerm(pred),
						asTerm(obj)));
			}
		} finally {
			reader.close();
		}
	}

	public void accept(TriplePattern pattern) {
		if (patterns == null) {
			patterns = new LinkedHashSet<TriplePattern>();
		}
		patterns.add(pattern);
	}

	public boolean isDisconnectedNodePresent() {
		return !disconnected.isEmpty();
	}

	public Set<Statement> getConnections() {
		Set<Statement> set = new HashSet<Statement>();
		for (Set<Statement> nodes : connected.values()) {
			set.addAll(nodes);
		}
		return set;
	}

	public boolean isAbout(Resource about) {
		if (isEmpty())
			return true;
		if (!isSingleton())
			return false;
		URI subject = getSubject();
		if (subject.equals(about))
			return true;
		if (subject.getNamespace().equals(about.stringValue() + '#'))
			return true;
		return false;
	}

	public boolean isEmpty() {
		return empty;
	}

	public boolean isSingleton() {
		if (subjects.isEmpty())
			return false;
		if (subjects.size() == 1)
			return true;
		URI about = getSubject();
		String hash = about.stringValue() + "#";
		for (URI subj : subjects) {
			if (subj.equals(about))
				continue;
			if (subj.getNamespace().equals(hash))
				continue;
			return false;
		}
		return true;
	}

	public boolean isContainmentTriplePresent() {
		return ldpURIs.contains(new URIImpl(LDP_CONTAINS));
	}

	public URI getSubject() {
		URI about = null;
		for (URI subj : subjects) {
			String ns = subj.getNamespace();
			if (!ns.endsWith("#")) {
				return subj;
			} else if (about == null) {
				about = subj;
			} else if (ns.equals(about.getNamespace())) {
				return new URIImpl(ns.substring(0, ns.length() - 1));
			}
		}
		return about;
	}

	public void addSubject(URI subj) {
		subjects.add(subj);
	}

	public Set<URI> getAllTypes() {
		return allTypes;
	}

	public Set<URI> getTypes(URI subject) {
		if (types.containsKey(subject))
			return types.get(subject);
		return Collections.emptySet();
	}

	public Set<URI> getPartners() {
		return partners;
	}

	public void verify(Resource subj, URI pred, Value obj)
			throws RDFHandlerException {
		Set<TriplePattern> alternatives = findAlternatives(subj, pred, obj);
		if (alternatives != null && alternatives.isEmpty())
			throw new BadRequest("Triple pattern " + subj + " " + pred + " "
					+ obj + " must be present in template to use it")
					.addLdpConstraint(NOT_IN_EDIT_TEMPLATE);
		if (alternatives != null)
			throw new BadRequest("Triple " + subj + " " + pred + " " + obj
					+ " must match one of " + alternatives);
		if (subj instanceof URI) {
			addSubject((URI) subj);
		}
		if (RDF.TYPE.equals(pred) && obj instanceof URI) {
			if (!types.containsKey(subj)) {
				types.put(subj, new HashSet<URI>());
			}
			types.get(subj).add((URI) obj);
			allTypes.add((URI) obj);
			if (obj.stringValue().startsWith(LDP)) {
				ldpURIs.add((URI) obj);
			}
		} else if (pred.stringValue().startsWith(LDP)) {
			ldpURIs.add(pred);
		}
		link(subj, pred, obj);
		empty = false;
	}

	private Set<TriplePattern> findAlternatives(Resource subj, URI pred, Value obj) throws RDFHandlerException {
		if (patterns == null)
			return null;
		Term sterm = asTerm(subj);
		Term pterm = asTerm(pred);
		Term oterm = asTerm(obj);
		for (TriplePattern tp : patterns) {
			if (tp.getSubject().isIRI()) {
				if (!tp.getSubject().equals(sterm))
					continue;
			}
			if (tp.getProperty().isIRI()) {
				if (!tp.getProperty().equals(pterm))
					continue;
			}
			if (tp.getObject().isIRI() || tp.getObject().isLiteral()) {
				if (!tp.getObject().equals(oterm))
					continue;
			}
			if (tp.isInverse())
				throw new RDFHandlerException("Inverse relationships cannot be used here");
			return null;
		}
		Set<TriplePattern> alt1 = new LinkedHashSet<TriplePattern>();
		Set<TriplePattern> alt2 = new LinkedHashSet<TriplePattern>();
		for (TriplePattern tp : patterns) {
			if (tp.getProperty().equals(pterm)) {
				alt1.add(tp);
				if (tp.getSubject().equals(sterm)
						|| tp.getObject().equals(oterm)) {
					alt2.add(tp);
				}
			}
		}
		return alt2.isEmpty() ? alt1 : alt2;
	}

	private Term asTerm(Value obj) {
		if (obj instanceof Literal) {
			Literal lit = (Literal) obj;
			if (lit.getDatatype() != null) {
				return tf.literal(obj.stringValue(), tf.iri(lit.getDatatype()
						.stringValue()));
			} else if (lit.getLanguage() != null) {
				return tf.literal(obj.stringValue(), lit.getLanguage());
			} else {
				return tf.literal(obj.stringValue());
			}
		} else if (obj instanceof URI) {
			return tf.iri(obj.stringValue());
		} else {
			return tf.node(obj.stringValue());
		}
	}

	private void link(Resource subj, URI pred, Value obj) {
		boolean subjConnected = subj instanceof URI || connected.containsKey(subj);
		if (!subjConnected && !disconnected.containsKey(subj)) {
			disconnected.put(subj, new HashSet<Statement>());
		}
		if (obj instanceof URI) {
			URI uri = (URI) obj;
			String ns = uri.getNamespace();
			if (ns.endsWith("#")) {
				partners.add(new URIImpl(ns.substring(0, ns.length() - 1)));
			} else {
				partners.add(uri);
			}
		} else if (obj instanceof Resource && !connected.containsKey(obj)) {
			if (subjConnected) {
				connect(new StatementImpl(subj, pred, obj));
			} else {
				disconnected.get(subj).add(new StatementImpl(subj, pred, obj));
			}
		}
	}

	private void connect(Statement st) {
		Set<Statement> set = connected.get(st.getObject());
		if (set == null) {
			connected.put((Resource) st.getObject(), set = new HashSet<Statement>());
			set.add(st);
			Set<Statement> removed = disconnected.remove(st.getObject());
			if (removed != null) {
				for (Statement connecting : removed) {
					connect(connecting);
				}
			}
		}
	}

}
