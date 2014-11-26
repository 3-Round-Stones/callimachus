/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.consumers.helpers.MessageWriterBase;
import org.callimachusproject.io.TurtleStreamWriterFactory;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;

/**
 * Writes RDF messages.
 * 
 * @author James Leigh
 * 
 */
public class GraphMessageWriter extends
		MessageWriterBase<RDFFormat, RDFWriterFactory, GraphQueryResult> {
	private static final int SMALL = 100;

	public GraphMessageWriter() {
		super(RDFWriterRegistry.getInstance(), GraphQueryResult.class);
	}

	@Override
	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		String mimeType = mtype.preferred();
		if (mimeType != null && mimeType.startsWith("text/plain"))
			return false;
		return super.isConsumable(mtype, builder);
	}

	@Override
	protected void close(GraphQueryResult result) throws OpenRDFException {
		result.close();
	}

	@Override
	public void writeTo(RDFWriterFactory factory, GraphQueryResult result,
			OutputStream out, Charset charset, String base,
			ObjectConnection con) throws RDFHandlerException,
			QueryEvaluationException {
		RDFFormat rdfFormat = factory.getRDFFormat();
		RDFWriter writer = getWriter(out, charset, factory, base);
		// writer.setBaseURI(base);
		writer.startRDF();

		List<Statement> firstStatements = new ArrayList<Statement>(SMALL);

		// Only try to trim namespace if the RDF format supports
		// namespaces in the first place
		boolean trimNamespaces = rdfFormat.supportsNamespaces();

		if (trimNamespaces && result != null) {
			// Gather the first few statements
			for (int i = 0; result.hasNext() && i < SMALL; i++) {
				firstStatements.add(result.next());
			}

			// Only trim namespaces if the set is small enough
			trimNamespaces = firstStatements.size() < SMALL;

			// Gather the namespaces from the first few statements
			Set<String> firstNamespaces = new LinkedHashSet<String>(SMALL);
			Set<String> reportedNamespaces = new HashSet<String>(SMALL);

			for (Statement st : firstStatements) {
				addNamespace(st.getSubject(), firstNamespaces);
				addNamespace(st.getPredicate(), firstNamespaces);
				addNamespace(st.getObject(), firstNamespaces);
				addNamespace(st.getContext(), firstNamespaces);
			}

			// Report namespace prefixes
			for (Map.Entry<String, String> ns : result.getNamespaces()
					.entrySet()) {
				String prefix = ns.getKey();
				String namespace = ns.getValue();
				if (!trimNamespaces || firstNamespaces.contains(namespace)) {
					writer.handleNamespace(prefix, namespace);
					firstNamespaces.remove(namespace);
					reportedNamespaces.add(namespace);
				}
			}

			// Report other namespace
			if (!firstNamespaces.isEmpty() && con != null) {
				try {
					RepositoryResult<Namespace> names = con.getNamespaces();
					while (names.hasNext()) {
						Namespace ns = names.next();
						String name = ns.getName();
						if ((!trimNamespaces || firstNamespaces.contains(name))
								&& !reportedNamespaces.contains(name)) {
							writer.handleNamespace(ns.getPrefix(), name);
							firstNamespaces.remove(name);
							reportedNamespaces.add(name);
						}
					}
				} catch (RepositoryException e) {
					throw new RDFHandlerException(e);
				}
			}
		}

		// Report statements
		for (Statement st : firstStatements) {
			writer.handleStatement(st);
		}

		while (result != null && result.hasNext()) {
			Statement st = result.next();
			writer.handleStatement(st);
		}

		writer.endRDF();
	}

	private RDFWriter getWriter(OutputStream out, Charset charset,
			RDFWriterFactory factory, String systemId) {
		if (RDFFormat.TURTLE.equals(factory.getRDFFormat()) && systemId != null) {
			try {
				TurtleStreamWriterFactory tf = new TurtleStreamWriterFactory();
				if (charset == null)
					return tf.createWriter(out, systemId);
				return tf.createWriter(new OutputStreamWriter(out, charset), systemId);
			} catch (URISyntaxException e) {
				// ignore
			}
		}
		if (charset == null)
			return factory.getWriter(out);
		return factory.getWriter(new OutputStreamWriter(out, charset));
	}

	private void addNamespace(Value value, Set<String> namespaces) {
		if (value instanceof URI) {
			URI uri = (URI) value;
			namespaces.add(uri.getNamespace());
		} else if (value instanceof Literal) {
			Literal lit = (Literal) value;
			if (lit.getDatatype() != null) {
				namespaces.add(lit.getDatatype().getNamespace());
			}
		}
	}

}
