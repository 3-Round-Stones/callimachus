/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved

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
package org.callimachusproject.io;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RioSetting;
import org.openrdf.rio.WriterConfig;

public class ArrangedWriter implements RDFWriter { 
	private static int MAX_QUEUE_SIZE = 100;
	private final RDFWriter delegate;
	private int queueSize = 0;
	private final Deque<Resource> stack = new LinkedList<Resource>();
	private final Map<String, String> prefixes = new TreeMap<String, String>();
	private final Map<Resource, Set<Statement>> statements = new LinkedHashMap<Resource, Set<Statement>>();
	private final Comparator<Statement> comparator = new Comparator<Statement>() {
		public int compare(Statement s1, Statement s2) {
			URI p1 = s1.getPredicate();
			URI p2 = s2.getPredicate();
			if (p1.equals(RDF.TYPE) && !p2.equals(RDF.TYPE)) {
				return -1;
			} else if (!p1.equals(RDF.TYPE) && p2.equals(RDF.TYPE)) {
				return 1;
			}
			Value o1 = s1.getObject();
			Value o2 = s2.getObject();
			if (!(o1 instanceof BNode) && o2 instanceof BNode) {
				return -1;
			} else if (o1 instanceof BNode && !(o2 instanceof BNode)) {
				return 1;
			}
			if (!(o1 instanceof URI) && o2 instanceof URI) {
				return -1;
			} else if (o1 instanceof URI && !(o2 instanceof URI)) {
				return 1;
			}
			int cmp = p1.stringValue().compareTo(p2.stringValue());
			if (cmp != 0)
				return cmp;
			return o1.stringValue().compareTo(o2.stringValue());
		}
	};

	public ArrangedWriter(RDFWriter delegate) {
		this.delegate = delegate;
	}

	public void setWriterConfig(WriterConfig config) {
		delegate.setWriterConfig(config);
	}

	public WriterConfig getWriterConfig() {
		return delegate.getWriterConfig();
	}

	public Collection<RioSetting<?>> getSupportedSettings() {
		return delegate.getSupportedSettings();
	}

	public RDFFormat getRDFFormat() {
		return delegate.getRDFFormat();
	}

	public void startRDF() throws RDFHandlerException {
		delegate.startRDF();
	}

	public void endRDF() throws RDFHandlerException {
		trimNamespaces();
		flushStatements();
		delegate.endRDF();
	}

	public void handleNamespace(String prefix, String uri)
			throws RDFHandlerException {
		flushStatements();
		if (!prefixes.containsKey(uri)) {
			prefixes.put(uri, prefix);
		}
	}

	public void handleComment(String comment) throws RDFHandlerException {
		flushStatements();
		delegate.handleComment(comment);
	}

	public synchronized void handleStatement(Statement st)
			throws RDFHandlerException {
		store(st);
		while (queueSize > MAX_QUEUE_SIZE) {
			flushNamespaces();
			delegate.handleStatement(nextStatement());
		}
	}

	private synchronized Statement nextStatement() {
		if (statements.isEmpty())
			return null;
		Iterator<Statement> set = null;
		while (set == null) {
			Set<Statement> stmts = statements.get(stack.peekLast());
			if (stmts == null) {
				stack.pollLast();
			} else {
				set = stmts.iterator();
			}
			if (stack.isEmpty()) {
				set = statements.values().iterator().next().iterator();
			}
		}
		Statement next = set.next();
		queueSize--;
		set.remove();
		if (set.hasNext()) {
			if (!next.getSubject().equals(stack.peekLast())) {
				stack.addLast(next.getSubject());
			}
		} else {
			statements.remove(next.getSubject());
		}
		Value obj = next.getObject();
		if (obj instanceof BNode && statements.containsKey(obj)) {
			stack.addLast((BNode) obj);
		}
		return next;
	}

	private synchronized void store(Statement st) {
		Set<Statement> set = statements.get(st.getSubject());
		if (set == null) {
			statements.put(st.getSubject(), set = new TreeSet<Statement>(
					comparator));
		}
		set.add(st);
		queueSize++;
	}

	private synchronized void flushStatements() throws RDFHandlerException {
		if (!statements.isEmpty()) {
			flushNamespaces();
			Statement st;
			while ((st = nextStatement()) != null) {
				delegate.handleStatement(st);
			}
			queueSize = 0;
		}
	}

	private synchronized void flushNamespaces() throws RDFHandlerException {
		Map<String, String> namespaces = new TreeMap<String, String>();
		for (Map.Entry<String, String> e : prefixes.entrySet()) {
			namespaces.put(e.getValue(), e.getKey());
		}
		for (Map.Entry<String, String> e : namespaces.entrySet()) {
			delegate.handleNamespace(e.getKey(), e.getValue());
		}
		prefixes.clear();
	}

	private synchronized void trimNamespaces() {
		if (!prefixes.isEmpty()) {
			Set<String> used = new HashSet<String>(prefixes.size());
			for (Set<Statement> set : statements.values()) {
				for (Statement st : set) {
					used.add(st.getPredicate().getNamespace());
					if (st.getObject() instanceof URI) {
						URI uri = (URI) st.getObject();
						used.add(uri.getNamespace());
					} else if (st.getObject() instanceof Literal) {
						Literal lit = (Literal) st.getObject();
						if (lit.getDatatype() != null) {
							used.add(lit.getDatatype().getNamespace());
						}
					}
				}
			}
			prefixes.keySet().retainAll(used);
		}
	}

}
