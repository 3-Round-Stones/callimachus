package org.callimachusproject.script;

import java.net.URL;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.apache.http.HttpResponse;

public class HttpResponseScriptBuilder {
	private static final String BUILD_HTTP_RESPONSE = "buildHttpResponse";
	private static final String SCRIPT = "function "
			+ BUILD_HTTP_RESPONSE
			+ "(resp, systemId){\n"
			+ "if (resp instanceof org.apache.http.HttpResponse) return resp;\n"
			+ "var contentType = null;\n"
			+ "var charset = 'UTF-8';\n"
			+ "var status = resp.status;\n"
			+ "var message = resp.message;\n"
			+ "if (typeof status != 'number') {\n"
			+ "	status = 200;\n"
			+ "}\n"
			+ "if (typeof message != 'string') {\n"
			+ "	message = '' + status;\n"
			+ "}\n"
			+ "var http11 = org.apache.http.HttpVersion.HTTP_1_1;\n"
			+ "var response = new org.apache.http.message.BasicHttpResponse(http11, status, message);\n"
			+ "if (typeof resp.headers == 'object') {\n"
			+ "	contentType = resp.headers['content-type'];\n"
			+ "	for (var name in resp.headers) {\n"
			+ "		var value = resp.headers[name];\n"
			+ "		if (typeof value == 'string') {\n"
			+ "			response.addHeader(name.toString(), value);\n"
			+ "		} else if (value && value.length && value.join) {\n"
			+ "			response.addHeader(name.toString(), value.join(','));\n"
			+ "		}\n"
			+ "	}\n"
			+ "}\n"
			+ "if (contentType && contentType.indexOf('charset=') > 0) {\n"
			+ "	charset = new javax.activation.MimeType(contentType).getParameter('charset');\n"
			+ "}\n"
			+ "var body = resp.entity || resp.body;\n"
			+ "if (typeof body == 'string') {"
			+ "	response.setEntity(new org.apache.http.entity.StringEntity(body, contentType, charset));\n"
			+ "} else if (body && body.length && body.join) {\n"
			+ "	response.setEntity(new org.apache.http.entity.StringEntity(body.join(''), contentType, charset));\n"
			+ "} else if (body && typeof body.getClass == 'function' && body.getClass() instanceof java.lang.Class) {\n"
			+ "	var factory = org.openrdf.http.object.fluid.FluidFactory.getInstance();\n"
			+ "	var media = contentType ? [contentType] : [];\n"
			+ "	var fluid = factory.builder().consume(body, systemId, body.getClass(), media);\n"
			+ "	response.setEntity(fluid.asHttpEntity(media));\n" + "}\n"
			+ "return response;\n" + "}\n";
	private final ObjectPool<Invocable> engines;
	final String systemId = getSystemId("SCRIPT");

	public HttpResponseScriptBuilder() throws ScriptException {
		final ScriptEngineManager man = new ScriptEngineManager();
		this.engines = new SoftReferenceObjectPool<Invocable>(new BasePoolableObjectFactory<Invocable>() {

			@Override
			public Invocable makeObject() throws ScriptException {
				ScriptEngine engine = man.getEngineByName("rhino");
				engine.put(ScriptEngine.FILENAME, systemId);
				engine.eval(SCRIPT);
				return (Invocable) engine;
			}
		});
	}

	public HttpResponse asHttpResponse(Object result)
			throws NoSuchMethodException, ScriptException {
		return asHttpResponse(result, systemId);
	}

	public HttpResponse asHttpResponse(Object result, String systemId)
			throws NoSuchMethodException, ScriptException {
		if (result == null)
			return null;
		Invocable engine = borrowObject();
		try {
			return (HttpResponse) engine.invokeFunction(BUILD_HTTP_RESPONSE,
					result, systemId);
		} finally {
			returnObject(engine);
		}
	}

	private Invocable borrowObject() throws ScriptException {
		try {
			return engines.borrowObject();
		} catch (ScriptException e) {
			throw e;
		} catch (Exception e) {
			throw new ScriptException(e);
		}
	}

	private void returnObject(Invocable engine) throws ScriptException {
		try {
			engines.returnObject(engine);
		} catch (ScriptException e) {
			throw e;
		} catch (Exception e) {
			throw new ScriptException(e);
		}
	}

	private String getSystemId(String frag) {
		Class<?> dclass = this.getClass();
		String name = dclass.getSimpleName() + ".class";
		URL url = dclass.getResource(name);
		if (url != null)
			return url.toExternalForm() + "#" + frag;
		return "java:" + dclass.getName() + "#" + frag;
	}

}
