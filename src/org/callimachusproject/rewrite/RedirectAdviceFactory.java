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
package org.callimachusproject.rewrite;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.annotations.alternate;
import org.callimachusproject.annotations.canonical;
import org.callimachusproject.annotations.describedby;
import org.callimachusproject.annotations.moved;
import org.callimachusproject.annotations.resides;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Type;
import org.openrdf.http.object.fluid.FluidType;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class RedirectAdviceFactory implements AdviceProvider, AdviceFactory {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (canonical.class.equals(annotationType))
			return this;
		if (alternate.class.equals(annotationType))
			return this;
		if (describedby.class.equals(annotationType))
			return this;
		if (resides.class.equals(annotationType))
			return this;
		if (moved.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		Substitution[] replacers = createSubstitution(getCommands(method));
		String[] bindingNames = getBindingNames(method, replacers);
		FluidType[] bindingTypes = getBindingTypes(method, bindingNames);
		StatusLine status = getStatusLine(method);
		return new RedirectAdvice(bindingNames, bindingTypes, replacers, status, method);
	}

	String[] getBindingNames(Method method, Substitution[] substitutions) {
		Annotation[][] anns = method.getParameterAnnotations();
		String[] bindingNames = new String[anns.length];
		for (int i = 0; i < bindingNames.length; i++) {
			for (Annotation ann : anns[i]) {
				if (Iri.class.equals(ann.annotationType())) {
					String local = local(((Iri) ann).value());
					for (Substitution substitution : substitutions) {
						if (substitution.containsVariableName(local)) {
							bindingNames[i] = local;
						}
					}
				}
			}
		}
		return bindingNames;
	}

	FluidType[] getBindingTypes(Method method, String[] bindingNames) {
		java.lang.reflect.Type[] types = method.getGenericParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		FluidType[] bindingTypes = new FluidType[anns.length];
		loop: for (int i = 0; i < bindingTypes.length; i++) {
			if (bindingNames[i] == null)
				continue;
			for (Annotation ann : anns[i]) {
				if (Type.class.equals(ann.annotationType())) {
					bindingTypes[i] = new FluidType(types[i], ((Type) ann).value());
					continue loop;
				}
			}
			bindingTypes[i] = new FluidType(types[i]);
		}
		return bindingTypes;
	}

	Substitution[] createSubstitution(String[] commands) {
		if (commands == null)
			return null;
		Substitution[] result = new Substitution[commands.length];
		for (int i=0; i<result.length; i++) {
			result[i] = Substitution.compile(commands[i]);
		}
		return result;
	}

	String local(String iri) {
		String string = iri;
		if (string.lastIndexOf('#') >= 0) {
			string = string.substring(string.lastIndexOf('#') + 1);
		}
		if (string.lastIndexOf('?') >= 0) {
			string = string.substring(string.lastIndexOf('?') + 1);
		}
		if (string.lastIndexOf('/') >= 0) {
			string = string.substring(string.lastIndexOf('/') + 1);
		}
		if (string.lastIndexOf(':') >= 0) {
			string = string.substring(string.lastIndexOf(':') + 1);
		}
		return string;
	}

	private String[] getCommands(Method method) {
		if (method.isAnnotationPresent(canonical.class))
			return method.getAnnotation(canonical.class).value();
		if (method.isAnnotationPresent(alternate.class))
			return method.getAnnotation(alternate.class).value();
		if (method.isAnnotationPresent(describedby.class))
			return method.getAnnotation(describedby.class).value();
		if (method.isAnnotationPresent(resides.class))
			return method.getAnnotation(resides.class).value();
		if (method.isAnnotationPresent(moved.class))
			return method.getAnnotation(moved.class).value();
		return null;
	}

	private StatusLine getStatusLine(Method method) {
		if (method.isAnnotationPresent(alternate.class))
			return new BasicStatusLine(HttpVersion.HTTP_1_1, 302, "Alternate");
		if (method.isAnnotationPresent(describedby.class))
			return new BasicStatusLine(HttpVersion.HTTP_1_1, 303, "Described By");
		if (method.isAnnotationPresent(resides.class))
			return new BasicStatusLine(HttpVersion.HTTP_1_1, 307, "Resides");
		if (method.isAnnotationPresent(moved.class))
			return new BasicStatusLine(HttpVersion.HTTP_1_1, 308, "Moved");
		throw new AssertionError();
	}

}
