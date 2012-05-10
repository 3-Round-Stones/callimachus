package org.callimachusproject.logging.trace;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodCall {
	private static final Map<String, Integer> variables = new HashMap<String, Integer>();
	private static final List<MethodCall> active = new ArrayList<MethodCall>();
	private static final int KEEP_TOP = 100;
	private static long min_top_duration;
	private static final Comparator<MethodCall> comparator = new Comparator<MethodCall>() {
		public int compare(MethodCall o1, MethodCall o2) {
			long d1 = o1.getReturnedCallDuration();
			long d2 = o2.getReturnedCallDuration();
			if (d1 > d2)
				return -1;
			if (d1 == d2)
				return 0;
			return 1;
		}
	};
	private static final List<MethodCall> top = new ArrayList<MethodCall>();

	public static MethodCall[] getActiveCallTraces() {
		synchronized (active) {
			return active.toArray(new MethodCall[active.size()]);
		}
	}

	public static MethodCall[] getTopCallTraces() {
		synchronized (top) {
			return top.toArray(new MethodCall[top.size()]);
		}
	}

	private final MethodCall returnedTarget;
	private final String methodName;
	private final String returnType;
	private final boolean returnPrimitive;
	private final List<String> assignments;
	private final String vargs;
	private long started;
	private long ended;
	private long spent;
	private String threw;
	private String returnName;

	public MethodCall(Method method, Object... args) {
		this(null, method, args);
	}

	public MethodCall(Class<?> returnType, String methodName, Object... args) {
		this(null, returnType, methodName, args);
	}

	public MethodCall(MethodCall returnedTarget, Method method, Object... args) {
		this(returnedTarget, method.getReturnType(), method.getName(), args);
	}

	public MethodCall(MethodCall returnedTarget, Class<?> returnType, String methodName, Object... args) {
		if (returnedTarget != null) {
			returnedTarget.done();
		}
		this.returnedTarget = returnedTarget;
		this.methodName = methodName;
		this.returnType = returnType.getSimpleName();
		this.returnPrimitive = returnType.isPrimitive();
		if (args == null) {
			this.assignments = new ArrayList<String>();
			vargs = "";
		} else {
			this.assignments = new ArrayList<String>(args.length);
			vargs = assign(args);
		}
	}

	public MethodCall getParent() {
		return returnedTarget;
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
			synchronized (active) {
				active.remove(this);
			}
			if (returnedTarget != null) {
				returnedTarget.returnedCallDuration(ended - started);
			}
		}
	}

	public void returnedCallDuration(long returnedCallDuration) {
		if (returnedTarget != null) {
			synchronized (top) {
				spent += returnedCallDuration;
				if (spent > min_top_duration && !top.contains(this)) {
					top.add(this);
					Collections.sort(top, comparator);
					if (top.size() > KEEP_TOP) {
						MethodCall last = top.remove(top.size() - 1);
						min_top_duration = last.getReturnedCallDuration();
					}
				}
			}
		}
	}

	public long getReturnedCallDuration() {
		if (returnedTarget == null)
			return 0;
		synchronized (top) {
			return spent;
		}
	}

	public double getCallTimeOfResponse() {
		long duration = getReturnedCallDuration();
		return (duration / 1000000) / 1000.0;
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

	public synchronized String getReturnName() {
		if (returnName == null && !returnPrimitive) {
			returnName = var(methodName);
		}
		return returnName;
	}

	public List<String> getAssignments() {
		return assignments;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String threw = getThrown();
		if (isDone() && threw == null) {
			if (!returnPrimitive) {
				sb.append(returnType).append(" ");
				sb.append(getReturnName()).append(" = ");
			}
		}
		if (returnedTarget != null) {
			sb.append(returnedTarget.getReturnName()).append(".");
		}
		sb.append(methodName).append("(");
		sb.append(vargs).append(")");
		if (threw != null) {
			sb.append(" threw ").append(threw);
		}
		sb.append("; // ");
		sb.append(getDuration()).append("s");
		return sb.toString();
	}

	private String assign(Object[] args) {
		StringBuilder sb = new StringBuilder();
		if (args != null) {
			for (int i=0;i<args.length;i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(assign(args[i]));
			}
		}
		return sb.toString();
	}

	private String assign(Object arg) {
		if (arg == null)
			return "null";
		String ref = getReference(arg, arg.getClass());
		if (ref != null)
			return ref;
		if (arg instanceof String) {
			String var = var("string");
			assignments.add("String " + var + " = " + str((String) arg) + ";");
			return var;
		}
		String simple = arg.getClass().getSimpleName();
		String var = var(simple);
		assignments.add(simple + " " + var + " = new " + simple + "(" + str(arg.toString()) +");");
		return var;
	}

	private String getReference(Object arg, Class<?> type) {
		if (arg instanceof Proxy) {
			if (Proxy.isProxyClass(type)) {
				InvocationHandler handler = Proxy.getInvocationHandler(arg);
				if (handler instanceof Tracer) {
					Tracer tracer = (Tracer) handler;
					String name = tracer.getName();
					if (name != null)
						return name;
					if (!tracer.getDeclaredType().equals(type))
						return getReference(arg, tracer.getDeclaredType());
				}
			}
		}
		if (arg instanceof Enum<?>)
			return type.getSimpleName() + "." + arg.toString();
		if (arg instanceof Boolean)
			return arg.toString();
		if (arg instanceof Character)
			return str(arg.toString());
		if (arg instanceof Byte)
			return arg.toString();
		if (arg instanceof Short)
			return arg.toString();
		if (arg instanceof Integer)
			return arg.toString();
		if (arg instanceof Long)
			return arg.toString() + "l";
		if (arg instanceof Float)
			return arg.toString() + "f";
		if (arg instanceof Double)
			return arg.toString();
		if (arg instanceof Void)
			return "void";
		for (Field field : type.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				try {
					if (arg.equals(field.get(null))) {
						return type.getSimpleName() + "." + field.getName();
					}
				} catch (IllegalArgumentException e) {
					throw new AssertionError(e);
				} catch (IllegalAccessException e) {
					throw new AssertionError(e);
				}
			}
		}
		return null;
	}

	private String str(String string) {
		if (string == null)
			return "null";
		if (string.length() > 80 && string.indexOf('\n')  >= 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("\"\"\"");
			sb.append(string.replace("\\", "\\\\"));
			sb.append("\"\"\"");
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			String e = string.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
			sb.append('"').append(e).append('"');
			return sb.toString();
		}
	}

	private String var(String method) {
		String name = name(method);
		Integer count = count(name);
		return name + count;
	}

	private Integer count(String name) {
		synchronized (variables) {
			Integer count = variables.get(name);
			if (count == null) {
				variables.put(name, count = 1);
			} else {
				variables.put(name, count += 1);
			}
			return count;
		}
	}

	private String name(String method) {
		for (int i=0,n=method.length() - 1; i<n; i++) {
			char chr = method.charAt(i);
			if (Character.isUpperCase(chr)) {
				return Character.toLowerCase(chr) + method.substring(i + 1);
			}
		}
		return Character.toLowerCase(method.charAt(0)) + method.substring(1);
	}

}
