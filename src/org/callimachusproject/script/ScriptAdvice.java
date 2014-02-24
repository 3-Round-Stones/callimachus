/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.SimpleBindings;

import org.callimachusproject.annotations.imports;
import org.callimachusproject.script.EmbeddedScriptEngine.ScriptResult;
import org.openrdf.annotations.Iri;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;

public class ScriptAdvice implements Advice {
	private final EmbeddedScriptEngine engine;
	private final Class<?> rty;
	private final Map<Method, String[]> bindingNames = new HashMap<Method, String[]>(1);
	private final Map<Method, String[]> defaults = new HashMap<Method, String[]>(1);

	public ScriptAdvice(EmbeddedScriptEngine engine, Method method) {
		this.engine = engine;
		this.rty = method.getReturnType();
		Annotation[][] panns = method.getParameterAnnotations();
		String[] names = getBindingNames(panns);
		this.bindingNames.put(method, names);
		this.defaults.put(method, getDefaultValues(panns));
		if (method.isAnnotationPresent(imports.class)) {
			for (Class<?> imports : method.getAnnotation(imports.class).value()) {
				engine = engine.importClass(imports.getName());
			}
		}
		engine = engine.returnType(rty);
		for (String name : names) {
			engine = engine.binding(name);
		}
	}

	@Override
	public String toString() {
		return engine.toString();
	}

	@Override
	public Object intercept(ObjectMessage message) throws Exception {
		return cast(engine.eval(message, getBindings(message)));
	}

	private synchronized String[] getBindingNames(Method method) {
		String[] names = bindingNames.get(method);
		if (names != null)
			return names;
		Annotation[][] panns = method.getParameterAnnotations();
		names = getBindingNames(panns);
		bindingNames.put(method, names);
		return names;
	}

	private synchronized String[] getDefaults(Method method) {
		String[] values = defaults.get(method);
		if (values != null)
			return values;
		Annotation[][] panns = method.getParameterAnnotations();
		values = getDefaultValues(panns);
		defaults.put(method, values);
		return values;
	}

	private SimpleBindings getBindings(ObjectMessage message)
			throws RepositoryException, QueryEvaluationException {
		SimpleBindings bindings = new SimpleBindings();
		Object target = message.getTarget();
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
		}
		Object[] parameters = message.getParameters();
		Class<?>[] ptypes = message.getMethod().getParameterTypes();
		assert parameters.length == ptypes.length;
		String[] bindingNames = getBindingNames(message.getMethod());
		assert parameters.length == bindingNames.length;
		String[] defaults = getDefaults(message.getMethod());
		for (int i = 0; i < bindingNames.length; i++) {
			String name = bindingNames[i];
			Object value = parameters[i];
			String defaultValue = defaults[i];
			if (value == null && defaultValue != null && con != null) {
				Class<?> vtype = ptypes[i];
				value = getDefaultObject(defaultValue, vtype, con);
			}
			bindings.put(name, value);
		}
		return bindings;
	}

	private String[] getBindingNames(Annotation[][] anns) {
		String[] bindingNames = new String[anns.length];
		for (int i=0; i<anns.length; i++) {
			for (Annotation ann : anns[i]) {
				if (Iri.class.equals(ann.annotationType())) {
					bindingNames[i] = local(((Iri) ann).value());
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

	private Object getDefaultObject(String value, Class<?> type,
			ObjectConnection con) throws RepositoryException,
			QueryEvaluationException {
		if (Set.class.equals(type))
			return null;
		ObjectFactory of = con.getObjectFactory();
		if (of.isDatatype(type)) {
			URIImpl datatype = new URIImpl("java:" + type.getName());
			return of.createObject(new LiteralImpl(value, datatype));
		}
		return con.getObject(type, value);
	}

	private Object cast(ScriptResult result) {
		if (Set.class.equals(rty)) {
			return result.asSet();

		} else if (Void.class.equals(rty) || Void.TYPE.equals(rty)) {
			return result.asVoidObject();
		} else if (Boolean.class.equals(rty) || Boolean.TYPE.equals(rty)) {
			return result.asBooleanObject();
		} else if (Byte.class.equals(rty) || Byte.TYPE.equals(rty)) {
			return result.asByteObject();
		} else if (Character.class.equals(rty) || Character.TYPE.equals(rty)) {
			return result.asCharacterObject();
		} else if (Double.class.equals(rty) || Double.TYPE.equals(rty)) {
			return result.asDoubleObject();
		} else if (Float.class.equals(rty) || Float.TYPE.equals(rty)) {
			return result.asFloatObject();
		} else if (Integer.class.equals(rty) || Integer.TYPE.equals(rty)) {
			return result.asIntegerObject();
		} else if (Long.class.equals(rty) || Long.TYPE.equals(rty)) {
			return result.asLongObject();
		} else if (Short.class.equals(rty) || Short.TYPE.equals(rty)) {
			return result.asShortObject();

		} else if (Number.class.equals(rty)) {
			return result.asNumberObject();

		} else {
			return result.asObject();
		}
	}

}
