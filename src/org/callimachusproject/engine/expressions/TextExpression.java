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
