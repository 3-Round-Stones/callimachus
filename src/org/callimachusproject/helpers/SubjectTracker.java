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
package org.callimachusproject.helpers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.Term;
import org.callimachusproject.rdfa.model.TermFactory;

public class SubjectTracker extends RDFHandlerWrapper {
	private TermFactory tf = TermFactory.newInstance();
	private Set<URI> subjects = new HashSet<URI>();
	private Set<URI> types = new HashSet<URI>();
	private boolean empty = true;
	private Resource matcher;
	private Resource replacement;
	private Set<TriplePattern> patterns;

	public SubjectTracker(RDFHandler delegate) {
		super(delegate);
	}

	public void replace(Resource match, Resource replacement) {
		this.matcher = match;
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

	public boolean isSubject(Resource subject) {
		return isSingleton() && getSubject().equals(subject);
	}

	public boolean isEmpty() {
		return empty;
	}

	public boolean isSingleton() {
		return subjects.size() == 1;
	}

	public URI getSubject() {
		return subjects.iterator().next();
	}

	public Set<URI> getTypes() {
		return types;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		Resource subj = st.getSubject();
		if (subj.equals(matcher)) {
			subj = replacement;
		}
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		if (subj instanceof URI) {
			subjects.add((URI) subj);
			if (RDF.TYPE.equals(pred) && obj instanceof URI) {
				types.add((URI) obj);
			}
		}
		if (!accept(subj, pred, obj))
			throw new RDFHandlerException("Invalid triple: " + subj + " "
					+ pred + " " + obj);
		empty = false;
		super.handleStatement(new StatementImpl(subj, pred, obj));
	}

	private boolean accept(Resource subj, URI pred, Value obj) {
		if (patterns == null)
			return true;
		Term sterm = asTerm(subj);
		Term pterm = asTerm(pred);
		Term oterm = asTerm(obj);
		boolean accepted = false;
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
			accepted = true;
		}
		return accepted;
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