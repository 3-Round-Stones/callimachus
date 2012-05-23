package org.callimachusproject.script;

import java.util.Iterator;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.traits.MessageContext;

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

	public ScriptEngine getEngine() {
		return engine;
	}

	public Object eval(MessageContext msg, Bindings bindings) throws ScriptException {
		Object[] args = getArguments(msg, bindings);
		try {
			return ((Invocable) engine).invokeFunction(invoke, args);
		} catch (NoSuchMethodException e) {
			throw new BehaviourException(e, String.valueOf(systemId));
		}
	}

	private Object[] getArguments(Object msg, Bindings bindings) {
		Object[] args = new Object[bindingNames.size() + 1];
		args[0] = msg;

		Iterator<String> iter = bindingNames.iterator();
		for (int i=1; i<args.length; i++) {
			args[i] = bindings.get(iter.next());
		}
		return args;
	}
}