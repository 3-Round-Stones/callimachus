/*
   Copyright (c) 2014 3 Round Stones Inc, Some Rights Reserved

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
package org.callimachusproject.engine.impl;

import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.GraphNodePathBase;

/**
 * RDF property path.
 * 
 * @author James Leigh
 *
 */
public class GraphNodePathImpl extends GraphNodePathBase implements Node {
	private String path;

	public GraphNodePathImpl(String path) {
		assert path != null;
		this.path = path;
	}

	public String stringValue() {
		return path;
	}

	public String toString() {
		return path;
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GraphNodePathImpl other = (GraphNodePathImpl) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

}
