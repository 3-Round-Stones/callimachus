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
package org.callimachusproject.server.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.callimachusproject.server.HTTPObjectServer;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.server.model.EntityRemovedHttpResponse;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.xslt.XSLTransformer;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A step for handling a request that can be performed in isolation.
 * 
 * @author James Leigh
 * 
 */
public abstract class Task implements Runnable {
	private static final Pattern URL_PATTERN = Pattern
			.compile("\\w+://(?:\\.?[^\\s}>\\)\\]])+");
	private static final ProtocolVersion HTTP11 = new ProtocolVersion("HTTP",
			1, 1);
	private static final BasicHttpResponse _500 = new BasicHttpResponse(HTTP11,
			500, "Internal Server Error");
	private static final ThreadLocal<Boolean> inError = new ThreadLocal<Boolean>();
	static {
		_500.setHeader("Content-Length", "0");
	}
	private Logger logger = LoggerFactory.getLogger(Task.class);
	private Logger access = LoggerFactory.getLogger(HTTPObjectServer.class);
	private Executor executor;
	private final Request req;
	private NHttpResponseTrigger trigger;
	private Task child;
	private volatile boolean done;
	private volatile boolean aborted;
	private volatile boolean triggered;
	private HttpException http;
	private IOException io;
	private HttpResponse resp;
	private Filter filter;
	private Runnable onDone;
	private XSLTransformer transformer;

	public Task(Request request, Filter filter) {
		assert request != null;
		assert !(request instanceof ResourceOperation);
		this.req = request;
		this.filter = filter;
	}

	public void setErrorXSLT(XSLTransformer transformer) {
		this.transformer = transformer;
	}

	public final boolean isStorable() {
		return req.isStorable();
	}

	public final boolean isSafe() {
		return req.isSafe();
	}

