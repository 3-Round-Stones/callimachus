package org.callimachusproject.behaviours;

import java.security.SecureRandom;
import java.util.Random;

public class PasswordGenerator {
	private static final Random rnd = new SecureRandom();
	private static final int LENGTH_MIN = 6;
	private static final int LENGTH_MAX = 10;
	private static final char[] CHAR_POOL = { 'a', 'b', 'c', 'd', 'e', 'f',
			'g', 'h', 'k', 'm', 'n', 'p', 'r', 'w', 'x', 'y', 'A', 'B', 'C',
			'D', 'E', 'F', 'G', 'H', 'L', 'M', 'N', 'R', 'S', 'T', '2', '3',
			'4', '5', '6', '7', '8', '!', '^', '*', '-', '_', '=', '.' };

	public String generatePassword() {
		int length = LENGTH_MIN + rnd.nextInt(LENGTH_MAX - LENGTH_MIN + 1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; ++i) {
			sb.append(CHAR_POOL[rnd.nextInt(CHAR_POOL.length)]);
		}
		return sb.toString();
	}
}
