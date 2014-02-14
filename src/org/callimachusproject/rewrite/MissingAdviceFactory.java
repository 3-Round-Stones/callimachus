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

import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.annotations.deleted;
import org.callimachusproject.annotations.disabled;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class MissingAdviceFactory implements AdviceProvider, AdviceFactory {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (disabled.class.equals(annotationType))
			return this;
		if (deleted.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		StatusLine status = getStatusLine(method);
		return new MissingAdvice(status, method);
	}

	private StatusLine getStatusLine(Method method) {
		if (method.isAnnotationPresent(disabled.class)) {
			String[] phrase = method.getAnnotation(disabled.class).value();
			if (phrase.length < 1) {
				phrase = new String[] { "Disabled" };
			}
			return new BasicStatusLine(HttpVersion.HTTP_1_1, 404, phrase[0]);
		}
		if (method.isAnnotationPresent(deleted.class)) {
			String[] phrase = method.getAnnotation(deleted.class).value();
			if (phrase.length < 1) {
				phrase = new String[] { "Deleted" };
			}
			return new BasicStatusLine(HttpVersion.HTTP_1_1, 410, phrase[0]);
		}
		throw new AssertionError();
	}

}
