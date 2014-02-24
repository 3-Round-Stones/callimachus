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
package org.callimachusproject.auth;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.util.Map;

import org.callimachusproject.concepts.Realm;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.PrefixMap;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmManager {
	private static final String SELECT_BY_TYPE = "SELECT ?object { ?object a $type }";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_REALM = CALLI + "Realm";
	private static final String CALLI_ORIGIN = CALLI + "Origin";
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private final Logger logger = LoggerFactory.getLogger(RealmManager.class);
	private int revision = cache;
	private final CalliRepository repo;
	private PrefixMap<DetachedRealm> realms;

	public RealmManager(CalliRepository repository) {
		this.repo = repository;
	}

	public synchronized void resetCache() {
		realms = null;
	}

	public DetachedRealm getRealm(String target) throws OpenRDFException, IOException {
		return getRealms().getClosest(target);
	}

	public DetachedAuthenticationManager getAuthenticationManager(Resource uri)
			throws OpenRDFException, IOException {
		for (DetachedRealm realm : getRealms().values()) {
			DetachedAuthenticationManager auth = realm.getAuthenticationManager(uri);
			if (auth != null)
				return auth;
		}
		return null;
	}

	private synchronized PrefixMap<DetachedRealm> getRealms()
			throws OpenRDFException, IOException {
		if (realms != null && revision == cache)
			return realms;
		revision = cache;
		ObjectConnection con = repo.getConnection();
		try {
			realms = loadRealms(con);
			for (Map.Entry<String, DetachedRealm> e : realms.entrySet()) {
				e.getValue().init(con, this);
			}
			return realms;
		} finally {
			con.close();
		}
	}

	private PrefixMap<DetachedRealm> loadRealms(ObjectConnection con)
			throws OpenRDFException {
		PrefixMap<DetachedRealm> realms = new PrefixMap<DetachedRealm>();
		ValueFactory vf = con.getValueFactory();
		addRealmsOfType(vf.createURI(CALLI_REALM), realms, con);
		addRealmsOfType(vf.createURI(CALLI_ORIGIN), realms, con);
		return realms;
	}

	private void addRealmsOfType(URI type, PrefixMap<DetachedRealm> realms,
			ObjectConnection con) throws OpenRDFException {
		ObjectQuery qry = con.prepareObjectQuery(SPARQL, SELECT_BY_TYPE);
		qry.setBinding("type", type);
		for (Object o : qry.evaluate(Object.class).asList()) {
			String key = o.toString();
			try {
				if (!realms.containsKey(key)) {
					realms.put(key, ((Realm) o).detachRealm(this));
				}
			} catch (ClassCastException e) {
				logger.error(o.toString() + " cannot be detached", e);
			}
		}
	}

}
