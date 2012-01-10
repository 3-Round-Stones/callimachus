/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.http.object.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import javax.tools.FileObject;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link FileObject} interface in all objects of the store.
 */
public abstract class FileObjectSupport implements RDFObject, FileObject {
	private final Logger logger = LoggerFactory.getLogger(FileObjectSupport.class);

	public java.net.URI toUri() {
		return java.net.URI.create(getResource().stringValue());
	}

	public String getName() {
		String uri = getResource().stringValue();
		int last = uri.length() - 1;
		int idx = uri.lastIndexOf('/', last - 1) + 1;
		if (idx > 0 && uri.charAt(last) != '/')
			return uri.substring(idx);
		if (idx > 0 && idx != last)
			return uri.substring(idx, last);
		return uri;
	}

	public long getLastModified() {
		try {
			String uri = getResource().stringValue();
			BlobObject blob = getObjectConnection().getBlobObject(uri);
			return blob.getLastModified();
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
			return 0;
		}
	}

	public InputStream openInputStream() throws IOException {
		String uri = getResource().stringValue();
		try {
			return getObjectConnection().getBlobObject(uri).openInputStream();
		} catch (RepositoryException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw new IOException(e);
		}
	}

	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		InputStream in = openInputStream();
		if (in == null)
			return null;
		return new InputStreamReader(in);
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors)
			throws IOException {
		Reader reader = openReader(ignoreEncodingErrors);
		if (reader == null)
			return null;
		try {
			StringWriter writer = new StringWriter();
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
			return writer.toString();
		} finally {
			reader.close();
		}
	}

	public OutputStream openOutputStream() throws IOException {
		String uri = getResource().stringValue();
		try {
			return getObjectConnection().getBlobObject(uri).openOutputStream();
		} catch (RepositoryException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw new IOException(e);
		}
	}

	public Writer openWriter() throws IOException {
		OutputStream out = openOutputStream();
		if (out == null)
			return null;
		return new OutputStreamWriter(out);
	}

	public boolean delete() {
		try {
			String uri = getResource().stringValue();
			return getObjectConnection().getBlobObject(uri).delete();
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
			return false;
		}
	}

}
