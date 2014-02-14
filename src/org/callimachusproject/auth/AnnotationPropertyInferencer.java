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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

public class AnnotationPropertyInferencer {
	private static final String SELECT_SUBPROPERTIES = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "SELECT ?sub { ?sub rdfs:subPropertyOf* $property }";
	private static volatile int cache = 0;

	public static void reset() {
		cache++;
	}

	private int revision = cache;
	private final Repository repo;
	private final Map<String, Set<String>> expanded = new HashMap<String, Set<String>>();

	public AnnotationPropertyInferencer(Repository repo) {
		this.repo = repo;
	}

	public synchronized void resetCache() {
		expanded.clear();
	}

	public synchronized Set<String> expand(String[] property)
			throws OpenRDFException {
		if (revision != cache) {
			resetCache();
			revision = cache;
		}
		if (property == null || property.length == 0)
			return Collections.emptySet();
		if (property.length == 1)
			return expand(property[0]);
		Set<String> set = new HashSet<String>();
		for (String p : property) {
			set.addAll(expand(p));
		}
		return set;
	}

	private synchronized Set<String> expand(String property)
			throws OpenRDFException {
		if (expanded.containsKey(property))
			return expanded.get(property);
		RepositoryConnection con = repo.getConnection();
		try {
			URI uri = con.getValueFactory().createURI(property);
			Set<String> subProperties = findSubPropertiesOf(uri, con);
			expanded.put(property, subProperties);
			return subProperties;
		} finally {
			con.close();
		}
	}

	private Set<String> findSubPropertiesOf(URI property,
			RepositoryConnection con) throws OpenRDFException {
		assert property != null;
		Set<String> set = new HashSet<String>();
		set.add(property.stringValue());
		TupleQuery qry = con.prepareTupleQuery(QueryLanguage.SPARQL,
				SELECT_SUBPROPERTIES);
		qry.setBinding("property", property);
		TupleQueryResult results = qry.evaluate();
		try {
			while (results.hasNext()) {
				Value sub = results.next().getValue("sub");
				if (sub instanceof URI) {
					set.add(sub.stringValue());
				}
			}
			return set;
		} finally {
			results.close();
		}
	}
}
