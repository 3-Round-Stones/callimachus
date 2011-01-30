/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

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
package org.callimachusproject.stream;

import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

/**
 * Overrides the namespaces in a {@link StartElement}.
 * 
 * @author James Leigh
 *
 */
public class NamespaceStartElement extends DelegatingStartElement {
	private Map<String, Namespace> namespaces;

	public NamespaceStartElement(StartElement element,
			Map<String, Namespace> namespaces) {
		super(element);
		this.namespaces = namespaces;
	}

	public Iterator<Namespace> getNamespaces() {
		return namespaces.values().iterator();
	}

	public String getNamespaceURI(String prefix) {
		if (namespaces.containsKey(prefix))
			return namespaces.get(prefix).getValue();
		return null;
	}

}
