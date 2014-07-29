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
package org.callimachusproject.types;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.Test;

public class NamedQueryIntegrationTest extends TemporaryServerIntegrationTestCase {
	private static final String RQ = "SELECT ?title WHERE { \n" +
			"<http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title . \n" +
			"}";

	@Test
	public void testLess() throws Exception {
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/RdfQuery") + "&resource=test-query.rq");
		WebResource query = create.create("application/sparql-query", RQ.getBytes());
		query.ref("?sparql").get("application/sparql-query");
		query.ref("?results").get("application/sparql-results+xml");
		query.ref("?results&tqx=reqId:0").get("text/javascript");
		query.ref("?results&tqx=reqId:0:out=html").get("text/html");
		query.ref("?results&tqx=reqId:0:out=csv").get("text/csv");
		query.ref("?results&tqx=reqId:0:out=tsv-excel").get("text/tab-separated-values");
	}

}
