package org.callimachusproject.io;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

public class DescribeResult implements GraphQueryResult {
	private final Set<Resource> seen = new HashSet<Resource>();
	private final LinkedList<Resource> queue = new LinkedList<Resource>();
	private final String base;
	private final boolean baseIsHash;
	private final RepositoryConnection con;
	private final LinkedList<RepositoryResult<Statement>> results = new LinkedList<RepositoryResult<Statement>>();
	private final boolean closeConnection;
	private Statement last;

	public DescribeResult(URI resource, RepositoryConnection con)
			throws OpenRDFException {
		this(resource, con, false);
	}

	public DescribeResult(URI resource, RepositoryConnection toBeClosed, boolean closeConnection)
			throws OpenRDFException {
		this.con = toBeClosed;
		this.closeConnection = closeConnection;
		seen.add(resource);
		queue.push(resource);
		base = resource.stringValue();
		baseIsHash = base.charAt(base.length() - 1) == '#';
		RepositoryResult<Statement> stmts = con.getStatements(null, RDFS.ISDEFINEDBY, resource, false);
		try {
			while (stmts.hasNext()) {
				pushIfMember(stmts.next().getSubject());
			}
		} finally {
			stmts.close();
		}
	}

	public synchronized void close() throws QueryEvaluationException {
		try {
			try {
				for (RepositoryResult<Statement> stmts : results) {
					stmts.close();
				}
				results.clear();
			} finally {
				if (closeConnection) {
					con.close();
				}
			}
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	public Map<String, String> getNamespaces()
			throws QueryEvaluationException {
		try {
			RepositoryResult<Namespace> namespaces = con.getNamespaces();
			Map<String, String> map = new LinkedHashMap<String, String>();
			while (namespaces.hasNext()) {
				Namespace ns = namespaces.next();
				map.put(ns.getPrefix(), ns.getName());
			}
			return map;
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	public synchronized boolean hasNext() throws QueryEvaluationException {
		try {
			while (!results.isEmpty() && !results.peek().hasNext()) {
				results.poll().close();
			}
			while ((results.isEmpty() || !results.peek().hasNext()) && queue.size() > 0) {
				if (!results.isEmpty()) {
					results.poll().close();
				}
				results.push(con.getStatements(queue.poll(), null, null, false));
			}
			while (!results.isEmpty() && !results.peek().hasNext()) {
				results.poll().close();
			}
			return !results.isEmpty();
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	public synchronized Statement next() throws QueryEvaluationException {
		if (!hasNext())
			throw new NoSuchElementException();
		try {
			RepositoryResult<Statement> stmts = results.peek();
			Statement st = stmts.next();
			while (last != null && stmts.hasNext()
					&& st.getSubject() == last.getSubject()
					&& st.getPredicate() == last.getPredicate()
					&& st.getObject() == last.getObject()) {
				st = stmts.next();
			}
			if (pushIfMember(st.getObject())) {
				results.push(con.getStatements(queue.poll(), null, null, false));
			}
			last = st;
			return st;
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	public synchronized void remove() throws QueryEvaluationException {
		try {
			results.peekLast().remove();
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private boolean pushIfMember(Value object) {
		String uri = object.stringValue();
		if (object instanceof URI) {
			if (uri.length() > base.length() && uri.indexOf(base) == 0
					&& !seen.contains(object)) {
				char chr = uri.charAt(base.length());
				if (baseIsHash || chr == '#') {
					seen.add((URI) object);
					queue.push((URI) object);
				} else if (chr == '?') {
					seen.add((URI) object);
					queue.push((URI) object);
				}
			}
		} else if (object instanceof Resource) {
			if (!seen.contains(object)) {
				seen.add((Resource) object);
				queue.push((Resource) object);
				return true;
			}
		}
		return false;
	}
}