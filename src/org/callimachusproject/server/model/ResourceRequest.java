/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.model;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.HttpEntity;
import org.callimachusproject.annotations.type;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the target resource with the request.
 * 
 * @author James Leigh
 * 
 */
public class ResourceRequest extends Request {
	private final FluidFactory ff = FluidFactory.getInstance();
	private final Logger logger = LoggerFactory.getLogger(ResourceRequest.class);
	private final ValueFactory vf;
	private final ObjectConnection con;
	private VersionedObject target;
	private final URI uri;
	private final FluidBuilder writer;
	private Fluid body;
	private final FluidType accepter;
	private final Set<String> vary = new LinkedHashSet<String>();
	private Result<VersionedObject> result;
	private boolean closed;
	private String credential;

	public ResourceRequest(Request request, CalliRepository repository)
			throws QueryEvaluationException, RepositoryException {
		super(request);
		List<String> headers = getVaryHeaders("Accept");
		if (headers.isEmpty()) {
			accepter = new FluidType(HttpEntity.class);
		} else {
			StringBuilder sb = new StringBuilder();
			for (String hd : headers) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(hd);
			}
			accepter = new FluidType(HttpEntity.class, sb.toString().split("\\s*,\\s*"));
		}
		this.con = repository.getConnection();
		this.vf = con.getValueFactory();
		this.writer = ff.builder(con);
		String iri = getIRI();
		try {
			this.uri = vf.createURI(iri);
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
	}

	public String getCredential() {
		return credential;
	}

	public void setCredential(String cred) {
		this.credential = cred;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		InetAddress addr = getRemoteAddr();
		if (addr == null) {
			sb.append('-');
		} else {
			sb.append(addr.getHostAddress());
		}
		sb.append('\t');
		Object credential = getCredential();
		if (credential == null) {
			sb.append('-');
		} else {
			String relative = credential.toString();
			if (relative.startsWith(getScheme())) {
				String origin = getScheme() + "://" + getAuthority() + "/";
				if (relative.startsWith(origin)) {
					relative = relative.substring(origin.length() - 1);
				}
			}
			sb.append(relative);
		}
		sb.append('\t');
		sb.append('"').append(getRequestLine().toString()).append('"');
		return sb.toString();
	}

	public void begin() throws RepositoryException, QueryEvaluationException,
			DatatypeConfigurationException {
		if (target == null) {
			if (!this.isSafe()) {
				initiateActivity();
			}
			con.begin();
			result = con.getObjects(VersionedObject.class, uri);
			target = result.singleResult();
		}
	}

	public String getVaryHeader(String name) {
		if (!vary.contains(name)) {
			vary.add(name);
		}
		return getHeader(name);
	}

	public List<String> getVaryHeaders(String... name) {
		vary.addAll(Arrays.asList(name));
		return getHeaderValues(name);
	}

	public Collection<String> getVary() {
		return vary;
	}

	public URI createURI(String uriSpec) {
		return vf.createURI(resolve(uriSpec));
	}

	public void flush() throws RepositoryException, QueryEvaluationException,
			IOException {
		ObjectConnection con = getObjectConnection();
		this.target = con.getObject(VersionedObject.class, getRequestedResource().getResource());
	}

	public void rollback() throws RepositoryException {
		getObjectConnection().rollback();
	}

	public void commit() throws IOException, RepositoryException {
		getObjectConnection().commit();
	}

