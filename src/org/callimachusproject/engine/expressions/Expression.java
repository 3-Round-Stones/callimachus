package org.callimachusproject.engine.expressions;

import java.util.List;

import javax.xml.stream.Location;

import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.TermOrigin;

public interface Expression {

	Location getLocation();

	CharSequence getTemplate();

	CharSequence bind(ExpressionResult variables);

	boolean isPatternPresent();

	List<RDFEvent> pattern(Node subject, TermOrigin origin, Location location);

}