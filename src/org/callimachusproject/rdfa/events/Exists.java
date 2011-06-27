package org.callimachusproject.rdfa.events;

/**
 * SPARQL keyword.
 * 
 * @author Steve Battle
 *
 */
public class Exists extends RDFEvent {

	public Exists() {
		super();
	}

	public Exists(boolean start) {
		super(start);
	}

	public String toString() {
		if (isStart()) return "EXISTS {";
		else return "}";
	}
}
