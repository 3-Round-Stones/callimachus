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
package org.callimachusproject.setup;

import java.io.IOException;

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.sail.config.ProxyRepositoryConfig;

public class SparqlEndpointProvider extends UpdateProvider {
	private static final String SD = "http://www.w3.org/ns/sparql-service-description#";
	private static final String READER_GROUP = "/auth/groups/power";
	private static final String ADMIN = "/auth/groups/admin";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String ADMINISTRATOR = CALLI + "administrator";
	private static final String READER = CALLI + "reader";
	private static final String HASCOMPONENT = CALLI + "hasComponent";

	@Override
	public Updater updateCallimachusWebapp(final String origin)
			throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				ValueFactory vf = repository.getValueFactory();
				URI uri = vf.createURI(origin + "/sparql");
				String proxiedID = repository.getRepositoryID();
				String datasourceID = repository.getDatasourceRepositoryId(uri);
				RepositoryManager manager = repository.getRepositoryManager();
				if (proxiedID != null && !manager.hasRepositoryConfig(datasourceID)) {
					manager.addRepositoryConfig(new RepositoryConfig(
							datasourceID, uri.stringValue(),
							new ProxyRepositoryConfig(proxiedID)));
					ObjectConnection con = repository.getConnection();
					try {
						con.begin();
						TermFactory tf = TermFactory.newInstance(webapp);
						if (addMetadata(origin, tf, uri, vf, con)) {
							con.commit();
						}
					} finally {
						con.close();
					}
				}
				return true;
			}
		};
	}

	boolean addMetadata(String origin, TermFactory tf, URI uri, ValueFactory vf,
			ObjectConnection con) throws RepositoryException {
		URI root = vf.createURI(origin + "/");
		URI Datasource = vf.createURI(CALLI, "Datasource");
		if (con.hasStatement(uri, RDF.TYPE, Datasource))
			return false;
		con.add(root, vf.createURI(HASCOMPONENT), uri);
		con.add(uri, RDF.TYPE, vf.createURI(tf.resolve("types/RdfDatasource")));
		con.add(uri, RDF.TYPE, Datasource);
		con.add(uri, RDF.TYPE, vf.createURI(SD, "Service"));
		con.add(uri, RDFS.LABEL, vf.createLiteral("SPARQL"));
		con.add(uri, RDFS.COMMENT, vf.createLiteral("SPARQL endpoint to default dataset"));
		con.add(uri, vf.createURI(READER),
				vf.createURI(tf.resolve(READER_GROUP)));
		con.add(uri, vf.createURI(ADMINISTRATOR),
				vf.createURI(tf.resolve(ADMIN)));
		con.add(uri, vf.createURI(SD, "endpoint"), uri);
		con.add(uri, vf.createURI(SD, "supportedLanguage"), vf.createURI(SD, "SPARQL11Query"));
		con.add(uri, vf.createURI(SD, "supportedLanguage"), vf.createURI(SD, "SPARQL11Update"));
		con.add(uri, vf.createURI(SD, "feature"), vf.createURI(SD, "UnionDefaultGraph"));
		con.add(uri, vf.createURI(SD, "feature"), vf.createURI(SD, "BasicFederatedQuery"));
		con.add(uri, vf.createURI(SD, "inputFormat"), vf.createURI("http://www.w3.org/ns/formats/RDF_XML"));
		con.add(uri, vf.createURI(SD, "inputFormat"), vf.createURI("http://www.w3.org/ns/formats/Turtle"));
		con.add(uri, vf.createURI(SD, "resultFormat"), vf.createURI("http://www.w3.org/ns/formats/RDF_XML"));
		con.add(uri, vf.createURI(SD, "resultFormat"), vf.createURI("http://www.w3.org/ns/formats/SPARQL_Results_XML"));
		return true;
	}

}