	/**
	 * Request has been fully read and response has been fully written.
	 */
	public void endExchange() {
		if (!closed) {
			closed = true;
			ObjectConnection con = getObjectConnection();
			try {
				con.rollback();
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.toString(), e);
			}
		}
		if (getEnclosingRequest() instanceof ResourceRequest) {
			((ResourceRequest) getEnclosingRequest()).endExchange();
		}
	}

	public Fluid getBody() {
		if (body != null)
			return body;
		String mediaType = getHeader("Content-Type");
		String location = getResolvedHeader("Content-Location");
		if (location == null) {
			location = getIRI();
		} else {
			location = createURI(location).stringValue();
		}
		FluidType ftype = new FluidType(HttpEntity.class, mediaType);
		return getFluidBuilder().consume(getEntity(), location, ftype);
	}

	public String getContentType(Method method) {
		Type genericType = method.getGenericReturnType();
		String[] mediaTypes = getTypes(method);
		return writer.nil(new FluidType(genericType, mediaTypes)).toMedia(accepter);
	}

	public String[] getTypes(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

	public ObjectConnection getObjectConnection() {
		return con;
	}

	public FluidBuilder getFluidBuilder() {
		return FluidFactory.getInstance().builder(con);
	}

	public String getOperation() {
		String qs = getQueryString();
		if (qs == null)
			return null;
		int a = qs.indexOf('&');
		int e = qs.indexOf('=');
		try {
			if (a < 0 && e < 0)
				return URLDecoder.decode(qs, "UTF-8");
			if (a > 0 && (a < e || e < 0))
				return URLDecoder.decode(qs.substring(0, a), "UTF-8");
			if (e > 0 && (e < a || a < 0))
				return URLDecoder.decode(qs.substring(0, e), "UTF-8");
		} catch (UnsupportedEncodingException exc) {
			throw new AssertionError(exc);
		}
		return "";
	}

	public Fluid getHeader(String... names) {
		List<String> list = getVaryHeaders(names);
		String[] values = list.toArray(new String[list.size()]);
		FluidType ftype = new FluidType(String[].class, "text/plain", "text/*");
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		return fb.consume(values, getIRI(), ftype);
	}

	public Fluid getQueryStringParameter() {
		String value = getQueryString();
		FluidType ftype = new FluidType(String.class, "application/x-www-form-urlencoded", "text/*");
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		return fb.consume(value, getIRI(), ftype);
	}

	public RDFObject getRequestedResource() {
		return target;
	}

	public long getLastModified() {
		if (target instanceof FileObject) {
			long lastModified = ((FileObject) target).getLastModified();
			if (lastModified > 0)
				return lastModified / 1000 * 1000;
		}
		try {
			Activity trans = target.getProvWasGeneratedBy();
			if (trans != null) {
				XMLGregorianCalendar xgc = trans.getProvEndedAtTime();
				if (xgc != null) {
					GregorianCalendar cal = xgc.toGregorianCalendar();
					cal.set(Calendar.MILLISECOND, 0);
					return cal.getTimeInMillis() / 1000 * 1000;
				}
			}
		} catch (ClassCastException e) {
			logger.warn(e.toString(), e);
		}
		return 0;
	}

	public String getContentVersion() {
		try {
			Activity activity = target.getProvWasGeneratedBy();
			if (activity == null)
				return null;
			String uri = ((RDFObject) activity).getResource().stringValue();
			int f = uri.indexOf('#');
			if (f >= 0) {
				uri = uri.substring(0, f);
			}
			String origin = this.getOrigin();
			if (uri.startsWith(origin) && '/' == uri.charAt(origin.length()))
				return uri.substring(origin.length());
			return uri;
		} catch (ClassCastException e) {
			return null;
		}
	}

	public FluidType getAcceptable() {
		return accepter;
	}

	public boolean isAcceptable(Type genericType, String... mediaType) {
		FluidType ftype = new FluidType(genericType, mediaType);
		return writer.isConsumable(ftype) && writer.nil(ftype).toMedia(accepter) != null;
	}

	public boolean isQueryStringPresent() {
		return getQueryString() != null;
	}

	private void initiateActivity() throws RepositoryException,
			DatatypeConfigurationException {
		AuditingRepositoryConnection audit = findAuditing(con);
		if (audit != null) {
			ActivityFactory delegate = audit.getActivityFactory();
			URI bundle = con.getVersionBundle();
			assert bundle != null;
			URI activity = delegate.createActivityURI(bundle, vf);
			con.setVersionBundle(bundle); // use the same URI for blob version
			audit.setActivityFactory(new RequestActivityFactory(activity, delegate, this));
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
