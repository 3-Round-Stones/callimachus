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
package org.callimachusproject.engine.impl;

import org.callimachusproject.engine.model.Var;

/**
 * SPARQL variable.
 * 
 * @author James Leigh
 *
 */
public class VarImpl extends Var {
	private final String prefix;
	private final String name;

	public VarImpl(String name) {
		this('?', name);
	}

	public VarImpl(char prefix, String name) {
		assert prefix == '?' || prefix == '$';
		assert name != null;
		this.prefix = Character.toString(prefix);
		this.name = name;
	}

	@Override
	public String prefix() {
		return prefix;
	}

	@Override
	public String stringValue() {
		return name;
	}

}
