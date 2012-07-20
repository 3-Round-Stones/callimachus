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
import java.net.URLDecoder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.tools.FileObject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.annotations.expect;
import org.callimachusproject.annotations.type;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.util.Accepter;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.MessageType;
import org.callimachusproject.server.writers.AggregateWriter;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.auditing.ActivityFactory;
import org.openrdf.repository.auditing.AuditingRepositoryConnection;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
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
	private static final Pattern EXPECT_REDIRECT = Pattern.compile("^(301|302|303|307)\\b");
	private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
	private static Type parameterMapType;
	static {
		try {
			parameterMapType = ResourceRequest.class.getDeclaredMethod(
					"getParameterMap").getGenericReturnType();
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}
	private final Logger logger = LoggerFactory.getLogger(ResourceRequest.class);
	private ValueFactory vf;
	private ObjectConnection con;
	private VersionedObject target;
	private URI uri;
	private AggregateWriter writer = AggregateWriter.getInstance();
	private BodyEntity body;
	private Accepter accepter;
	private Set<String> vary = new LinkedHashSet<String>();
	private Result<VersionedObject> result;
	private boolean closed;

	public ResourceRequest(Request request, CallimachusRepository repository)
			throws QueryEvaluationException, RepositoryException,
			MimeTypeParseException {
		super(request);
		List<String> headers = getVaryHeaders("Accept");
		if (headers.isEmpty()) {
			accepter = new Accepter();
		} else {
			StringBuilder sb = new StringBuilder();
			for (String hd : headers) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(hd);
			}
			accepter = new Accepter(sb.toString());
		}
		this.con = repository.getConnection();
		this.vf = con.getValueFactory();
		String iri = getIRI();
		try {
			this.uri = vf.createURI(iri);
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
	}

	public void begin() throws RepositoryException, QueryEvaluationException,
			MimeTypeParseException, DatatypeConfigurationException {
		if (target == null) {
			if (!this.isSafe()) {
				initiateActivity();
			}
			con.setAutoCommit(false); // begin()
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

	public ResponseEntity createResultEntity(Object result, Class<?> ctype,
			Type gtype, String[] mimeTypes) {
		if (result instanceof RDFObjectBehaviour) {
			result = ((RDFObjectBehaviour) result).getBehaviourDelegate();
		}
		return new ResponseEntity(mimeTypes, result, ctype, gtype, uri
				.stringValue(), con);
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
		ObjectConnection con = getObjectConnection();
		con.rollback();
		con.setAutoCommit(true); // rollback()
	}

	public void commit() throws IOException, RepositoryException {
		try {
			ObjectConnection con = getObjectConnection();
			con.setAutoCommit(true); // commit()
		} catch (RepositoryException e) {
			rollback();
		}
	}

	public void cleanup() throws RepositoryException, IOException {
		if (!closed) {
			closed = true;
			ObjectConnection con = getObjectConnection();
			con.rollback();
			con.close();
		}
		super.cleanup();
	}

	public BodyEntity getBody() {
		if (body != null)
			return body;
		String mediaType = getHeader("Content-Type");
		String location = getResolvedHeader("Content-Location");
		if (location != null) {
			location = createURI(location).stringValue();
		}
		try {
			Charset charset = getCharset(mediaType);
			return body = new BodyEntity(mediaType, isMessageBody(), charset,
					uri.stringValue(), location, con) {

				@Override
				public void close() throws IOException {
					super.close();
					EntityUtils.consume(getEntity());
				}

				@Override
				protected ReadableByteChannel getReadableByteChannel()
						throws IOException {
					HttpEntity entity = getEntity();
					if (entity == null)
						return null;
					return ChannelUtil.newChannel(entity.getContent());
				}
			};
		} catch (MimeTypeParseException e) {
			throw new BadRequest("Invalid mime type: " + mediaType);
		}
	}

	public String getContentType(Method method) throws MimeTypeParseException {
		Class<?> type = method.getReturnType();
		Type genericType = method.getGenericReturnType();
		if (method.isAnnotationPresent(type.class)) {
			String[] mediaTypes = method.getAnnotation(type.class).value();
			for (MimeType m : accepter.getAcceptable(mediaTypes)) {
				if (writer.isWriteable(new MessageType(m.toString(), type, genericType, con))) {
					return getContentType(type, genericType, m);
				}
			}
		} else {
			if (method.isAnnotationPresent(expect.class)) {
				for (String expect : method.getAnnotation(expect.class).value()) {
					if (EXPECT_REDIRECT.matcher(expect).find()) {
						return "text/uri-list";
					}
				}
			}
			for (MimeType m : accepter.getAcceptable()) {
				if (writer.isWriteable(new MessageType(m.toString(), type, genericType, con))) {
					return getContentType(type, genericType, m);
				}
			}
		}
		return null;
	}

	public ObjectConnection getObjectConnection() {
		return con;
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

	public Entity getHeader(String[] mediaTypes, String... names) {
		List<String> list = getVaryHeaders(names);
		String[] values = list.toArray(new String[list.size()]);
		return new ParameterEntity(mediaTypes, "text/plain", values, uri
				.stringValue(), con);
	}

	public Entity getParameter(String[] mediaTypes, String... names) {
		String[] values = getParameterValues(names);
		return new ParameterEntity(mediaTypes, "text/plain", values, uri
				.stringValue(), con);
	}

	public Entity getHeaderAndQuery(String[] mediaTypes, String[] headers,
			String[] queries) {
		String[] qvalues = getParameterValues(queries);
		if (qvalues == null)
			return getHeader(mediaTypes, headers);
		List<String> hvalues = getVaryHeaders(headers);
		int size = qvalues.length + hvalues.size();
		List<String> list = new ArrayList<String>(size);
		if (qvalues.length > 0) {
			list.addAll(Arrays.asList(qvalues));
		}
		list.addAll(hvalues);
		String[] values = list.toArray(new String[list.size()]);
		return new ParameterEntity(mediaTypes, "text/plain", values, uri
				.stringValue(), con);
	}

	public Entity getQueryString(String[] mediaTypes) {
		String mimeType = "application/x-www-form-urlencoded";
		String value = getQueryString();
		if (value == null) {
			return new ParameterEntity(mediaTypes, mimeType, new String[0], uri
					.stringValue(), con);
		}
		return new ParameterEntity(mediaTypes, mimeType,
				new String[] { value }, uri.stringValue(), con);
	}

	public RDFObject getRequestedResource() {
		return target;
	}

	public long getLastModified() throws MimeTypeParseException {
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

	public String revision() {
		return target.revision();
	}

	public SortedSet<? extends MimeType> getAcceptable()
			throws MimeTypeParseException {
		return accepter.getAcceptable();
	}

	public boolean isAcceptable(Class<?> type, Type genericType)
			throws MimeTypeParseException {
		return isAcceptable(null, type, genericType);
	}

	public boolean isAcceptable(String mediaType) throws MimeTypeParseException {
		return isAcceptable(mediaType, null, null);
	}

	public boolean isAcceptable(String mediaType, Class<?> type,
			Type genericType) throws MimeTypeParseException {
		if (type == null)
			return accepter.isAcceptable(mediaType);
		for (MimeType accept : accepter.getAcceptable(mediaType)) {
			if (writer.isWriteable(new MessageType(accept.toString(), type, genericType, con)))
				return true;
		}
		return false;
	}

	public boolean isQueryStringPresent() {
		return getQueryString() != null;
	}

	private Charset getCharset(String mediaType) throws MimeTypeParseException {
		if (mediaType == null)
			return null;
		MimeType m = new MimeType(mediaType);
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}

	private String getContentType(Class<?> type, Type genericType, MimeType m) {
		m.removeParameter("q");
		if (writer.isText(new MessageType(m.toString(), type, genericType, con))) {
			Charset charset = null;
			String cname = m.getParameters().get("charset");
			try {
				if (cname != null) {
					charset = Charset.forName(cname);
					return writer.getContentType(new MessageType(m.toString(), type,
							genericType, con), charset);
				}
			} catch (UnsupportedCharsetException e) {
				// ignore
			}
			if (charset == null) {
				charset = getPreferredCharset();
			}
			return writer.getContentType(new MessageType(m.toString(), type,
					genericType, con), charset);
		} else {
			return writer.getContentType(new MessageType(m.toString(), type,
					genericType, con), null);
		}
	}

	private Charset getPreferredCharset() {
		Charset charset = null;
		int rating = 0;
		for (String value : getVaryHeaders("Accept-Charset")) {
			String header = value.replaceAll("\\s", "");
			for (String item : header.split(",")) {
				int q = 1;
				String name = item;
				int c = item.indexOf(';');
				if (c > 0) {
					name = item.substring(0, c);
					if ("*".equals(name))
						return DEFAULT_CHARSET;
					q = getQuality(item);
				}
				if (q > rating) {
					try {
						charset = Charset.forName(name);
						rating = q;
						if (DEFAULT_CHARSET.equals(charset))
							return DEFAULT_CHARSET;
					} catch (UnsupportedCharsetException e) {
						// ignore
					}
				} else if (name.equalsIgnoreCase(DEFAULT_CHARSET.name())) {
					return DEFAULT_CHARSET;
				}
			}
		}
		return charset;
	}

	private int getQuality(String item) {
		int s = item.indexOf(";q=");
		if (s > 0) {
			int e = item.indexOf(';', s + 1);
			if (e < 0) {
				e = item.length();
			}
			try {
				return Integer.parseInt(item.substring(s + 3, e));
			} catch (NumberFormatException exc) {
				// ignore q
			}
		}
		return 1;
	}

	private Map<String, String[]> getParameterMap() {
		try {
			return getQueryString(null).read(Map.class, parameterMapType,
					new String[] { "application/x-www-form-urlencoded" });
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	private String[] getParameterValues(String... names) {
		if (names.length == 0) {
			return new String[0];
		} else {
			Map<String, String[]> map = getParameterMap();
			if (map == null) {
				return null;
			} else if (names.length == 1) {
				return map.get(names[0]);
			} else {
				List<String> list = new ArrayList<String>(names.length * 2);
				for (String name : names) {
					list.addAll(Arrays.asList(map.get(name)));
				}
				return list.toArray(new String[list.size()]);
			}
		}
	}

	private void initiateActivity() throws RepositoryException,
			DatatypeConfigurationException {
		AuditingRepositoryConnection audit = findAuditing(con);
		if (audit != null) {
			final ActivityFactory delegate = audit.getActivityFactory();
			final URI activity = delegate.createActivityURI(audit.getValueFactory());
			con.setActivityURI(activity); // use the same URI for blob version
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
