package org.callimachusproject.engine.expressions;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.Location;

import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.TermOrigin;

public class VariableExpression implements Expression {
	private final String variable;
	private final Location location;

	public VariableExpression(String variable, Location location) {
		assert variable.charAt(0) == '?';
		this.variable = variable.substring(1);
		this.location = location;
	}

	@Override
	public String toString() {
		return variable.toString();
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String bind(ExpressionResult variables) {
		return variables.getVariable(variable, location);
	}

	@Override
	public String getTemplate() {
		return "";
	}

	@Override
	public boolean isPatternPresent() {
		return false;
	}

	@Override
	public List<RDFEvent> pattern(Node subject, TermOrigin origin, Location location) {
		return Collections.emptyList();
	}

}
