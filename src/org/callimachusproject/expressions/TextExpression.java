package org.callimachusproject.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.model.Node;
import org.openrdf.model.Value;

public class TextExpression implements Expression {
	private String text;

	public TextExpression(String text) {
		this.text = text;
	}

	public String getTemplate() {
		return text;
	}

	public String bind(Map<String,Value> variables) {
		return text;
	}

	public List<RDFEvent> pattern(Node subject) {
		return Collections.emptyList();
	}
}
