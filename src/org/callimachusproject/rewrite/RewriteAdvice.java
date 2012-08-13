package org.callimachusproject.rewrite;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.callimachusproject.annotations.type;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;

public abstract class RewriteAdvice implements Advice {
	private final Substitution[] replacers;
	private final String[] bindingNames;
	private final Type returnType;
	private final String[] returnMedia;
	private final TermFactory systemId;

	public RewriteAdvice(String[] bindingNames, Substitution[] replacers,
			Method method) {
		this.replacers = replacers;
		this.bindingNames = bindingNames;
		this.returnType = method.getGenericReturnType();
		this.returnMedia = getMediaType(method);
		this.systemId = TermFactory.newInstance(getSystemId(method));
	}

	public Object intercept(ObjectMessage message) throws GatewayTimeout,
			IOException, FluidException {
		Object target = message.getTarget();
		String uri = target.toString();
		Object[] parameters = message.getParameters();
		String location = resolve(substitute(uri, getVariables(parameters)));
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
		}
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		return service(location, parameters, fb).as(returnType, returnMedia);
	}

	protected String getSystemId() {
		return systemId.getSystemId();
	}

	protected String[] getReturnMedia() {
		return returnMedia;
	}

	protected abstract Fluid service(String location, Object[] parameters,
			FluidBuilder fb) throws GatewayTimeout, IOException, FluidException;

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
		return null;
	}

	private String resolve(String path) {
		if (path == null)
			return null;
		return systemId.reference(path).stringValue();
	}

}
