package org.callimachusproject.server.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.server.model.EntityRemovedHttpResponse;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.xproc.Pipe;
import org.callimachusproject.xproc.Pipeline;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public abstract class ExchangeActor {
	private static final int MAX_QUEUE_SIZE = 32;
	private static final Pattern URL_PATTERN = Pattern
			.compile("\\w+://(?:\\.?[^\\s}>\\)\\]\\.])+");
	private static final ProtocolVersion HTTP11 = HttpVersion.HTTP_1_1;
	private static final ThreadLocal<Boolean> inError = new ThreadLocal<Boolean>();
	private static final BasicHttpResponse SHUTDOWN_503 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 503, "Service Unavailable For Maintenance");
	private static final BasicHttpResponse _503 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 503, "Service Temporary Overloaded");
	static final BasicHttpResponse _500 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 500, "Internal Server Error");

	private final Logger logger = LoggerFactory.getLogger(ExchangeActor.class);
	private final ExecutorService executor;
	private final BlockingQueue<Runnable> queue;
	private Pipeline pipeline;

	public ExchangeActor(ExecutorService executor, BlockingQueue<Runnable> queue) {
		this.executor = executor;
		this.queue = queue;
	}

	public void shutdown() {
		for (Runnable run : executor.shutdownNow()) {
			ExchangeTask task = (ExchangeTask) run;
			task.getExchange().submitResponse(SHUTDOWN_503);
		}
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public Pipeline getErrorPipe() {
		return pipeline;
	}

	public void setErrorPipe(Pipeline pipeline) throws IOException {
		this.pipeline = pipeline;
	}

	public void submit(Exchange exchange) {
		try {
			executor.execute(new ExchangeTask(exchange) {
				public void run() {
					execute(getExchange(), false);
				}
			});
		} catch (RejectedExecutionException e) {
			exchange.submitResponse(_503);
		}
		if (queue.size() > MAX_QUEUE_SIZE) {
			Object[] tasks = queue.toArray();
			if (tasks.length > MAX_QUEUE_SIZE) {
				Arrays.sort(tasks);
				ExchangeTask task = (ExchangeTask) tasks[tasks.length - 1];
				if (queue.remove(task)) {
					task.getExchange().submitResponse(_503);
				}
			}
		}
	}

	public void execute(Exchange exchange) {
		execute(exchange, true);
	}

	protected abstract void process(Exchange exchange, boolean foreground) throws Exception;

	void execute(Exchange exchange, boolean foreground) {
		boolean processed = false;
		try {
			process(exchange, foreground);
			processed = true;
		} catch (Exception e) {
			exchange.submitResponse(createErrorResponse(exchange.getRequest(), e));
			processed = true;
		} finally {
			if (!processed) {
				exchange.submitResponse(_500);
			}
		}
	}

	protected HttpResponse createErrorResponse(Request req, Exception e) {
		while (e instanceof BehaviourException
				|| e instanceof InvocationTargetException
				|| e instanceof ExecutionException
				&& e.getCause() instanceof Exception) {
			e = (Exception) e.getCause();
		}
		ResponseException re = asResponseException(req, e);
		try {
			return createHttpResponse(req, re);
		} catch (Exception e1) {
			logger.error(e1.toString(), e1);
			return _500;
		}
	}

	protected ResponseException asResponseException(Request req, Exception e) {
		if (e instanceof ResponseException)
			return (ResponseException) e;
		logger.error("Internal Server Error while responding to " + req.getRequestLine().getUri(), e);
		return new InternalServerError(e);
	}

	private HttpResponse createHttpResponse(Request req, ResponseException exception)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException,
			SAXException {
		ProtocolVersion ver = HTTP11;
		int code = exception.getStatusCode();
		String phrase = exception.getShortMessage();
		HttpResponse response = new EntityRemovedHttpResponse(ver, code, phrase);
		String type = "text/html;charset=UTF-8";
		response.setHeader("Content-Type", type);
		byte[] body = createErrorPage(req, exception);
		int size = body.length;
		response.setHeader("Content-Length", String.valueOf(size));
		ReadableByteChannel in = ChannelUtil.newChannel(body);
		HttpEntity entity = new ReadableHttpEntityChannel(type, size, in);
		response.setEntity(entity);
		return response;
	}

	private byte[] createErrorPage(Request req, ResponseException exception) throws IOException,
			TransformerException {
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		try {
			printHTMLTo(exception.getStatusCode(), exception, print);
		} finally {
			print.close();
		}
		String body = writer.toString();
		if (pipeline != null && inError.get() == null) {
			String id = pipeline.getSystemId();
			if (id == null || !req.getRequestURL().startsWith(id)) {
				try {
					inError.set(true);
					Pipe pb = pipeline.pipeReader(new StringReader(body), null);
					pb.passOption("target", req.getIRI());
					pb.passOption("query", req.getQueryString());
					body = pb.asString();
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
		if (exc.getCause() != null) {
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
				sb.append("<a href='").append(enc(url)).append("'>");
				int path = url.indexOf('/', url.indexOf("://") + 3);
				String label = path > 0 ? url.substring(path) : url;
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

}
