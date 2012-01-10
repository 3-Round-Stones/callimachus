/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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
package org.openrdf.http.object.readers;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.http.object.util.MessageType;

/**
 * Reads primitive types and their wrappers.
 * 
 * @author James Leigh
 * 
 */
public class PrimitiveBodyReader implements MessageBodyReader<Object> {
	private StringBodyReader delegate = new StringBodyReader();
	private Set<Class<?>> wrappers = new HashSet<Class<?>>();
	{
		wrappers.add(Boolean.class);
		wrappers.add(Character.class);
		wrappers.add(Byte.class);
		wrappers.add(Short.class);
		wrappers.add(Integer.class);
		wrappers.add(Long.class);
		wrappers.add(Float.class);
		wrappers.add(Double.class);
		wrappers.add(Void.class);
	}

	public boolean isReadable(MessageType mtype) {
		Class<?> type = mtype.clas();
		if (type.isPrimitive() || !type.isInterface()
				&& wrappers.contains(type))
			return delegate.isReadable(mtype.as(String.class));
		return false;
	}

	public Object readFrom(MessageType mtype, ReadableByteChannel in,
			Charset charset, String base, String location) throws IOException {
		Class<?> type = mtype.clas();
		String value = delegate.readFrom(mtype.as(String.class), in, charset,
				base, location);
		if (Boolean.TYPE.equals(type) || Boolean.class.equals(type))
			return (Boolean.valueOf(value));
		if (Character.TYPE.equals(type) || Character.class.equals(type))
			return (Character.valueOf(value.charAt(0)));
		if (Byte.TYPE.equals(type) || Byte.class.equals(type))
			return (Byte.valueOf(value));
		if (Short.TYPE.equals(type) || Short.class.equals(type))
			return (Short.valueOf(value));
		if (Integer.TYPE.equals(type) || Integer.class.equals(type))
			return (Integer.valueOf(value));
		if (Long.TYPE.equals(type) || Long.class.equals(type))
			return (Long.valueOf(value));
		if (Float.TYPE.equals(type) || Float.class.equals(type))
			return (Float.valueOf(value));
		if (Double.TYPE.equals(type) || Double.class.equals(type))
			return (Double.valueOf(value));
		return null;
	}
}
