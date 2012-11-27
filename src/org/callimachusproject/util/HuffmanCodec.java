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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;

public class HuffmanCodec implements BinaryEncoder, BinaryDecoder {
	private final int[] frequency;
	private final Node root;
	private final Map<Byte, List<Boolean>> table;

	public HuffmanCodec(byte[] alphabet) {
		int length = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;
		frequency = new int[length];
		for (int i = 0; i < alphabet.length; i++) {
			frequency[unsigned(alphabet[i])]++;
		}
		TreeSet<Node> priorityQueue = new TreeSet<Node>();
		priorityQueue.add(new Node()); // end-of-transmission
		byte b = Byte.MIN_VALUE;
		for (int i = 0; i < length; i++) {
			// include unused bytes
			priorityQueue.add(new Node(b++));
		}
		// merge two smallest trees
		while (priorityQueue.size() > 1) {
			Node left = priorityQueue.pollFirst();
			Node right = priorityQueue.pollFirst();
			Node parent = new Node(left, right);
			priorityQueue.add(parent);
		}
		root = priorityQueue.pollFirst();
		table = new HashMap<Byte, List<Boolean>>(length + 1); // plus eot
		buildLookupTable(table, root, new ArrayList<Boolean>());
	}

	@Override
	public Object decode(Object encoded) throws DecoderException {
		return decode((byte[]) encoded);
	}

	@Override
	public Object encode(Object decoded) throws EncoderException {
		return encode((byte[]) decoded);
	}

	public byte[] encode(byte[] decoded) {
		List<Boolean> bits = new ArrayList<Boolean>(decoded.length * 8);
		for (int i = 0; i < decoded.length; i++) {
			bits.addAll(table.get(decoded[i]));
		}
		bits.addAll(table.get(null)); // eot
		return toByteArray(bits);
	}

	public byte[] decode(byte[] encoded) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		List<Boolean> bits = toBooleanList(encoded);
		for (int i = 0, n = bits.size(); i < n;) {
			Node x = root;
			while (!x.isSymbol() && !x.isEndOfTransmission()) {
				boolean bit = bits.get(i++);
				if (bit)
					x = x.right;
				else
					x = x.left;
			}
			if (x.isEndOfTransmission())
				break;
			out.write(x.symbol);
		}
		return out.toByteArray();
	}

	private int unsigned(byte b) {
		return b & 0xFF;
	}

	private void buildLookupTable(Map<Byte, List<Boolean>> table, Node symbols,
			List<Boolean> encodings) {
		if (symbols.isEndOfTransmission()) {
			table.put(null, encodings); // eot
		} else if (symbols.isSymbol()) {
			table.put(symbols.symbol, encodings);
		} else {
			// left side bit is clear
			List<Boolean> left = new ArrayList<Boolean>(encodings.size() + 1);
			left.addAll(encodings);
			left.add(false);
			buildLookupTable(table, symbols.left, left);
			// right side bit is set
			List<Boolean> right = new ArrayList<Boolean>(encodings.size() + 1);
			right.addAll(encodings);
			right.add(true);
			buildLookupTable(table, symbols.right, right);
		}
	}

	private List<Boolean> toBooleanList(byte[] raw) {
		final int[] positions = new int[] { 0x80, 0x40, 0x20, 0x10, 0x08, 0x04,
				0x02, 0x01 };
		if (raw == null || raw.length == 0) {
			return Collections.emptyList();
		}
		Boolean[] bits = new Boolean[raw.length * Byte.SIZE];
		for (int r = 0, b = 0; r < raw.length; r++) {
			for (int bit : positions) {
				bits[b++] = (raw[r] & bit) != 0;
			}
		}
		return Arrays.asList(bits);
	}

	private byte[] toByteArray(List<Boolean> booleans) {
		final int[] positions = new int[] { 0x80, 0x40, 0x20, 0x10, 0x08, 0x04,
				0x02, 0x01 };
		if (booleans == null || booleans.isEmpty()) {
			return new byte[0];
		}
		Boolean[] bits = booleans.toArray(new Boolean[booleans.size()]);
		byte[] raw = new byte[(bits.length + Byte.SIZE - 1) / Byte.SIZE];
		for (int r = 0, b = 0; r < raw.length; r++) {
			for (int bit : positions) {
				if (b < bits.length && bits[b++]) {
					raw[r] |= bit;
				}
			}
		}
		return raw;
	}

	/**
	 * Compared based on frequency
	 */
	private class Node implements Comparable<Node> {
		private final boolean eot;
		private final byte symbol;
		private final Node left, right;

		public Node() {
			this.eot = true;
			this.symbol = 0;
			this.left = null;
			this.right = null;
		}

		public Node(byte ch) {
			this.eot = false;
			this.symbol = ch;
			this.left = null;
			this.right = null;
		}

		public Node(Node left, Node right) {
			assert left != null && right != null;
			this.eot = false;
			this.symbol = 0;
			this.left = left;
			this.right = right;
		}

		public boolean isEndOfTransmission() {
			return eot;
		}

		public boolean isSymbol() {
			return left == null && right == null && !eot;
		}

		public int getFrequency() {
			if (isEndOfTransmission()) {
				return 1;
			} else if (isSymbol()) {
				return frequency[unsigned(symbol)];
			} else {
				return left.getFrequency() + right.getFrequency();
			}
		}

		public int getDepth() {
			if (isEndOfTransmission()) {
				return 0;
			} else if (isSymbol()) {
				return 0;
			} else {
				return Math.max(left.getDepth(), right.getDepth());
			}
		}

		public int getPosition() {
			if (isEndOfTransmission()) {
				return -1;
			} else if (isSymbol()) {
				return unsigned(symbol);
			} else {
				return left.getPosition();
			}
		}

		public int compareTo(Node that) {
			int cmp = this.getFrequency() - that.getFrequency();
			if (cmp != 0)
				return cmp;
			cmp = this.getDepth() - that.getDepth();
			if (cmp != 0)
				return cmp;
			return this.getPosition() - that.getPosition();
		}
	}
}
