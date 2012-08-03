package org.callimachusproject.rewrite;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.callimachusproject.annotations.flag;
import org.callimachusproject.annotations.location;
import org.callimachusproject.annotations.pattern;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class RewriteAdviceFactory implements AdviceProvider, AdviceFactory {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (location.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		String[] patterns = getPatterns(method);
		String[] bindingNames = getBindingNames(method);
		String location = getLocation(method);
		String[] flags = getFlags(method);
		return new RewriteAdvice(patterns, bindingNames, location, flags);
	}

	private String[] getPatterns(Method method) {
		pattern ann = method.getAnnotation(pattern.class);
		if (ann == null)
			return new String[0];
		return ann.value();
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

	private String getLocation(Method method) {
		return method.getAnnotation(location.class).value();
	}

	private String[] getFlags(Method method) {
		flag ann = method.getAnnotation(flag.class);
		if (ann == null)
			return new String[0];
		return ann.value();
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
