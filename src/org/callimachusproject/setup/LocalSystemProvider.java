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
import java.net.InetAddress;

import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;

public class LocalSystemProvider extends UpdateProvider {
	private static final String SYSTEM_GROUP = "/auth/groups/system";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_ANONYMOUSFROM = CALLI + "anonymousFrom";
	private final DomainNameSystemResolver dnsResolver = DomainNameSystemResolver
			.getInstance();

	@Override
	public Updater updateCallimachusWebapp(final String origin) throws IOException {
		return new Updater() {
			public boolean update(String webapp,
					CalliRepository repository) throws IOException,
					OpenRDFException {
				String group = origin + SYSTEM_GROUP;
				ObjectConnection con = repository.getConnection();
				try {
					con.begin();
					ValueFactory vf = con.getValueFactory();
					URI subj = vf.createURI(group);
					URI pred = vf.createURI(CALLI_ANONYMOUSFROM);
					boolean modified = false;
					InetAddress loop = dnsResolver.getLocalHost();
					String localhost = dnsResolver.reverse(loop);
					// CachingExec#generateViaHeader hardcodes "localhost"
					for (String host : new String[] { "localhost", localhost }) {
						Literal obj = vf.createLiteral(host);
						if (!con.hasStatement(subj, pred, obj)) {
							con.add(subj, pred, obj);
							modified = true;
						}
					}
					con.commit();
					return modified;
				} finally {
					con.close();
				}
			}
		};
	}

}
