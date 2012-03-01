package org.callimachusproject.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.callimachusproject.engine.model.TermFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.rio.turtle.TurtleUtil;

public class ParameterizedQuery {
	private static ValueFactory vf = ValueFactoryImpl.getInstance();
	private final String sparql;
	private final String systemId;
	private final List<String> bindingNames;
	private final BindingSet bindings;
	private final TermFactory tf;

	public ParameterizedQuery(String sparql, String systemId,
			BindingSet bindings) {
		assert sparql != null;
		assert systemId != null;
		assert bindings != null;
		this.sparql = sparql;
		this.systemId = systemId;
		this.bindingNames = new ArrayList<String>(bindings.getBindingNames());
		this.bindings = bindings;
		this.tf = TermFactory.newInstance(systemId);
	}

	public String toString() {
		return sparql;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + sparql.hashCode();
		result = prime * result + systemId.hashCode();
		result = prime * result + bindings.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterizedQuery other = (ParameterizedQuery) obj;
		if (!sparql.equals(other.sparql))
			return false;
		if (!systemId.equals(other.systemId))
			return false;
		if (!bindings.equals(other.bindings))
			return false;
		return true;
	}

	public String prepare(Map<String, String[]> parameters) {
		if (bindings.getBindingNames().isEmpty())
			return sparql;
		StringBuilder sb = new StringBuilder(sparql);
		sb.append("\nBINDINGS");
		for (String name : bindingNames) {
			sb.append(" $").append(name);
		}
		sb.append(" {\n");
		for (List<Value> values : getParameterBindingValues(parameters)) {
			sb.append("\t(");
			for (Value value : values) {
				if (value == null) {
					sb.append("UNDEF");
				} else if (value instanceof Literal) {
					writeLiteral(sb, value);
				} else {
					writeURI(sb, value);
				}
				sb.append(" ");
			}
			sb.append(")\n");
		}
		sb.append("}\n");
		return sb.toString();
	}

	private List<List<Value>> getParameterBindingValues(
			Map<String, String[]> parameters) {
		List<List<Value>> bindingValues = Collections.singletonList(Collections
				.<Value> emptyList());
		for (String name : bindingNames) {
			String[] strings = parameters == null ? null : parameters.get(name);
			if (strings == null || strings.length == 0) {
				List<List<Value>> list;
				list = new ArrayList<List<Value>>(bindingValues.size());
				appendBinding(bindingValues, bindings.getValue(name), list);
				bindingValues = list;
			} else {
				List<List<Value>> list;
				int size = bindingValues.size() + strings.length - 1;
				list = new ArrayList<List<Value>>(size);
				for (String string : strings) {
					Value value = resolve(name, string);
					appendBinding(bindingValues, value, list);
				}
				bindingValues = list;
			}
		}
		return bindingValues;
	}

	private Value resolve(String name, String value) {
		Value sample = bindings.getValue(name);
		if (sample instanceof Literal) {
			Literal lit = (Literal) sample;
			if (lit.getLanguage() != null) {
				return vf.createLiteral(value, lit.getLanguage());
			} else if (lit.getDatatype() != null) {
				return vf.createLiteral(value, lit.getDatatype());
			} else {
				return vf.createLiteral(value);
			}
		} else {
			return vf.createURI(tf.reference(value).stringValue());
		}
	}

	private void appendBinding(List<List<Value>> existingBindings, Value value,
			List<List<Value>> target) {
		for (List<Value> bindings : existingBindings) {
			List<Value> set = new ArrayList<Value>(bindings.size() + 1);
			set.addAll(bindings);
			set.add(value);
			target.add(set);
		}
	}

	private void writeLiteral(StringBuilder sb, Value value) {
		Literal lit = (Literal) value;
		sb.append("\"");
		String label = value.stringValue();
		sb.append(TurtleUtil.encodeString(label));
		sb.append("\"");
		if (lit.getLanguage() != null) {
			// Append the literal's language
			sb.append("@");
			sb.append(lit.getLanguage());
		} else if (lit.getDatatype() != null) {
			// Append the literal's datatype
			sb.append("^^");
			writeURI(sb, lit.getDatatype());
		}
	}

	private void writeURI(StringBuilder sb, Value value) {
		sb.append("<");
		String uri = value.stringValue();
		sb.append(TurtleUtil.encodeURIString(uri));
		sb.append(">");
	}

}
