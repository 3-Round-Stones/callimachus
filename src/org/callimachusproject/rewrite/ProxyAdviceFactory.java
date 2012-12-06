package org.callimachusproject.rewrite;

import java.lang.reflect.Method;

import org.callimachusproject.annotations.copy;
import org.callimachusproject.annotations.post;
import org.callimachusproject.fluid.FluidType;
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
		String[] commands = getCommands(method);
		Substitution[] substitutions = createSubstitution(commands);
		String[] bindingNames = getBindingNames(method, substitutions);
		FluidType[] bindingTypes = getBindingTypes(method, bindingNames);
		if (method.isAnnotationPresent(copy.class))
			return new ProxyGetAdvice(bindingNames, bindingTypes, substitutions, method);
		if (method.isAnnotationPresent(post.class))
			return new ProxyPostAdvice(bindingNames, bindingTypes, substitutions, method);
		throw new AssertionError();
	}

	private String[] getCommands(Method method) {
		if (method.isAnnotationPresent(copy.class))
			return method.getAnnotation(copy.class).value();
		if (method.isAnnotationPresent(post.class))
			return method.getAnnotation(post.class).value();
		throw new AssertionError();
	}

}
