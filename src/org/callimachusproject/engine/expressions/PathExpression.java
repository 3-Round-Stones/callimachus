/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.engine.expressions;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.Location;

import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.impl.GraphNodePathImpl;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.TermOrigin;

public class PathExpression implements Expression {
	private final AbsoluteTermFactory tf;
	private final Location location;
	private final String path;
	private final GraphNodePathImpl node;

	public PathExpression(String path, NamespaceContext namespaces,
			Location location, AbsoluteTermFactory tf) throws RDFParseException {
		this.tf = tf;
		this.path = path;
		this.location = location;
		this.node = tf.path(path);
	}

	@Override
	public String toString() {
		return path;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String getTemplate() {
		return "";
	}

	@Override
	public String bind(ExpressionResult variables) {
		return variables.getPropertyValue(path, location);
	}

	@Override
	public boolean isPatternPresent() {
		return true;
	}

	@Override
	public List<RDFEvent> pattern(Node subject, TermOrigin origin, Location location) {
		PlainLiteral lit = tf.literal("");
		lit.setOrigin(origin.term(location, node));
		RDFEvent triple = new TriplePattern(subject, node, lit, location);
		return Collections.singletonList(triple);
	}

}
