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
package org.callimachusproject.helpers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.Term;
import org.callimachusproject.rdfa.model.TermFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
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
	private Set<URI> types = new HashSet<URI>();
	private boolean empty = true;
	private Resource matcher;
	private String hash;
	private Resource replacement;
	private Set<TriplePattern> patterns;

	public SubjectTracker(RDFHandler delegate) {
		super(delegate);
	}

	public void replace(Resource match, Resource replacement) {
		this.matcher = match;
		if (match instanceof URI && !match.stringValue().contains("#")) {
			this.hash = matcher.stringValue() + "#";
		}
		this.replacement = replacement;
	}

	public void accept(RDFEventReader reader) throws RDFParseException {
		try {
			while (reader.hasNext()) {
				RDFEvent next = reader.next();
				if (next.isTriplePattern()) {
					accept(next.asTriplePattern());
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

	public Set<URI> getTypes() {
		return types;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		if (subj.equals(matcher)) {
			subj = replacement;
		} else if (obj.equals(matcher)) {
			obj = replacement;
		} else if (subj instanceof URI && ((URI)subj).getNamespace().equals(hash)) {
			subj = new URIImpl(replacement.stringValue() + "#" + ((URI)subj).getLocalName());
		} else if (obj instanceof URI && ((URI)obj).getNamespace().equals(hash)) {
			obj = new URIImpl(replacement.stringValue() + "#" + ((URI)obj).getLocalName());
		}
		Boolean rev = accept(subj, pred, obj);
		if (rev == null)
			throw new RDFHandlerException("Invalid triple: " + subj + " "
					+ pred + " " + obj);
		if (rev && obj instanceof URI) {
			subjects.add((URI) obj);
		} else if (!rev && subj instanceof URI) {
			subjects.add((URI) subj);
			if (RDF.TYPE.equals(pred) && obj instanceof URI) {
				types.add((URI) obj);
			}
		}
		empty = false;
		super.handleStatement(new StatementImpl(subj, pred, obj));
	}

	private Boolean accept(Resource subj, URI pred, Value obj) {
		if (patterns == null)
			return false;
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
			return tp.isInverse();
		}
		return null;
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
