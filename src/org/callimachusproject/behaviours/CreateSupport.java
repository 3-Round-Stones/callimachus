/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

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

import org.callimachusproject.concepts.Page;
import org.callimachusproject.helpers.SubjectTracker;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.events.Ask;
import org.callimachusproject.rdfa.events.Group;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Union;
import org.callimachusproject.rdfa.events.Where;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.callimachusproject.stream.SPARQLWriter;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.Conflict;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLParser;

/**
 * Save the provided RDF/XML triples into the RDF store provided they match the
 * patterns present in this template.
 * 
 * @author James Leigh
 * 
 */
public abstract class CreateSupport implements Page {

	public RDFObject calliCreateResource(final RDFObject source, InputStream in,
			final Set<?> spaces) throws Exception {
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
					if (isResourceAlreadyPresent(con, about)) {
						throw new Conflict("Resource already exists: " + about);
					} else if (subject.equals(source.getResource())) {
						throw new RDFHandlerException("Target resource URI not provided");
					}
					accept(openBoundedPatterns("create", subject.stringValue()));
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
			checkUriSpace(spaces, uri);
			ObjectFactory of = con.getObjectFactory();
			for (URI partner : tracker.getResources()) {
				of.createObject(partner, VersionedObject.class).touchRevision();
			}
			return of.createObject(subject, tracker.getTypes());
		} catch (URISyntaxException  e) {
			throw new BadRequest(e);
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			in.close();
		}
	}

	private void checkUriSpace(final Set<?> spaces, String uri) {
		if (spaces != null && !spaces.isEmpty()) {
			boolean found = false;
			Iterator<?> iter = spaces.iterator();
			try {
				while (iter.hasNext()) {
					String ns = iter.next().toString();
					if (uri.length() > ns.length() && uri.startsWith(ns)) {
						String local = uri.substring(ns.length());
						if (local.length() < 2) {
							char l = local.charAt(0);
							if (l != '/' && l != '#' && l != ':') {
								found = true;
								break;
							}
						} else {
							found = true;
							break;
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
		RDFEventReader reader = openBoundedPatterns("create", about);
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
					if (subj.isIRI() && subj.stringValue().equals(about)
							|| subj.isVar()
							&& subj.stringValue().equals("this")) {
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
			ValueFactory vf = con.getValueFactory();
			BooleanQuery query = con.prepareBooleanQuery(SPARQL, qry);
			query.setBinding("this", vf.createURI(about));
			return query.evaluate();
		} finally {
			reader.close();
		}
	}
}
