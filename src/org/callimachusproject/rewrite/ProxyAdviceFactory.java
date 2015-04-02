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

import java.lang.reflect.Method;

import org.callimachusproject.annotations.copy;
import org.callimachusproject.annotations.post;
import org.openrdf.http.object.fluid.FluidType;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class ProxyAdviceFactory extends RedirectAdviceFactory implements AdviceProvider, AdviceFactory {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (copy.class.equals(annotationType))
			return this;
		if (post.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		String[] templates = getTemplates(method);
		URITemplate[] substitutions = createSubstitution(templates);
		String[] bindingNames = getBindingNames(method, substitutions);
		FluidType[] bindingTypes = getBindingTypes(method, bindingNames);
		if (method.isAnnotationPresent(copy.class))
			return new ProxyGetAdvice(bindingNames, bindingTypes, substitutions, method);
		if (method.isAnnotationPresent(post.class))
			return new ProxyPostAdvice(bindingNames, bindingTypes, substitutions, method);
		throw new AssertionError();
	}

	private String[] getTemplates(Method method) {
		if (method.isAnnotationPresent(copy.class))
			return method.getAnnotation(copy.class).value();
		if (method.isAnnotationPresent(post.class))
			return method.getAnnotation(post.class).value();
		throw new AssertionError();
	}

}
