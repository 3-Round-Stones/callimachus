package org.callimachusproject.io;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.callimachusproject.engine.events.AndExpression;
import org.callimachusproject.engine.events.BuiltInCall;
import org.callimachusproject.engine.events.Construct;
import org.callimachusproject.engine.events.Delete;
import org.callimachusproject.engine.events.Filter;
import org.callimachusproject.engine.events.Optional;
import org.callimachusproject.engine.events.OrExpression;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.VarOrTermExpression;
import org.callimachusproject.engine.events.Where;
import org.callimachusproject.engine.helpers.SPARQLWriter;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.MethodNotAllowed;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.ContextStatementCollector;

public abstract class BoundedDescription {
	private static final String DESCRIBE_RESOURCE = "CONSTRUCT { <$resource> ?p ?o } WHERE { <$resource> ?p ?o }";

	private final AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
	private final String resource;
	final int limit;
	private Collection<RDFEvent> pattern;
	private String query;
	private Model graph;

	public BoundedDescription(String resource, int limit) {
		assert resource != null;
		this.resource = resource;
		this.limit = limit;
	}

	public synchronized Model getDescriptionGraph() throws IOException, OpenRDFException {
		if (graph == null) {
			buildDescription();
		}
		return graph;
	}

	public synchronized String getDescribeQuery() throws IOException, OpenRDFException {
		if (query == null){
			buildDescription();
		}
		return query;
	}

	public synchronized String getDeleteUpdate() throws IOException, OpenRDFException {
		if (pattern == null) {
			buildDescription();
		}
		return asSparqlDelete(pattern);
	}

	protected abstract void evaluate(String construct, RDFHandler handler) throws IOException, OpenRDFException;

	private synchronized void buildDescription() throws IOException, OpenRDFException {
		String describe = getDescribeResource(resource);
		Model model = evaluate(describe);
		Collection<RDFEvent> where = buildDescribeQuery(resource, model);
		while (!isBounded(resource, model)) {
			String rq = asSparqlConstruct(where);
			if (describe.equals(rq))
				break;
			describe = rq;
			model = evaluate(describe);
			where = buildDescribeQuery(resource, model);
		}
		pattern = where;
		graph = model;
		query = describe;
	}

	private Model evaluate(String query) throws IOException, OpenRDFException {
		final Model model = new TreeModel();
		evaluate(query, new ContextStatementCollector(model, ValueFactoryImpl.getInstance(), (Resource) null) {
			private int size;
			@Override
			public void handleNamespace(String prefix, String uri)
					throws RDFHandlerException {
				model.setNamespace(prefix, uri);
			}

			@Override
			public void handleStatement(Statement st) {
				if (++size > limit) 
					throw new MethodNotAllowed("Resource description is too large");
				super.handleStatement(st);
			}
		});
		return model;
	}

	private String getDescribeResource(String resource) {
		return DESCRIBE_RESOURCE.replace("$resource", resource);
	}

	private Collection<RDFEvent> buildDescribeQuery(String iri, Model model)
			throws IOException {
		String frag = iri + "#";
		String qs = iri + "?";
		TriplePattern outbound = new TriplePattern(tf.iri(iri), tf.var("p"), tf.var("o"));
		List<List<String>> paths = new ArrayList<List<String>>();
		for (Statement st : model) {
			Value obj = st.getObject();
			if (obj instanceof Literal)
				continue;
			if (model.contains((Resource) obj, null, null))
				continue;
			if (obj instanceof BNode || obj.stringValue().startsWith(frag) || obj.stringValue().startsWith(qs)) {
				addPath(getPath((Resource) obj, model), paths);
			}
		}
		return buildWhereClause(outbound, paths, frag, qs);
	}

	private boolean isBounded(String iri, Model model)
			throws IOException {
		String frag = iri + "#";
		String qs = iri + "?";
		for (Statement st : model) {
			Value obj = st.getObject();
			if (obj instanceof Literal)
				continue;
			if (model.contains((Resource) obj, null, null))
				continue;
			if (obj instanceof BNode || obj.stringValue().startsWith(frag) || obj.stringValue().startsWith(qs)) {
				return false;
			}
		}
		return true;
	}

	private List<String> getPath(Resource obj, Model model) {
		for (Statement st : model.filter(null, null, obj)) {
			String subj = st.getSubject().stringValue();
			String pred = st.getPredicate().stringValue();
			if (st.getSubject() instanceof URI && subj.indexOf('#') < 0 && subj.indexOf('?') < 0) {
				return new ArrayList<String>();
			} else {
				List<String> list = getPath(st.getSubject(), model);
				if (list == null)
					return null;
				list.add(pred);
				return list;
			}
		}
		return null;
	}

