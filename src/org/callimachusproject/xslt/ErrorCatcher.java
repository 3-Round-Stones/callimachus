/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
package org.callimachusproject.xslt;

import java.io.IOException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores XSLT errors.
 *
 * @author James Leigh
 */
public class ErrorCatcher implements ErrorListener {
	private Logger logger = LoggerFactory.getLogger(ErrorCatcher.class);
	private String target;
	private TransformerException error;
	private TransformerException fatal;
	private IOException io;

	public ErrorCatcher(String target) {
		if (target == null) {
			target = "";
		}
		this.target = target;
	}

	public boolean isFatal() {
		return fatal != null;
	}

	public TransformerException getFatalError() {
		return fatal;
	}

	public boolean isIOException() {
		return io != null;
	}

	public IOException getIOException() {
		return new IOException(io);
	}

	public void ioException(IOException exception) {
		if (io == null) {
			io = exception;
		}
		if (!"Pipe closed".equals(exception.getMessage())) {
			logger.info(exception.toString(), exception);
		}
	}

	public void error(TransformerException ex) {
		if (!"Pipe closed".equals(ex.getMessage())) {
			logger.warn("{} in {}", ex.getMessageAndLocation(), target);
		}
		if (error != null && ex.getCause() == null) {
			ex.initCause(error);
		}
		error = ex;
	}

	public void fatalError(TransformerException ex) {
		logger.error("{} in {}", ex.getMessageAndLocation(), target);
		if (error != null && ex.getCause() == null) {
			ex.initCause(error);
		}
		if (fatal == null) {
			fatal = ex;
		}
	}

	public void warning(TransformerException exception) {
		logger.info(exception.toString(), exception);
	}
}
