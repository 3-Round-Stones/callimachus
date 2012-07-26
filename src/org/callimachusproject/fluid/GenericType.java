/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011, Talis Inc., Some rights reserved.
 * Copyright (c) 2012, 3 Round Stones Inc., Some rights reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
		assert genericType != null;
		this.gtype = genericType;
	}

	public Class<?> asClass() {
		return toClass(gtype);
	}

	public final Type asType() {
		return gtype;
	}

	public String toString() {
		return gtype.toString();
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

	public boolean isAnnotation() {
		return asClass().isAnnotation();
	}

	public boolean isEnum() {
		return asClass().isEnum();
	}

	public boolean isInterface() {
		return asClass().isInterface();
	}

	public boolean isPrimitive() {
		return asClass().isPrimitive();
	}

	public boolean is(Type type) {
		return toClass(type).equals(asClass());
	}

	public boolean isMap() {
		return Map.class.equals(asClass());
	}

	public boolean isSet() {
		return Set.class.equals(asClass());
	}

	public boolean isArray() {
		return asClass().isArray();
	}

	public boolean isUnknown() {
		return Object.class.equals(asClass());
	}

	public boolean isSetOrArray() {
		return isSet() || isArray();
	}

	public boolean isSetOrArrayOf(Type componentType) {
		return isSetOf(componentType) || isArrayOf(componentType);
	}

	public boolean isOrIsSetOf(Type type) {
		if (is(type))
			return true;
		if (!isSet())
			return false;
		return component().is(type);
	}

	public GenericType key() {
		if (isArray())
			return new GenericType(Integer.TYPE);
		if (gtype instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) gtype;
			Type[] args = ptype.getActualTypeArguments();
			return new GenericType(args[0]);
		}
		return new GenericType(Object.class);
	}

	public GenericType component() {
		if (isArray())
			return new GenericType(asClass().getComponentType());
		if (gtype instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) gtype;
			Type[] args = ptype.getActualTypeArguments();
			return new GenericType(args[args.length - 1]);
		}
		return new GenericType(Object.class);
	}

	public Object cast(Object obj) {
		if (obj == null)
			return nil();
		try {
			if (asClass().isPrimitive())
				return obj;
			return asClass().cast(obj);
		} catch (ClassCastException e) {
			ClassCastException cce;
			String msg = "Cannot cast " + obj + " to "
					+ asClass().getSimpleName();
			cce = new ClassCastException(msg);
			cce.initCause(e);
			throw cce;
		}
	}

	public Object castComponent(Object obj) {
		if (obj == null)
			return nil();
		if (isSet()) {
			return cast(Collections.singleton(obj));
		} else if (isArray()) {
			Object result = Array.newInstance(component().asClass(), 1);
			Array.set(result, 0, obj);
			return cast(result);
		}
		return cast(obj);
	}

	public Object castSet(Set<?> set) {
		return castCollection(set);
	}

	public Object castArray(Object ar) {
		if (ar == null || Array.getLength(ar) == 0)
			return nil();
		if (isSet()) {
			int len = Array.getLength(ar);
			Set<Object> set = new HashSet<Object>(len);
			for (int i = 0; i < len; i++) {
				set.add(Array.get(ar, i));
			}
			return cast(set);
		} else if (isArray()) {
			return cast(ar);
		}
		return cast(Array.get(ar, 0));
	}

	public Object castMap(Map<Object, Collection<Object>> map) {
		if (map == null || map.isEmpty())
			return nil();
		if (isMap()) {
			GenericType keyType = key();
			GenericType valueType = component();
			Map<Object, Object> result = new LinkedHashMap<Object, Object>();
			for (Map.Entry<Object, Collection<Object>> e : map.entrySet()) {
				Object key = keyType.cast(e.getKey());
				Object value = valueType.castCollection(e.getValue());
				result.put(key, value);
			}
			return cast(result);
		}
		List<Object> list = new ArrayList<Object>();
		for (Map.Entry<Object, Collection<Object>> e : map.entrySet()) {
			list.addAll(e.getValue());
		}
		return castCollection(list);
	}

	public Iterator<?> iteratorOf(Object obj) {
		if (isSet())
			return ((Set<?>) obj).iterator();
		if (isArray()) {
			int len = Array.getLength(obj);
			List<Object> list = new ArrayList<Object>(len);
			for (int i = 0; i < len; i++) {
				list.add(Array.get(obj, i));
			}
			return list.iterator();
		}
		return Collections.singleton(obj).iterator();
	}

	private boolean isSetOf(Type componentType) {
		if (!isSet())
			return false;
		return component().is(componentType);
	}

	private boolean isArrayOf(Type componentType) {
		if (!isArray())
			return false;
		return component().is(componentType);
	}

	private Object castCollection(Collection<?> list) {
		if (list == null || list.isEmpty())
			return nil();
		if (isSet()) {
			if (list instanceof Set)
				return cast(list);
			return cast(new LinkedHashSet<Object>(list));
		} else if (isArray()) {
			int len = list.size();
			Object result = Array.newInstance(component().asClass(), len);
			Iterator<?> iter = list.iterator();
			for (int i = 0; i < len; i++) {
				Array.set(result, i, iter.next());
			}
			return cast(result);
		}
		Iterator<?> iter = list.iterator();
		return cast(iter.next());
	}

	private Object nil() {
		if (isSet())
			return cast(Collections.emptySet());
		if (isArray())
			return cast(Array.newInstance(component().asClass(), 0));
		if (isMap())
			return cast(Collections.emptyMap());
		return null;
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
