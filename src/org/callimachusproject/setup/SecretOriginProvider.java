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
import java.io.Writer;
import java.security.SecureRandom;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public class SecretOriginProvider extends UpdateProvider {
	private static final String FOAF_DOC = "http://xmlns.com/foaf/0.1/Document";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_SECRET = CALLI + "secret";
	private static final String CALLI_HAS_COMPONENT = CALLI + "hasComponent";

	@Override
	public Updater updateOrigin(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				ObjectConnection con = repository.getConnection();
				try {
					con.begin();
					ValueFactory vf = con.getValueFactory();
					URI subj = vf.createURI(origin + "/");
					if (!con.hasStatement(subj, vf.createURI(CALLI_SECRET),
							null)) {
						URI secret = createSecretFile(webapp, con);
						Writer writer = con.getBlobObject(secret).openWriter();
						try {
							byte[] bytes = new byte[1024];
							new SecureRandom().nextBytes(bytes);
							writer.write(Base64.encodeBase64String(bytes));
						} finally {
							writer.close();
						}
						con.add(subj, vf.createURI(CALLI_SECRET), secret);
						con.commit();
						return true;
					} else {
						con.commit();
					}
					return false;
				} finally {
					con.close();
				}
			}
		};
	}

	public static URI createSecretFile(String webapp, ObjectConnection con)
			throws RepositoryException {
		TermFactory tf = TermFactory.newInstance(webapp);
		String TextFile = tf.resolve("types/TextFile");
		String secrets = tf.resolve("/auth/secrets/");
		ValueFactory vf = con.getValueFactory();
		String label = UUID.randomUUID().toString();
		URI secret = vf.createURI(secrets + label);
		con.add(secret, RDF.TYPE, vf.createURI(TextFile));
		con.add(secret, RDF.TYPE, vf.createURI(FOAF_DOC));
		con.add(secret, RDFS.LABEL, vf.createLiteral(label.replace('-', ' ')));
		con.add(vf.createURI(secrets), vf.createURI(CALLI_HAS_COMPONENT), secret);
		return secret;
	}

}
