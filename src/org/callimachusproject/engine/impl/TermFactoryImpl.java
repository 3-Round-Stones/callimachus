/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

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
package org.callimachusproject.engine.impl;

import info.aduna.net.ParsedURI;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.callimachusproject.engine.model.CURIE;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.Reference;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.engine.model.Literal;
import org.callimachusproject.engine.model.Var;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Contains factory methods for RDF terms.
 * 
 * @author James Leigh
 * 
 */
public class TermFactoryImpl extends TermFactory {
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private static final CURIE XMLLiteral = new CURIEImpl(RDF.NAMESPACE,
			"XMLLiteral", "rdf");
	private static final CURIE STRING = new CURIEImpl(XMLSchema.NAMESPACE, "string", "xsd");
	private static final CURIE LANGSTRING = new CURIEImpl(RDF.NAMESPACE, "langString", "rdf");
	private final Map<String, String> namespaces = new HashMap<String, String>();
	private final String systemId;
	private ParsedURI base;

	public TermFactoryImpl(String systemId) {
		assert systemId != null;
		this.systemId = canonicalize(systemId);
		base = new ParsedURI(this.systemId);
		assert base.isAbsolute() : base;
	}

	@Override
	public String getSystemId() {
		return systemId;
	}

	@Override
	public synchronized Reference base(String reference) {
		String resolved = resolve(reference);
		base = new ParsedURI(resolved);
		assert base.isAbsolute() : base;
		return reference(resolved, reference);
	}

	@Override
	public Reference reference(String reference) {
		return reference(resolve(reference), reference);
	}

	@Override
	public synchronized Reference prefix(String prefix, String reference) {
		String resolved = resolve(reference);
		namespaces.put(prefix, resolved);
		return reference(resolved, reference);
	}

	@Override
	public synchronized CURIE curie(String prefix, String reference) {
		String ns = namespaces.get(prefix);
		return curie(ns, reference, prefix);
	}

	@Override
	public CURIE curie(String ns, String reference, String prefix) {
		if (ns == null || reference == null || prefix == null)
			throw new IllegalArgumentException();
		return new CURIEImpl(ns, reference, prefix);
	}

	@Override
	public IRI iri(String iri) {
		if (iri == null)
			throw new IllegalArgumentException();
		return new IRIImpl(iri);
	}

	@Override
	public PlainLiteral literal(String label, String lang) {
		if (label == null)
			throw new IllegalArgumentException();
		if (lang == null)
			return new PlainLiteralImpl(label, STRING);
		return new PlainLiteralImpl(label, LANGSTRING, lang);
	}

	@Override
	public Literal literal(String label, IRI datatype) {
		if (label == null)
			throw new IllegalArgumentException();
		if (XMLLiteral.equals(datatype))
			return new XMLLiteralImpl(label, datatype);
		return new TypedLiteralImpl(label, datatype);
	}

	@Override
	public Var var(String name) {
		return var('?', name);
	}

	@Override
	public Var var(char prefix, String name) {
		if (name == null)
			throw new IllegalArgumentException();
		return new VarImpl(prefix, name);
	}

	@Override
	public Node node(String id) {
		if (id == null)
			throw new IllegalArgumentException();
		return new BlankNode(id);
	}

	@Override
	public Node node() {
		return new BlankNode(prefix + seq.getAndIncrement());
	}

	@Override
	public Reference reference(String absolute, String relative) {
		if (absolute == null || relative == null)
			throw new IllegalArgumentException();
		return new ReferenceImpl(absolute, relative);
	}

	public synchronized String resolve(String relative) {
		return canonicalize(resolve(base, new ParsedURI(relative)).toString());
	}

	private String canonicalize(String iri) {
		try {
			java.net.URI net = new java.net.URI(iri);
			net.normalize();
			String scheme = net.getScheme();
			if (scheme != null) {
				scheme = scheme.toLowerCase();
			}
			String frag = net.getFragment();
			if (net.isOpaque()) {
				String part = net.getSchemeSpecificPart();
				net = new java.net.URI(scheme, part, frag);
				return net.toASCIIString(); // URI
			}
			String auth = net.getAuthority();
			if (auth != null) {
				auth = auth.toLowerCase();
			}
			String qs = net.getQuery();
			net = new java.net.URI(scheme, auth, net.getPath(), qs, frag);
			return net.toASCIIString(); // URI
		} catch (URISyntaxException x) {
			throw new IllegalArgumentException(x);
		}
	}

	/**
	 * Resolves a relative URI using this URI as the base URI.
	 */
	private ParsedURI resolve(ParsedURI baseURI, ParsedURI relURI) {
		// This algorithm is based on the algorithm specified in chapter 5 of
		// RFC 2396: URI Generic Syntax. See http://www.ietf.org/rfc/rfc2396.txt

		// RFC, step 3:
		if (relURI.isAbsolute() || baseURI.isOpaque()) {
			return relURI;
		}

		// relURI._scheme == null

		// RFC, step 2:
		if (relURI.getAuthority() == null && relURI.getQuery() == null
				&& relURI.getPath().length() == 0) {

			// Inherit any fragment identifier from relURI
			String fragment = relURI.getFragment();

			return new ParsedURI(baseURI.getScheme(), baseURI.getAuthority(),
					baseURI.getPath(), baseURI.getQuery(), fragment);
		} else if (relURI.getAuthority() == null
				&& relURI.getPath().length() == 0) {

			// Inherit any query or fragment from relURI
			String query = relURI.getQuery();
			String fragment = relURI.getFragment();

			return new ParsedURI(baseURI.getScheme(), baseURI.getAuthority(),
					baseURI.getPath(), query, fragment);
		}

		// We can start combining the URIs
		String scheme, authority, path, query, fragment;
		boolean normalizeURI = false;

		scheme = baseURI.getScheme();
		query = relURI.getQuery();
		fragment = relURI.getFragment();

		// RFC, step 4:
		if (relURI.getAuthority() != null) {
			authority = relURI.getAuthority();
			path = relURI.getPath();
		} else {
			authority = baseURI.getAuthority();

			// RFC, step 5:
			if (relURI.getPath().startsWith("/")) {
				path = relURI.getPath();
			} else {
				// RFC, step 6:
				path = baseURI.getPath();

				if (path == null) {
					path = "/";
				} else {
					if (!path.endsWith("/")) {
						// Remove the last segment of the path. Note: if
						// lastSlashIdx is -1, the path will become empty,
						// which is fixed later.
						int lastSlashIdx = path.lastIndexOf('/');
						path = path.substring(0, lastSlashIdx + 1);
					}

					if (path.length() == 0) {
						// No path means: start at root.
						path = "/";
					}
				}

				// Append the path of the relative URI
				path += relURI.getPath();

				// Path needs to be normalized.
				normalizeURI = true;
			}
		}

		ParsedURI result = new ParsedURI(scheme, authority, path, query,
				fragment);

		if (normalizeURI) {
			result.normalize();
		}

		return result;
	}

}
