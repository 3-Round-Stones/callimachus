/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.rel;
import org.openrdf.http.object.annotations.title;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.MethodNotAllowed;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;

/**
 * Parses RDF from a file.
 * 
 * @author James Leigh
 * 
 */
public abstract class NamedGraphSupport implements HTTPFileObject, RDFObject {
	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	@rel("alternate")
	@title("RDF Graph")
	@query("graph")
	@type( { "application/rdf+xml", "application/x-turtle", "text/rdf+n3",
			"application/trix", "application/x-trig" })
	public GraphQueryResult exportNamedGraph() throws RepositoryException,
			RDFHandlerException, QueryEvaluationException,
			MalformedQueryException {
		Resource self = getResource();
		if (self instanceof URI) {
			DatasetImpl dataset = new DatasetImpl();
			dataset.addDefaultGraph((URI) self);

			RepositoryConnection con = getObjectConnection();
			GraphQuery query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL);
			query.setDataset(dataset);

			// Use the namespaces of the repository (not the query)
			RepositoryResult<Namespace> namespaces = con.getNamespaces();
			Map<String, String> map = new HashMap<String, String>();
			while (namespaces.hasNext()) {
				Namespace ns = namespaces.next();
				map.put(ns.getPrefix(), ns.getName());
			}
			return new GraphQueryResultImpl(map, query.evaluate());
		} else {
			return null;
		}
	}

	@query({})
	@method("DELETE")
	public void deleteObject() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		con.clear(getResource());
		if (!delete())
			throw new MethodNotAllowed();
	}

	@query( {})
	@method("PUT")
	public void putRDFIntputStream(
			@header("Content-Type") String mediaType,
			@type( { "application/rdf+xml", "application/x-turtle",
					"text/rdf+n3", "application/trix", "application/x-trig" }) InputStream in)
			throws RepositoryException {
		try {
			OutputStream out = openOutputStream();
			try {
				int read;
				byte[] buf = new byte[1024];
				while ((read = in.read(buf)) >= 0) {
					out.write(buf, 0, read);
				}
			} finally {
				out.close();
				in.close();
			}
			importRDF(mediaType);
		} catch (IOException e) {
			throw new BadRequest(e);
		}
	}

	private void importRDF(String mediaType) {
		ObjectConnection con = getObjectConnection();
		String mime = mimeType(mediaType);
		RDFFormat format = RDFFormat.forMIMEType(mime);
		String iri = getResource().stringValue();
		try {
			InputStream in = openInputStream();
			try {
				con.add(in, iri, format, getResource());
			} finally {
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String mimeType(String media) {
		if (media == null)
			return null;
		int idx = media.indexOf(';');
		if (idx > 0)
			return media.substring(0, idx);
		return media;
	}
}
