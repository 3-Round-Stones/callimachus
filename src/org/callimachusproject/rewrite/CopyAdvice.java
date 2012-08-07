package org.callimachusproject.rewrite;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.annotations.type;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.xml.sax.SAXException;

public class CopyAdvice implements Advice {
	private final Substitution[] replacers;
	private final String[] bindingNames;
	private final Type returnType;
	private final String[] returnMedia;

	public CopyAdvice(String[] bindingNames, Substitution[] replacers,
			Method method) {
		this.replacers = replacers;
		this.bindingNames = bindingNames;
		this.returnType = method.getGenericReturnType();
		this.returnMedia = getMediaType(method);
	}

	public Object intercept(ObjectMessage message) throws Exception {
		Object target = message.getTarget();
		String uri = target.toString();
		Object[] parameters = message.getParameters();
		String path = substitute(uri, getVariables(parameters));
		String location = resolve(uri, path);
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
		}
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		return service(location, fb).as(returnType, returnMedia);
	}

	protected Fluid service(String location, FluidBuilder fb)
			throws GatewayTimeout, IOException,
			TransformerConfigurationException, OpenRDFException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerException {
		if (location == null)
			return fb.nil(new FluidType(returnType, returnMedia));
		String systemId = location;
		String redirect = systemId;
		HttpResponse resp = null;
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		for (int i = 0; i < 20 && redirect != null; i++) {
			systemId = redirect;
			HttpRequest req = new BasicHttpRequest("GET", redirect);
			if (returnMedia.length > 0) {
				for (String media : returnMedia) {
					req.addHeader("Accept", media);
				}
			}
			resp = client.service(req);
			redirect = client.redirectLocation(redirect, resp);
		}
		String contentType = "*/*";
		InputStream content = null;
		if (resp.getEntity() != null) {
			content = resp.getEntity().getContent();
		}
		if (resp.getFirstHeader("Content-Type") != null) {
			contentType = resp.getFirstHeader("Content-Type").getValue();
		}
		return fb.stream(content, systemId, contentType);
	}

	private String[] getMediaType(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

	private Map<String, String> getVariables(Object[] parameters) {
		if (bindingNames == null || bindingNames.length == 0)
			return Collections.emptyMap();
		Map<String, String> map = new HashMap<String, String>(
				bindingNames.length);
		for (int i = 0; i < bindingNames.length; i++) {
			String key = bindingNames[i];
			if (key != null) {
				Object param = parameters[i];
				if (param != null) {
					map.put(key, param.toString());
				}
			}
		}
		return map;
	}

	private String substitute(String uri, Map<String, String> variables) {
		if (replacers == null || replacers.length <= 0)
			return null;
		for (Substitution pattern : replacers) {
			String result = pattern.replace(uri, variables);
			if (result != null)
				return result;
		}
		String path = new ParsedURI(uri).getPath();
		for (Substitution pattern : replacers) {
			String result = pattern.replace(path, variables);
			if (result != null)
				return result;
		}
		return null;
	}

	private String resolve(String base, String path) {
		if (path == null)
			return null;
		return TermFactory.newInstance(base).reference(path).stringValue();
	}

}
