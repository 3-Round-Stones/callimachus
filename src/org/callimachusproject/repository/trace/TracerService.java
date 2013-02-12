package org.callimachusproject.repository.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracerService {
	private static ServiceLoader<TracerProvider> fallback = ServiceLoader.load(
			TracerProvider.class, TracerService.class.getClassLoader());

	public static TracerService newInstance() {
		return new TracerService(Thread.currentThread().getContextClassLoader());
	}

	private final ServiceLoader<TracerProvider> loader;
	private final Map<Class<?>, TracerFactory> factories = new HashMap<Class<?>, TracerFactory>();
	private final TraceAnalyser analyser = new TraceAnalyser();
	private final List<MethodCall> active = new ArrayList<MethodCall>();
	private String[] prefixEnabled = new String[0];

	public TracerService(ClassLoader cl) {
		loader = ServiceLoader.load(TracerProvider.class, cl);
	}

	public synchronized String[] getTracingPackages() {
		return prefixEnabled;
	}

	public synchronized void setTracingPackages(String... prefix) {
		prefixEnabled = prefix;
	}

	public Logger getLogger(Class<?> declaredType) {
		return LoggerFactory.getLogger(declaredType);
	}

	public synchronized boolean isTraceEnabled(String name) {
		for (String prefix : prefixEnabled) {
			if (name.startsWith(prefix))
				return true;
		}
		return false;
	}

	public Trace[] getTracesByTotalTime() {
		return analyser.getTracesByTotalTime();
	}

	public Trace[] getTracesByAverageTime() {
		return analyser.getTracesByAverageTime();
	}

	public void resetAnalysis() {
		analyser.reset();
	}

	public Trace[] getActiveCallTraces() {
		synchronized (active) {
			return active.toArray(new Trace[active.size()]);
		}
	}

	public void enter(MethodCall call) {
		synchronized (active) {
			active.add(call);
		}
	}

	public void exit(MethodCall call) {
		synchronized (active) {
			active.remove(call);
		}
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

	public TraceAggregate getAggregate(TraceAggregate previous,
			Class<?> returnType, String methodName, Class<?>[] types,
			Object... args) {
		return analyser.getAggregate(previous, returnType,
				methodName, types, args);
	}

}
