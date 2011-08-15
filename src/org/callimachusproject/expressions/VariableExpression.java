package org.callimachusproject.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.model.Node;
import org.openrdf.model.Value;

public class VariableExpression implements Expression {
	private String variable;

	public VariableExpression(String variable) {
		assert variable.charAt(0) == '?';
		this.variable = variable.substring(1);
	}

	@Override
	public String bind(Map<String, Value> variables) {
		if (variables.containsKey(variable))
			return variables.get(variable).stringValue();
		return "";
	}

	@Override
	public String getTemplate() {
		return "";
	}

	@Override
	public List<RDFEvent> pattern(Node subject) {
		return Collections.emptyList();
	}

}
