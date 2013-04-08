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
