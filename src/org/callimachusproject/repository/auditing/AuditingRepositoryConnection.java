/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.repository.auditing;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.repository.auditing.helpers.BasicGraphPatternVisitor;
import org.callimachusproject.sail.auditing.AuditingConnection;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.query.algebra.Add;
import org.openrdf.query.algebra.Clear;
import org.openrdf.query.algebra.Copy;
import org.openrdf.query.algebra.Create;
import org.openrdf.query.algebra.DeleteData;
import org.openrdf.query.algebra.InsertData;
import org.openrdf.query.algebra.Load;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.Move;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.repository.DelegatingRepositoryConnection;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.helpers.SailConnectionWrapper;

/**
 * RepositoryConnection that exposes the activityFactory property for a connection.
 */
public class AuditingRepositoryConnection extends ContextAwareConnection {

	private static final int MAX_SIZE = 1024;
	private static final String RECENT_BUNDLE = "http://www.openrdf.org/rdf/2012/auditing#RecentBundle";
	private static final String GENERATED = "http://www.w3.org/ns/prov#generated";
	private static final String WAS_GENERATED_BY = "http://www.w3.org/ns/prov#wasGeneratedBy";
	private static final String SPECIALIZATION_OF = "http://www.w3.org/ns/prov#specializationOf";
	private static final String UPDATE_ACTIVITY = "PREFIX prov:<http://www.w3.org/ns/prov#>\n"
			+ "DELETE {\n\t"
			+ "GRAPH ?influencedBy { ?used prov:wasGeneratedBy ?generatedBy }\n\t"
			+ "GRAPH ?influencedBy { ?entity prov:wasGeneratedBy ?generatedBy }\n"
			+ "} INSERT {\n\t"
			+ "GRAPH $bundle { ?used prov:wasGeneratedBy $activity }\n"
			+ "GRAPH $bundle { $activity prov:generated ?generated . ?specialization prov:specializationOf ?entity }\n\t"
			+ "GRAPH $bundle { ?entity prov:wasGeneratedBy $activity }\n"
			+ "} WHERE {\n\t"
			+ "{\n\t\t"
			+ "GRAPH $bundle { $activity prov:generated ?gen . ?gen prov:specializationOf ?used }\n\t\t"
			+ "GRAPH ?influencedBy { ?used prov:wasGeneratedBy ?generatedBy } FILTER (?influencedBy != $bundle)\n\t\t"
			+ "BIND (iri(concat(str(?influencedBy),'#!',str(?used))) AS ?revised)\n\t"
			+ "} UNION {\n\t\t"
			+ "GRAPH $bundle { ?entity prov:wasGeneratedBy $activity }\n\t\t"
			+ "FILTER ($bundle != ?entity)\n\t\t"
			+ "OPTIONAL { GRAPH ?influencedBy { ?entity prov:wasGeneratedBy ?generatedBy }\n\t\t\t"
			+ "BIND (iri(concat(str(?influencedBy),'#!',str(?entity))) AS ?revised)\n\t\t\t"
			+ "FILTER (?influencedBy != $bundle) }\n\t\t"
			+ "BIND (if(contains(str(?entity),'#'),?entity,iri(concat(str($bundle),'#!',str(?entity)))) AS ?generated)\n\t"
			+ "BIND (if(contains(str(?entity),'#'),?nil,iri(concat(str($bundle),'#!',str(?entity)))) AS ?specialization)\n\t"
			+ "} UNION {\n\t\t"
			+ "GRAPH $bundle { ?resource ?predicate ?object }\n\t\t"
			+ "FILTER isIri(?resource)\n\t\t"
			+ "FILTER ( !sameTerm($bundle,?resource) )\n\t\t"
			+ "BIND ( if( contains(str(?resource),\"#\"), iri(strbefore(str(?resource),\"#\")), ?resource ) AS ?entity)\n\t\t"
			+ "FILTER ( !sameTerm($bundle,?entity) )\n\t\t"
			+ "OPTIONAL { GRAPH ?influencedBy { ?entity prov:wasGeneratedBy ?generatedBy }\n\t\t\t"
			+ "BIND (iri(concat(str(?influencedBy),'#!',str(?entity))) AS ?revised)}\n\t\t"
			+ "FILTER (!bound(?influencedBy) || ?influencedBy != $bundle)\n\t\t"
			+ "BIND (if(contains(str(?entity),'#'),?entity,iri(concat(str($bundle),'#!',str(?entity)))) AS ?generated)\n\t"
			+ "BIND (if(contains(str(?entity),'#'),?nil,iri(concat(str($bundle),'#!',str(?entity)))) AS ?specialization)\n\t"
			+ "}\n"
			+ "}";
	private static final String BALANCE_ACTIVITY = UPDATE_ACTIVITY.substring(0,
			UPDATE_ACTIVITY.length() - 2)
			+ "\n\t"
			+ "FILTER (\n\t\t"
			+ "!bound(?influencedBy) ||\n\t\t"
			+ "EXISTS { $bundle prov:wasInformedBy ?influencedBy } ||\n\t\t"
			+ "EXISTS { $activity prov:endedAtTime ?after . ?generatedBy prov:endedAtTime ?before FILTER (?before < ?after) }\n\t"
			+ ")\n" + "}";

