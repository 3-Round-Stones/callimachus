package org.callimachusproject.engine.expressions;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.Location;

import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.TermOrigin;

public class TextExpression implements Expression {
	private final CharSequence text;
	private final Location location;

	public TextExpression(CharSequence text, Location location) {
		this.text = text;
		this.location = location;
	}

	public String toString() {
		return text.toString();
	}

	@Override
	public Location getLocation() {
		return location;
	}

	public CharSequence getTemplate() {
		return text;
	}

	public CharSequence bind(ExpressionResult variables) {
		return text;
	}

	@Override
	public boolean isPatternPresent() {
		return false;
	}

	public List<RDFEvent> pattern(Node subject, TermOrigin origin, Location location) {
		return Collections.emptyList();
	}
}
