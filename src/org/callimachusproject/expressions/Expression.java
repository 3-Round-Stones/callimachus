package org.callimachusproject.expressions;

import java.util.List;
import java.util.Map;

import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.model.Node;
import org.openrdf.model.Value;

public interface Expression {

	public abstract String getTemplate();

	public abstract String bind(Map<String, Value> variables);

	public abstract List<RDFEvent> pattern(Node subject);

}