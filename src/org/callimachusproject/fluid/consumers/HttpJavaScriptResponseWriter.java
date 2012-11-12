package org.callimachusproject.fluid.consumers;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.http.HttpResponse;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Vapor;

public class HttpJavaScriptResponseWriter implements Consumer<Object> {
	private static final String BUILD_HTTP_RESPONSE = "buildHttpResponse";
	private static final String SCRIPT = "function "+BUILD_HTTP_RESPONSE+"(resp, systemId){\n" +
			"var contentType = null;\n" +
			"var charset = 'UTF-8';\n" +
			"var status = resp.status;\n" +
			"var message = resp.message;" +
			"if (typeof status != 'number') {\n" +
			"	status = 200;\n" +
			"}\n" +
			"if (typeof message != 'string') {\n" +
			"	message = '' + status;\n" +
			"}\n" +
			"var http11 = org.apache.http.HttpVersion.HTTP_1_1;\n" +
			"var response = new org.apache.http.message.BasicHttpResponse(http11, status, message);\n" +
			"if (typeof resp.headers == 'object') {\n" +
			"	contentType = resp.headers['content-type'];\n" +
			"	for (name in resp.headers) {\n" +
			"		var value = resp.headers[name];\n" +
			"		if (typeof value == 'string') {\n" +
			"			response.addHeader(name.toString(), value);" +
			"		} else if (value && value.length && value.join) {\n" +
			"			response.addHeader(name.toString(), value.join(','));" +
			"		}\n" +
			"	}\n" +
			"}\n" +
			"if (contentType && contentType.indexOf('charset=') > 0) {\n" +
			"	charset = new javax.activation.MimeType(contentType).getParameter('charset');\n" +
			"}\n" +
			"if (typeof resp.body == 'string') {" +
			"	response.setEntity(new org.apache.http.entity.StringEntity(resp.body, contentType, charset));\n" +
			"} else if (resp.body && resp.body.length && resp.body.join) {\n" +
			"	response.setEntity(new org.apache.http.entity.StringEntity(resp.body.join(''), contentType, charset));\n" +
			"} else if (resp.body && typeof resp.body.getClass == 'function' && resp.body.getClass() instanceof java.lang.Class) {\n" +
			"	var factory = org.callimachusproject.fluid.FluidFactory.getInstance();\n" +
			"	var media = contentType ? [contentType] : [];\n" +
			"	var fluid = factory.builder().consume(resp.body, systemId, resp.body.getClass(), media);\n" +
            "	response.setEntity(fluid.asHttpEntity(media));\n" +
			"}\n" +
			"return response;\n" +
			"}\n";
	private final Invocable engine;
	private final HttpMessageWriter delegate;

	public HttpJavaScriptResponseWriter() throws ScriptException {
		String systemId = getSystemId("SCRIPT");
		ScriptEngineManager man = new ScriptEngineManager();
		ScriptEngine engine = man.getEngineByName("rhino");
		engine.put(ScriptEngine.FILENAME, systemId);
		engine.eval(SCRIPT);
		this.engine = (Invocable) engine;
		this.delegate = new HttpMessageWriter();
	}

	@Override
	public boolean isConsumable(FluidType ftype, FluidBuilder builder) {
		return ftype.is(Object.class) && (ftype.is("message/http") || ftype.is("message/x-response"));
	}

	@Override
	public Fluid consume(final Object result, final String base,
			final FluidType ftype, final FluidBuilder builder) {
		return new Vapor() {

			@Override
			public String getSystemId() {
				return base;
			}

			@Override
			public FluidType getFluidType() {
				return ftype;
			}

			@Override
			public void asVoid() throws IOException, FluidException {
				// do nothing
			}

			@Override
			protected String toHttpResponseMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected HttpResponse asHttpResponse(FluidType media)
					throws Exception {
				return (HttpResponse) engine.invokeFunction(
						BUILD_HTTP_RESPONSE, result, base);
			}

			@Override
			protected String toChannelMedia(FluidType media) {
				return toHttpResponseMedia(media);
			}

			@Override
			protected ReadableByteChannel asChannel(FluidType media)
					throws Exception {
				return delegate.consume(asHttpResponse(media), base, ftype,
						builder).asChannel(media.media());
			}
		};
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
