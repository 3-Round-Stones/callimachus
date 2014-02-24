/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.script;

import java.util.Iterator;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.traits.ObjectMessage;

final class EmbeddedScript {
	private final String systemId;
	private final ScriptEngine engine;
	private final String invoke;
	private final Set<String> bindingNames;

	EmbeddedScript(ScriptEngine engine, String invoke, Set<String> bindingNames, String systemId) {
		this.engine = engine;
		this.invoke = invoke;
		this.bindingNames = bindingNames;
		this.systemId = systemId;
	}

	@Override
	public String toString() {
		return String.valueOf(systemId);
	}

	public ScriptEngine getEngine() {
		return engine;
	}

	public Object eval(ObjectMessage msg, Bindings bindings) throws ScriptException {
		Object[] args = getArguments(msg, bindings);
		try {
			return ((Invocable) engine).invokeFunction(invoke, args);
		} catch (NoSuchMethodException e) {
			throw new BehaviourException(e, String.valueOf(systemId));
		}
	}

	private Object[] getArguments(ObjectMessage msg, Bindings bindings) {
		Object[] args = new Object[bindingNames.size() + 1];
		args[0] = msg;

		Iterator<String> iter = bindingNames.iterator();
		for (int i=1; i<args.length; i++) {
			args[i] = bindings.get(iter.next());
		}
		return args;
	}
}