	private final AuditingRepository repository;
	private final Map<URI, Set<URI>> modifiedGraphs = new HashMap<URI, Set<URI>>();
	private final Map<URI, Map<URI, Boolean>> modifiedEntities = new HashMap<URI, Map<URI, Boolean>>();
	private final URI provGenerated;
	private final URI provSpecializationOf;
	private final URI provWasGeneratedBy;
	private Map<URI, URI> uncommittedBundles = new LinkedHashMap<URI, URI>();
	private ActivityFactory activityFactory;
	private URI insertContext;
	private URI activityURI;
	private boolean auditingRemoval = true;

	public AuditingRepositoryConnection(AuditingRepository repository,
			RepositoryConnection connection) throws RepositoryException {
		super(repository, connection);
		this.repository = repository;
		provGenerated = connection.getValueFactory().createURI(GENERATED);
		provSpecializationOf = connection.getValueFactory().createURI(SPECIALIZATION_OF);
		provWasGeneratedBy = connection.getValueFactory().createURI(WAS_GENERATED_BY);
	}

	public ActivityFactory getActivityFactory() {
		return activityFactory;
	}

	public void setActivityFactory(ActivityFactory activityFactory) {
		this.activityFactory = activityFactory;
	}

	@Override
	public AuditingRepository getRepository() {
		return repository;
	}

	public boolean isAuditingRemoval() {
		return auditingRemoval;
	}

	public void setAuditingRemoval(boolean auditingRemoval) throws RepositoryException {
		this.auditingRemoval = auditingRemoval;
		setAuditingRemoval(getDelegate(), auditingRemoval);
	}

	@Override
	public synchronized URI getInsertContext() {
		URI bundle = super.getInsertContext();
		ActivityFactory activityFactory = getActivityFactory();
		if (bundle == null && activityFactory != null) {
			ValueFactory vf = getValueFactory();
			activityURI = activityFactory.createActivityURI(bundle, vf);
			if (activityURI != null) {
				String str = activityURI.stringValue();
				int h = str.indexOf('#');
				if (h > 0) {
					insertContext = vf.createURI(str.substring(0, h));
					setInsertContext(insertContext);
				} else {
					insertContext = activityURI;
					setInsertContext(activityURI);
				}
				return super.getInsertContext();
			}
		}
		return bundle;
	}

	@Override
	public void commit() throws RepositoryException {
		Map<URI,URI> recentBundles = finalizeBundles();
		super.commit();
		closeBundle(recentBundles);
	}

	@Override
	public void rollback() throws RepositoryException {
		super.rollback();
		reset();
	}

	@Override
	public void close() throws RepositoryException {
		super.close();
		getRepository().cleanup();
	}

