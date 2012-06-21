package org.callimachusproject.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import org.callimachusproject.annotations.imports;
import org.callimachusproject.annotations.script;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Iri;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;

public class ScriptAdviceFactory implements AdviceFactory, AdviceProvider {

	@Override
	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (script.class.equals(annotationType))
			return this;
		return null;
	}

	@Override
	public Advice createAdvice(Method method) {
		EmbededScriptEngine engine = createEmbededScriptEngine(method);
		if (method.isAnnotationPresent(imports.class)) {
			for (Class<?> imports : method.getAnnotation(imports.class).value()) {
				engine = engine.importClass(imports.getName());
			}
		}
		Annotation[][] panns = method.getParameterAnnotations();
		Class<?> rtype = method.getReturnType();
		engine = engine.returnType(rtype);
		String[][] names = getBindingNames(panns);
		String[] defaults = getDefaultValues(panns);
		for (String[] ar : names) {
			for (String name : ar) {
				engine = engine.binding(name);
			}
		}
		return new ScriptAdvice(engine, rtype, names, defaults);
	}

	private EmbededScriptEngine createEmbededScriptEngine(Method method) {
		ClassLoader cl = method.getDeclaringClass().getClassLoader();
		String[] script = method.getAnnotation(script.class).value();
		return EmbededScriptEngine.newInstance(cl, getSystemId(method), script);
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

	private String[][] getBindingNames(Annotation[][] anns) {
		String[][] bindingNames = new String[anns.length][];
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
		}
		return bindingNames;
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

	private String[] getDefaultValues(Annotation[][] anns) {
		String[] defaults = new String[anns.length];
		for (int i=0; i<anns.length; i++) {
			Object value = getDefaultValue(anns[i]);
			if (value != null) {
				defaults[i] = value.toString();
			}
		}
		return defaults;
	}

	private Object getDefaultValue(Annotation[] anns) {
		for (Annotation ann : anns) {
			for (Method m : ann.annotationType().getDeclaredMethods()) {
				Iri iri = m.getAnnotation(Iri.class);
				if (iri != null && OWL.HASVALUE.stringValue().equals(iri.value()) && m.getParameterTypes().length == 0) {
					return invoke(m, ann);
				}
			}
		}
		return null;
	}

	private Object invoke(Method m, Object obj) {
		try {
			return m.invoke(obj);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			IllegalAccessError error = new IllegalAccessError(e.getMessage());
			error.initCause(e);
			throw error;
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (RuntimeException cause) {
				throw cause;
			} catch (Error cause) {
				throw cause;
			} catch (Throwable cause) {
				throw new UndeclaredThrowableException(cause);
			}
		}
	}

}
