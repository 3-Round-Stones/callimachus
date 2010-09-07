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
package org.callimachusproject.rdfa.model;

/**
 * Compact Uniform Resource Identifier (CURIE). Contains a prefix (namespace
 * map) and local reference.
 * 
 * @author James Leigh
 * 
 */
public abstract class CURIE extends IRI {

	public abstract String getNamespaceURI();

	public abstract String getReference();

	public abstract String getPrefix();

	public String stringValue() {
		return getNamespaceURI() + getReference();
	}

	public String toString() {
		return getPrefix() + ":" + getReference();
	}

}
