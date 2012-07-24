/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011, Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.fluid;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for dealing with generic types.
 * 
 * @author James Leigh
 */
public class GenericType {
	private final Type gtype;

	public GenericType(Type genericType) {
		this.gtype = genericType;
	}

	public Class<?> getClassType() {
		return toClass(gtype);
	}

	public final Type getGenericType() {
		return gtype;
	}

	public String toString() {
		return gtype.toString();
	}

	public boolean isAnnotation() {
		return getClassType().isAnnotation();
	}

	public boolean isEnum() {
		return getClassType().isEnum();
	}

	public boolean isInterface() {
		return getClassType().isInterface();
	}

	public boolean isPrimitive() {
		return getClassType().isPrimitive();
	}

	public boolean is(Type type) {
		return toClass(type).equals(getClassType());
	}

	public boolean isComponent(Type type) {
		return toClass(type).equals(getComponentClass());
	}

	public boolean isKey(Type type) {
		return toClass(type).equals(getKeyClass());
	}

	public boolean isMap() {
		return Map.class.equals(getClassType());
	}

	public boolean isSet() {
		return Set.class.equals(getClassType());
	}

	public boolean isArray() {
		return getClassType().isArray();
	}

	public boolean isUnknown() {
		return Object.class.equals(getClassType());
	}

	public Class<?> getComponentClass() {
		return toClass(getComponentType());
	}

	public Type getComponentType() {
		if (isArray())
			return getClassType().getComponentType();
		if (gtype instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) gtype;
			Type[] args = ptype.getActualTypeArguments();
			return args[args.length - 1];
		}
		if (isSet() || isMap())
			return Object.class;
		return null;
	}

	public Class<?> getKeyClass() {
		return toClass(getKeyType());
	}

	public Type getKeyType() {
		if (isArray())
			return getClassType().getComponentType();
		if (gtype instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) gtype;
			Type[] args = ptype.getActualTypeArguments();
			return args[0];
		}
		if (isSet() || isMap())
			return Object.class;
		return null;
	}

	@Override
	public int hashCode() {
		return gtype.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericType other = (GenericType) obj;
		return gtype.equals(other.gtype);
	}

	private Class<?> toClass(Type type) {
		if (type == null)
			return null;
		if (type instanceof Class<?>)
			return (Class<?>) type;
		if (type instanceof GenericArrayType) {
			GenericArrayType atype = (GenericArrayType) type;
			Class<?> componentType = toClass(atype.getGenericComponentType());
			return Array.newInstance(toClass(componentType), 0).getClass();
		}
		if (type instanceof ParameterizedType) {
			return toClass(((ParameterizedType) type).getRawType());
		}
		return Object.class; // wildcard
	}

}
