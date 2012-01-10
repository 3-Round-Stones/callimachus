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
package org.openrdf.http.object.writers;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.OpenRDFException;
import org.openrdf.http.object.util.MessageType;
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
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Describes the set of RDFObjects as RDF.
 * 
 * @author James Leigh
 * 
 */
public class SetOfRDFObjectWriter implements MessageBodyWriter<Set<?>> {
	private static final String DESCRIBE = "CONSTRUCT {?self ?pred ?obj}\n"
			+ "WHERE {?self ?pred ?obj}";
	private ModelMessageWriter delegate = new ModelMessageWriter();
	private RDFObjectWriter helper = new RDFObjectWriter();

	public boolean isText(MessageType mtype) {
		Class<Model> g = Model.class;
		return delegate.isText(mtype.as(g));
	}

	public long getSize(MessageType mtype, Set<?> result, Charset charset) {
		return -1;
	}

	public boolean isWriteable(MessageType mtype) {
		Class<?> type = mtype.clas();
		Class<Model> g = Model.class;
		if (!delegate.isWriteable(mtype.as(g)))
			return false;
		if (Model.class.isAssignableFrom(type))
			return false;
		if (!Set.class.equals(type))
			return false;
		return helper.isWriteable(mtype.component());
	}

	public String getContentType(MessageType mtype, Charset charset) {
		return delegate.getContentType(mtype.as(Model.class), charset);
	}

	public ReadableByteChannel write(MessageType mtype, Set<?> result,
			String base, Charset charset) throws IOException, OpenRDFException {
		Model result1 = getGraphResult(result);
		return delegate.write(mtype.as(Model.class), result1, base, charset);
	}

	public void writeTo(MessageType mtype, Set<?> set, String base,
			Charset charset, WritableByteChannel out, int bufSize)
			throws IOException, OpenRDFException {
		Model result = getGraphResult(set);
		delegate.writeTo(mtype.as(Model.class), result, base, charset, out,
				bufSize);
	}

	private Model getGraphResult(Set<?> set) throws RepositoryException,
			QueryEvaluationException {
		Model model = new LinkedHashModel();
		if (!set.isEmpty()) {
			ObjectConnection con = null;
			StringBuilder qry = new StringBuilder();
			qry.append(DESCRIBE, 0, DESCRIBE.lastIndexOf('}'));
			qry.append("\nFILTER (");
			List<Value> list = new ArrayList<Value>();
			Iterator<?> iter = set.iterator();
			for (int i = 0; iter.hasNext(); i++) {
				Object obj = iter.next();
				if (con == null) {
					con = ((RDFObject) obj).getObjectConnection();
				}
				list.add(((RDFObject) obj).getResource());
				qry.append("?self = $_").append(i).append("||");
			}
			qry.delete(qry.length() - 2, qry.length());
			qry.append(")}");
			try {
				GraphQuery query = con
						.prepareGraphQuery(SPARQL, qry.toString());
				for (int i = 0, n = list.size(); i < n; i++) {
					query.setBinding("_" + i, list.get(i));
				}
				GraphQueryResult result = query.evaluate();
				try {
					while (result.hasNext()) {
						Statement st = result.next();
						model.add(st);
						Value obj = st.getObject();
						if (obj instanceof BNode) {
							helper.describeInto(con, DESCRIBE, (Resource) obj, model);
						} else if (obj instanceof URI) {
							String ns = ((URI) obj).getNamespace();
							if (ns.endsWith("#")) {
								String uri = ns.substring(0, ns.length() - 1);
								set.contains(con.getObjectFactory()
										.createObject(uri));
							}
						}
					}
				} finally {
					result.close();
				}
			} catch (MalformedQueryException e) {
				throw new AssertionError(e);
			}

		}
		return model;
	}

}
