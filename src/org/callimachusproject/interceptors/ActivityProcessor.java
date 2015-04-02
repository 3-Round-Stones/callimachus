package org.callimachusproject.interceptors;

import static java.lang.Integer.toHexString;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.server.helpers.RequestActivityFactory;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.client.HttpUriResponse;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.http.object.helpers.ObjectContext;
import org.openrdf.http.object.helpers.ResourceTarget;
import org.openrdf.http.object.util.HTTPDateFormat;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityProcessor implements HttpRequestChainInterceptor {
	private final HTTPDateFormat format = new HTTPDateFormat();
	private final Logger logger = LoggerFactory.getLogger(ActivityProcessor.class);

	private ParserConfig parserConfig;

	public ActivityProcessor() {
		parserConfig = new ParserConfig();
		parserConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
	}

	@Override
	public HttpResponse intercept(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		try {
			ObjectContext ctx = ObjectContext.adapt(context);
			long now = ctx.getReceivedOn();
			ObjectConnection con = ctx.getResourceTarget().getTargetObject().getObjectConnection();
			initiateActivity(now, con, ctx);
			return null;
		} catch (RepositoryException e) {
			throw new InternalServerError(e);
		} catch (DatatypeConfigurationException e) {
			throw new InternalServerError(e);
		}
	}

	public void process(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		ObjectContext ctx = ObjectContext.adapt(context);
		HttpRequest oreq = ctx.getOriginalRequest();
		HttpRequest req = oreq == null ? request : oreq;
		ResourceTarget resource = ctx.getResourceTarget();
		RDFObject target = resource.getTargetObject();
		String version = getContentVersion((CalliObject) target);
		if (version != null && !response.containsHeader("Content-Version")) {
			response.setHeader("Content-Version", "\"" + version + "\"");
		}
		String entityTag = getEntityTag(req, response, resource, version);
		if (entityTag != null && !response.containsHeader("ETag")) {
			response.setHeader("ETag", entityTag);
		}
		long lastModified = getLastModified(req, response, (CalliObject) target);
		if (lastModified > 0) {
			response.setHeader("Last-Modified", format.format(lastModified));
		}
	}

	private void initiateActivity(long now, ObjectConnection con,
			HttpContext ctx) throws RepositoryException,
			DatatypeConfigurationException {
		con.setParserConfig(parserConfig);
		AuditingRepositoryConnection auditing = findAuditing(con);
		if (auditing != null) {
			ActivityFactory delegate = auditing.getActivityFactory();
			URI bundle = con.getVersionBundle();
			if (bundle == null) {
				bundle = con.getInsertContext();
				ActivityFactory activityFactory = auditing.getActivityFactory();
				if (bundle == null && activityFactory != null) {
					ValueFactory vf = con.getValueFactory();
					URI activityURI = activityFactory.createActivityURI(bundle, vf);
					String str = activityURI.stringValue();
					int h = str.indexOf('#');
					if (h > 0) {
						bundle = vf.createURI(str.substring(0, h));
					} else {
						bundle = activityURI;
					}
				}
				con.setVersionBundle(bundle); // use the same URI for blob version
			}
			URI activity = delegate.createActivityURI(bundle, con.getValueFactory());
			auditing.setActivityFactory(new RequestActivityFactory(activity, delegate, ctx, now));
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

	private String getContentVersion(CalliObject target) {
		try {
			Activity activity = target.getProvWasGeneratedBy();
			if (activity == null)
				return null;
			String uri = ((RDFObject) activity).getResource().stringValue();
			int f = uri.indexOf('#');
			if (f >= 0) {
				uri = uri.substring(0, f);
			}
			int p = uri.indexOf('/', 8);
			if (p < 0)
				return uri;
			return uri.substring(p, uri.length());
		} catch (ClassCastException e) {
			logger.warn(e.getMessage());
			return null;
		}
	}

	private String getEntityTag(HttpRequest request, HttpResponse response,
			ResourceTarget resource, String version) throws IOException,
			HttpException {
		int headers = getHeaderCodeFor(request, response);
		Header cache = response.getFirstHeader("Cache-Control");
		Header contentType = response.getFirstHeader("Content-Type");
		boolean strong = cache != null && cache.getValue().contains("cache-range");
		String method = request.getRequestLine().getMethod();
		if (contentType != null) {
			return variantTag(version, strong, contentType.getValue(), headers);
		} else if ("GET".equals(method) || "HEAD".equals(method)) {
			int code = response.getStatusLine().getStatusCode();
			Header loc = response.getFirstHeader("Location");
			String iri = resource.getTargetObject().getResource().stringValue();
			if ((code == 302 || code == 303) && loc != null && loc.getValue().startsWith(iri)) {
				HttpRequest redirect = new BasicHttpRequest("HEAD", loc.getValue());
				redirect.setHeaders(request.getAllHeaders());
				HttpUriResponse alt;
				if (resource.getHandlerMethod(redirect) != null) {
					alt = resource.invoke(redirect);
				} else {
					HttpRequest get = new BasicHttpRequest("GET", loc.getValue());
					get.setHeaders(request.getAllHeaders());
					alt = resource.head(get);
				}
				if (alt.getStatusLine().getStatusCode() >= 400)
					return null;
				Header ct = alt.getFirstHeader("Content-Type");
				String media = ct != null ? ct.getValue() : "application/octet-stream";
				return variantTag(version, strong, media, headers);
			}
		} else {
			Header putContentType = null;
			if ("PUT".equals(method)) {
				putContentType = request.getFirstHeader("Content-Type");
			}
			HttpUriResponse content = head(request, putContentType, resource);
			int getCode = content.getStatusLine().getStatusCode();
			int getHeaders = getCode < 400 ? getHeaderCodeFor(request, content) : 0;
			if (getCode >= 400 && putContentType == null) {
				return revisionTag(version, strong, getHeaders);
			} else if (getCode >= 400) {
				return variantTag(version, strong, putContentType.getValue(), getHeaders);
			} else {
				Header get_cache = content.getFirstHeader("Cache-Control");
				boolean get_strong = get_cache != null && get_cache.getValue().contains("cache-range");
				Header ct = content.getFirstHeader("Content-Type");
				String media = ct != null ? ct.getValue() : "application/octet-stream";
				return variantTag(version, get_strong, media, getHeaders);
			}
		}
		return null;
	}

	private HttpUriResponse head(HttpRequest request, Header contentType,
			ResourceTarget resource) throws IOException, HttpException {
		HttpRequest head = new BasicHttpRequest("HEAD", request.getRequestLine().getUri());
		HttpRequest get = new BasicHttpRequest("GET", head.getRequestLine().getUri());
		head.setHeaders(request.getHeaders("Host"));
		get.setHeaders(request.getHeaders("Host"));
		if (contentType != null) {
			get.setHeader("Accept", contentType.getValue());
			if (resource.getHandlerMethod(get) == null) {
				head.setHeader("Accept", "*/*");
				get.setHeader("Accept", "*/*");
			} else {
				head.setHeader("Accept", contentType.getValue());
			}
		}
		HttpUriResponse content = resource.head(get);
		if (resource.getHandlerMethod(head) != null) {
			HttpUriResponse result = resource.invoke(head);
			for (Header hd : content.getAllHeaders()) {
				if (!result.containsHeader(hd.getName())) {
					for (Header h : content.getHeaders(hd.getName())) {
						result.addHeader(h);
					}
				}
			}
			content = result;
		}
		return content;
	}

	private int getHeaderCodeFor(HttpRequest request, HttpResponse response) {
		Map<String, String> headers = new HashMap<String, String>();
		for (Header vary : response.getHeaders("Vary")) {
			for (String name : vary.getValue().split(",")) {
				for (Header hd : request.getHeaders(name.trim())) {
					if (headers.containsKey(name)) {
						headers.put(name, headers.get(name) + "," + hd.getValue());
					} else {
						headers.put(name, hd.getValue());
					}
				}
			}
		}
		return headers.hashCode();
	}

	private String variantTag(String version, boolean strong, String mediaType, int code) {
		if (mediaType == null)
			return revisionTag(version, strong, code);
		if (version == null)
			return null;
		String revision = toHexString(version.hashCode());
		String weak = strong ? "" : "W/";
		String cd = toHexString(code);
		String v = toHexString(mediaType.hashCode());
		if (code == 0)
			return weak + '"' + revision + '-' + v + '"';
		return weak + '"' + revision + '-' + cd + '-' + v + '"';
	}

	private String revisionTag(String version, boolean strong, int code) {
		if (version == null)
			return null;
		String revision = toHexString(version.hashCode());
		String weak = strong ? "" : "W/";
		if (code == 0)
			return weak + '"' + revision + '"';
		return weak + '"' + revision + '-' + toHexString(code) + '"';
	}

	private long getLastModified(HttpRequest request, HttpResponse response, CalliObject target) {
		if (isNoValidate(request, response))
			return System.currentTimeMillis() / 1000 * 1000;
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
			logger.warn(e.getMessage());
		}
		return 0;
	}

	private boolean isNoValidate(HttpRequest request, HttpResponse response) {
		String method = request.getRequestLine().getMethod();
		if (!"PUT".equals(method) && !"DELETE".equals(method)
				&& !"OPTIONS".equals(method)) {
			for (Header hd : response.getHeaders("Cache-Control")) {
				if (hd.getValue().contains("must-reevaluate"))
					return true;
				if (hd.getValue().contains("no-validate"))
					return true;
			}
		}
		return false;
	}
}
