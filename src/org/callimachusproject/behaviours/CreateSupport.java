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

	public RDFObject calliCreateResource(InputStream in, String base,
			final RDFObject target) throws Exception {
		try {
			ObjectConnection con = target.getObjectConnection();
			if (target.toString().equals(base))
				throw new RDFHandlerException("Target resource URI not provided");
			if (isResourceAlreadyPresent(con, target.toString()))
				throw new Conflict("Resource already exists: " + target);
			SubjectTracker tracker = new SubjectTracker(new RDFInserter(con));
			tracker.accept(openPatternReader(target.toString(), "create", null));
			RDFXMLParser parser = new RDFXMLParser();
			parser.setValueFactory(con.getValueFactory());
			parser.setRDFHandler(tracker);
			parser.parse(in, base);
			if (tracker.isEmpty())
				throw new BadRequest("Missing Information");
			if (!tracker.isSingleton())
				throw new BadRequest("Wrong Subject");
			ObjectFactory of = con.getObjectFactory();
			for (URI partner : tracker.getResources()) {
				if (!partner.toString().equals(base)) {
					of.createObject(partner, VersionedObject.class).touchRevision();
				}
			}
			return of.createObject(target.toString(), tracker.getTypes());
		} catch (URISyntaxException  e) {
			throw new BadRequest(e);
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			in.close();
		}
	}

	private boolean isResourceAlreadyPresent(ObjectConnection con, String about) throws Exception {
		RDFEventReader reader = openPatternReader(about, "create", null);
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
