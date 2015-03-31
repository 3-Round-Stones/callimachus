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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.management.ObjectServerMXBean;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;

/**
 * Provides access to CalliRepository and revision hash tag.
 */
public abstract class CalliObjectSupport implements CalliObject {
	private final static Map<ObjectRepository, WeakReference<CalliRepository>> repositories = new WeakHashMap<ObjectRepository, WeakReference<CalliRepository>>();

	public static synchronized void associate(CalliRepository repository,
			ObjectRepository repo) throws MalformedURLException {
		Iterator<ObjectRepository> iter = repositories.keySet().iterator();
		while (iter.hasNext()) {
			ObjectRepository key = iter.next();
			if (!key.isInitialized()) {
				iter.remove();
			}
		}
		repositories.put(repo, new WeakReference<CalliRepository>(repository));
	}

	public static synchronized CalliRepository getCalliRepositroyFor(
			ObjectRepository repository) throws OpenRDFException, IOException {
		WeakReference<CalliRepository> ref = repositories.get(repository);
		if (ref != null) {
			CalliRepository result = ref.get();
			if (result != null)
				return result;
		}
		File dataDir = repository.getDataDir();
		if (dataDir == null)
			throw new IllegalArgumentException("Not a local repsitory: " + repository);
		try {
			String url = dataDir.toURI().toASCIIString();
			RepositoryManager manager = RepositoryProvider.getRepositoryManagerOfRepository(url);
			if (!(manager instanceof LocalRepositoryManager))
				throw new IllegalArgumentException("Not a local repsitory manager: " + url);
			LocalRepositoryManager lrm = (LocalRepositoryManager) manager;
			String id = RepositoryProvider.getRepositoryIdOfRepository(url);
			CalliRepository result = new CalliRepository(id, repository, lrm);
			associate(result, repository);
			return result;
		} catch (IllegalArgumentException e) {
			LocalRepositoryManager manager = new LocalRepositoryManager(dataDir);
			CalliRepository result = new CalliRepository(null, repository, manager);
			associate(result, repository);
			return result;
		}
	}

	public void resetAllCache() {
		MBeanServer mbsc = ManagementFactory.getPlatformMBeanServer();
		QueryExp instanceOf = Query.isInstanceOf(Query.value(ObjectServerMXBean.class.getName()));
		for (ObjectName name : mbsc.queryNames(ObjectName.WILDCARD, instanceOf)) {
			ObjectServerMXBean server = JMX.newMXBeanProxy(mbsc, name, ObjectServerMXBean.class);
			server.resetCache();
		}
	}

	public CalliRepository getCalliRepository() throws OpenRDFException, IOException {
		return getCalliRepositroyFor(getObjectConnection().getRepository());
	}

	public DetachedRealm getRealm() throws OpenRDFException, IOException {
		return getCalliRepository().getRealm(this.getResource().stringValue());
	}

	public HttpUriClient getHttpClient() throws OpenRDFException, IOException {
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
