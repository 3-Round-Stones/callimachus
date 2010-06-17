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

import org.callimachusproject.helpers.GraphPatternBuilder;
import org.callimachusproject.helpers.MultipartParser;
import org.callimachusproject.helpers.SubjectTracker;
import org.callimachusproject.traits.Template;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.rdfxml.RDFXMLParser;

public abstract class EditSupport implements Template {

	private static class Remover extends RDFHandlerBase {
		private final ObjectConnection con;

		private Remover(ObjectConnection con) {
			this.con = con;
		}

		public void handleStatement(Statement st) throws RDFHandlerException {
			try {
				con.remove(st.getSubject(), st.getPredicate(), st.getObject());
			} catch (RepositoryException e) {
				throw new RDFHandlerException(e);
			}
		}
	}

	public void calliEditResource(RDFObject target, InputStream in)
			throws Exception {
		MultipartParser multipart = new MultipartParser(in);
		try {
			RDFXMLParser parser = new RDFXMLParser();
			ObjectConnection con = target.getObjectConnection();
			remove((URI) target.getResource(), multipart.next(), parser, con);
			add((URI) target.getResource(), multipart.next(), parser, con);
			assert multipart.next() == null;
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			multipart.close();
		}
	}

	private void remove(URI resource, InputStream in, RDFXMLParser parser,
			ObjectConnection con) throws Exception {
		SubjectTracker remover = createSubjectTracker(resource,
				new Remover(con));
		GraphPatternBuilder pattern = new GraphPatternBuilder();
		parser.setRDFHandler(pattern);
		parser.parse(in, resource.stringValue());
		if (!pattern.isEmpty()) {
			String sparql = pattern.toSPARQLQuery();
			con.prepareGraphQuery(SPARQL, sparql).evaluate(remover);
			if (remover.isEmpty())
				throw new BadRequest("Removed Content Not Found");
			if (!remover.isSubject(resource))
				throw new BadRequest("Wrong Subject");
		}
	}

	private void add(URI resource, InputStream in, RDFXMLParser parser,
			ObjectConnection con) throws Exception {
		SubjectTracker inserter = createSubjectTracker(resource,
				new RDFInserter(con));
		parser.setValueFactory(con.getValueFactory());
		parser.setRDFHandler(inserter);
		parser.parse(in, resource.stringValue());
		if (!inserter.isEmpty() && !inserter.isSubject(resource))
			throw new BadRequest("Wrong Subject");
	}

	private SubjectTracker createSubjectTracker(URI resource,
			RDFHandler delegate) throws Exception {
		SubjectTracker tracker = new SubjectTracker(delegate);
		String about = resource.stringValue();
		tracker.accept(openBoundedPatterns("edit", about));
		return tracker;
	}
}
