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
package org.callimachusproject.repository.trace;

import java.util.HashMap;
import java.util.Map;

public class CommonTrace extends AbstractTrace {
	private Map<String, Integer> variables;
	private final int hash;

	public CommonTrace(CommonTrace previousTrace, Class<?> returnType,
			String methodName, Class<?>[] types, Object... args) {
		super(previousTrace, returnType, methodName, types, args);
		hash = computeHashCode();
	}

	@Override
	public CommonTrace getPreviousTrace() {
		return (CommonTrace) super.getPreviousTrace();
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommonTrace other = (CommonTrace) obj;
		if (hash != other.hash)
			return false;
		if (!getAssignments().equals(other.getAssignments()))
			return false;
		if (!getMethodName().equals(other.getMethodName()))
			return false;
		if (!getParameters().equals(other.getParameters()))
			return false;
		if (!getReturnType().equals(other.getReturnType()))
			return false;
		if (getReturnVariable() == null) {
			if (other.getReturnVariable() != null)
				return false;
		} else if (!getReturnVariable().equals(other.getReturnVariable()))
			return false;
		if (getPreviousTrace() == null) {
			if (other.getPreviousTrace() != null)
				return false;
		} else if (!getPreviousTrace().equals(other.getPreviousTrace()))
			return false;
		return true;
	}

	@Override
	protected synchronized String getVariableSuffix(String name) {
		if (variables == null) {
			variables = new HashMap<String, Integer>();
		}
		Integer count = variables.get(name);
		if (count == null) {
			variables.put(name, count = -1);
			return "";
		} else {
			variables.put(name, count += 1);
			return Character.toString((char)('A' + count));
		}
	}

	@Override
	protected String getReturnVariableOf(MethodCall trace) {
		return trace.getTraceAggregate().getReturnVariable();
	}

	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getAssignments().hashCode();
		result = prime * result + getMethodName().hashCode();
		result = prime * result + getParameters().hashCode();
		result = prime * result + getReturnType().hashCode();
		result = prime
				* result
				+ ((getReturnVariable() == null) ? 0 : getReturnVariable()
						.hashCode());
		result = prime
				* result
				+ ((getPreviousTrace() == null) ? 0 : getPreviousTrace()
						.hashCode());
		return result;
	}

}
