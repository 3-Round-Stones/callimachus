/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved

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

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.callimachusproject.helpers.SubjectTracker;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.events.Ask;
import org.callimachusproject.rdfa.events.Group;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Union;
import org.callimachusproject.rdfa.events.Where;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.callimachusproject.stream.SPARQLWriter;
import org.callimachusproject.traits.Template;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLParser;

public abstract class CopySupport implements Template {
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);

	public RDFObject calliCopyResource(final RDFObject source, InputStream in,
			final Set<?> namespaces) throws Exception {
		try {
			final ObjectConnection con = source.getObjectConnection();
			final ValueFactory vf = con.getValueFactory();
			SubjectTracker tracker = new SubjectTracker(new RDFInserter(con)) {
				boolean first = true;

				public void handleStatement(Statement st)
						throws RDFHandlerException {
					if (first) {
						first = false;
						try {
							init(st);
						} catch (RuntimeException e) {
							throw e;
						} catch (Exception e) {
							throw new RDFHandlerException(e);
						}
					}
					super.handleStatement(st);
				}

				private void init(Statement st) throws Exception {
					Resource subject = st.getSubject();
					String about = subject.stringValue();
					if (subject.equals(source.getResource())) {
						URI replacement = vf.createURI(nextId(namespaces));
						replace(source.getResource(), replacement);
						subject = replacement;
					} else if (isResourceAlreadyPresent(con, about)) {
						throw new RDFHandlerException("Subject Already Exists");
					}
					accept(openBoundedPatterns("copy", about));
				}
			};
			String base = source.getResource().stringValue();
			RDFXMLParser parser = new RDFXMLParser();
			parser.setValueFactory(vf);
			parser.setRDFHandler(tracker);
			parser.parse(in, base);
			if (tracker.isEmpty())
				throw new BadRequest("Missing Information");
			if (!tracker.isSingleton())
				throw new BadRequest("Wrong Subject");
			URI subject = tracker.getSubject();
			String uri = subject.stringValue();
			new java.net.URI(uri);
			checkNamespace(namespaces, uri);
			ObjectFactory of = con.getObjectFactory();
			return of.createObject(subject, tracker.getTypes());
		} catch (URISyntaxException  e) {
			throw new BadRequest(e);
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			in.close();
		}
	}

	private void checkNamespace(final Set<?> namespaces, String uri) {
		if (namespaces != null && !namespaces.isEmpty()) {
			boolean found = false;
			Iterator<?> iter = namespaces.iterator();
			try {
				while (iter.hasNext()) {
					String ns = iter.next().toString();
					if (uri.length() > ns.length() && uri.startsWith(ns)) {
						String local = uri.substring(ns.length());
						if (local.length() == 1) {
							char l = local.charAt(0);
							if (l != '/' && l != '#' && l != ':') {
								found = true;
								break;
							}
						} else {
							int e = local.length() - 2;
							if (local.lastIndexOf('/', e) == -1
									&& local.lastIndexOf('#', e) == -1
									&& local.lastIndexOf(':', e) == -1) {
								found = true;
								break;
							}
						}
					}
				}
			} finally {
				ObjectConnection.close(iter);
			}
			if (!found)
				throw new BadRequest("Incorrect Subject Namespace");
		}
	}

	private boolean isResourceAlreadyPresent(ObjectConnection con, String about) throws Exception {
		RDFEventReader reader = openBoundedPatterns("copy", about);
		try {
			StringWriter str = new StringWriter();
			SPARQLWriter writer = new SPARQLWriter(str);
			boolean empty = true;
			while (reader.hasNext()) {
				RDFEvent next = reader.next();
				if (next.isStartDocument() || next.isBase()
						|| next.isNamespace()) {
					writer.write(next);
				} else if (next.isTriplePattern()) {
					VarOrTerm subj = next.asTriplePattern().getSubject();
					if (subj.isIRI() && subj.stringValue().equals(about)) {
						if (empty) {
							empty = false;
							writer.write(new Ask());
							writer.write(new Where(true));
						} else {
							writer.write(new Union());
						}
						writer.write(new Group(true));
						writer.write(next);
						writer.write(new Group(false));
					}
				} else if (next.isEndDocument()) {
					writer.write(new Where(false));
					writer.write(next);
				}
			}
			writer.close();
			if (empty)
				return false;
			String qry = str.toString();
			return con.prepareBooleanQuery(SPARQL, qry).evaluate();
		} finally {
			reader.close();
		}
	}

	private String nextId(Set<?> namespaces) throws RDFHandlerException {
		if (namespaces != null) {
			Iterator<?> iter = namespaces.iterator();
			try {
				while (iter.hasNext()) {
					String ns = iter.next().toString();
					return ns + prefix + seq.getAndIncrement();
				}
			} finally {
				ObjectConnection.close(iter);
			}
		}
		throw new RDFHandlerException("Invalid Subject Identifier");
	}
}
