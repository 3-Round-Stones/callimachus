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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.callimachusproject.server.client.ObjectResolver.ObjectFactory;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a CompiledScript that can execute named functions.
 *
 * @author James Leigh
 **/
public class FunctionScriptFactory implements ObjectFactory<CompiledScript> {
	private static final String BEHAVIOUR = BehaviourException.class.getName();
	private static final Pattern KEYWORDS = Pattern
			.compile("(?:var\\s+|\\.)(break|case|catch|const|continue|default|delete|do|else|export|finally|for|function|if|in|instanceof|import|name|new|return|switch|this|throw|try|typeof|var|void|while|with)\b");
	private final Logger logger = LoggerFactory
			.getLogger(FunctionScriptFactory.class);
	private final ClassLoader cl;

	public FunctionScriptFactory(ClassLoader cl) {
		this.cl = cl;
	}

	public CompiledScript create(String systemId, InputStream in)
			throws Exception {
		return create(systemId, new InputStreamReader(in, "UTF-8"));
	}

	public CompiledScript create(String systemId, Reader in) throws Exception {
		try {
			return createCompiledScript(cl, systemId, read(in));
		} finally {
			in.close();
		}
	}

	public String[] getContentTypes() {
		return new String[] { "text/javascript", "application/javascript",
				"text/ecmascript", "application/ecmascript" };
	}

	public boolean isReusable() {
		return true;
	}

	private String read(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		int read;
		char[] cbuf = new char[1024];
		while ((read = in.read(cbuf)) >= 0) {
			out.write(cbuf, 0, read);
		}
		out.write("\n");
		out.write("function ");
		out.write(getInvokeName());
		out.write("(msg, funcname) {\n\t");
		out.write("try {\n\t\t");
		out.write("return this[funcname].call(msg.msgTarget, msg);\n\t");
		out.write("} catch (e if e instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e);\n\t");
		out
				.write("} catch (e if e.javaException instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e.javaException);\n\t");
		out.write("}\n");
		out.write("}\n");
		return out.toString();
	}

	private String getInvokeName() {
		return "_invoke" + Math.abs(hashCode());
	}

	private CompiledScript createCompiledScript(ClassLoader cl,
			String systemId, String code) throws ObjectStoreConfigException,
			ScriptException {
		warnIfKeywordUsed(code);
		Thread current = Thread.currentThread();
		ClassLoader previously = current.getContextClassLoader();
		current.setContextClassLoader(cl);
		ScriptEngineManager man = new ScriptEngineManager();
		final ScriptEngine engine = man.getEngineByName("rhino");
		current.setContextClassLoader(previously);
		engine.put(ScriptEngine.FILENAME, systemId);
		engine.eval(code);
		return new CompiledScript() {
			public Object eval(ScriptContext context) throws ScriptException {
				Bindings bindings = context
						.getBindings(ScriptContext.ENGINE_SCOPE);
				Object msg = bindings.get("msg");
				String script = (String) bindings.get("script");
				String funcname = script.substring(script.indexOf('#') + 1);
				try {
					return ((Invocable) engine).invokeFunction(getInvokeName(),
							msg, funcname);
				} catch (NoSuchMethodException e) {
					throw new BehaviourException(e, script);
				}
			}

			public ScriptEngine getEngine() {
				return engine;
			}
		};
	}

	protected void warnIfKeywordUsed(String code) {
		Matcher m = KEYWORDS.matcher(code);
		if (m.find()) {
			logger.warn("{} is a ECMA script keyword", m.group(1));
		}
	}
}
