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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.Location;

import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.TermOrigin;

public class QuotedString implements Expression {
	private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u(\\w\\w\\w\\w)");
	private final Location location;
	private final String string;

	public QuotedString(String text, Location location) {
		assert text.charAt(0) == '"' || text.charAt(0) == '\'';
		assert text.charAt(0) == text.charAt(text.length() - 1);
		String val = text.substring(1, text.length() - 1);
		this.string = backslash(val);
		this.location = location;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String bind(ExpressionResult variables) {
		return string;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public String getTemplate() {
		return string;
	}

	@Override
	public boolean isPatternPresent() {
		return false;
	}

	@Override
	public List<RDFEvent> pattern(Node subject, TermOrigin origin, Location location) {
		return Collections.emptyList();
	}

	/**
	 * substitute escaped characters
	 */
	private String backslash(String val) {
		Matcher m = UNICODE_ESCAPE.matcher(val);
		loop: while (m.find()) {
			int unicode = 0;
			for (char c : m.group(1).toCharArray()) {
				if ((c >= '0') && (c <= '9')) {
				    unicode = (unicode << 4) + c - '0';
				}
				else if ((c >= 'a') && (c <= 'f')) {
				    unicode = (unicode << 4) + 10 + c - 'a';
				}
				else if ((c >= 'A') && (c <= 'F')) {
				    unicode = (unicode << 4) + 10 + c - 'A';
				}
				else {
				    break loop;
				}
			}
			val.replace(m.group(), Character.toString((char) unicode));
		}
		val = val.replace("\\b", "\b");
		val = val.replace("\\f", "\f");
		val = val.replace("\\n", "\n");
		val = val.replace("\\r", "\r");
		val = val.replace("\\t", "\t");
		val = val.replace("\\'", "\'");
		val = val.replace("\\\"", "\"");
		val = val.replaceAll("\\\\(.)", "$1");
		return val;
	}

}
