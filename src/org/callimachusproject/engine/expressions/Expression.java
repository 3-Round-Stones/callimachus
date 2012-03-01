package org.callimachusproject.engine.expressions;

import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;

import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.Node;
import org.openrdf.model.Value;

public interface Expression {

	public abstract String getTemplate();

	public abstract String bind(Map<String, Value> variables);

	public abstract List<RDFEvent> pattern(Node subject, Location location);

}