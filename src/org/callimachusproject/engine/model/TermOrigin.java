package org.callimachusproject.engine.model;

import java.util.regex.Pattern;

import javax.xml.stream.Location;

public class TermOrigin {

	private final String xptr;
	private final String property;
	// The BLANK signifies blank nodes introduced by typeof
	private final boolean blank;
	// The TEXT_CONTENT signifier is required to distinguish content from property expressions
	// where the same property is used for both in the same element.
	// This is not used where content is assigned a variable.
	private final boolean text;
	private final boolean anonymous;
	private final Location location;

	public TermOrigin(Location location) {
		assert location != null;
		assert location.getCharacterOffset() >= 0;
		this.xptr = "";
		this.property = null;
		this.blank = false;
		this.text = false;
		this.anonymous = false;
		this.location = location;
	}

	private TermOrigin(Location location, String string, String property, boolean blank, boolean text) {
		assert location != null;
		assert location.getCharacterOffset() >= 0;
		this.xptr = string;
		this.property = property;
		this.blank = blank;
		this.text = text;
		this.anonymous = false;
		this.location = location;
	}

	private TermOrigin(Location location, String string, String property, boolean blank, boolean text, boolean anonymous) {
		assert location != null;
		assert location.getCharacterOffset() >= 0;
		this.xptr = string;
		this.property = property;
		this.blank = blank;
		this.text = text;
		this.anonymous = anonymous;
		this.location = location;
	}

	public String toString() {
		return location.toString();
	}

	public Location getLocation() {
		return location;
	}

	public boolean hasLocation(Location location) {
		return this.location.getCharacterOffset() == location.getCharacterOffset();
	}

	public TermOrigin textContent(Location location) {
		return new TermOrigin(location, xptr, property, false, true);
	}

	public TermOrigin slash(Integer next) {
		return new TermOrigin(location, xptr + "/" + next, property, blank, text);
	}

	public TermOrigin term(Location location, Node term) {
		return new TermOrigin(location, xptr, term.toString(), blank, text);
	}

	public TermOrigin blank() {
		return new TermOrigin(location, xptr, property, true, text);
	}

	public boolean pathEquals(String path) {
		return path.equals(this.xptr);
	}

	public boolean propertyEquals(String curie) {
		return curie.equals(property);
	}

	public boolean startsWith(String path) {
		return this.xptr.startsWith(path);
	}

	public boolean isPropertyPresent() {
		return property != null;
	}

	public String getPath() {
		return xptr;
	}

	public boolean isTextContent() {
		return text;
	}

	public boolean isBlankNode() {
		return blank;
	}

	public boolean startsWith(TermOrigin y) {
		return this.xptr.startsWith(y.xptr);
	}

	public boolean isAnchor() {
		if (text || blank)
			return false;
		return Pattern.compile("^(/\\d+){2}$").matcher(this.xptr).matches();
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public TermOrigin anonymous() {
		return new TermOrigin(location, xptr, property, blank, text, true);
	}

}
