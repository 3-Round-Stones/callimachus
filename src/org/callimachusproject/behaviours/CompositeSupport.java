/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.form.helpers.TripleInserter;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.Conflict;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.helpers.RDFHandlerWrapper;

public abstract class CompositeSupport implements CalliObject {

	public Object insertComponentGraph(InputStream in, String type, String uri,
			RDFHandler collector) throws OpenRDFException, IOException {
		try {
			ObjectConnection con = this.getObjectConnection();
			RDFHandler handler = new RDFInserter(con);
			if (collector != null) {
				handler = new RDFHandlerWrapper(handler, collector);
			}
			TripleInserter tracker = new TripleInserter(handler, con);
			tracker.parseAndInsert(in, type, uri);
			if (tracker.isEmpty())
				throw new BadRequest("Missing Information");
			if (!tracker.isSingleton())
				throw new BadRequest("Wrong Subject");
			if (tracker.isDisconnectedNodePresent())
				throw new BadRequest("Blank nodes must be connected");
			if (tracker.isContainmentTriplePresent())
				throw new Conflict("ldp:contains is prohibited");
			URI created = tracker.getPrimaryTopic();

			ObjectFactory of = con.getObjectFactory();
			for (URI partner : tracker.getPartners()) {
				if (!partner.toString().equals(uri)) {
					of.createObject(partner, CalliObject.class).touchRevision();
				}
			}
			Set<URI> types = tracker.getTypes(created);
			return of.createObject(created, types);
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			in.close();
		}
	}

	public boolean isAuthorized(String user, RDFObject target, String[] roles)
			throws RepositoryException, OpenRDFException {
		AuthorizationManager manager = getCalliRepository().getAuthorizationManager();
		return manager.isAuthorized(user, target, roles);
	}

	public String peekAtStatementSubject(BufferedInputStream in, String type, String base)
			throws RDFParseException, IOException {
		try {
			in.mark(65536);
			RDFFormat format = RDFFormat.forMIMEType(type);
			RDFParserRegistry registry = RDFParserRegistry.getInstance();
			RDFParser parser = registry.get(format).getParser();
			parser.setRDFHandler(new RDFHandlerBase() {
				public void handleStatement(Statement st)
						throws RDFHandlerException {
					if (st.getSubject() instanceof URI) {
						throw new RDFHandlerException(st.getSubject().stringValue());
					}
				}
			});
			parser.parse(new FilterInputStream(in) {
				public void close() throws IOException {
					// ignore
				}
			}, base);
			return null;
		} catch (RDFHandlerException e) {
			return canonicalize(e.getMessage());
		} finally {
			in.reset();
		}
	}

	private String canonicalize(String uri) {
		return TermFactory.newInstance(uri).getSystemId();
	}

}
