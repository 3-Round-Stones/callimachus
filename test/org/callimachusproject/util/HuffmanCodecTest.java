package org.callimachusproject.util;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HuffmanCodecTest extends TestCase {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testABC() throws EncoderException, DecoderException, UnsupportedEncodingException {
		byte[] alphabet = "abcdefghijklmnopqrstuvwxy ".getBytes("UTF-8");
		HuffmanCodec codec = new HuffmanCodec(alphabet);
		String string = "this is an example of a huffman encoding";
		byte[] decoded = string.getBytes("UTF-8");
		byte[] encoded = codec.encode(decoded);
		assertTrue(encoded.length < decoded.length);
		assertRoundTrip(decoded, codec);
	}

	@Test
	public void testURL() throws EncoderException, DecoderException, UnsupportedEncodingException {
		byte[] alphabet = "%:/?#[]@!$&'()*+,;=-._~0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes("UTF-8");
		HuffmanCodec codec = new HuffmanCodec(alphabet);
		String string = "this+is+an+example+of+a+huffman+encoding";
		byte[] decoded = string.getBytes("UTF-8");
		byte[] encoded = codec.encode(decoded);
		assertTrue(encoded.length < decoded.length);
		assertRoundTrip(decoded, codec);
	}

	@Test
	public void test1() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(1));
	}

	@Test
	public void test2() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(2));
	}

	@Test
	public void test4() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(4));
	}

	@Test
	public void test8() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(8));
	}

	@Test
	public void test16() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(16));
	}

	@Test
	public void test32() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(32));
	}

	@Test
	public void test64() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(64));
	}

	@Test
	public void test128() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(128));
	}

	@Test
	public void test256() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(256));
	}

	@Test
	public void test512() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(512));
	}

	@Test
	public void test1024() throws EncoderException, DecoderException {
		assertRoundTrip(randomData(1024));
	}
	
    private byte[] randomData(int size) {
        Random r = new Random();
        byte[] decoded = new byte[size];
        r.nextBytes(decoded);
        return decoded;
    }

	private void assertRoundTrip(byte[] decoded) throws EncoderException,
			DecoderException {
    	byte[] allBytes = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
		byte b = Byte.MIN_VALUE;
		for (int i = 0; i < allBytes.length; i++) {
			assert b <= Byte.MAX_VALUE;
			allBytes[i] = b++;
		}
    	HuffmanCodec codec = new HuffmanCodec(allBytes);
		assertRoundTrip(decoded, codec);
	}

	private void assertRoundTrip(byte[] decoded, HuffmanCodec codec) throws EncoderException,
			DecoderException {
		byte[] encoded = codec.encode(decoded);
		assertBinaryEquals(decoded, codec.decode(encoded));
		assertHexEquals(decoded, codec.decode(encoded));
	}

	private void assertBinaryEquals(byte[] expected, byte[] actual) {
		assertEquals(BinaryCodec.toAsciiString(expected), BinaryCodec.toAsciiString(actual));
	}

	private void assertHexEquals(byte[] expected, byte[] actual) {
		assertEquals(Hex.encodeHexString(expected), Hex.encodeHexString(actual));
	}

}
