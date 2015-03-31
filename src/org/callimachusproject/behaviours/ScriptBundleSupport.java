/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.http.util.EntityUtils;
import org.callimachusproject.concepts.ScriptBundle;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Sparql;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.client.HttpUriEntity;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.http.object.exceptions.InternalServerError;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

public abstract class ScriptBundleSupport implements ScriptBundle, CalliObject {
	/** very short lived cache to avoid race condition */
	private static final Map<String, Callable<String>> cache = new HashMap<String, Callable<String>>();

	@Override
	public String calliGetBundleSource() throws GatewayTimeout, IOException, OpenRDFException {
		HttpUriClient client = this.getHttpClient();
		List<SourceFile> scripts = new ArrayList<SourceFile>();
		for (Object ext : getCalliScriptsAsList()) {
			String url = ext.toString();
			String code = getJavaScriptCode(client, url);
			scripts.add(SourceFile.fromCode(url, code));
		}

		StringBuilder sb = new StringBuilder();
		for (SourceFile script : scripts) {
			sb.append(script.getCode()).append("\n");
		}
		return sb.toString();
	}

	@Override
	public String calliGetMinifiedBundle() throws Exception {
		final int minification = this.getMinification();
		if (minification < 1)
			return calliGetBundleSource();
		String uri = this.getResource().stringValue();
		final HttpUriClient client = this.getHttpClient();
		final List<String> scripts = new ArrayList<String>(getCalliScriptsAsList());
		Callable<String> future;
		synchronized (cache) {
			future = cache.get(uri);
			if (future == null) {
				cache.put(uri, future = new Callable<String>() {
					private String result;
					public synchronized String call() throws Exception {
						if (result == null)
							return result = compress(minification, scripts, client);
						return result;
					}
				});
			}
		}
		String source = future.call();
		synchronized (cache) {
			cache.remove(uri);
		}
		return "// @source: " + uri + "?source\n" + source;
	}

	@Sparql("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "SELECT DISTINCT (str(?script) AS ?url)\n"
			+ "WHERE { {$this ?one ?script FILTER (regex(str(?one), \"#_\\\\d$\"))}\n"
			+ "UNION {$this ?two ?script FILTER (regex(str(?two), \"#_\\\\d\\\\d$\"))}\n"
			+ "UNION {$this ?three ?script FILTER (regex(str(?three), \"#_\\\\d\\\\d\\\\d+$\"))}\n"
			+ "UNION {?member rdfs:member ?script FILTER (?member = $this)}\n"
			+ "} ORDER BY ?member ?three ?two ?one")
	protected abstract List<String> getCalliScriptsAsList();

	static String compress(int minification, List<String> links, HttpUriClient client)
			throws IOException {
		final List<SourceFile> scripts = new ArrayList<SourceFile>();
		for (String url : links) {
			String code = getJavaScriptCode(client, url);
			scripts.add(SourceFile.fromCode(url, code));
		}

		Compiler compiler = new Compiler();
		CompilerOptions options = new CompilerOptions();
		options.setLanguageIn(LanguageMode.ECMASCRIPT5);
		options.setLanguageOut(LanguageMode.ECMASCRIPT5);
		options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF);
		options.setWarningLevel(DiagnosticGroups.CHECK_USELESS_CODE, CheckLevel.OFF);
		getCompilationLevel(minification).setOptionsForCompilationLevel(options);

		List<SourceFile> externals = CommandLineRunner.getDefaultExterns();

		Result result = compiler.compile(externals, scripts, options);
		if (result.errors != null && result.errors.length > 0) {
			throw new InternalServerError(result.errors[0].toString());
		}
		return compiler.toSource();
	}

	private int getMinification() {
		int result = Integer.MAX_VALUE;
		for (Number number : getCalliMinified()) {
			if (number.intValue() < result) {
				result = number.intValue();
			}
		}
		if (result == Integer.MAX_VALUE)
			return 2;
		return result;
	}

	private static CompilationLevel getCompilationLevel(int minification) {
		if (minification == 1)
			return CompilationLevel.WHITESPACE_ONLY;
		if (minification == 2)
			return CompilationLevel.SIMPLE_OPTIMIZATIONS;
		return CompilationLevel.ADVANCED_OPTIMIZATIONS;
	}

	private static String getJavaScriptCode(HttpUriClient client, String url) throws IOException {
		HttpUriEntity entity = client.getEntity(url, "text/javascript");
		return EntityUtils.toString(entity, "UTF-8");
	}

}
