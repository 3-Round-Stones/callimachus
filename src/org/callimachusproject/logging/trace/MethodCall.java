package org.callimachusproject.logging.trace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodCall extends AbstractTrace {
	private static final Map<String, Integer> variables = new HashMap<String, Integer>();
	private static final List<MethodCall> active = new ArrayList<MethodCall>();

	public static Trace[] getActiveCallTraces() {
		synchronized (active) {
			return active.toArray(new Trace[active.size()]);
		}
	}

	private final TraceAggregate aggregate;
	private long started;
	private long ended;
	private String threw;

	public MethodCall(Method method, Object... args) {
		this(null, method, args);
	}

	public MethodCall(Class<?> returnType, String methodName, Object... args) {
		this(null, returnType, methodName, null, args);
	}

	public MethodCall(MethodCall returnedTarget, Method method, Object... args) {
		this(returnedTarget, method.getReturnType(), method.getName(), method.getParameterTypes(), args);
	}

	public MethodCall(MethodCall previous, Class<?> returnType,
			String methodName, Class<?>[] types, Object... args) {
		super(previous, returnType, methodName, types, args);
		TraceAggregate trace = null;
		if (previous != null) {
			previous.done();
			trace = previous.getTraceAggregate();
		}
		TraceAnalyser a = TraceAnalyser.getInstance();
		aggregate = a.getAggregate(trace, returnType, methodName, types, args);
	}

	public TraceAggregate getTraceAggregate() {
		return aggregate;
	}

	@Override
	public MethodCall getPreviousTrace() {
		return (MethodCall) super.getPreviousTrace();
	}

	public synchronized void calling() {
		if (started == 0) {
			synchronized (active) {
				active.add(this);
			}
			started = System.nanoTime();
		}
	}

	public synchronized void done() {
		if (ended == 0) {
			ended = System.nanoTime();
			getTraceAggregate().spent(ended - started);
			synchronized (active) {
				active.remove(this);
			}
		}
	}

	public synchronized Throwable threw(Throwable threw) {
		done();
		StringBuilder sb = new StringBuilder();
		sb.append(threw.getClass().getSimpleName());
		sb.append("(");
		if (threw.getMessage() != null) {
			sb.append(str(threw.getMessage()));
		}
		sb.append(")");
		this.threw = sb.toString();
		return threw;
	}

	public synchronized String getThrown() {
		return threw;
	}

	public synchronized double getCallTimeSince(long now) {
		long duration = started - now;
		return (duration / 1000000) / 1000.0;
	}

	public synchronized double getDuration() {
		if (isDone()) {
			long duration = ended - started;
			return (duration / 1000000) / 1000.0;
		} else {
			long duration = System.nanoTime() - started;
			return (duration / 1000000) / 1000.0;
		}
	}

	public synchronized boolean isDone() {
		return ended > 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String threw = getThrown();
		if (isDone() && threw == null) {
			if (getReturnVariable() != null) {
				sb.append(getReturnType()).append(" ");
				sb.append(getReturnVariable()).append(" = ");
			}
		}
		if (getPreviousTrace() != null) {
			sb.append(getPreviousTrace().getReturnVariable()).append(".");
		}
		sb.append(getMethodName()).append("(");
		sb.append(getParameters()).append(")");
		if (threw != null) {
			sb.append(" threw ").append(threw);
		}
		sb.append("; // ");
		sb.append(getDuration()).append("s");
		return sb.toString();
	}

	@Override
	protected String getVariableSuffix(String name) {
		synchronized (variables) {
			Integer count = variables.get(name);
			if (count == null) {
				variables.put(name, count = 1);
			} else {
				variables.put(name, count += 1);
			}
			return count.toString();
		}
	}

	@Override
	protected String getReturnVariableOf(MethodCall call) {
		return call.getReturnVariable();
	}

}
