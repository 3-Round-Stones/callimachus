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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
			"application/xml-external-parsed-entity");
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
		for (String m : media) {
			if (m != null) {
				MediaType mediaType = MediaType.valueOf(m);
				mediaTypes.add(mediaType.multiply(1.0 - mediaTypes.size() / 100));
			}
		}
	}

	private FluidType(Type gtype, Collection<MediaType> media, boolean nonEmpty) {
		super(gtype);
		assert media != null && !media.isEmpty();
		for (MediaType mediaType : media) {
			mediaTypes.add(mediaType.multiply(1.0 - mediaTypes.size() / 1000.0));
		}
	}

	@Override
	public String toString() {
		String list = mediaTypes.toString();
		return super.toString() + " " + list.substring(1, list.length() - 1);
	}

	public String[] media() {
		String[] media = new String[mediaTypes.size()];
		Iterator<MediaType> iter = mediaTypes.iterator();
		for (int i = 0; i < media.length; i++) {
			media[i] = iter.next().toString();
		}
		return media;
	}

	public String preferred() {
		if (mediaTypes.isEmpty())
			return null;
		for (MediaType mime : mediaTypes) {
			if (!"*".equals(mime.getPrimaryType())
					&& !"*".equals(mime.getSubType()))
				return mime.toString();
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
		if (acceptable == null)
			return false;
		if (acceptable.length == 0)
			return true;
		for (String a : acceptable) {
			if (a != null) {
				MediaType accept = MediaType.valueOf(a);
				for (MediaType mime : mediaTypes) {
					if (mime.match(accept))
						return true;
				}
			}
		}
		return false;
	}

	public boolean is(FluidType ftype) {
		if (!is(ftype.asType()))
			return false;
		for (MediaType accept : ftype.mediaTypes) {
			for (MediaType mime : mediaTypes) {
				if (mime.match(accept))
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
			return o1.toString().compareTo(o2.toString());
		}
	}

}
