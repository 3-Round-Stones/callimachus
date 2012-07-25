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

import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * Utility class for dealing with generic types.
 * 
 * @author James Leigh
 */
public class FluidType extends GenericType {
	private final String mediaType;

	public FluidType(Type gtype, String media) {
		super(gtype);
		this.mediaType = media;
	}

	@Override
	public String toString() {
		return super.toString() + " " + mediaType;
	}

	public String getMediaType() {
		return mediaType;
	}

	public Charset getCharset() {
		Charset cs = null;
		if (getMediaType() != null && getMediaType().startsWith("text/")) {
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
		return cs;
	}

	public FluidType as(Type type) {
		return new FluidType(type, getMediaType());
	}

	public FluidType as(String mediaType) {
		return new FluidType(asType(), mediaType);
	}

	public FluidType key(String mediaType) {
		return new FluidType(key().asType(), mediaType);
	}

	public FluidType component() {
		return new FluidType(super.component().asType(), getMediaType());
	}

	public FluidType component(String mediaType) {
		return new FluidType(super.component().asType(), mediaType);
	}

}
