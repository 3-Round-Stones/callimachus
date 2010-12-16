/*
 * Copyright (c) 2010, Zepheira LLC, James Leigh Some rights reserved.
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
package org.callimachusproject.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.callimachusproject.Server;

/**
 * Formats the log messages onto a single line (plus stack trace).
 * 
 * @author James Leigh
 *
 */
public class LogMessageFormatter extends Formatter {
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	private static final String newline = System
			.getProperty("line.separator");
	private long nextDateAt;
	private long minuteExpire;
	private String minute;

	public LogMessageFormatter() {
		advanceNextDate(System.currentTimeMillis());
	}

	@Override
	public String getHead(Handler h) {
		long now = System.currentTimeMillis();
		StringBuilder sb = new StringBuilder(128);
		sb.append("#Software: ").append(Server.NAME).append(newline);
		appendDateString(sb.append("#Date: "), now).append(newline);
		return sb.toString();
	}

	@Override
	public String getTail(Handler h) {
		long now = System.currentTimeMillis();
		StringBuilder sb = new StringBuilder(32);
		appendDateString(sb.append("#Date: "), now).append(newline);
		return sb.toString();
	}

	public String format(LogRecord record) {
		long now = record.getMillis();
		StringBuilder sb = new StringBuilder(256);
		String message = formatMessage(record);
		if (now >= nextDateAt) {
			synchronized (this) {
				if (now >= nextDateAt) {
					appendDateString(sb.append("#Date: "), now).append(newline);
				}
			}
		}
		sb.append(getTimeString(now)).append('\t');
		sb.append(message).append(newline);
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}

	private void advanceNextDate(long now) {
		long m = now / 1000 / 60;
		nextDateAt = (m + 60 * 24 - 1) / 60 / 24 * 24 * 60 * 60 * 1000;
	}

	private StringBuilder appendDateString(StringBuilder sb, long now) {
		advanceNextDate(now);
		GregorianCalendar cal = new GregorianCalendar(UTC);
		cal.setTimeInMillis(now);
		sb.append(cal.get(Calendar.YEAR)).append('-');
		pad(sb, cal.get(Calendar.MONTH) + 1).append('-');
		pad(sb, cal.get(Calendar.DAY_OF_MONTH)).append(' ');
		pad(sb, cal.get(Calendar.HOUR_OF_DAY)).append(':');
		pad(sb, cal.get(Calendar.MINUTE)).append(':');
		pad(sb, cal.get(Calendar.SECOND));
		return sb;
	}

	private Object getTimeString(long now) {
		if (now >= minuteExpire) {
			long m = now / 1000 / 60;
			int hr = (int)((m / 60) % 24);
			int min = (int)(m % 60);
			StringBuilder sb = new StringBuilder(6);
			pad(pad(sb, hr).append(':'), min);
			minute = sb.toString();
			minuteExpire = (m + 1) * 60 * 1000;
		}
		return minute;
	}

	private StringBuilder pad(StringBuilder sb, int i) {
		if (i < 10) {
			sb.append('0');
		}
		return sb.append(i);
	}
}
