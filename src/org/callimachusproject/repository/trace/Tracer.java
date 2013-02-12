package org.callimachusproject.repository.trace;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;

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
		this.logger = service.getLogger(declaredType);
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
		if (service.isTraceEnabled(declaredType.getName())) {
			Method imethod = getInterfaceMethod(method);
			MethodCall call = new MethodCall(service, returnedFrom, imethod, args);
			try {
				for (String assign : call.getAssignments()) {
					logger.trace(assign);
				}
				call.calling();
				return invokeCall(call, imethod, args);
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

	public String toString() {
		return target.toString();
	}

	private Method getInterfaceMethod(Method method) {
		assert method != null;
		if (declaredType.equals(method.getDeclaringClass()))
			return method;
		try {
			Class<?>[] ptypes = method.getParameterTypes();
			return declaredType.getMethod(method.getName(), ptypes);
		} catch (NoSuchMethodException e) {
			return method;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object invokeCall(MethodCall call, Method method, Object[] args) throws Throwable {
		try {
			Class type = method.getReturnType();
			if (!type.isPrimitive() && service.isTraceEnabled(type.getName())) {
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
