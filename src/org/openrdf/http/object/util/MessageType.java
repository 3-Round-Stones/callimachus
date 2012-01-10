/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.http.object.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
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

import org.openrdf.annotations.Iri;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.util.GenericType;

/**
 * Utility class for dealing with generic types.
 * 
 * @author James Leigh
 */
public class MessageType extends GenericType {
	private String mimeType;
	private ObjectConnection con;

	@Deprecated
	public MessageType(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		this(mimeType, type, genericType, con);
	}

	public MessageType(String media, Class<?> ptype, Type gtype,
			ObjectConnection con) {
		super(gtype == null ? ptype : gtype);
		assert con != null;
		this.mimeType = media;
		this.con = con;
	}

	public String getMimeType() {
		return mimeType;
	}

	public ObjectConnection getObjectConnection() {
		return con;
	}

	public ObjectFactory getObjectFactory() {
		return con.getObjectFactory();
	}

	public ValueFactory getValueFactory() {
		return con.getValueFactory();
	}

	public Class<?> clas() {
		return getClassType();
	}

	public Type type() {
		return getGenericType();
	}

	public boolean isConcept(Class<?> component) {
		for (Annotation ann : component.getAnnotations()) {
			for (Method m : ann.annotationType().getDeclaredMethods()) {
				if (m.isAnnotationPresent(Iri.class))
					return true;
			}
		}
		return getObjectFactory().isNamedConcept(component);
	}

	public boolean isDatatype(Class<?> type2) {
		return getObjectFactory().isDatatype(type2);
	}

	public MessageType as(Class<?> t) {
		return new MessageType(mimeType, t, t, con);
	}

	public MessageType key(String mimetype) {
		MessageType kt = getKeyGenericType();
		return new MessageType(mimetype, kt.clas(), kt.type(), con);
	}

	public MessageType component() {
		MessageType gtype = getComponentGenericType();
		return new MessageType(mimeType, gtype.clas(), gtype.type(), con);
	}

	public MessageType component(String mimetype) {
		MessageType vt = getComponentGenericType();
		return new MessageType(mimetype, vt.clas(), vt.type(), con);
	}

	public MessageType as(String mimetype) {
		return new MessageType(mimeType, clas(), type(), con);
	}

	public MessageType as(Class<?> clas, Type type) {
		return new MessageType(mimeType, clas, type, con);
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
		return type.equals(getComponentType());
	}

	public MessageType getKeyGenericType() {
		return new MessageType(mimeType, getKeyClass(), getKeyType(), con);
	}

	public Object cast(Object obj) {
		if (obj == null)
			return nil();
		try {
			if (clas().isPrimitive())
				return obj;
			return clas().cast(obj);
		} catch (ClassCastException e) {
			ClassCastException cce;
			String msg = "Cannot cast " + obj + " to " + clas().getSimpleName();
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
			Object result = Array.newInstance(getComponentClass(), 1);
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
			Set set = new HashSet(len);
			for (int i = 0; i < len; i++) {
				set.add(Array.get(ar, i));
			}
			return cast(set);
		} else if (isArray()) {
			return cast(ar);
		}
		return cast(Array.get(ar, 0));
	}

	public Object castMap(Map<?, Collection<?>> map) {
		if (map == null || map.isEmpty())
			return nil();
		if (isMap()) {
			MessageType keyType = getKeyGenericType();
			MessageType valueType = getComponentGenericType();
			Map result = new LinkedHashMap();
			for (Map.Entry<?, Collection<?>> e : map.entrySet()) {
				Object key = keyType.cast(e.getKey());
				Object value = valueType.castCollection(e.getValue());
				result.put(key, value);
			}
			return cast(result);
		}
		List list = new ArrayList();
		for (Map.Entry<?, Collection<?>> e : map.entrySet()) {
			list.addAll(e.getValue());
		}
		return castCollection(list);
	}

	public Iterator<?> iteratorOf(Object obj) {
		if (isSet())
			return ((Set) obj).iterator();
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
		return componentType.equals(getComponentType());
	}

	private boolean isArrayOf(Type componentType) {
		if (!isArray())
			return false;
		return componentType.equals(getComponentType());
	}

	private MessageType getComponentGenericType() {
		return new MessageType(mimeType, getComponentClass(),
				getComponentType(), con);
	}

	private Object nil() {
		if (isSet())
			return cast(Collections.emptySet());
		if (isArray())
			return cast(Array.newInstance(getComponentClass(), 0));
		if (isMap())
			return cast(Collections.emptyMap());
		return null;
	}

	private Object castCollection(Collection<?> list) {
		if (list == null || list.isEmpty())
			return nil();
		if (isSet()) {
			if (list instanceof Set)
				return cast(list);
			return cast(new LinkedHashSet(list));
		} else if (isArray()) {
			int len = list.size();
			Object result = Array.newInstance(getComponentClass(), len);
			Iterator iter = list.iterator();
			for (int i = 0; i < len; i++) {
				Array.set(result, i, iter.next());
			}
			return cast(result);
		}
		Iterator<?> iter = list.iterator();
		return cast(iter.next());
	}

}
