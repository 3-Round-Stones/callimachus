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
package org.callimachusproject.helpers;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.Term;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;

/**
 * Track what node the triples are about and ensures they match one of the given
 * patterns.
 * 
 * @author James Leigh
 * 
 */
public class SubjectTracker extends RDFHandlerWrapper {
	private TermFactory tf = TermFactory.newInstance();
	private Set<URI> subjects = new HashSet<URI>();
	private Set<URI> resources = new HashSet<URI>();
	private Map<URI, Set<URI>> types = new HashMap<URI, Set<URI>>();
	private boolean reverseAllowed = true;
	private boolean wildPropertiesAllowed = true;
	private boolean empty = true;
	private final ValueFactory vf;
	private Set<TriplePattern> patterns;

	public SubjectTracker(RDFHandler delegate, ValueFactory vf) {
		super(delegate);
		this.vf = vf;
	}

	public void setReverseAllowed(boolean reverseAllowed) {
		this.reverseAllowed = reverseAllowed;
	}

	public void setWildPropertiesAllowed(boolean wildPropertiesAllowed) {
		this.wildPropertiesAllowed = wildPropertiesAllowed;
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
					VarOrIRI pred = tp.getPredicate();
					if (reverseAllowed || !tp.isInverse()) {
						if (wildPropertiesAllowed || pred.isIRI()) {
							accept(tp);
						}
					}
				}
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

	public boolean isAbout(Resource about) {
		return isSingleton() && getSubject().equals(about);
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

	public URI getSubject() {
		URI about = null;
		for (URI subj : subjects) {
			if (about == null) {
				about = subj;
			}
			if (!subj.getNamespace().endsWith("#"))
				return subj;
		}
		return about;
	}

	public void addSubject(URI subj) throws RDFHandlerException {
		subjects.add(canonicalize(subj));
	}

	public Set<URI> getTypes(URI subject) {
		if (types.containsKey(subject))
			return types.get(subject);
		return Collections.emptySet();
	}

	public Set<URI> getResources() {
		return resources;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		Resource subj = canonicalize(st.getSubject());
		URI pred = canonicalize(st.getPredicate());
		Value obj = canonicalize(st.getObject());
		Boolean rev = accept(subj, pred, obj);
		if (rev == null)
			throw new RDFHandlerException("Invalid triple: " + subj + " "
					+ pred + " " + obj);
		if (rev && obj instanceof URI) {
			addSubject((URI) obj);
		} else if (!rev && subj instanceof URI) {
			addSubject((URI) subj);
			if (RDF.TYPE.equals(pred) && obj instanceof URI) {
				if (!types.containsKey(subj)) {
					types.put((URI) subj, new HashSet<URI>());
				}
				types.get(subj).add((URI) obj);
			}
		}
		if (rev && subj instanceof URI) {
			resources.add((URI) subj);
		} else if (!rev && obj instanceof URI) {
			resources.add((URI) obj);
		}
		empty = false;
		super.handleStatement(new StatementImpl(subj, pred, obj));
	}

	private <V extends Value> V canonicalize(V value) throws RDFHandlerException {
		try {
			if (value instanceof URI) {
				return (V) canonicalizeURI((URI) value);
			}
		} catch (URISyntaxException e) {
			throw new RDFHandlerException(e.toString(), e);
		}
		return value;
	}

	private URI canonicalizeURI(URI uri) throws URISyntaxException {
		java.net.URI net = new java.net.URI(uri.stringValue());
		net.normalize();
		String scheme = net.getScheme().toLowerCase();
		String frag = net.getFragment();
		if (net.isOpaque()) {
			String part = net.getSchemeSpecificPart();
			net = new java.net.URI(scheme, part, frag);
			return vf.createURI(net.toASCIIString());
		}
		String auth = net.getAuthority().toLowerCase();
		String qs = net.getQuery();
		net = new java.net.URI(scheme, auth, net.getPath(), qs, frag);
		return vf.createURI(net.toASCIIString());
	}

	private Boolean accept(Resource subj, URI pred, Value obj) {
		if (patterns == null)
			return false;
		Boolean result = null;
		Term sterm = asTerm(subj);
		Term pterm = asTerm(pred);
		Term oterm = asTerm(obj);
		for (TriplePattern tp : patterns) {
			if (tp.getSubject().isIRI()) {
				if (!tp.getSubject().equals(sterm))
					continue;
			}
			if (tp.getPredicate().isIRI()) {
				if (!tp.getPredicate().equals(pterm))
					continue;
			}
			if (tp.getObject().isIRI() || tp.getObject().isLiteral()) {
				if (!tp.getObject().equals(oterm))
					continue;
			}
			result = tp.isInverse();
			if (!tp.isInverse() && subjects.contains(subj) || tp.isInverse()
					&& subjects.contains(obj))
				return result;
		}
		return result;
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

}
