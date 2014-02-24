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
package org.callimachusproject.repository.trace;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class InterfaceTracerFactory implements TracerFactory {

	/** parameter types of a proxy class constructor */
	private final static Class<?>[] params = { InvocationHandler.class };

	public <T> T trace(MethodCall returnedFrom, T target, Class<T> cls, TracerService service) {
		assert cls.isInterface();
		if (target == null)
			return null;

		Class<? extends Object> tcls = target.getClass();
		ClassLoader cl = tcls.getClassLoader();
		Collection<Class<?>> list = getInterfaces(tcls, new HashSet<Class<?>>());
		Class<?>[] array = list.toArray(new Class<?>[list.size()]);
		Class<?> pc = Proxy.getProxyClass(cl, array);

		try {
			Tracer tracer = new Tracer(returnedFrom, target, cls, service);
			return cls.cast(pc.getConstructor(params).newInstance(tracer));
		} catch (NoSuchMethodException e) {
			throw new InternalError(e.toString());
		} catch (IllegalAccessException e) {
			throw new InternalError(e.toString());
		} catch (InstantiationException e) {
			throw new InternalError(e.toString());
		} catch (InvocationTargetException e) {
			throw new InternalError(e.toString());
		}
	}

	private Collection<Class<?>> getInterfaces(Class<?> cls, Collection<Class<?>> list) {
		list.addAll(Arrays.asList(cls.getInterfaces()));
		if (cls.getSuperclass() != null)
			return getInterfaces(cls.getSuperclass(), list);
		return list;
	}

}
