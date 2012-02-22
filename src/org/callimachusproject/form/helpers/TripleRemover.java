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

import java.util.Set;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.TriplePattern;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * Track what node the triples are about and ensures they match one of the given
 * patterns.
 * 
 * @author James Leigh
 * 
 */
public class TripleRemover extends RDFHandlerBase {
	private final RepositoryConnection con;
	private final TripleVerifier verifier;

	public TripleRemover(RepositoryConnection con) {
		this.con = con;
		this.verifier = new TripleVerifier(con.getValueFactory());
	}

	public void handleStatement(Statement st) throws RDFHandlerException {
		try {
			verifier.verify(st.getSubject(), st.getPredicate(), st.getObject());
			con.remove(st.getSubject(), st.getPredicate(), st.getObject());
		} catch (RepositoryException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void setReverseAllowed(boolean reverseAllowed) {
		verifier.setReverseAllowed(reverseAllowed);
	}

	public void setWildPropertiesAllowed(boolean wildPropertiesAllowed) {
		verifier.setWildPropertiesAllowed(wildPropertiesAllowed);
	}

	public void accept(RDFEventReader reader) throws RDFParseException {
		verifier.accept(reader);
	}

	public void accept(TriplePattern pattern) {
		verifier.accept(pattern);
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

	public URI getSubject() {
		return verifier.getSubject();
	}

	public void addSubject(URI subj) throws RDFHandlerException {
		verifier.addSubject(subj);
	}

	public Set<URI> getResources() {
		return verifier.getResources();
	}

	public String toString() {
		return con.toString();
	}

}
