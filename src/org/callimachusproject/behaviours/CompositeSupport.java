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

import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.RDFHandlerBase;

public abstract class CompositeSupport implements CalliObject {

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
			return e.getMessage();
		} finally {
			in.reset();
		}
	}

}
