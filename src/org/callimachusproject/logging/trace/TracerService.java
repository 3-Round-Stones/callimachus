package org.callimachusproject.logging.trace;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class TracerService {
	private static ServiceLoader<TracerProvider> fallback = ServiceLoader.load(
			TracerProvider.class, TracerService.class.getClassLoader());

	public static TracerService newInstance() {
		return new TracerService(Thread.currentThread().getContextClassLoader());
	}

	private final ServiceLoader<TracerProvider> loader;
	private final Map<Class<?>, TracerFactory> factories = new HashMap<Class<?>, TracerFactory>();

	public TracerService(ClassLoader cl) {
		loader = ServiceLoader.load(TracerProvider.class, cl);
	}

	public <T> T trace(T target, Class<T> cls) {
		return trace(null, target, cls);
	}

	public <T> T trace(MethodCall returnedFrom, T target, Class<T> cls) {
		TracerFactory factory = getTracerFactory(cls);
		if (factory == null)
			return target;
		return factory.trace(returnedFrom, target, cls, this);
	}

	public TracerFactory getTracerFactory(Class<?> cls) {
		if (cls.isPrimitive())
			return null;
		synchronized (factories) {
			if (factories.containsKey(cls))
				return factories.get(cls);
			TracerFactory factory = lookupTracerFactory(cls);
			factories.put(cls, factory);
			return factory;
		}
	}

	private TracerFactory lookupTracerFactory(Class<?> cls) {
		for (TracerProvider proivder : loader) {
			TracerFactory factory = proivder.getTracerFactory(cls);
			if (factory != null)
				return factory;
		}
		for (TracerProvider proivder : fallback) {
			TracerFactory factory = proivder.getTracerFactory(cls);
			if (factory != null)
				return factory;
		}
		if (cls.isInterface())
			return new InterfaceTracerFactory();
		return null;
	}

}
