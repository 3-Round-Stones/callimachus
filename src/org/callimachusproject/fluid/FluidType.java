/*
   Copyright (c) 2012 3 Round Stones Inc, Some Rights Reserved

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
package org.callimachusproject.fluid;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Document;

/**
 * Represents a Java type and possible media types for serialisation.
 * 
 * @author James Leigh
 */
public class FluidType extends GenericType {
	private final static FluidType XML = new FluidType(Document.class,
			"application/xml", "text/xml", "image/xml", "text/xsl",
			"application/xml-external-parsed-entity", "text/xml-external-parsed-entity");
	private final TreeSet<MediaType> mediaTypes = new TreeSet<MediaType>(
			new MediaTypeComparator());

	/**
	 * Constructs a new FluidType with the given Java and media types.
	 * 
	 * @param gtype
	 *            generic Java class type
	 * @param media
	 *            If omitted it is equivalent to a wild media type. If null
	 *            there is no media type.
	 */
	public FluidType(Type gtype, String... media) {
		super(gtype);
		if (media != null && media.length == 0) {
			media = new String[] { "*/*" };
		}
		if (media == null) {
			media = new String[0];
		}
		Set<String> set = new HashSet<String>();
		for (String m : media) {
			if (m != null && !set.contains(m)) {
				MediaType mediaType = MediaType.valueOf(m);
				if (!set.contains(mediaType.toExternal())) {
					set.add(mediaType.toExternal());
					mediaTypes.add(mediaType.multiply(1.0 - mediaTypes.size() / 1000000.0));
				}
			}
		}
	}

	private FluidType(Type gtype, Collection<MediaType> media, boolean nonEmpty) {
		super(gtype);
		assert media != null;
		Set<String> set = new HashSet<String>();
		for (MediaType mediaType : media) {
			if (!set.contains(mediaType.toExternal())) {
				set.add(mediaType.toExternal());
				mediaTypes.add(mediaType.multiply(1.0 - mediaTypes.size() / 1000000.0));
			}
		}
	}

	@Override
	public String toString() {
		String list = Arrays.toString(media());
		return super.toString() + " " + list.substring(1, list.length() - 1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + mediaTypes.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FluidType other = (FluidType) obj;
		if (!mediaTypes.equals(other.mediaTypes))
			return false;
		return true;
	}

	public String[] media() {
		String[] media = new String[mediaTypes.size()];
		Iterator<MediaType> iter = mediaTypes.iterator();
		for (int i = 0; i < media.length; i++) {
			media[i] = iter.next().toExternal();
		}
		return media;
	}

	public String preferred() {
		if (mediaTypes.isEmpty())
			return null;
		for (MediaType mime : mediaTypes) {
			if (!"*".equals(mime.getPrimaryType())
					&& !"*".equals(mime.getSubType()))
				return mime.toExternal();
		}
		return null;
	}

	public Charset getCharset() {
		for (MediaType m : mediaTypes) {
			String name = m.getParameter("charset");
			if (name != null) {
				return Charset.forName(name);
			}
		}
		return null;
	}

	public boolean isText() {
		return is("text/*");
	}

	public boolean is(String... acceptable) {
		return is(new FluidType(asType(), acceptable));
	}

	public boolean is(FluidType ftype) {
		if (!is(ftype.asType()))
			return false;
		for (MediaType accept : ftype.mediaTypes) {
			for (MediaType mime : mediaTypes) {
				if (mime.match(accept)
						&& (!accept.getSubType().contains("+") || mime
								.getSubType().contains("+")))
					return true;
			}
		}
		return false;
	}

	public FluidType as(Type type) {
		return new FluidType(type, mediaTypes, true);
	}

	public FluidType as(String... acceptable) {
		if (acceptable == null)
			return new FluidType(asType(), (String[]) null);
		if (acceptable.length == 0 || mediaTypes.isEmpty())
			return this;
		return as(asType(), acceptable);
	}

	public FluidType as(Type type, String... acceptable) {
		return as(new FluidType(type, acceptable));
	}

	public FluidType as(FluidType acceptable) {
		List<MediaType> combined = new ArrayList<MediaType>(mediaTypes.size());
		for (MediaType accept : acceptable.mediaTypes) {
			for (MediaType mime : mediaTypes) {
				if (mime.match(accept)) {
					combined.add(mime.combine(accept));
				}
			}
		}
		if (combined.isEmpty()) {
			return new FluidType(acceptable.asType(), (String[]) null);
		}
		return new FluidType(acceptable.asType(), combined, true);
	}

	public boolean isXML() {
		return is(XML.as(asType()));
	}

	public FluidType asXML() {
		return as(XML.as(asType()));
	}

	public FluidType key(String... mediaType) {
		return new FluidType(key().asType(), mediaType);
	}

	public FluidType component() {
		return new FluidType(super.component().asType(), mediaTypes, true);
	}

	public FluidType component(String... mediaType) {
		return new FluidType(super.component().asType(), mediaType);
	}

	private final class MediaTypeComparator implements Comparator<MediaType> {
		public int compare(MediaType o1, MediaType o2) {
			Double q1 = o1.getQuality();
			Double q2 = o2.getQuality();
			int compare = q2.compareTo(q1);
			if (compare != 0)
				return compare;
			if (!"*".equals(o1.getPrimaryType())
					&& "*".equals(o2.getPrimaryType()))
				return -1;
			if ("*".equals(o1.getPrimaryType())
					&& !"*".equals(o2.getPrimaryType()))
				return 1;
			if (!"*".equals(o1.getSubType()) && "*".equals(o2.getSubType()))
				return -1;
			if ("*".equals(o1.getSubType()) && !"*".equals(o2.getSubType()))
				return 1;
			if (!"*".equals(o1.getSubType()) && "*".equals(o2.getSubType()))
				return -1;
			if (o1.getSubType().contains("+") && !o2.getSubType().contains("+"))
				return -1;
			if (!o1.getSubType().contains("+") && o2.getSubType().contains("+"))
				return 1;
			return o1.toExternal().compareTo(o2.toExternal());
		}
	}

}