	@Override
	public Update prepareUpdate(String query) throws MalformedQueryException,
			RepositoryException {
		return prepareUpdate(getQueryLanguage(), query);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareUpdate(ql, query, getBaseURI());
	}

	@Override
	public Update prepareUpdate(final QueryLanguage ql, final String update, final String baseURI)
			throws MalformedQueryException, RepositoryException {
		final Update prepared = super.prepareUpdate(ql, update, baseURI);
		if (prepared == null)
			return prepared;
		return new Update(){
			public void execute() throws UpdateExecutionException {
				try {
					boolean autoCommit = !isActive();
					try {
						if (autoCommit) {
							begin();
						}
						try {
							BindingSet bindings = prepared.getBindings();
							Dataset dataset = prepared.getDataset();
							if (dataset != null) {
								activity(ql, update, baseURI, bindings, dataset);
							}
						} catch (MalformedQueryException e) {
							// ignore
						} catch (QueryEvaluationException e) {
							// ignore
						}
						prepared.execute();
						if (autoCommit) {
							commit();
						}
					} finally {
						if (autoCommit) {
							rollback();
						}
					}
				} catch (RepositoryException e) {
					throw new UpdateExecutionException(e);
				}
			}

			public void setBinding(String name, Value value) {
				prepared.setBinding(name, value);
			}

			public void removeBinding(String name) {
				prepared.removeBinding(name);
			}

			public void clearBindings() {
				prepared.clearBindings();
			}

			public BindingSet getBindings() {
				return prepared.getBindings();
			}

			public void setDataset(Dataset dataset) {
				prepared.setDataset(dataset);
			}

			public Dataset getDataset() {
				return prepared.getDataset();
			}

			public void setIncludeInferred(boolean includeInferred) {
				prepared.setIncludeInferred(includeInferred);
			}

			public boolean getIncludeInferred() {
				return prepared.getIncludeInferred();
			}
		};
	}

	@Override
	protected boolean isDelegatingAdd() throws RepositoryException {
		return getInsertContext() == null;
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		activity(getInsertContext(), true, subject);
		for (Resource ctx : contexts) {
			activity(getInsertContext(), true, ctx);
		}
		getDelegate().add(subject, predicate, object, contexts);
	}

