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
