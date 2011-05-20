/*
 * Portions Copyright (c) 2011 Talis Inc, Some Rights Reserved
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
package org.callimachusproject.rdfa.events;

/**
 * SPARQL comment.
 * 
 * @author Steve Battle
 *
 */
public class Comment extends RDFEvent {
	String comment;
	public Comment(String comment) {
		super();
		this.comment = comment;
	}
	public String toString() {
		return "#"+comment+"\n";
	}
}
