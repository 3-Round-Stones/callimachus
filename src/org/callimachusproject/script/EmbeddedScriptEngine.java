/*
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
package org.callimachusproject.script;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade to execute EMCAScript code from URL or code block.
 *
 * @author James Leigh
 **/
public class EmbeddedScriptEngine {
	private static final String[] RHINO_CONTEXT = {
			"sun.org.mozilla.javascript.Context",
			"sun.org.mozilla.javascript.internal.Context" };
	private static Class<?> Context;
	static {
		// load the script engine now, to import any binary libraries
		if (null == new ScriptEngineManager().getEngineByName("rhino"))
			throw new AssertionError("Rhino not available");
		for (String cn : RHINO_CONTEXT) {
			try {
				ClassLoader cl = EmbeddedScriptEngine.class.getClassLoader();
				Context = Class.forName(cn, false, cl);
				break;
			} catch (ClassNotFoundException exc) {
				continue;
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}
		if (Context == null)
			throw new AssertionError("Could not find rhino context");
	}
	private static HttpResponseScriptBuilder rb;
	static {
		try {
			rb = new HttpResponseScriptBuilder();
		} catch (ScriptException e) {
			throw new AssertionError(e);
		}
	}

	public static EmbeddedScriptEngine newInstance(ClassLoader cl,
			String systemId, String... code) {
		return new EmbeddedScriptEngine(cl, systemId, code);
	}

	public static class ScriptResult {
		private Object result;

		public ScriptResult(Object result) {
			this.result = result;
		}

		@Override
		public String toString() {
			return String.valueOf(result);
		}

		public Object asObject() {
			return result;
		}

		public void asVoid() {
			asObject();
		}

		public Void asVoidObject() {
			asObject();
			return null;
		}

		public Set asSet() {
			return (Set) asObject();
		}

		public boolean asBoolean() {
			return asBooleanObject().booleanValue();
		}

		public char asChar() {
			return asCharacterObject().charValue();
		}

		public byte asByte() {
			return asNumberObject().byteValue();
		}

		public short asShort() {
			return asNumberObject().shortValue();
		}

		public int asInt() {
			return asNumberObject().intValue();
		}

		public long asLong() {
			return asNumberObject().longValue();
		}

		public float asFloat() {
			return asNumberObject().floatValue();
		}

		public double asDouble() {
			return asNumberObject().doubleValue();
		}

		public Number asNumberObject() {
			return (Number) asObject();
		}

		public Boolean asBooleanObject() {
			return (Boolean) asObject();
		}

		public Byte asByteObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Byte(number.byteValue());
		}

		public Character asCharacterObject() {
			return (Character) asObject();
		}

		public Short asShortObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Short(number.shortValue());
		}

