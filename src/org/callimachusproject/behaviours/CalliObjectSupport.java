/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.callimachusproject.behaviours;

import static java.lang.Integer.toHexString;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

/**
 * Provides access to CalliRepository and revision hash tag.
 */
public abstract class CalliObjectSupport implements CalliObject {
	private final static Map<ObjectRepository, WeakReference<CalliRepository>> repositories = new WeakHashMap<ObjectRepository, WeakReference<CalliRepository>>();

	public static void associate(CalliRepository repository, ObjectRepository repo) {
		synchronized (repositories) {
			repositories.put(repo, new WeakReference<CalliRepository>(repository));
		}
	}

	public CalliRepository getCalliRepository() {
		ObjectRepository key = getObjectConnection().getRepository();
		synchronized (repositories) {
			WeakReference<CalliRepository> ref = repositories.get(key);
			assert ref != null;
			assert ref.get() != null;
			return ref.get();
		}
	}

	public DetachedRealm getRealm() throws OpenRDFException {
		return getCalliRepository().getRealm(this.getResource().stringValue());
	}

	public HttpUriClient getHttpClient() throws OpenRDFException {
		return getCalliRepository().getHttpClient(this.getResource().stringValue());
	}

	public void touchRevision() throws RepositoryException {
		Resource resource = getResource();
		if (resource instanceof URI) {
			String self = resource.stringValue();
			ObjectConnection con = getObjectConnection();
			ObjectFactory of = con.getObjectFactory();
			if (self.contains("#")) {
				CalliObject parent = (CalliObject) of.createObject(self.substring(0, self.indexOf('#')));
				parent.touchRevision();
			} else {
				URI bundle = con.getVersionBundle();
				if (bundle == null) {
					setProvWasGeneratedBy(null);
				} else {
					AuditingRepositoryConnection audit = findAuditing(con);
					if (audit == null) {
						setProvWasGeneratedBy(null);
					} else {
						ValueFactory vf = con.getValueFactory();
						ActivityFactory delegate = audit.getActivityFactory();
						URI activity = delegate.createActivityURI(bundle, vf);
						setProvWasGeneratedBy(of.createObject(activity, Activity.class));
					}
				}
			}
		}
	}

	public String revision() {
		try {
			Activity activity = getProvWasGeneratedBy();
			if (activity == null)
				return null;
			String uri = ((RDFObject) activity).getResource().stringValue();
			return toHexString(uri.hashCode());
		} catch (ClassCastException e) {
			return null;
		}
	}

	private AuditingRepositoryConnection findAuditing(
			RepositoryConnection con) throws RepositoryException {
		if (con instanceof AuditingRepositoryConnection)
			return (AuditingRepositoryConnection) con;
		if (con instanceof RepositoryConnectionWrapper)
			return findAuditing(((RepositoryConnectionWrapper) con).getDelegate());
		return null;
	}

}
