package org.callimachusproject.logging.trace;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractTrace implements Trace {
	/* @Nullable */
	private final AbstractTrace previousTrace;
	private final List<String> assignments;
	private final String returnType;
	/* @Nullable */
	private final String returnVariable;
	private final String methodName;
	private final String parameters;

	public AbstractTrace(AbstractTrace previousTrace, Class<?> returnType,
			String methodName, Class<?>[] types, Object... args) {
		assert methodName != null;
		assert returnType != null;
		this.previousTrace = previousTrace;
		this.methodName = methodName;
		this.returnType = returnType.getSimpleName();
		if (returnType.isPrimitive()) {
			returnVariable = null;
		} else {
			returnVariable = var(methodName);
		}
		if (args == null) {
			this.assignments = new ArrayList<String>();
			parameters = "";
		} else {
			this.assignments = new ArrayList<String>(args.length);
			parameters = assign(args, types);
		}
		assert assignments != null;
		assert parameters != null;
	}

	public AbstractTrace getPreviousTrace() {
		return previousTrace;
	}

	public List<String> getAssignments() {
		return assignments;
	}

	public String getReturnType() {
		return returnType;
	}

	public String getReturnVariable() {
		return returnVariable;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getParameters() {
		return parameters;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (getReturnVariable() != null) {
			sb.append(getReturnType()).append(" ");
			sb.append(getReturnVariable()).append(" = ");
		}
		if (getPreviousTrace() != null) {
			sb.append(getPreviousTrace().getReturnVariable()).append(".");
		}
		sb.append(getMethodName()).append("(");
		sb.append(getParameters()).append(")");
		return sb.toString();
	}

	protected String str(String string) {
		if (string == null)
			return "null";
		if (string.length() > 80 && string.indexOf('\n') >= 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("\"\"\"");
			sb.append(string.replace("\\", "\\\\"));
			sb.append("\"\"\"");
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			String e = string.replace("\\", "\\\\").replace("\n", "\\n")
					.replace("\"", "\\\"");
			sb.append('"').append(e).append('"');
			return sb.toString();
		}
	}

	protected abstract String getVariableSuffix(String name);

	protected abstract String getReturnVariableOf(MethodCall trace);

	private String assign(Object[] args, Class<?>[] types) {
		StringBuilder sb = new StringBuilder();
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				Class<?> type = Object.class;
				if (types != null) {
					type = types[i];
				} else if (args[i] != null) {
					type= args[i].getClass();
				}
				sb.append(assign(args[i], type));
			}
		}
		return sb.toString();
	}

	private String assign(Object arg, Class<?> type) {
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
		String simple = type.getSimpleName();
		String var = var(simple);
		StringBuilder sb = new StringBuilder();
		sb.append(simple).append(" ").append(var).append(" = ");
		sb.append(getConstructor(arg)).append(";");
		assignments.add(sb.toString());
		return var;
	}

	private String getConstructor(Object arg) {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		if (arg.getClass().isArray()) {
			sb.append(arg.getClass().getComponentType().getSimpleName());
			sb.append(toString(arg));
		} else {
			sb.append(arg.getClass().getSimpleName()).append("(");
			sb.append(str(toString(arg))).append(")");
		}
		return sb.toString();
	}

	private String toString(Object arg) {
		if (arg.getClass().isArray()) {
			Class<?> componentType = arg.getClass().getComponentType();
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			Iterator<?> iter = asList(arg).iterator();
			while (iter.hasNext()) {
				sb.append(inline(iter.next(), componentType));
				if (iter.hasNext()) {
					sb.append(", ");
				}
			}
			sb.append("]");
			return sb.toString();
		} else {
			return arg.toString();
		}
	}

	private String inline(Object arg, Class<?> type) {
		if (arg == null)
			return "null";
		String ref = getReference(arg, arg.getClass());
		if (ref != null)
			return ref;
		if (arg instanceof String) {
			return str((String) arg);
		}
		return getConstructor(arg);
	}

	private List<?> asList(Object arg) {
		assert arg.getClass().isArray();
		if (arg.getClass().getComponentType().isPrimitive()) {
			try {
				return (List<?>) Arrays.class.getMethod("asList", arg.getClass()).invoke(null, arg);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		} else {
			return Arrays.asList((Object[]) arg);
		}
	}

	private String getReference(Object arg, Class<?> type) {
		if (arg instanceof Proxy) {
			if (Proxy.isProxyClass(type)) {
				InvocationHandler handler = Proxy.getInvocationHandler(arg);
				if (handler instanceof Tracer) {
					Tracer tracer = (Tracer) handler;
					MethodCall trace = tracer.getFactoryMethod();
					if (trace != null)
						return getReturnVariableOf(trace);
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
		if (arg instanceof String) {
			String s = (String) arg;
			if (s.indexOf('\n') < 0 && s.length() < 80) {
				return str(s);
			}
		}
		if (arg.getClass().isArray()) {
			List<?> list = asList(arg);
			Class<?> componentType = arg.getClass().getComponentType();
			String simple = componentType.getSimpleName();
			if (list.isEmpty()) {
				return "new " + simple + "[0]";
			} else if (list.size() == 1) {
				String item = inline(list.get(0), componentType);
				if (item.indexOf('\n') < 0 && item.length() < 80) {
					return "new " + simple + "[]{" + item + "}";
				}
			}
		}
		return null;
	}

	private String var(String method) {
		String name = lower(name(method.replaceAll("\\W", "")));
		String count = getVariableSuffix(name);
		return name + count;
	}

	private String name(String method) {
		for (int i = 0, n = method.length() - 1; i < n; i++) {
			char chr = method.charAt(i);
			if (Character.isUpperCase(chr)) {
				return method.substring(i);
			}
		}
		return method;
	}

	private String lower(String name) {
		char[] chrs = name.toCharArray();
		for (int i = 0; i < chrs.length && Character.isUpperCase(chrs[i]); i++) {
			chrs[i] = Character.toLowerCase(chrs[i]);
		}
		return new String(chrs);
	}

}
