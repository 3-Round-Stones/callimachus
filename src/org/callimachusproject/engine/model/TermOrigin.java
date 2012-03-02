package org.callimachusproject.engine.model;

public class TermOrigin {
	
	// The TEXT_CONTENT signifier is required to distinguish content from property expressions
	// where the same property is used for both in the same element. 
	// The content @origin ends with '!', while the property expression ends with the property
	// This is not used where content is assigned a variable.
	public static final String TEXT_CONTENT = "!";
	
	// The BLANK signifies blank nodes introduced by typeof
	public static final String BLANK = "_";
	private final String string;

	public TermOrigin() {
		this.string = "";
	}

	private TermOrigin(TermOrigin xptr, Term term) {
		this.string = xptr.getString() + " " + term.toString();
	}

	private TermOrigin(TermOrigin xptr, String string) {
		this.string = xptr.getString() + " " + string;
	}

	private TermOrigin(TermOrigin parent, Integer next) {
		this.string = parent.getString() + "/" + next;
	}

	public String getString() {
		return string;
	}

	public String toString() {
		return string;
	}

	public TermOrigin textContent() {
		return new TermOrigin(this, TEXT_CONTENT);
	}

	public TermOrigin slash(Integer next) {
		return new TermOrigin(this, next);
	}

	public TermOrigin term(Node c) {
		return new TermOrigin(this, c);
	}

	public TermOrigin blank() {
		return new TermOrigin(this, BLANK);
	}

}
