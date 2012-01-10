/*
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
package org.callimachusproject.server.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.Header;

/**
 * Parses and formats Dates as per RFC 1123.
 *
 * @author James Leigh
 **/
public class HTTPDateFormat extends DateFormat {
	private static final long serialVersionUID = -2636174153773598968L;
	/** Date format pattern used to generate the header in RFC 1123 format. */
	private static final String HTTP_RESPONSE_DATE_HEADER = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static ThreadLocal<SimpleDateFormat> simple = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat format = new SimpleDateFormat(
					HTTP_RESPONSE_DATE_HEADER, Locale.US);
			format.setTimeZone(TimeZone.getTimeZone("GMT"));
			return format;
		}
	};
	private long format = 0;
	private String parse = "Thu, 01 Jan 1970 00:00:00 GMT";

	public synchronized String format(long date) {
		if (format == date)
			return parse;
		return parse = simple.get().format(format = date);
	}

	public long parseHeader(Header hd) {
		if (hd == null)
			return System.currentTimeMillis() / 1000 * 1000;
		return parseDate(hd.getValue());
	}

	public long parseDate(String source) {
		if (source == null)
			return System.currentTimeMillis() / 1000 * 1000;
		synchronized (this) {
			if (source.equals(parse))
				return format;
		}
		try {
			return simple.get().parse(source).getTime();
		} catch (ParseException e) {
			return System.currentTimeMillis() / 1000 * 1000;
		}
	}

	public Date parse(String source) {
		if (source == null)
			return new Date(System.currentTimeMillis() / 1000 * 1000);
		synchronized (this) {
			if (source.equals(parse))
				return new Date(format);
		}
		try {
			return simple.get().parse(source);
		} catch (ParseException e) {
			return new Date(System.currentTimeMillis() / 1000 * 1000);
		}
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo,
			FieldPosition fieldPosition) {
		return simple.get().format(date, toAppendTo, fieldPosition);
	}

	@Override
	public Date parse(String source, ParsePosition pos) {
		return simple.get().parse(source, pos);
	}

}
