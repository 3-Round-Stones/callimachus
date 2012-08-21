package org.callimachusproject.xproc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

import org.callimachusproject.annotations.xproc;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class XprocAdviceFactory implements AdviceProvider, AdviceFactory {
	private final PipelineFactory factory = PipelineFactory.getInstance();

	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (xproc.class.equals(annotationType))
			return this;
		return null;
	}

	public Advice createAdvice(Method m) {
		Annotation[][] anns = m.getParameterAnnotations();
		String[] bindingNames = new String[anns.length];
		int source = anns.length - 1;
		for (int i = 0; i < source; i++) {
			for (Annotation ann : anns[i]) {
				if (Iri.class.equals(ann.annotationType())) {
					bindingNames[i] = local(((Iri) ann).value());
				}
			}
		}
		String url = resolve(m.getAnnotation(xproc.class).value(), m);
		Pipeline pipeline = factory.createPipeline(url);
		return new XprocAdvice(pipeline, m, bindingNames, source);
	}

	private String resolve(String ref, Method method) {
		if (URI.create(ref).isAbsolute())
			return ref;
		URL url = method.getDeclaringClass().getResource(ref);
		if (url != null)
			return url.toExternalForm();
		return URI.create(getSystemId(method)).resolve(ref).toASCIIString();
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