	public final long getReceivedOn() {
		return req.getReceivedOn();
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public synchronized void setTrigger(NHttpResponseTrigger trigger) {
		this.trigger = trigger;
		if (http != null) {
			logger.debug("submit exception {} {}", req, http.toString());
			trigger.handleException(http);
			triggered = true;
		} else if (io != null) {
			logger.debug("submit exception {} {}", req, io.toString());
			trigger.handleException(io);
			triggered = true;
		} else if (resp != null) {
			access.trace("{} {}", req, resp.getStatusLine().getStatusCode());
			trigger.submitResponse(resp);
			triggered = true;
		} else if (child == null && aborted) {
			access.trace("{} {}", req, 500);
			trigger.submitResponse(_500);
			triggered = true;
		}
		if (child != null) {
			child.setTrigger(trigger);
			triggered = true;
		}
	}

	public void run() {
		try {
			perform();
		} catch (HttpException e) {
			handleException(e);
			logger.debug(req.toString(), e);
		} catch (IOException e) {
			handleException(e);
			logger.debug(req.toString(), e);
		} catch (ResponseException e) {
			submitException(e);
			if (e.getStatusCode() >= 500) {
				logger.error(req.toString(), e);
			}
		} catch (Exception e) {
			submitException(e);
			logger.error(req.toString(), e);
		} catch (Error e) {
			abort();
			logger.error(req.toString(), e);
		} finally {
			performed();
		}
	}

	public abstract int getGeneration();

	abstract void perform() throws Exception;

	public synchronized <T extends Task> T bear(T child) {
		assert this != child;
		this.child = child;
		if (trigger != null) {
			child.setTrigger(trigger);
		}
		if (onDone != null) {
			child.onDone(onDone);
		}
		if (transformer != null) {
			child.setErrorXSLT(transformer);
		}
		assert executor != null;
		child.setExecutor(executor);
		executor.execute(child);
		return child;
	}

	public boolean isDone() {
		if (child == null)
			return done;
		return child.isDone();
	}

	public void awaitVerification(long time, TimeUnit unit)
			throws InterruptedException {
		if (child != null) {
			child.awaitVerification(time, unit);
		}
	}

	public void abort() {
		aborted = true;
		cleanup();
		close();
		if (resp != null) {
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					logger.error(e.toString(), e);
				}
			}
			resp.setEntity(null);
		}
		if (child != null) {
			child.abort();
		} else if (!triggered && trigger != null) {
			access.trace("{} {}", req, 500);
			trigger.submitResponse(_500);
			triggered = true;
		}
	}

	public synchronized void cleanup() {
		try {
			HttpEntity entity = req.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}
	}

	public synchronized void onDone(Runnable onDone) {
		this.onDone = onDone;
		if (isDone()) {
			onDone.run();
		} else if (child != null) {
			child.onDone(onDone);
		}
	}

	public HttpResponse getHttpResponse() throws HttpException, IOException {
		if (child != null)
			return child.getHttpResponse();
		if (io != null)
			throw io;
		if (http != null)
			throw http;
		return resp;
	}

	public void submitResponse(Response resp) throws Exception {
		cleanup();
		HttpResponse response;
		try {
			try {
				try {
					response = createHttpResponse(req, resp);
				} catch (Exception exc) {
					List<Runnable> onClose = resp.getOnClose();
					if (onClose != null) {
						for (Runnable task : onClose) {
							try {
								task.run();
							} catch (RuntimeException e) {
							} catch (Error e) {
							}
						}
					}
					throw exc;
				}
			} catch (ResponseException e) {
				response = createHttpResponse(req, new Response().exception(e));
			} catch (ConcurrencyException e) {
				response = createHttpResponse(req, new Response().conflict(e));
			} catch (MimeTypeParseException e) {
				response = createHttpResponse(req,
						new Response().status(406, "Not Acceptable"));
			} catch (Exception e) {
				logger.error(e.toString(), e);
				response = createHttpResponse(req, new Response().server(e));
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
			ProtocolVersion ver = HTTP11;
			response = new BasicHttpResponse(ver, 500, "Internal Server Error");
		}
		submitResponse(response);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(req);
		if (http != null) {
			sb.append(" ").append(http);
		}
		if (io != null) {
			sb.append(" ").append(io);
		}
		if (resp != null) {
			sb.append(" ").append(resp.getStatusLine());
		}
		return sb.toString();
	}

	protected abstract void close();

	/**
	 * {@link #cleanup()} must be called before calling this method.
	 * 
	 * @param response
	 * @throws IOException
	 */
	void submitResponse(HttpResponse response) throws IOException {
		try {
			HttpResponse resp = filter(req, response);
			HttpEntity entity = resp.getEntity();
			if ("HEAD".equals(req.getMethod()) && entity != null) {
				entity.consumeContent();
				resp.setEntity(null);
			}
			synchronized (this) {
				if (triggered || aborted || this.resp != null) {
					abort();
					if (entity != null) {
						try {
							entity.consumeContent();
						} catch (IOException e) {
							logger.error(e.toString(), e);
						}
					}
					resp.setEntity(null);
				} else {
					this.resp = resp;
					if (trigger != null) {
						int code = resp.getStatusLine().getStatusCode();
						access.trace("{} {}", req, code);
						trigger.submitResponse(resp);
						triggered = true;
					}
				}
			}
		} catch (RuntimeException e) {
			handleException(new IOException(e));
		} catch (IOException e) {
			handleException(e);
		} finally {
			close();
		}
	}

	private synchronized void performed() {
		if (child == null) {
			done = true;
			if (onDone != null) {
				onDone.run();
			}
		}
	}

	private void submitException(ResponseException e) {
		try {
			cleanup();
			submitResponse(createHttpResponse(req, new Response().exception(e)));
		} catch (IOException e1) {
			handleException(e1);
		} catch (Exception e1) {
			handleException(new IOException(e1));
		}
	}

	private void submitException(Exception e) {
		try {
			cleanup();
			submitResponse(createHttpResponse(req, new Response().server(e)));
		} catch (IOException e1) {
			handleException(e1);
		} catch (Exception e1) {
			handleException(new IOException(e1));
		}
	}

	private synchronized void handleException(HttpException ex) {
		http = ex;
		try {
			abort();
		} finally {
			if (!triggered && trigger != null) {
				logger.debug("submit exception {} {}", req, ex.toString());
				trigger.handleException(ex);
				triggered = true;
			} else if (triggered) {
				logger.warn(ex.toString(), ex);
			}
		}
	}

	private synchronized void handleException(IOException ex) {
		io = ex;
		try {
			abort();
		} finally {
			if (!triggered && trigger != null) {
				logger.debug("submit exception {} {}", req, ex.toString());
				trigger.handleException(ex);
				triggered = true;
			} else if (triggered) {
				logger.warn(ex.toString(), ex);
			}
		}
	}

	private HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		return filter.filter(request, response);
	}

	private HttpResponse createHttpResponse(Request req, Response resp)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException,
			MimeTypeParseException {
		ProtocolVersion ver = HTTP11;
		int code = resp.getStatus();
		String phrase = resp.getMessage();
		HttpResponse response = new EntityRemovedHttpResponse(ver, code, phrase);
		for (Header hd : resp.getAllHeaders()) {
			response.addHeader(hd);
		}
		if (resp.isException()) {
			String type = "text/html;charset=UTF-8";
			response.setHeader("Content-Type", type);
			byte[] body = createErrorPage(resp);
			int size = body.length;
			response.setHeader("Content-Length", String.valueOf(size));
			ReadableByteChannel in = ChannelUtil.newChannel(body);
			List<Runnable> onClose = resp.getOnClose();
			HttpEntity entity = new ReadableHttpEntityChannel(type, size, in,
					onClose);
			response.setEntity(entity);
		} else if (resp.isContent()) {
			String type = resp.getHeader("Content-Type");
			Charset charset = getCharset(type);
			long size = resp.getSize(type, charset);
			if (size >= 0) {
				response.setHeader("Content-Length", String.valueOf(size));
			} else if (!response.containsHeader("Content-Length")) {
				response.setHeader("Transfer-Encoding", "chunked");
			}
			ReadableByteChannel in = resp.write(type, charset);
			List<Runnable> onClose = resp.getOnClose();
			HttpEntity entity = new ReadableHttpEntityChannel(type, size, in,
					onClose);
			response.setEntity(entity);
		} else {
			response.setHeader("Content-Length", "0");
		}
		return response;
	}

	private byte[] createErrorPage(Response resp) throws IOException,
			TransformerException {
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		try {
			printHTMLTo(resp.getStatusCode(), resp.getException(), print);
		} finally {
			print.close();
		}
		String body = writer.toString();
		if (transformer != null && inError.get() == null) {
			String id = transformer.getSystemId();
			if (id == null || !req.getRequestURL().startsWith(id)) {
				String iri = req.getIRI();
				try {
					inError.set(true);
					body = transformer.transform(body, null).with("this", iri)
							.with("query", req.getQueryString()).asString();
				} catch (Throwable exc) {
					logger.error(exc.toString(), exc);
				} finally {
					inError.remove();
				}
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
		OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
		w.append(body);
		w.close();
		return out.toByteArray();
	}

	private void printHTMLTo(int code, ResponseException exc, PrintWriter writer) {
		writer.append("<html>\n");
		writer.append("<head><title>");
		writer.append(enc(exc.getLongMessage()));
		writer.append("</title></head>\n");
		writer.append("<body>\n");
		writer.append("<h1>");
		writer.append(html(exc.getLongMessage()));
		writer.append("</h1>\n");
		if (code == 500) {
			writer.append("<pre>");
			Writer sw = new StringWriter();
			PrintWriter print = new PrintWriter(sw);
			exc.printStackTrace(print);
			print.close();
			writer.append(enc(sw.toString()));
			writer.append("</pre>\n");
		} else if (code > 500) {
			writer.append("<pre>");
			writer.append(enc(exc.getDetailMessage()));
			writer.append("</pre>\n");
		}
		writer.append("</body>\n");
		writer.append("</html>\n");
	}

	private String html(String string) {
		if (string.contains("://")) {
			int end = 0;
			StringBuilder sb = new StringBuilder();
			Matcher m = URL_PATTERN.matcher(string);
			while (m.find()) {
				String url = m.group();
				sb.append(enc(string.substring(end, m.start())));
				sb.append("<a href='").append(url).append("'>");
				int path = url.indexOf('/', url.indexOf("://") + 3);
				String label = url.substring(path);
				sb.append(enc(label));
				sb.append("</a>");
				end = m.end();
			}
			sb.append(enc(string.substring(end, string.length())));
			return sb.toString();
		} else {
			return enc(string);
		}
	}

	private String enc(String string) {
		String result = string.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		return result;
	}

	private Charset getCharset(String type) {
		if (type == null)
			return null;
		try {
			MimeType m = new MimeType(type);
			String name = m.getParameters().get("charset");
			if (name == null)
				return null;
			return Charset.forName(name);
		} catch (MimeTypeParseException e) {
			logger.debug(e.toString(), e);
			return null;
		}
	}
}
