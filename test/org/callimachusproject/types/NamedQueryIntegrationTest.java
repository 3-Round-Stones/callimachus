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
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/RdfQuery") + "&location=test-query.rq");
		WebResource query = create.create("application/sparql-query", RQ.getBytes());
		query.ref("?sparql").get("application/sparql-query");
		query.ref("?results").get("application/sparql-results+xml");
		query.ref("?results&tqx=reqId:0").get("text/javascript");
		query.ref("?results&tqx=reqId:0:out=html").get("text/html");
		query.ref("?results&tqx=reqId:0:out=csv").get("text/csv");
		query.ref("?results&tqx=reqId:0:out=tsv-excel").get("text/tab-separated-values");
	}

}