	private List<List<String>> addPath(List<String> path, List<List<String>> paths) {
		Iterator<List<String>> iter = paths.iterator();
		while (iter.hasNext()) {
			List<String> p = iter.next();
			if (p.equals(path) || path.size() < p.size() && p.subList(0, path.size()).equals(path)) {
				return paths;
			} else if (p.size() < path.size() && path.subList(0, p.size()).equals(p)) {
				iter.remove();
				break;
			}
		}
		paths.add(path);
		return paths;
	}

	private Collection<RDFEvent> buildWhereClause(TriplePattern outbound,
			List<List<String>> paths, String frag, String qs) throws IOException {
		List<RDFEvent> writer = new ArrayList<RDFEvent>();
		writer.add(outbound);
		for (int i=0,n=paths.size();i<n;i++) {
			List<String> path = paths.get(i);
			VarOrTerm term = outbound.getSubject();
			String prefix = "r" + Integer.toHexString(i);
			addOptional(term, prefix, 0, path, frag, qs, writer);
		}
		return writer;
	}

	private String asSparqlConstruct(Collection<RDFEvent> queue) throws IOException {
		StringWriter sw = new StringWriter();
		SPARQLWriter writer = new SPARQLWriter(sw);
		writer.write(new Construct(true, null));
		for (RDFEvent event : queue) {
			if (event.isTriplePattern() && event.asTriplePattern().getProperty().isVar()) {
				writer.write(event);
			}
		}
		writer.write(new Construct(false, null));
		writer.write(new Where(true, null));
		for (RDFEvent event : queue) {
			writer.write(event);
		}
		writer.write(new Where(false, null));
		writer.close();
		return sw.toString();
	}

	private String asSparqlDelete(Collection<RDFEvent> queue) throws IOException {
		StringWriter sw = new StringWriter();
		SPARQLWriter writer = new SPARQLWriter(sw);
		writer.write(new Delete(true, null));
		for (RDFEvent event : queue) {
			if (event.isTriplePattern() && event.asTriplePattern().getProperty().isVar()) {
				writer.write(event);
			}
		}
		writer.write(new Delete(false, null));
		writer.write(new Where(true, null));
		for (RDFEvent event : queue) {
			writer.write(event);
		}
		writer.write(new Where(false, null));
		writer.close();
		return sw.toString();
	}

	private void addOptional(VarOrTerm term, String prefix, int j,
			List<String> path, String frag, String qs, Collection<RDFEvent> queue)
			throws IOException {
		assert !path.isEmpty();
		queue.add(new Optional(true, null));
		String first = path.get(0);
		String name = prefix + Integer.toHexString(j);
		Var var = tf.var(name);
		queue.add(new TriplePattern(term, tf.iri(first), var));
		filterBlankOrStrStarts(var, frag, qs, queue);
		queue.add(outbound(name));
		int size = path.size();
		if (size > 2) {
			addOptional(var, prefix, j + 1, path.subList(1, size), frag, qs, queue);
		}
		queue.add(new Optional(false, null));
	}

	private void filterBlankOrStrStarts(Var var, String frag, String qs,
			Collection<RDFEvent> queue) throws IOException {
		queue.add(new Filter(true, null));
		queue.add(new BuiltInCall(true, "isBlank", null));
		queue.add(new VarOrTermExpression(var, null));
		queue.add(new BuiltInCall(false, "isBlank", null));
		queue.add(new OrExpression(null));
		queue.add(new BuiltInCall(true, "isIRI", null));
		queue.add(new VarOrTermExpression(var, null));
		queue.add(new BuiltInCall(false, "isIRI", null));
		queue.add(new AndExpression(null));
		queue.add(new BuiltInCall(true, "strStarts", null));
		queue.add(new BuiltInCall(true, "str", null));
		queue.add(new VarOrTermExpression(var, null));
		queue.add(new BuiltInCall(false, "str", null));
		queue.add(new VarOrTermExpression(tf.literal(frag), null));
		queue.add(new BuiltInCall(false, "strStarts", null));
		queue.add(new OrExpression(null));
		queue.add(new BuiltInCall(true, "isIRI", null));
		queue.add(new VarOrTermExpression(var, null));
		queue.add(new BuiltInCall(false, "isIRI", null));
		queue.add(new AndExpression(null));
		queue.add(new BuiltInCall(true, "strStarts", null));
		queue.add(new BuiltInCall(true, "str", null));
		queue.add(new VarOrTermExpression(var, null));
		queue.add(new BuiltInCall(false, "str", null));
		queue.add(new VarOrTermExpression(tf.literal(qs), null));
		queue.add(new BuiltInCall(false, "strStarts", null));
		queue.add(new Filter(false, null));
	}

	private TriplePattern outbound(String name) {
		return new TriplePattern(tf.var(name), tf.var("p" + name), tf.var("o" + name));
	}
}
