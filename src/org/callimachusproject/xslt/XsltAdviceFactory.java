package org.callimachusproject.xslt;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

import org.callimachusproject.annotations.xslt;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class XsltAdviceFactory implements AdviceProvider, AdviceFactory {
	private static final Pattern NOT_URI = Pattern.compile("\\s|\\}|\\]|\\>|\"");

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (xslt.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdviser(Method m) {
		Annotation[][] anns = m.getParameterAnnotations();
		String[][] bindingNames = new String[anns.length][];
		int input = -1;
		loop: for (int i=0; i<anns.length; i++) {
			bindingNames[i] = new String[0];
			for (Annotation ann : anns[i]) {
				if (Bind.class.equals(ann.annotationType())) {
					bindingNames[i] = ((Bind) ann).value();
					continue loop;
				} else if (Iri.class.equals(ann.annotationType())) {
					bindingNames[i] = new String[] { local(((Iri) ann).value()) };
				}
			}
			input = i;
		}
		Class<?> inputClass = null;
		if (input >= 0) {
			bindingNames[input] = new String[0];
			inputClass = m.getParameterTypes()[input];
		}
		XSLTransformer xslt = createXSLTransformer(m);
		return new XsltAdvice(xslt, m.getReturnType(), inputClass, input, bindingNames);
	}

	private XSLTransformer createXSLTransformer(Method m) {
		String xslt = getXslValue(m);
		if (NOT_URI.matcher(xslt).find()) {
			return new XSLTransformer(new StringReader(xslt), getSystemId(m));
		}
		return new XSLTransformer(resolve(xslt, m));
	}

	private String getXslValue(Method m) {
		if (m.isAnnotationPresent(xslt.class))
			return m.getAnnotation(xslt.class).value();
		return null;
	}

	private String resolve(String xslt, Method method) {
		if (URI.create(xslt).isAbsolute())
			return xslt;
		URL url = method.getDeclaringClass().getResource(xslt);
		if (url != null)
			return url.toExternalForm();
		return URI.create(getSystemId(method)).resolve(xslt).toASCIIString();
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
