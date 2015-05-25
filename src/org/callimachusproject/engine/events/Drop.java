/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved

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
package org.callimachusproject.engine.events;

import java.util.Arrays;

import javax.xml.stream.Location;

import org.callimachusproject.engine.model.IRI;

/**
 * SPARQL keyword.
 * 
 * @author James Leigh
 *
 */
public class Drop extends RDFEvent {
	private final boolean silent;
	private final String named;
	private final IRI graph;

	public Drop(boolean silent, IRI graph, Location location) {
		super(location);
		this.silent = silent;
		this.named = "GRAPH";
		this.graph = graph;
	}

	public Drop(boolean silent, String named, Location location) {
		super(location);
		assert Arrays.asList("DEFAULT", "NAMED", "ALL").contains(named.toUpperCase());
		this.silent = silent;
		this.named = named;
		this.graph = null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DROP ");
		if (silent) {
			sb.append("SILENT ");
		}
		sb.append(named);
		if (graph != null) {
			sb.append(" ").append(graph);
		}
		return sb.append(";").toString();
	}
}
