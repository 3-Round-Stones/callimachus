/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.behaviours;

import static org.openrdf.http.object.behaviours.ProxyObjectSupport.GET_PROXY_ADDRESS;
import static org.openrdf.repository.object.composition.helpers.InvocationMessageContext.PARAMETERS;
import static org.openrdf.repository.object.composition.helpers.InvocationMessageContext.PROCEED;
import static org.openrdf.repository.object.composition.helpers.InvocationMessageContext.selectMessageType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openrdf.annotations.ParameterTypes;
import org.openrdf.annotations.Precedes;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.traits.ProxyObject;
import org.openrdf.repository.object.composition.BehaviourFactory;
import org.openrdf.repository.object.composition.ClassTemplate;
import org.openrdf.repository.object.composition.MethodBuilder;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;

/**
 * Creates dynamic behaviours based on @method and @operation annotations.
 */
public class ProxyObjectBehaviourFactory extends BehaviourFactory {

	@Override
	protected boolean isEnhanceable(Class<?> role)
			throws ObjectStoreConfigException {
		for (Method method : role.getDeclaredMethods()) {
			if (method.isAnnotationPresent(ParameterTypes.class))
				continue;
			if (method.isAnnotationPresent(query.class))
				return true;
			if (method.isAnnotationPresent(operation.class))
				return true;
			if (method.isAnnotationPresent(method.class))
				return true;
		}
		return false;
	}

	@Override
	protected Collection<? extends Class<?>> findImplementations(Class<?> role)
			throws Exception {
		List<Class<?>> behaviours = new ArrayList<Class<?>>();
		for (Method method : role.getDeclaredMethods()) {
			if (method.isAnnotationPresent(ParameterTypes.class))
				continue;
			if (!method.isAnnotationPresent(query.class)
					&& !method.isAnnotationPresent(operation.class)
					&& !method.isAnnotationPresent(method.class))
				continue;
			behaviours.add(findBehaviour(role, method));
		}
		return behaviours;
	}

	private Class<?> findBehaviour(Class<?> concept, Method method)
			throws Exception {
		String className = getJavaClassName(concept, method);
		synchronized (cp) {
			try {
				return Class.forName(className, true, cp);
			} catch (ClassNotFoundException e2) {
				return implement(className, concept, method);
			}
		}
	}

	private String getJavaClassName(Class<?> concept, Method method) {
		String suffix = getClass().getSimpleName().replaceAll("Factory$", "");
		String m = "$" + method.getName() + toHexString(method);
		return CLASS_PREFIX + concept.getName() + m + suffix;
	}

	private Class<?> implement(String className, Class<?> role, Method method)
			throws Exception {
		ClassTemplate cc = createBehaviourTemplate(className, role);
		cc.addAnnotation(Precedes.class);
		overrideMethod(cc, method);
		return cp.createClass(cc);
	}

	private void overrideMethod(ClassTemplate cc, Method method) {
		Class<?> rt = method.getReturnType();
		Class<?>[] ptypes = method.getParameterTypes();
		boolean intercepting = method.isAnnotationPresent(ParameterTypes.class)
				&& ptypes.length == 1;
		Method conceptMethod = method;
		Class<?> mt = selectMessageType(rt);
		MethodBuilder code = cc.createMethod(rt, method.getName(), mt);
		if (intercepting) {
			ptypes = method.getAnnotation(ParameterTypes.class).value();
			code.ann(ParameterTypes.class, ptypes);
			conceptMethod = findConceptMethod(method, ptypes);
		} else {
			code.ann(ParameterTypes.class, ptypes);
			conceptMethod = method;
		}
		if (!Void.TYPE.equals(rt)) {
			code.code(Object.class.getName()).code(" result").semi();
		}
		code.code("if ((").castObject(ProxyObject.class).code(BEAN_FIELD_NAME);
		code.code(").").code(GET_PROXY_ADDRESS).code("() == null) {");
		if (Void.TYPE.equals(rt)) {
			code.code("$1." + PROCEED + "()").semi();
		} else if (rt.isPrimitive()) {
			code.code("return $1." + PROCEED + "()").semi();
		} else {
			code.code("result = $1." + PROCEED + "()").semi();
		}
		code.code("} else {");
		if (!Void.TYPE.equals(rt)) {
			code.code("result = ");
		}
		code.code("(").castObject(ProxyObject.class).code(BEAN_FIELD_NAME);
		code.code(").invokeRemote(").insert(conceptMethod);
		code.code(", $1." + PARAMETERS + "())").semi();
		code.code("}");
		if (rt.isPrimitive() && !Void.TYPE.equals(rt)) {
			if (Boolean.TYPE.equals(rt)) {
				code.code("if (result == null) return false;\n");
			} else {
				code.code("if (result == null) return ").cast(rt).code("0").semi();
			}
			code.declareWrapper(rt, "wrap").castObject(rt).code("result").semi();
			code.code("return wrap.").code(rt.getName()).code("Value()").semi();
		} else if (!Void.TYPE.equals(rt)) {
			code.code("return ").castObject(rt).code("result").semi();
		}
		code.end();
	}

	private Method findConceptMethod(Method method, Class<?>[] ptypes) {
		try {
			return method.getDeclaringClass().getMethod(method.getName(),
					ptypes);
		} catch (NoSuchMethodException e) {
			return method;
		}
	}

	private String toHexString(Method m) {
		List<Class<?>> p = Arrays.asList(m.getParameterTypes());
		return Integer.toHexString(Math.abs(31 * m.hashCode() + p.hashCode()));
	}
}
