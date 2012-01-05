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
import java.util.Set;

import org.callimachusproject.concepts.Page;
import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.events.Ask;
import org.callimachusproject.engine.events.Group;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.Union;
import org.callimachusproject.engine.events.Where;
import org.callimachusproject.engine.helpers.SPARQLWriter;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;
import org.callimachusproject.form.helpers.SubjectTracker;
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
	private static final String HAS_COMPONENT = "http://callimachusproject.org/rdf/2009/framework#" + "hasComponent";

	public RDFObject calliCreateResource(InputStream in, String base,
			final RDFObject target) throws Exception {
		try {
			ObjectConnection con = target.getObjectConnection();
			if (target.toString().equals(base))
				throw new RDFHandlerException("Target resource URI not provided");
			if (isResourceAlreadyPresent(con, target.toString()))
				throw new Conflict("Resource already exists: " + target);
			ValueFactory vf = con.getValueFactory();
			SubjectTracker tracker = new SubjectTracker(new RDFInserter(con), vf);
			tracker.setWildPropertiesAllowed(false);
			tracker.setReverseAllowed(false);
			tracker.accept(openPatternReader(target.toString(), "create", null));
			RDFXMLParser parser = new RDFXMLParser();
			parser.setValueFactory(vf);
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
			Set<URI> types = tracker.getTypes(tracker.getSubject());
			return of.createObject(tracker.getSubject(), types);
		} catch (URISyntaxException  e) {
			throw new BadRequest(e);
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			in.close();
		}
	}

	private boolean isResourceAlreadyPresent(ObjectConnection con, String about)
			throws Exception {
		TermFactory tf = TermFactory.newInstance();
		RDFEventReader reader = openPatternReader(about, "create", null);
		try {
			boolean first = true;
			StringWriter str = new StringWriter();
			SPARQLWriter writer = new SPARQLWriter(str);
			while (reader.hasNext()) {
				RDFEvent next = reader.next();
				if (next.isStartDocument() || next.isBase()
						|| next.isNamespace()) {
					writer.write(next);
				} else if (first) {
					first = false;
					writer.write(new Ask());
					writer.write(new Where(true));
					writer.write(new Group(true));
					IRI has = tf.iri(HAS_COMPONENT);
					Var var = tf.var("calliHasComponent");
					writer.write(new TriplePattern(var, has, tf.var("this")));
					writer.write(new Group(false));
				}
				if (next.isTriplePattern()) {
					VarOrTerm subj = next.asTriplePattern().getSubject();
					if (subj.isIRI() && subj.stringValue().equals(about)
							|| subj.isVar()
							&& subj.stringValue().equals("this")) {
						writer.write(new Union());
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
