package org.callimachusproject.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.GraphQueryResultImpl;

public class FilteredGraphResult implements GraphQueryResult {
	public final LinkedList<Statement> statements = new LinkedList<>();
	public final LinkedList<GraphQueryResult> results = new LinkedList<>();
	private final Set<URI> omit = new HashSet<URI>();

	public FilteredGraphResult addTriple(URI subj, URI pred, Value obj) {
		Map<String, String> ns = Collections.emptyMap();
		StatementImpl st = new StatementImpl(subj, pred, obj);
		GraphQueryResultImpl result = new GraphQueryResultImpl(ns,
				Arrays.asList(new Statement[] { st }));
		return this.addResult(result);
	}

	public FilteredGraphResult addResult(GraphQueryResult result) {
		results.add(result);
		return this;
	}

	public FilteredGraphResult omitPredicate(URI predicate) {
		omit.add(predicate);
		return this;
	}

	@Override
	public void close() throws QueryEvaluationException {
		for (GraphQueryResult result : results) {
			result.close();
		}
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		while (statements.isEmpty() && !results.isEmpty()) {
			if (results.getFirst().hasNext()) {
				Statement st = results.getFirst().next();
				if (!omit.contains(st.getPredicate())) {
					statements.add(st);
				}
			} else {
				results.poll().close();
			}
		}
		return !statements.isEmpty();
	}

	@Override
	public Statement next() throws QueryEvaluationException {
		if (!hasNext())
			throw new NoSuchElementException();
		return statements.poll();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		results.getFirst().remove();
	}

	@Override
	public Map<String, String> getNamespaces() throws QueryEvaluationException {
		Map<String, String> namespaces = new HashMap<String, String>();
		for (GraphQueryResult result : results) {
			namespaces.putAll(result.getNamespaces());
		}
		return namespaces;
	}

}
