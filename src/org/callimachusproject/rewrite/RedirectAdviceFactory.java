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
		String[] bindingNames = getBindingNames(method);
		Substitution[] replacers = createSubstitution(getCommands(method));
		StatusLine status = getStatusLine(method);
		return new RedirectAdvice(bindingNames, replacers, status, method);
	}

	private String[] getBindingNames(Method method) {
		Annotation[][] anns = method.getParameterAnnotations();
		String[] bindingNames = new String[anns.length];
		for (int i = 0; i < bindingNames.length; i++) {
			for (Annotation ann : anns[i]) {
				if (Iri.class.equals(ann.annotationType())) {
					bindingNames[i] = local(((Iri) ann).value());
				}
			}
		}
		return bindingNames;
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

	private Substitution[] createSubstitution(String[] commands) {
		if (commands == null)
			return null;
		Substitution[] result = new Substitution[commands.length];
		for (int i=0; i<result.length; i++) {
			result[i] = Substitution.compile(commands[i]);
		}
		return result;
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

	private String local(String iri) {
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

}
