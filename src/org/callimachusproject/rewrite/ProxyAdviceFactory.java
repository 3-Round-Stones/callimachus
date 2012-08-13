package org.callimachusproject.rewrite;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.callimachusproject.annotations.get;
import org.callimachusproject.annotations.post;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class ProxyAdviceFactory implements AdviceProvider, AdviceFactory {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (get.class.equals(annotationType))
			return this;
		if (post.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		String m = getProxyMethod(method);
		String[] commands = getCommands(method);
		Substitution[] substitutions = createSubstitution(commands);
		String[] bindingNames = getBindingNames(method, substitutions);
		return new ProxyAdvice(bindingNames, substitutions, m, method);
	}

	private String[] getBindingNames(Method method, Substitution[] substitutions) {
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

	private String getProxyMethod(Method method) {
		if (method.isAnnotationPresent(get.class))
			return "GET";
		if (method.isAnnotationPresent(post.class))
			return "POST";
		throw new AssertionError();
	}

	private String[] getCommands(Method method) {
		if (method.isAnnotationPresent(get.class))
			return method.getAnnotation(get.class).value();
		if (method.isAnnotationPresent(post.class))
			return method.getAnnotation(post.class).value();
		throw new AssertionError();
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
