/*
 * Copyright (c) 2009-2010, James Leigh All rights reserved.
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
package org.callimachusproject.sail.auditing;

import info.aduna.iteration.CloseableIteration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.callimachusproject.sail.auditing.helpers.OperationEntityResolver;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.UpdateContext;
import org.openrdf.sail.helpers.SailConnectionWrapper;

/**
 * Intercepts the add and remove operations and add a revision to each resource.
 */
public class AuditingConnection extends SailConnectionWrapper {
	private static final String AUDIT_2012 = "http://www.openrdf.org/rdf/2012/auditing#";
	private static final String PROV = "http://www.w3.org/ns/prov#";
	private static final String WAS_INFLUENCED_BY = PROV + "wasInfluencedBy";
	private static final String REVISION_OF = PROV + "wasRevisionOf";
	private static final String GENERATED_BY = PROV + "wasGeneratedBy";
	private static final String WITH = AUDIT_2012 + "with";
	private static final String WITHOUT = AUDIT_2012 + "without";
	static int MAX_REVISED = 1024;
	private final ValueFactory vf;
	private final Map<Resource, Boolean> revised = new LinkedHashMap<Resource, Boolean>(
			128, 0.75f, true) {
		private static final long serialVersionUID = 1863694012435196527L;

		protected boolean removeEldestEntry(Entry<Resource, Boolean> eldest) {
			return size() > MAX_REVISED;
		}
	};
	private final Set<Resource> modified = new HashSet<Resource>();
	private final List<Statement> arch = new ArrayList<Statement>();
	private final OperationEntityResolver entityResolver;
	private final URI influencedBy;
	private final URI with;
	private final URI without;
	private final URI revisionOf;
	private final URI subject;
	private final URI predicate;
	private final URI object;
	private boolean auditingRemoval = true;
	private final Map<UpdateContext,URI> entities = Collections.synchronizedMap(new HashMap<UpdateContext, URI>());

	public AuditingConnection(AuditingSail sail, SailConnection wrappedCon) {
		super(wrappedCon);
		vf = sail.getValueFactory();
		entityResolver = new OperationEntityResolver(vf);
		influencedBy = vf.createURI(WAS_INFLUENCED_BY);
		revisionOf = vf.createURI(REVISION_OF);
		with = vf.createURI(WITH);
		without = vf.createURI(WITHOUT);
		subject = vf.createURI(RDF.SUBJECT.stringValue());
		predicate = vf.createURI(RDF.PREDICATE.stringValue());
		object = vf.createURI(RDF.OBJECT.stringValue());
	}

	public boolean isAuditingRemoval() {
		return auditingRemoval;
	}

	public void setAuditingRemoval(boolean auditingRemoval) {
		this.auditingRemoval = auditingRemoval;
	}

	@Override
	public SailConnection getWrappedConnection() {
		return super.getWrappedConnection();
	}

	@Override
	public void startUpdate(UpdateContext uc) throws SailException {
		super.startUpdate(uc);
		Dataset ds = uc.getDataset();
		URI bundle = ds.getDefaultInsertGraph();
		if (bundle != null) {
			QueryModelNode node = uc.getUpdateExpr();
			if (node instanceof Modify) {
				node = ((Modify) node).getDeleteExpr();
			}
			URI entity = entityResolver.getEntity(node, ds, uc.getBindingSet());
			entities.put(uc, entity);
		}
	}

	@Override
	public void removeStatement(UpdateContext uc, Resource subj, URI pred,
			Value obj, Resource... ctx) throws SailException {
		Dataset ds = uc.getDataset();
		URI bundle = ds.getDefaultInsertGraph();
		if (bundle == null || !isAuditingRemoval()) {
			super.removeStatement(uc, subj, pred, obj, ctx);
		} else {
			QueryModelNode node = uc.getUpdateExpr();
			if (node instanceof Modify) {
				node = ((Modify) node).getDeleteExpr();
			}
			URI entity = entities.get(uc);
			removeInforming(bundle, entity, uc, subj, pred, obj, ctx);
		}
	}

	@Override
	public void endUpdate(UpdateContext modify) throws SailException {
		entities.remove(modify);
		super.endUpdate(modify);
	}

	@Override
	public synchronized void commit() throws SailException {
		super.commit();
		revised.clear();
		modified.clear();
		arch.clear();
	}

	@Override
	public synchronized void rollback() throws SailException {
		revised.clear();
		modified.clear();
		arch.clear();
		super.rollback();
	}

	public String toString() {
		return super.toString();
	}

	void removeInforming(URI bundle, URI entity, UpdateContext uc, Resource subj, URI pred,
			Value obj, Resource... contexts) throws SailException {
		if (contexts != null && contexts.length == 0) {
			CloseableIteration<? extends Statement, SailException> stmts;
			stmts = super.getStatements(subj, pred, obj, false, contexts);
			try {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					Resource ctx = st.getContext();
					subj = st.getSubject();
					pred = st.getPredicate();
					obj = st.getObject();
					removeInformingGraph(bundle, entity, uc, subj, pred, obj, ctx);
				}
			} finally {
				stmts.close();
			}
		} else if (contexts == null) {
			removeInformingGraph(bundle, entity, uc, subj, pred, obj, null);
		} else {
			for (Resource ctx : contexts) {
				removeInformingGraph(bundle, entity, uc, subj, pred, obj, ctx);
			}
		}
	}

	private void removeInformingGraph(URI bundle, URI entity, UpdateContext uc,
			Resource subj, URI pred, Value obj, Resource ctx)
			throws SailException {
		reify(bundle, entity, subj, pred, obj, ctx);
		super.removeStatement(uc, subj, pred, obj, ctx);
	}

	private void reify(URI bundle, URI entity, Resource subj, URI pred,
			Value obj, Resource ctx) throws SailException {
		String ns = bundle.stringValue();
		if (!(ctx instanceof URI) || ctx.equals(bundle))
			return;
		super.addStatement(bundle, influencedBy, ctx, bundle);
		String graph = ctx.stringValue();
		if (GENERATED_BY.equals(pred.stringValue())) {
			if (!ctx.equals(subj)) {
				URI prev = vf.createURI(graph + "#!" + subj.stringValue());
				URI next = vf.createURI(ns + "#!" + subj.stringValue());
				super.addStatement(next, revisionOf, prev, bundle);
			}
		} else if (entity != null
				&& !(subj instanceof URI && !subj.stringValue().startsWith(
						entity.stringValue()))) {
			URI next = vf.createURI(ns + "#!" + entity.stringValue());
			URI node = vf.createURI(ns + "#!" + hash(subj, pred, obj));
			super.addStatement(next, without, node, bundle);
			super.addStatement(node, subject, subj, bundle);
			super.addStatement(node, predicate, pred, bundle);
			super.addStatement(node, object, obj, bundle);
			if (graph.indexOf('#') < 0) {
				String prev = graph + "#!" + entity.stringValue();
				super.addStatement(vf.createURI(prev), with, node, ctx);
			}
		}
	}

	private String hash(Resource subj, URI pred, Value obj) {
		long hash = 31 * 31 * subj.hashCode() + 31 * pred.hashCode() + obj.hashCode();
		if (obj instanceof Literal) {
			Literal lit = (Literal) obj;
			if (lit.getLanguage() != null) {
				hash += lit.getLanguage().hashCode() * 31 * 31 * 31;
			} else if (lit.getDatatype() != null) {
				hash += lit.getDatatype().hashCode() * 31 * 31 * 31;
			}
		}
		return Long.toHexString(Math.abs(hash));
	}
}
