package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.callimachusproject.annotations.type;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;

public abstract class RewriteAdvice implements Advice {
	private final Substitution[] replacers;
	private final String[] bindingNames;
	private final FluidType[] bindingTypes;
	private final Type returnType;
	private final String[] returnMedia;
	private final TermFactory systemId;

	public RewriteAdvice(String[] bindingNames, FluidType[] bindingTypes,
			Substitution[] replacers, Method method) {
		this.replacers = replacers;
		this.bindingNames = bindingNames;
		this.bindingTypes = bindingTypes;
		this.returnType = method.getGenericReturnType();
		this.returnMedia = getMediaType(method);
		this.systemId = TermFactory.newInstance(getSystemId(method));
	}

	public Object intercept(ObjectMessage message) throws GatewayTimeout,
			IOException, FluidException, OpenRDFException {
		Object target = message.getTarget();
		String uri;
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
			uri = ((RDFObject) target).getResource().stringValue();
		} else {
			uri = target.toString();
		}
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		Object[] parameters = message.getParameters();
		String substitute = substitute(uri, getVariables(parameters, uri, fb));
		String[] lines = substitute.split("\\s*\n\\s*");
		String location = resolve(lines[0]);
		Header[] headers = readHeaders(lines);
		return service(location, headers, message, fb).as(returnType,
				returnMedia);
	}

	protected String getSystemId() {
		return systemId.getSystemId();
	}

	protected String[] getReturnMedia() {
		return returnMedia;
	}

	protected abstract Fluid service(String location, Header[] headers,
			ObjectMessage message, FluidBuilder fb) throws GatewayTimeout,
			IOException, FluidException, ResponseException, OpenRDFException;

	private String getSystemId(Method m) {
		if (m.isAnnotationPresent(Iri.class))
			return m.getAnnotation(Iri.class).value();
		Class<?> dclass = m.getDeclaringClass();
		String mame = m.getName();
		if (dclass.isAnnotationPresent(Iri.class)) {
			String url = dclass.getAnnotation(Iri.class).value();
			if (url.indexOf('#') >= 0)
				return url.substring(0, url.indexOf('#') + 1) + mame;
			return url + "#" + mame;
		}
		String name = dclass.getSimpleName() + ".class";
		URL url = dclass.getResource(name);
		if (url != null)
			return url.toExternalForm() + "#" + mame;
		return "java:" + dclass.getName() + "#" + mame;
	}

	private String[] getMediaType(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

	private Map<String, String> getVariables(Object[] parameters, String uri,
			FluidBuilder fb) throws IOException, FluidException {
		if (bindingNames == null || bindingNames.length == 0)
			return Collections.emptyMap();
		Map<String, String> map = new HashMap<String, String>(
				bindingNames.length);
		for (int i = 0; i < bindingNames.length; i++) {
			String key = bindingNames[i];
			if (key != null) {
				FluidType type = bindingTypes[i];
				Object param = parameters[i];
				if (param != null) {
					map.put(key, asString(param, type, uri, fb));
				}
			}
		}
		return map;
	}

	private String asString(Object param, FluidType type, String uri,
			FluidBuilder fb) throws IOException, FluidException {
		return (String) fb.consume(param, uri, type).as(String.class);
	}

	private String substitute(String uri, Map<String, String> variables) {
		if (replacers == null || replacers.length <= 0)
			return null;
		for (Substitution pattern : replacers) {
			String result = pattern.replace(uri, variables);
			if (result != null)
				return result;
		}
		return null;
	}

	private String resolve(String path) throws UnsupportedEncodingException {
		if (path == null)
			return null;
		String uri = PercentCodec.encodeOthers(path, PercentCodec.ALLOWED);
		return systemId.resolve(uri);
	}

	private Header[] readHeaders(String[] lines) {
		List<Header> headers = new ArrayList<Header>();
		for (int i = 1; i < lines.length; i++) {
			int colon = lines[i].indexOf(':');
			if (colon > 0) {
				String name = lines[i].substring(0, colon).trim();
				String value = lines[i].substring(colon + 1).trim();
				headers.add(new BasicHeader(name, value));
			}
		}
		return headers.toArray(new Header[headers.size()]);
	}

}
