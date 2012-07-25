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
package org.callimachusproject.fluid;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
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

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * Utility class for dealing with generic types.
 * 
 * @author James Leigh
 */
public class FluidType extends GenericType {
	private final String mediaType;
	private final Charset charset;

	public FluidType(String media, Class<?> ctype) {
		this(media, ctype, ctype);
	}

	public FluidType(String media, Class<?> ctype, Type gtype) {
		this(media, ctype, gtype, null);
	}

	public FluidType(String media, Class<?> ctype, Charset charset) {
		this(media, ctype, ctype, charset);
	}

	public FluidType(String media, Class<?> ctype, Type gtype, Charset charset) {
		super(gtype == null ? ctype : gtype);
		this.mediaType = media;
		Charset cs = charset;
		if (media != null && media.startsWith("text/")) {
			try {
				MimeType m = new MimeType(getMediaType());
				String name = m.getParameters().get("charset");
				if (name != null) {
					cs = Charset.forName(name);
				}
			} catch (MimeTypeParseException e) {
				// ignore
			}
		}
		this.charset = cs;
	}

	@Override
	public String toString() {
		return super.toString() + " " + mediaType;
	}

	public String getMediaType() {
		return mediaType;
	}

	public Charset getCharset() {
		return charset;
	}

	public FluidType as(String mediaType) {
		return new FluidType(mediaType, getClassType(), getGenericType(),
				getCharset());
	}

	public FluidType as(String mediaType, Charset charset) {
		return new FluidType(mediaType, getClassType(), getGenericType(),
				charset);
	}

	public FluidType as(Class<?> t) {
		return new FluidType(getMediaType(), t, t, getCharset());
	}

	public FluidType key(String mediaType) {
		FluidType kt = getKeyGenericType();
		return new FluidType(mediaType, kt.getClassType(), kt.getGenericType(),
				getCharset());
	}

	public FluidType component() {
		FluidType gtype = getComponentGenericType();
		return new FluidType(getMediaType(), gtype.getClassType(),
				gtype.getGenericType(), getCharset());
	}

	public FluidType component(String mediaType) {
		FluidType vt = getComponentGenericType();
		return new FluidType(mediaType, vt.getClassType(), vt.getGenericType(),
				getCharset());
	}

	public FluidType as(Class<?> clas, Type type) {
		return new FluidType(getMediaType(), clas, type, getCharset());
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

	public FluidType getKeyGenericType() {
		return new FluidType(getMediaType(), getKeyClass(), getKeyType(),
				getCharset());
	}

	public Object cast(Object obj) {
		if (obj == null)
			return nil();
		try {
			if (getClassType().isPrimitive())
				return obj;
			return getClassType().cast(obj);
		} catch (ClassCastException e) {
			ClassCastException cce;
			String msg = "Cannot cast " + obj + " to "
					+ getClassType().getSimpleName();
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
			FluidType keyType = getKeyGenericType();
			FluidType valueType = getComponentGenericType();
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

	private FluidType getComponentGenericType() {
		return new FluidType(getMediaType(), getComponentClass(),
				getComponentType(), getCharset());
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
