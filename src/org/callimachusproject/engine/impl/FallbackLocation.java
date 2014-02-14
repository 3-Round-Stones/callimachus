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
package org.callimachusproject.engine.impl;

import javax.xml.stream.Location;
import javax.xml.stream.events.XMLEvent;

public class FallbackLocation implements Location {

	public static Location newInstance(XMLEvent primary, XMLEvent secondary) {
		return newInstance(primary.getLocation(), secondary.getLocation());
	}

	public static Location newInstance(Location primary, Location secondary) {
		if (primary == null)
			return secondary;
		if (secondary == null)
			return primary;
		return new FallbackLocation(primary, secondary);
	}

	private final Location primary;
	private final Location secondary;

	private FallbackLocation(Location primary, Location secondary) {
		this.primary = primary;
		this.secondary = secondary;
	}

	public int getLineNumber() {
		if (primary.getLineNumber() >= 0)
			return primary.getLineNumber();
		return secondary.getLineNumber();
	}

	public int getColumnNumber() {
		if (primary.getColumnNumber() >= 0)
			return primary.getColumnNumber();
		return secondary.getColumnNumber();
	}

	public int getCharacterOffset() {
		if (primary.getCharacterOffset() >= 0)
			return primary.getCharacterOffset();
		return secondary.getCharacterOffset();
	}

	public String getPublicId() {
		if (primary.getPublicId() != null)
			return primary.getPublicId();
		return secondary.getPublicId();
	}

	public String getSystemId() {
		if (primary.getPublicId() != null)
			return primary.getPublicId();
		return secondary.getPublicId();
	}

	public String toString() {
		StringBuffer sbuffer = new StringBuffer();
		sbuffer.append("Line number = " + getLineNumber());
		sbuffer.append("\nColumn number = " + getColumnNumber());
		sbuffer.append("\nSystem Id = " + getSystemId());
		sbuffer.append("\nPublic Id = " + getPublicId());
		sbuffer.append("\nCharacterOffset = " + getCharacterOffset());
		return sbuffer.append("\n").toString();
	}
}
