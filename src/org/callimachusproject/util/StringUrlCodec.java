/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.util;

import java.nio.charset.Charset;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringDecoder;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.binary.Base64;

public class StringUrlCodec implements StringEncoder, StringDecoder {
	private static final Charset utf8 = Charset.forName("UTF-8");
	private static final String common = "https://-.org?_com&net=https%3A%3F%2C";
	private static final String alphabet = "%:/?#[]@!$&'()*+,;=-._~0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private final HuffmanCodec huffman;

	public StringUrlCodec() {
		this(common);
	}

	public StringUrlCodec(String sample) {
		this(new String[] { sample });
	}

	public StringUrlCodec(String[] sample) {
		StringBuilder sb = new StringBuilder(alphabet);
		for (String s : sample) {
			sb.append(s);
		}
		this.huffman = new HuffmanCodec(sb.toString().getBytes(utf8));
	}

	@Override
	public Object encode(Object decoded) throws EncoderException {
		return encode((String) decoded);
	}

	@Override
	public Object decode(Object encoded) throws DecoderException {
		return decode((String) encoded);
	}

	public String encode(String decoded) {
		byte[] encoded = huffman.encode(decoded.getBytes(utf8));
		return Base64.encodeBase64URLSafeString(encoded);
	}

	public String decode(String encoded) {
		byte[] huff = Base64.decodeBase64(encoded);
		byte[] decoded = huffman.decode(huff);
		return new String(decoded, utf8);
	}

}
