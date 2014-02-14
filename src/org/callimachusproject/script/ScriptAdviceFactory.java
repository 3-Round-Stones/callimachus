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
package org.callimachusproject.script;

import java.lang.reflect.Method;
import java.net.URL;

import org.callimachusproject.annotations.script;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class ScriptAdviceFactory implements AdviceFactory, AdviceProvider {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (script.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		EmbeddedScriptEngine engine = createEmbededScriptEngine(method);
		return new ScriptAdvice(engine, method);
	}

	private EmbeddedScriptEngine createEmbededScriptEngine(Method method) {
		ClassLoader cl = method.getDeclaringClass().getClassLoader();
		String[] script = method.getAnnotation(script.class).value();
		return EmbeddedScriptEngine.newInstance(cl, getSystemId(method), script);
	}

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

}
