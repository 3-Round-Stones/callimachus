/*
   Copyright (c) 2010 Zepheira LLC, Some rights reserved

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

import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.model.ReadableHttpEntityChannel;
import org.openrdf.http.object.writers.AggregateWriter;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserFactory;
import org.openrdf.query.parser.QueryParserRegistry;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

public abstract class SPARQLServiceSupport implements RDFObject {
	private static final ProtocolVersion HTTP11 = new ProtocolVersion("HTTP",
			1, 1);
	private static final QueryParserFactory PARSER_FACTORY = QueryParserRegistry
			.getInstance().get(SPARQL);
	private static AggregateWriter writer = AggregateWriter.getInstance();

	@method("POST")
	@type("message/x-response")
	public HttpResponse evaluateSPARQL(@type("application/sparql-query") byte[] in)
			throws Exception {
		if (in == null)
			throw new BadRequest("Missing query");
		String qry = new String(in, "UTF-8");
		try {
			Class<?> type;
			String mime;
			Object rs;
			ObjectConnection con = getObjectConnection();
			ObjectFactory of = con.getObjectFactory();
			QueryParser parser = PARSER_FACTORY.getParser();
			String base = getResource().stringValue();
			ParsedQuery parsed = parser.parseQuery(qry, base);
			if (parsed instanceof ParsedBooleanQuery) {
				type = Boolean.class;
				mime = "application/sparql-results+xml";
				rs = con.prepareBooleanQuery(SPARQL, qry).evaluate();
			} else if (parsed instanceof ParsedGraphQuery) {
				type = GraphQueryResult.class;
				mime = "application/rdf+xml";
				rs = con.prepareGraphQuery(SPARQL, qry).evaluate();
			} else if (parsed instanceof ParsedTupleQuery) {
				type = TupleQueryResult.class;
				mime = "application/sparql-results+xml";
				rs = con.prepareTupleQuery(SPARQL, qry).evaluate();
			} else {
				throw new AssertionError("Unknown query type: "
						+ parsed.getClass());
			}
			ReadableByteChannel body;
			HttpResponse resp = new BasicHttpResponse(HTTP11, 200, "OK");
			body = writer.write(mime, type, type, of, rs, base, null);
			resp.setEntity(new ReadableHttpEntityChannel(mime, -1, body));
			return resp;
		} catch (MalformedQueryException e) {
			throw new BadRequest(e.toString());
		} catch (IllegalArgumentException e) {
			throw new BadRequest("Missing accept header: " + e.getMessage());
		}
	}
}
