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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RioSetting;
import org.openrdf.rio.WriterConfig;

public class ArrangedWriter implements RDFWriter { 
	private static int MAX_QUEUE_SIZE = 100;
	private final RDFWriter delegate;
	private int queueSize = 0;
	private final Map<String, String> prefixes = new TreeMap<String, String>();
	private final Map<Resource, Set<Statement>> statements = new LinkedHashMap<Resource, Set<Statement>>();
	private final Comparator<Statement> comparator = new Comparator<Statement>() {
		public int compare(Statement s1, Statement s2) {
			String p1 = s1.getPredicate().stringValue();
			String p2 = s2.getPredicate().stringValue();
			int cmp = p1.compareTo(p2);
			if (cmp != 0)
				return cmp;
			String o1 = s1.getObject().stringValue();
			String o2 = s2.getObject().stringValue();
			return o1.compareTo(o2);
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
			Iterator<Statement> set = statements.values().iterator().next()
					.iterator();
			Statement next = set.next();
			flushNamespaces();
			delegate.handleStatement(next);
			queueSize--;
			set.remove();
			if (!set.hasNext()) {
				statements.remove(next.getSubject());
			}
		}
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
			for (Set<Statement> set : statements.values()) {
				for (Statement st : set) {
					delegate.handleStatement(st);
				}
			}
			queueSize = 0;
			statements.clear();
		}
	}

	private synchronized void flushNamespaces() throws RDFHandlerException {
		for (Map.Entry<String, String> e : prefixes.entrySet()) {
			delegate.handleNamespace(e.getValue(), e.getKey());
		}
		prefixes.clear();
	}

	private synchronized void trimNamespaces() {
		if (!prefixes.isEmpty()) {
			Set<String> used = new HashSet<String>(prefixes.size());
			for (Set<Statement> set : statements.values()) {
				for (Statement st : set) {
					used.add(st.getPredicate().getNamespace());
				}
			}
			prefixes.keySet().retainAll(used);
		}
	}

}