		public Integer asIntegerObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Integer(number.intValue());
		}

		public Long asLongObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Long(number.longValue());
		}

		public Float asFloatObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Float(number.floatValue());
		}

		public Double asDoubleObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Double(number.doubleValue());
		}

		public HttpResponse asHttpResponse() throws NoSuchMethodException,
				ScriptException {
			return rb.asHttpResponse(asObject());
		}
	}

	private final Logger logger = LoggerFactory.getLogger(EmbeddedScriptEngine.class);
	private final String[] scripts;
	private EmbeddedScript engine;
	private final String systemId;
	private final String filename;
	private final EmbeddedScriptFactory factory;
	private final EmbeddedScriptContext context;

	public EmbeddedScriptEngine(ClassLoader cl, String systemId, String... scripts) {
		assert cl != null;
		assert scripts != null && scripts.length > 0;
		assert systemId != null;
		this.systemId = systemId;
		this.filename = systemId;
		this.context = new EmbeddedScriptContext();
		this.factory = new EmbeddedScriptFactory(cl, context);
		this.scripts = scripts;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String src : scripts) {
			sb.append(src);
		}
		return sb.toString();
	}

	public EmbeddedScriptEngine importClass(String className) {
		context.importClass(className);
		return this;
	}

	public EmbeddedScriptEngine binding(String name) {
		context.addBindingName(name);
		return this;
	}

	public EmbeddedScriptEngine returnType(Class<?> returnType) {
		context.setReturnType(returnType);
		return this;
	}

	public ScriptResult eval(ObjectMessage msg, SimpleBindings bindings) {
		Class<?> context = enter();
		try {
			Object ret = getCompiledScript(msg.getTarget()).eval(msg, bindings);
			if (ret instanceof BehaviourException) {
				BehaviourException exc = (BehaviourException) ret;
				if (exc.getCause() instanceof RuntimeException)
					throw (RuntimeException) exc.getCause();
				if (exc.getCause() instanceof Error)
					throw (Error) exc.getCause();
				throw exc;
			} else if (ret instanceof ScriptException) {
				throw (ScriptException) ret;
			}
			return new ScriptResult(ret);
		} catch (NullPointerException e) {
			throw new BehaviourException(e, systemId);
		} catch (IllegalArgumentException e) {
			throw new BehaviourException(e, systemId);
		} catch (IndexOutOfBoundsException e) {
			throw new BehaviourException(e, systemId);
		} catch (NoSuchElementException e) {
			throw new BehaviourException(e, systemId);
		} catch (RuntimeException e) {
			throw e;
		} catch (Error e) {
			throw e;
		} catch (Exception e) {
			throw new BehaviourException(e, systemId);
		} finally {
			if (context != null) {
				exit(context);
			}
		}
	}

	private Class<?> enter() {
		if (Context != null) {
			try {
				Object ctx = Context.getMethod("enter").invoke(null);
				Object wf = ctx.getClass().getMethod("getWrapFactory").invoke(
						ctx);
				wf.getClass().getMethod("setJavaPrimitiveWrap", Boolean.TYPE)
						.invoke(wf, false);
				return Context;
			} catch (Exception e) {
				logger.warn(e.toString(), e);
			}
		}
		logger.warn("Could not find rhino context");
		return null;
	}

	private void exit(Class<?> Context) {
		try {
			Context.getMethod("exit").invoke(null);
		} catch (Exception e) {
			logger.warn(e.toString(), e);
		}
	}

	private synchronized EmbeddedScript getCompiledScript(Object target) throws IOException,
			ScriptException, OpenRDFException {
		if (engine != null)
			return engine;
		Reader in = getScriptReader(target);
		return engine = factory.create(filename, in);
	}

	private Reader getScriptReader(Object target) throws IOException, OpenRDFException {
		if (scripts.length == 1 && !isAbsoluteUri(scripts[0]))
			return new StringReader(scripts[0]);
		CharArrayWriter writer = new CharArrayWriter(65536);
		for (String src : scripts) {
			if (isAbsoluteUri(src)) {
				assert target instanceof CalliObject;
				readUrlInto(src, writer, ((CalliObject) target).getHttpClient());
			} else {
				writer.append(src);
			}
		}
		return new CharArrayReader(writer.toCharArray());
	}

	private boolean isAbsoluteUri(String uri) {
		if (uri.indexOf(' ') >= 0 || uri.indexOf('\n') >= 0)
			return false;
		try {
			return new URI(uri).isAbsolute();
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private void readUrlInto(String systemId, CharArrayWriter writer, HttpUriClient client)
			throws IOException, UnsupportedEncodingException {
		HttpEntity entity = client.getEntity(systemId,
				"text/javascript;charset=UTF-8, application/javascript;charset=UTF-8");
		if (entity != null) {
			InputStream in = entity.getContent();
			try {
				InputStreamReader reader = new InputStreamReader(in, "UTF-8");
				int read;
				char[] cbuf = new char[1024];
				while ((read = reader.read(cbuf)) >= 0) {
					writer.write(cbuf, 0, read);
				}
			} finally {
				in.close();
			}
		}
	}
}
