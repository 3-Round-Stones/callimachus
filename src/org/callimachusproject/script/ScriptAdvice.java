package org.callimachusproject.script;

import java.util.Set;

import javax.script.SimpleBindings;

import org.callimachusproject.script.EmbededScriptEngine.ScriptResult;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;

public class ScriptAdvice implements Advice {
	private final EmbededScriptEngine engine;
	private final Class<?> rty;
	private final String[][] bindingNames;
	private final String[] defaults;

	public ScriptAdvice(EmbededScriptEngine engine, Class<?> returnClass,
			String[][] bindingNames, String[] defaultValues) {
		assert bindingNames.length == defaultValues.length;
		this.engine = engine;
		this.rty = returnClass;
		this.bindingNames = bindingNames;
		this.defaults = defaultValues;
	}

	@Override
	public String toString() {
		return engine.toString();
	}

	@Override
	public Object intercept(ObjectMessage message) throws Exception {
		return cast(engine.eval(message, getBindings(message)));
	}

	private SimpleBindings getBindings(ObjectMessage message)
			throws RepositoryException, QueryEvaluationException {
		SimpleBindings bindings = new SimpleBindings();
		Object target = message.getTarget();
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
		}
		Object[] parameters = message.getParameters();
		Class<?>[] ptypes = message.getMethod().getParameterTypes();
		assert parameters.length == ptypes.length;
		assert parameters.length == bindingNames.length;
		for (int i = 0; i < bindingNames.length; i++) {
			for (String name : bindingNames[i]) {
				Object value = parameters[i];
				String defaultValue = defaults[i];
				if (value == null && defaultValue != null && con != null) {
					Class<?> vtype = ptypes[i];
					value = getDefaultObject(defaultValue, vtype, con);
				}
				bindings.put(name, value);
			}
		}
		return bindings;
	}

	private Object getDefaultObject(String value, Class<?> type,
			ObjectConnection con) throws RepositoryException,
			QueryEvaluationException {
		if (Set.class.equals(type))
			return null;
		ObjectFactory of = con.getObjectFactory();
		if (of.isDatatype(type)) {
			URIImpl datatype = new URIImpl("java:" + type.getName());
			return of.createObject(new LiteralImpl(value, datatype));
		}
		return con.getObject(type, value);
	}

	private Object cast(ScriptResult result) {
		if (Set.class.equals(rty)) {
			return result.asSet();

		} else if (Void.class.equals(rty) || Void.TYPE.equals(rty)) {
			return result.asVoidObject();
		} else if (Boolean.class.equals(rty) || Boolean.TYPE.equals(rty)) {
			return result.asBooleanObject();
		} else if (Byte.class.equals(rty) || Byte.TYPE.equals(rty)) {
			return result.asByteObject();
		} else if (Character.class.equals(rty) || Character.TYPE.equals(rty)) {
			return result.asCharacterObject();
		} else if (Double.class.equals(rty) || Double.TYPE.equals(rty)) {
			return result.asDoubleObject();
		} else if (Float.class.equals(rty) || Float.TYPE.equals(rty)) {
			return result.asFloatObject();
		} else if (Integer.class.equals(rty) || Integer.TYPE.equals(rty)) {
			return result.asIntegerObject();
		} else if (Long.class.equals(rty) || Long.TYPE.equals(rty)) {
			return result.asLongObject();
		} else if (Short.class.equals(rty) || Short.TYPE.equals(rty)) {
			return result.asShortObject();

		} else if (Number.class.equals(rty)) {
			return result.asNumberObject();

		} else {
			return result.asObject();
		}
	}

}
