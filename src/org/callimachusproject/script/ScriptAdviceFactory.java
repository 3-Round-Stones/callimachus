package org.callimachusproject.script;

import java.lang.reflect.Method;
import java.net.URL;

import org.callimachusproject.annotations.script;
import org.openrdf.annotations.Iri;
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
		return new ScriptAdvice(engine, method);
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

}
