/*
   Copyright 2009 Zepheira LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.rdfa;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * RDF parse exception.
 * 
 * @author James Leigh
 *
 */
public class RDFParseException extends Exception {
	private static final long serialVersionUID = 8814297372447048808L;
	protected Location location;

	public RDFParseException() {
		super();
	}

	public RDFParseException(String msg) {
		super(msg);
	}

	public RDFParseException(Throwable cause) {
		super(cause);
	}

	public RDFParseException(XMLStreamException cause) {
		super(cause);
		this.location = cause.getLocation();
	}

	public RDFParseException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public RDFParseException(String msg, Location location, Throwable cause) {
		super("ParseError at [row,col]:[" + location.getLineNumber() + ","
				+ location.getColumnNumber() + "]\n" + "Message: " + msg, cause);
		this.location = location;
	}

	public RDFParseException(String msg, Location location) {
		super("ParseError at [row,col]:[" + location.getLineNumber() + ","
				+ location.getColumnNumber() + "]\n" + "Message: " + msg);
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

}
