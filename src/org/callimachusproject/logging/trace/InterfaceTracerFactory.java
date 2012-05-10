package org.callimachusproject.logging.trace;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterfaceTracerFactory implements TracerFactory {

	/** parameter types of a proxy class constructor */
	private final static Class<?>[] params = { InvocationHandler.class };

	public <T> T trace(MethodCall returnedFrom, T target, Class<T> cls, TracerService service) {
		assert cls.isInterface();
		if (target == null)
			return null;

		Class<? extends Object> tcls = target.getClass();
		ClassLoader cl = tcls.getClassLoader();
		List<Class<?>> list = getInterfaces(tcls, new ArrayList<Class<?>>());
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

	private List<Class<?>> getInterfaces(Class<?> cls, List<Class<?>> list) {
		list.addAll(Arrays.asList(cls.getInterfaces()));
		if (cls.getSuperclass() != null)
			return getInterfaces(cls.getSuperclass(), list);
		return list;
	}

}
