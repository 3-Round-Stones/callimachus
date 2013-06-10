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
package org.callimachusproject.server.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows concurrent access to statements as they are being parsed.
 * 
 * @author James Leigh
 *
 */
public class BackgroundGraphResult implements
		GraphQueryResult, Runnable, RDFHandler, Closeable {
	private volatile boolean closed;
	private volatile Thread parserThread;
	private Logger logger = LoggerFactory.getLogger(BackgroundGraphResult.class);
	private RDFParser parser;
	private Charset charset;
	private InputStream in;
	private String baseURI;
	private CountDownLatch namespacesReady = new CountDownLatch(1);
	private Map<String, String> namespaces = new ConcurrentHashMap<String, String>();
	private QueueCursor<Statement> queue;

	public BackgroundGraphResult(RDFParser parser, InputStream in,
			Charset charset, String baseURI) {
		this(new QueueCursor<Statement>(10), parser, in, charset, baseURI);
	}

	public BackgroundGraphResult(QueueCursor<Statement> queue,
			RDFParser parser, InputStream in, Charset charset, String baseURI) {
		if (baseURI == null) {
			baseURI = "";
		}
		this.queue = queue;
		this.parser = parser;
		this.in = in;
		this.charset = charset;
		this.baseURI = baseURI;
	}

	public String toString() {
		return parser.toString() + " in background thread";
	}

	public boolean hasNext() throws QueryEvaluationException {
		return queue.hasNext();
	}

	public Statement next() throws QueryEvaluationException {
		return queue.next();
	}

	public void remove() throws QueryEvaluationException {
		queue.remove();
	}

	public void close() {
		closed = true;
		if (parserThread != null) {
			parserThread.interrupt();
		}
		try {
			queue.close();
			in.close();
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		} catch (QueryEvaluationException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	public void run() {
		parserThread = Thread.currentThread();
		try {
			parser.setRDFHandler(this);
			if (charset == null) {
				parser.parse(in, baseURI);
			} else {
				parser.parse(new InputStreamReader(in, charset), baseURI);
			}
		} catch (RDFHandlerException e) {
			// parsing was cancelled or interrupted
		} catch (RDFParseException e) {
			queue.toss(e);
		} catch (IOException e) {
			queue.toss(e);
		} finally {
			parserThread = null;
			queue.done();
			namespacesReady.countDown();
		}
	}

	public void startRDF() throws RDFHandlerException {
		// no-op
	}

	public Map<String, String> getNamespaces() {
		try {
			namespacesReady.await();
			return namespaces;
		} catch (InterruptedException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public void handleComment(String comment) throws RDFHandlerException {
		// ignore
	}

	public void handleNamespace(String prefix, String uri)
			throws RDFHandlerException {
		namespaces.put(prefix, uri);
	}

	public void handleStatement(Statement st) throws RDFHandlerException {
		namespacesReady.countDown();
		if (closed)
			throw new RDFHandlerException("Result closed");
		try {
			queue.put(st);
		} catch (InterruptedException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void endRDF() throws RDFHandlerException {
		namespacesReady.countDown();
	}

}
