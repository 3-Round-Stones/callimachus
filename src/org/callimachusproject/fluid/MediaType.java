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

import java.io.Serializable;
import java.util.Enumeration;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * Represents a single media type.
 */
public class MediaType implements Serializable {
	private static final long serialVersionUID = 2653020125056041502L;
	private static final MediaType WILD;
	static {
		try {
			WILD = new MediaType("*/*", new MimeType("*/*"), new MediaType("*/*", new MimeType("*/*")).getQuality());
		} catch (MimeTypeParseException e) {
			throw new AssertionError(e);
		}
	}

	public static MediaType valueOf(String mediaType)
			throws IllegalArgumentException {
		try {
			if (mediaType == null || mediaType.equals("*/*")
					|| mediaType.equals("*")) {
				return WILD;
			}
			if (mediaType.indexOf('/') < 0) {
				int dash = mediaType.indexOf('-');
				if (dash >= 0) {
					String primary = mediaType.substring(0, dash);
					String rest = mediaType.substring(dash + 1);
					String lexical = primary + "/" + rest;
					return new MediaType(lexical, new MimeType(lexical));
				}
				int colon = mediaType.indexOf(';');
				if (colon > 0) {
					String primary = mediaType.substring(0, colon);
					String param = mediaType.substring(colon);
					String lexical = primary + "/*" + param;
					return new MediaType(lexical, new MimeType(lexical));
				}
				String lexical = mediaType + "/*";
				return new MediaType(lexical, new MimeType(mediaType, "*"));
			}
			return new MediaType(mediaType, new MimeType(mediaType));
		} catch (MimeTypeParseException e) {
			throw new IllegalArgumentException(e.getMessage() + ": "
					+ mediaType);
		}
	}

	private final String normal;
	private final double quality;
	private final MimeType parsed;

	private MediaType(String media, MimeType mime) {
		assert mime != null;
		String q = mime.getParameter("q");
		mime.removeParameter("q");
		double quality = q == null ? 1 : Double.valueOf(q);
		if ("*".equals(mime.getPrimaryType())) {
			quality *= 0.5;
		}
		if ("*".equals(mime.getSubType())) {
			quality *= 0.2;
		}
		if (!mime.getSubType().contains("+")) {
			quality *= 0.99999;
		}
		this.quality = quality;
		this.parsed = mime;
		String lexical = mime.toString();
		this.normal = lexical.equals(media) ? media : lexical;
	}

	private MediaType(String media, MimeType mime, double quality) {
		this.quality = quality;
		this.parsed = mime;
		this.normal = media;
	}

	public String toString() {
		if (quality == 1.0)
			return toExternal();
		return normal + ";q=" + quality;
	}

	public String toExternal() {
		return normal;
	}

	public int hashCode() {
		return normal.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MediaType other = (MediaType) obj;
		if (!normal.equals(other.normal))
			return false;
		if (getQuality() != other.getQuality())
			return false;
		return true;
	}

	public String getPrimaryType() {
		return parsed.getPrimaryType();
	}

	public String getSubType() {
		return parsed.getSubType();
	}

	public String getParameter(String name) {
		return parsed.getParameter(name);
	}

	public String getBaseType() {
		return parsed.getBaseType();
	}

	public double getQuality() {
		return quality;
	}

	public MediaType multiply(double multiplier) { 
		return new MediaType(normal, parsed, quality * multiplier);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getParameterNames() {
		return parsed.getParameters().getNames();
	}

	public MediaType combine(MediaType accept) {
		assert accept != null;
		if (toExternal().equals(accept.toExternal()) && accept.getQuality() == 1.0)
			return this;
		if (toExternal().equals(accept.toExternal()))
			return multiply(accept.getQuality());
		try {
			MimeType mime = new MimeType(normal);
			if ("*".equals(mime.getPrimaryType())) {
				mime.setPrimaryType(accept.getPrimaryType());
			}
			if ("*".equals(mime.getSubType())) {
				mime.setSubType(accept.getSubType());
			}
			Enumeration<String> e = accept.getParameterNames();
			while (e.hasMoreElements()) {
				String p = e.nextElement();
				assert !"q".equals(p);
				if (mime.getParameter(p) == null) {
					mime.setParameter(p, accept.getParameter(p));
				}
			}
			return new MediaType(mime.toString(), mime, getQuality()
					* accept.getQuality());
		} catch (MimeTypeParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public boolean match(String rawdata) throws IllegalArgumentException {
		return match(valueOf(rawdata));
	}

	public boolean match(MediaType accept) {
		if (accept == null)
			return false;
		if (equals(accept))
			return true;
		if (parsed.match(accept.parsed))
			return isParametersAcceptable(accept);
		if ("*".equals(getPrimaryType()))
			return isSubTypeAcceptable(accept);
		if ("*".equals(accept.getPrimaryType()))
			return isSubTypeAcceptable(accept);
		if (!getPrimaryType().equals(accept.getPrimaryType()))
			return false;
		return isSubTypeAcceptable(accept);
	}

	private boolean isSubTypeAcceptable(MediaType accept) {
		if ("*".equals(getSubType()))
			return isParametersAcceptable(accept);
		if ("*".equals(accept.getSubType()))
			return isParametersAcceptable(accept);
		if (getSubType().endsWith("+" + accept.getSubType()))
			return isParametersAcceptable(accept);
		if (accept.getSubType().endsWith("+" + getSubType()))
			return isParametersAcceptable(accept);
		return false;
	}

	private boolean isParametersAcceptable(MediaType accept) {
		Enumeration<String> names = accept.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if ("q".equals(name))
				continue;
			if (getParameter(name) == null)
				continue;
			if (!accept.getParameter(name).equals(getParameter(name)))
				return false;
		}
		return true;
	}
}
