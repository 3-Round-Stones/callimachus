/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.fluid.consumers;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Writes RDF DESCRIBE from an RDFObject.
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectWriter implements Consumer<RDFObject> {
	private static final String DEFINE_SELF = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "CONSTRUCT {$self ?pred ?obj . ?thing rdfs:isDefinedBy $self . ?thing ?p ?o}\n"
			+ "WHERE {{$self ?pred ?obj} UNION {?thing rdfs:isDefinedBy $self OPTIONAL { ?thing ?p ?o }}}";
	private static final String DESCRIBE_SELF = "CONSTRUCT {$self ?pred ?obj}\n"
			+ "WHERE {$self ?pred ?obj}";
	private ModelMessageWriter delegate = new ModelMessageWriter();

	public boolean isText(MessageType mtype) {
		return delegate.isText(mtype.as(Model.class));
	}

	public long getSize(MessageType mtype, RDFObject result, Charset charset) {
		return -1;
	}

	public boolean isWriteable(MessageType mtype) {
		Class<?> type = mtype.clas();
		Class<Model> t = Model.class;
		if (!delegate.isWriteable(mtype.as(t)))
			return false;
		if (QueryResult.class.isAssignableFrom(type))
			return false;
		if (Object.class.equals(type) || RDFObject.class.equals(type))
			return true;
		return mtype.isConcept(type);
	}

	public String getContentType(MessageType mtype, Charset charset) {
		return delegate.getContentType(mtype.as(Model.class), charset);
	}

	public ReadableByteChannel write(MessageType mtype, RDFObject result,
			String base, Charset charset) throws IOException, OpenRDFException {
		if (result == null)
			return null;
		ObjectConnection con = result.getObjectConnection();
		Resource resource = result.getResource();
		try {
			Model model = new LinkedHashModel();
			describeInto(con, DEFINE_SELF, resource, model);
			return delegate.write(mtype.as(Model.class), model, base, charset);
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		}
	}

	public void writeTo(MessageType mtype, RDFObject result, String base,
			Charset charset, WritableByteChannel out, int bufSize)
			throws IOException, OpenRDFException {
		ObjectConnection con = result.getObjectConnection();
		Resource resource = result.getResource();
		try {
			Model model = new LinkedHashModel();
			describeInto(con, DEFINE_SELF, resource, model);
			delegate.writeTo(mtype.as(Model.class), model, base, charset, out,
					bufSize);
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		}
	}

	protected void describeInto(ObjectConnection con, String qry,
			Resource resource, Model model) throws MalformedQueryException,
			RepositoryException, QueryEvaluationException {
		String ns = null;
		if (resource instanceof URI) {
			String uri = resource.stringValue();
			if (uri.contains("#")) {
				ns = uri.substring(0, uri.indexOf('#') + 1);
			} else {
				ns = uri + "#";
			}
		}
		GraphQuery query = con.prepareGraphQuery(SPARQL, qry);
		query.setBinding("self", resource);
		GraphQueryResult result = query.evaluate();
		try {
			while (result.hasNext()) {
				Statement st = result.next();
				if (model.add(st)) {
					Value obj = st.getObject();
					if (!resource.equals(obj) && isBNodeOrLocal(obj, ns)) {
						describeInto(con, DESCRIBE_SELF, (Resource) obj, model);
					}
				}
			}
		} finally {
			result.close();
		}
	}

	private boolean isBNodeOrLocal(Value obj, String ns) {
		return obj instanceof BNode || ns != null && obj instanceof URI
				&& ((URI) obj).getNamespace().equals(ns);
	}

}
