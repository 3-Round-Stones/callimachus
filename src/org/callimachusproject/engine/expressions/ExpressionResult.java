package org.callimachusproject.engine.expressions;

import javax.xml.stream.Location;

public interface ExpressionResult {

	String getVariable(String name, Location location);

	String getPropertyValue(String property, Location location);
}
