/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.tools.FileObject;
import javax.xml.datatype.XMLGregorianCalendar;

import org.callimachusproject.concepts.Activity;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocumentSupport implements CalliObject {
	private static final Set<String> locks = new HashSet<String>();
	private final Logger logger = LoggerFactory
			.getLogger(DocumentSupport.class);

	public String ifStringModifiedSince(String uri, Callable<String> producer)
			throws Exception {
		long since = getResourceLastModified(1);
		BlobObject file = this.getObjectConnection().getBlobObject(uri);
		if (file.getLastModified() >= since) {
			return readString(file);
		} else {
			lock(uri);
			try {
				return produce(file, since, producer);
			} finally {
				unlock(uri);
			}
		}
	}

	private String produce(BlobObject file, long since,
			Callable<String> producer) throws Exception {
		String text = null;
		try {
			ObjectConnection con = this.getCalliRepository().getConnection();
			try {
				String uri = file.toUri().toASCIIString();
				BlobObject blob = con.getBlobObject(uri);
				if (blob.getLastModified() >= since) {
					return readString(blob);
				} else {
					text = producer.call();
					OutputStream out = blob.openOutputStream();
					try {
						out.write(text.getBytes());
					} finally {
						out.close();
					}
				}
			} finally {
				con.close();
			}
		} catch (Exception e) {
			if (text == null)
				throw e;
			logger.error(e.toString(), e);
		} catch (Error e) {
			if (text == null)
				throw e;
			logger.error(e.toString(), e);
		}
		return text;
	}

	private void lock(String uri) throws InterruptedException {
		synchronized (locks) {
			while (!locks.add(uri)) {
				locks.wait();
			}
		}
	}

	private void unlock(String uri) {
		synchronized (locks) {
			locks.remove(uri);
			locks.notifyAll();
		}
	}

	private String readString(FileObject file) throws IOException,
			UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream in = file.openInputStream();
		try {
			ChannelUtil.transfer(in, baos);
		} finally {
			in.close();
		}
		return new String(baos.toByteArray());
	}

	private long getResourceLastModified(long defaultValue) {
		Activity activity = this.getProvWasGeneratedBy();
		if (activity == null)
			return defaultValue;
		XMLGregorianCalendar time = activity.getProvEndedAtTime();
		if (time == null)
			return defaultValue;
		return time.toGregorianCalendar().getTimeInMillis();
	}
}
