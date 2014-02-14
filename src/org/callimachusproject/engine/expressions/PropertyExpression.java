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
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.TermOrigin;

public class PropertyExpression implements Expression {
	private final AbsoluteTermFactory tf;
	private final Location location;
	private final String curie;
	private final IRI property;

	public PropertyExpression(String curie, NamespaceContext namespaces,
			Location location, AbsoluteTermFactory tf) throws RDFParseException {
		this.tf = tf;
		this.curie = curie;
		this.location = location;
		int idx = curie.indexOf(":");
		assert idx >= 0;
		// this may not be a curie
		if (curie.contains("://")) {
			property = tf.iri(curie);
		} else {
			String prefix = curie.substring(0, idx);
			String namespaceURI = namespaces.getNamespaceURI(prefix);
			if (namespaceURI == null)
				throw new RDFParseException("Undefined Prefix: " + prefix);
			String reference = curie.substring(idx + 1);
			property = tf.curie(namespaceURI, reference, prefix);
		}
	}

	@Override
	public String toString() {
		return property.toString();
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
		return variables.getPropertyValue(curie, location);
	}

	@Override
	public boolean isPatternPresent() {
		return true;
	}

	@Override
	public List<RDFEvent> pattern(Node subject, TermOrigin origin, Location location) {
		PlainLiteral lit = tf.literal("");
		lit.setOrigin(origin.term(location, property));
		RDFEvent triple = new Triple(subject, property, lit, location);
		return Collections.singletonList(triple);
	}

}
