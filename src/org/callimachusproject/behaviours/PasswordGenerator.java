/*
 * Copyright (c) 2010, James Leigh Some rights reserved.
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
package org.callimachusproject.behaviours;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Generates a random alphanumeric string with some punctuation that can be used
 * as a password.
 * 
 * @author James Leigh
 * 
 */
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
