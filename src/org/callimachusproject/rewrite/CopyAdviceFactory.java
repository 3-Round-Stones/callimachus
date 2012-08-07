package org.callimachusproject.rewrite;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.callimachusproject.annotations.copies;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class CopyAdviceFactory implements AdviceProvider, AdviceFactory {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (copies.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		String[] bindingNames = getBindingNames(method);
		Substitution[] replacers = createSubstitution(getCommands(method));
		return new CopyAdvice(bindingNames, replacers, method);
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
		return method.getAnnotation(copies.class).value();
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
