package org.callimachusproject.logging.trace;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tracer implements InvocationHandler {
	private final Logger logger;
	private final TracerService service;
	private final Class<?> declaredType;
	private final MethodCall returnedFrom;
	private final Object target;

	public Tracer(MethodCall returnedFrom, Object target, Class<?> declaredType, TracerService service) {
		assert target != null;
		assert declaredType != null;
		assert service != null;
		this.logger = LoggerFactory.getLogger(declaredType);
		this.service = service;
		this.declaredType = declaredType;
		this.returnedFrom = returnedFrom;
		this.target = target;
	}

	public Class<?> getDeclaredType() {
		return declaredType;
	}

	public MethodCall getFactoryMethod() {
		return returnedFrom;
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		if (logger.isTraceEnabled()) {
			MethodCall call = new MethodCall(returnedFrom, method, args);
			try {
				for (String assign : call.getAssignments()) {
					logger.trace(assign);
				}
				call.calling();
				return invokeCall(call, method, args);
			} catch (Throwable t) {
				throw call.threw(t);
			} finally {
				call.done();
				logger.trace(call.toString());
			}
		} else {
			return invokeCall(null, method, args);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object invokeCall(MethodCall call, Method method, Object[] args) throws Throwable {
		try {
			Class type = method.getReturnType();
			if (!type.isPrimitive() && LoggerFactory.getLogger(type).isTraceEnabled()) {
				return service.trace(call, method.invoke(target, args), type);
			} else {
				return method.invoke(target, args);
			}
		} catch (InvocationTargetException e) {
			throw e.getCause();
		} catch (IllegalArgumentException e) {
			InternalError error = new InternalError(e.getMessage());
			error.initCause(e);
			throw error;
		} catch (IllegalAccessException e) {
			IllegalAccessError error = new IllegalAccessError(e.getMessage());
			error.initCause(e);
			throw error;
		}
	}

}