	@Override
	protected boolean isDelegatingRemove() throws RepositoryException {
		return !isAuditingRemoval() || getInsertContext() == null;
	}

	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		Resource[] defRemove = getRemoveContexts();
		URI activityGraph = getInsertContext();
		activity(activityGraph, false, subject);
		if (contexts == null) {
			getDelegate().remove(subject, predicate, object, contexts);
		} else if (contexts.length > 0) {
			for (Resource ctx : contexts) {
				activity(activityGraph, false, ctx);
			}
			getDelegate().remove(subject, predicate, object, contexts);
		} else if (defRemove == null) {
			getDelegate().remove(subject, predicate, object, defRemove);
		} else if (defRemove.length > 0) {
			for (Resource ctx : defRemove) {
				activity(activityGraph, false, ctx);
			}
			getDelegate().remove(subject, predicate, object, defRemove);
		} else if (isAuditingRemoval()) {
			executeDelete(subject, predicate, object);
		} else {
			getDelegate().remove(subject, predicate, object, contexts);
		}
	}

	private void setAuditingRemoval(RepositoryConnection delegate,
			boolean auditingRemoval) throws RepositoryException {
		if (delegate instanceof AuditingRepositoryConnection) {
			((AuditingRepositoryConnection) delegate).setAuditingRemoval(auditingRemoval);
		} else if (delegate instanceof DelegatingRepositoryConnection) {
			setAuditingRemoval(((DelegatingRepositoryConnection) delegate).getDelegate(), auditingRemoval);
		} else if (delegate instanceof SailRepositoryConnection) {
			setAuditingRemoval(((SailRepositoryConnection) delegate).getSailConnection(), auditingRemoval);
		}
	}

	private void setAuditingRemoval(SailConnection delegate,
			boolean auditingRemoval) {
		if (delegate instanceof AuditingConnection) {
			((AuditingConnection) delegate).setAuditingRemoval(auditingRemoval);
		} else if (delegate instanceof SailConnectionWrapper) {
			setAuditingRemoval(((SailConnectionWrapper) delegate).getWrappedConnection(), auditingRemoval);
		}
	}

	private void executeDelete(Resource subject, URI predicate, Value object)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		if (subject instanceof URI && predicate instanceof URI
				&& (object instanceof URI || object instanceof Literal)) {
			sb.append("DELETE DATA");
		} else {
			sb.append("DELETE WHERE");
		}
		sb.append(" { ");
		append(subject, predicate, object, sb);
		sb.append(" }");
		String operation = sb.toString();
		try {
			Update update = prepareUpdate(QueryLanguage.SPARQL, operation);
			if (subject instanceof BNode) {
				update.setBinding("subject", subject);
			}
			if (object instanceof BNode) {
				update.setBinding("object", object);
			}
			update.execute();
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}
	}

	private void append(Resource subject, URI predicate, Value object,
			StringBuilder sb) {
		if (subject instanceof URI) {
			sb.append("<").append(enc(subject)).append("> ");
		} else {
			sb.append("?subject ");
		}
		if (predicate instanceof URI) {
			sb.append("<").append(enc(predicate)).append("> ");
		} else {
			sb.append("?predicate ");
		}
		if (object instanceof URI) {
			sb.append("<").append(enc(object)).append("> ");
		} else if (object instanceof Literal) {
			Literal lit = (Literal) object;
			sb.append('"');
			sb.append(encodeString(lit.stringValue()));
			sb.append('"');
			if (lit.getLanguage() != null) {
				sb.append("@");
				sb.append(lit.getLanguage());
			} else if (lit.getDatatype() != null) {
				sb.append("^^");
				sb.append("<").append(enc(lit.getDatatype())).append("> ");
			}
		} else {
			sb.append("?object ");
		}
	}

	private String enc(Value iri) {
		String uri = iri.stringValue();
		uri = uri.replace("\\", "\\\\");
		uri = uri.replace(">", "\\>");
		return uri;
	}

	private String encodeString(String label) {
		label = label.replace("\\", "\\\\");
		label = label.replace("\t", "\\t");
		label = label.replace("\n", "\\n");
		label = label.replace("\r", "\\r");
		label = label.replace("\"", "\\\"");
		return label;
	}

	void activity(QueryLanguage ql, String update, String baseURI,
			BindingSet bindings, Dataset dataset)
			throws MalformedQueryException, RepositoryException,
			QueryEvaluationException {
		QueryParser parser = QueryParserUtil.createParser(ql);
		ParsedUpdate parsed = parser.parseUpdate(update, baseURI);
		for (UpdateExpr expr : parsed.getUpdateExprs()) {
			if (expr instanceof Modify) {
				QueryModelNode deleteExpr = ((Modify) expr).getDeleteExpr();
				QueryModelNode insertExpr = ((Modify) expr).getInsertExpr();
				if (deleteExpr != null) {
					deleteActivity(deleteExpr, bindings, dataset);
				}
				if (insertExpr != null) {
					insertActivity(insertExpr, bindings, dataset);
				}
			} else if (isDeleteOperation(expr)) {
				deleteActivity(expr, bindings, dataset);
			} else if (isInsertOperation(expr)) {
				insertActivity(expr, bindings, dataset);
			}
		}
	}

	private void insertActivity(QueryModelNode insertExpr, BindingSet bindings,
			Dataset dataset) throws QueryEvaluationException,
			RepositoryException {
		URI activityGraph = dataset.getDefaultInsertGraph();
		for (URI entity : findEntity(insertExpr, bindings, activityGraph)) {
			activity(activityGraph, true, entity);
		}
		for (URI graph : findGraphs(insertExpr, bindings, dataset)) {
			activity(activityGraph, true, graph);
		}
	}

	private void deleteActivity(QueryModelNode deleteExpr, BindingSet bindings,
			Dataset dataset) throws QueryEvaluationException,
			RepositoryException {
		URI activityGraph = dataset.getDefaultInsertGraph();
		for (URI entity : findEntity(deleteExpr, bindings, activityGraph)) {
			activity(activityGraph, false, entity);
		}
		for (URI graph : findGraphs(deleteExpr, bindings, dataset)) {
			activity(activityGraph, false, graph);
		}
	}

	private boolean isInsertOperation(UpdateExpr expr) {
		return expr instanceof Add || expr instanceof Copy
				|| expr instanceof Create || expr instanceof InsertData
				|| expr instanceof Load || expr instanceof Move;
	}

	private boolean isDeleteOperation(UpdateExpr expr) {
		return expr instanceof Clear || expr instanceof DeleteData;
	}

	private Set<URI> findEntity(QueryModelNode expr, final BindingSet bindings, final URI bundle) throws QueryEvaluationException {
		final Set<URI> entities = new HashSet<URI>();
		expr.visit(new BasicGraphPatternVisitor() {
			public void meet(StatementPattern node) {
				Var var = node.getSubjectVar();
				Value subj = var.getValue();
				if (subj == null) {
					subj = bindings.getValue(var.getName());
				}
				if (subj instanceof URI) {
					entities.add(entity(bundle, (URI) subj));
				}
			}
		});
		return entities;
	}

	private URI[] findGraphs(QueryModelNode expr, final BindingSet bindings,
			Dataset dataset) throws QueryEvaluationException {
		final Set<URI> graphs = new LinkedHashSet<URI>();
		if (dataset != null) {
			if (dataset.getDefaultInsertGraph() != null) {
				graphs.add(dataset.getDefaultInsertGraph());
			}
			if (dataset.getDefaultRemoveGraphs() != null) {
				graphs.addAll(dataset.getDefaultRemoveGraphs());
			}
		}
		expr.visit(new BasicGraphPatternVisitor() {
			public void meet(StatementPattern node) {
				Var var = node.getContextVar();
				if (var != null) {
					Value ctx = var.getValue();
					if (ctx == null) {
						ctx = bindings.getValue(var.getName());
					}
					if (ctx instanceof URI) {
						graphs.add((URI) ctx);
					}
				}
			}
		});
		return graphs.toArray(new URI[graphs.size()]);
	}

	URI entity(URI bundle, URI subject) {
		URI entity = subject;
		String uri = entity.stringValue();
		int hash = uri.indexOf('#');
		if (hash > 0 && !uri.substring(0, hash).equals(bundle.stringValue())) {
			ValueFactory vf = getValueFactory();
			entity = vf.createURI(uri.substring(0, hash));
		}
		return entity;
	}

	private synchronized void activity(URI bundle, boolean inserted, Resource subject) throws RepositoryException {
		if (bundle == null)
			return;
		URI activity = uncommittedBundles.get(bundle);
		RepositoryConnection con = getDelegate();
		if (activity == null) {
			activity = getActivityURI(bundle);
			if (activity == null)
				return;
			uncommittedBundles.put(bundle, activity);
			ActivityFactory activityFactory = getActivityFactory();
			if (activityFactory != null) {
				activityFactory.activityStarted(activity, bundle, con);
			}
			con.add(bundle, provWasGeneratedBy, activity, bundle);
		}
		if (subject instanceof URI && !isBundledEntity(bundle, activity, subject)) {
			Map<URI, Boolean> entities = modifiedEntities.get(bundle);
			if (entities == null) {
				modifiedEntities.put(bundle, entities = new HashMap<URI, Boolean>());
			}
			URI entity = entity(bundle, (URI) subject);
			Boolean wasInserted = entities.get(entity);
			if (entities.size() >= MAX_SIZE) {
				entities.clear();
			}
			if (inserted && wasInserted != Boolean.TRUE) {
				entities.put(entity, Boolean.TRUE);
				con.add(entity, provWasGeneratedBy, activity, bundle);
			} else if (wasInserted == null) {
				entities.put(entity, inserted ? Boolean.TRUE : Boolean.FALSE);
				generated(activity, entity, bundle, bundle, con);
			}
		}
	}

	private synchronized URI getActivityURI(URI bundle) {
		if (bundle == null)
			return null;
		if (bundle.equals(this.insertContext))
			return activityURI;
		ActivityFactory af = getActivityFactory();
		if (af == null)
			return null;
		this.insertContext = bundle;
		ValueFactory vf = getValueFactory();
		return activityURI = af.createActivityURI(bundle, vf);
	}

	private void generated(URI activity, URI entity, URI targetGraph, URI bundle,
			RepositoryConnection con) throws RepositoryException {
		ValueFactory vf = getValueFactory();
		String target = targetGraph.stringValue();
		if (target.indexOf('#') > 0) {
			con.add(activity, provGenerated, entity, bundle);
		} else {
			URI gen = vf.createURI(target + "#!" + entity.stringValue());
			con.add(activity, provGenerated, gen, bundle);
			con.add(gen, provSpecializationOf, entity, bundle);
		}
	}

	private boolean isBundledEntity(URI bundle, URI activity, Resource entity) {
		if (bundle.equals(entity) || activity.equals(entity))
			return true;
		if (entity instanceof URI) {
			String ns = bundle.stringValue();
			String subj = entity.stringValue();
			return subj.startsWith(ns) && subj.startsWith(ns + "#!");
		}
		return false;
	}

	private synchronized Map<URI,URI> finalizeBundles()
			throws RepositoryException {
		Map<URI, URI> recentBundles = uncommittedBundles;
		int size = recentBundles.size();
		uncommittedBundles = new LinkedHashMap<URI,URI>(size);
		for (Map.Entry<URI, URI> e : recentBundles.entrySet()) {
			addMetadata(e.getValue(), e.getKey());
			if (getRepository().isTransactional()) {
				finalizeBundle(e.getValue(), e.getKey());
			}
		}
		uncommittedBundles.clear();
		modifiedGraphs.clear();
		modifiedEntities.clear();
		return recentBundles;
	}

	private synchronized void reset() {
		uncommittedBundles = new LinkedHashMap<URI, URI>(uncommittedBundles.size());
		modifiedGraphs.clear();
		modifiedEntities.clear();
	}

	private void addMetadata(URI activity, URI bundle) throws RepositoryException {
		URI recentBundle = getValueFactory().createURI(RECENT_BUNDLE);
		getDelegate().add(bundle, RDF.TYPE, recentBundle, bundle);
	}

	private void finalizeBundle(URI activity, URI bundle)
			throws RepositoryException {
		try {
			Update update = prepareUpdate(SPARQL, UPDATE_ACTIVITY);
			update.setBinding("bundle", bundle);
			update.setBinding("activity", activity);
			update.execute();
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
	}

	private void closeBundle(Map<URI, URI> recentBundles)
			throws RepositoryException {
		getRepository().addRecentBundles(recentBundles.keySet());
		if (!getRepository().isTransactional()) {
			for (Map.Entry<URI, URI> e : recentBundles.entrySet()) {
				balanceBundle(e.getValue(), e.getKey());
			}
		}
		ActivityFactory activityFactory = getActivityFactory();
		if (activityFactory != null) {
			for (Map.Entry<URI, URI> e : recentBundles.entrySet()) {
				activityFactory.activityEnded(e.getValue(), e.getKey(), getDelegate());
			}
		}
	}

	private void balanceBundle(URI provActivity, URI activityGraph)
			throws RepositoryException {
		try {
			Update update = prepareUpdate(SPARQL, BALANCE_ACTIVITY);
			update.setBinding("bundle", activityGraph);
			update.setBinding("activity", provActivity);
			update.execute();
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
	}

}
