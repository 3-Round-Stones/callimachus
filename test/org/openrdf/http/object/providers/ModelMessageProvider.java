package org.openrdf.http.object.providers;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import info.aduna.iteration.Iterations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.openrdf.http.object.util.BackgroundGraphResult;
import org.openrdf.http.object.util.ManagedExecutors;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;

public class ModelMessageProvider implements MessageBodyReader<Model>,
		MessageBodyWriter<Model> {
	private static Executor executor = ManagedExecutors.getParserThreadPool();
	private Class<Model> type = Model.class;
	private RDFParserRegistry parsers = RDFParserRegistry.getInstance();
	private RDFWriterRegistry writers = RDFWriterRegistry.getInstance();
	private UriInfo uri;

	public ModelMessageProvider(@Context UriInfo uri) {
		this.uri = uri;
	}

	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType media) {
		if (Object.class.equals(type))
			return false;
		if (!type.equals(this.type))
			return false;
		if (media == null || WILDCARD_TYPE.equals(media)
				|| APPLICATION_OCTET_STREAM_TYPE.equals(media))
			return false;
		return getParserFactory(media) != null;
	}

	public Model readFrom(Class<Model> type, Type genericType,
			Annotation[] annotations, MediaType media,
			MultivaluedMap<String, String> httpHeaders, InputStream in)
			throws IOException, WebApplicationException {
		RDFParser parser = getParserFactory(media).getParser();
		Charset charset = getCharset(media, null);
		String base = uri == null ? "" : uri.getAbsolutePath().toString();
		BackgroundGraphResult result;
		result = new BackgroundGraphResult(parser, in, charset, base);
		executor.execute(result);
		try {
			Map<String, String> ns = result.getNamespaces();
			return new LinkedHashModel(ns, Iterations.asList(result));
		} catch (QueryEvaluationException e) {
			e.printStackTrace(System.err);
			throw new AssertionError(e);
		}
	}

	public long getSize(Model t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!this.type.isAssignableFrom(type))
			return false;
		return getWriterFactory(mediaType) != null;
	}

	public void writeTo(Model model, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		httpHeaders.putSingle("Content-Type", getContentType(mediaType));
		GraphQueryResult result = new GraphQueryResultImpl(model
				.getNamespaces(), model);
		RDFWriterFactory factory = getWriterFactory(mediaType);
		Charset charset = getCharset(mediaType, null);
		RDFWriter writer = getWriter(out, charset, factory);
		// String base = uri == null ? "" : uri.getAbsolutePath().toString();
		// writer.setBaseURI(base);
		try {
			writer.startRDF();
			for (String prefix : model.getNamespaces().keySet()) {
				writer.handleNamespace(prefix, model.getNamespace(prefix));
			}
			while (result.hasNext()) {
				Statement st = result.next();
				writer.handleStatement(st);
			}
			writer.endRDF();
		} catch (RDFHandlerException e) {
			throw new WebApplicationException(e);
		} catch (QueryEvaluationException e) {
			throw new WebApplicationException(e);
		}
	}

	private String getContentType(MediaType mediaType) {
		RDFFormat format = getWriterFormat(mediaType);
		String contentType = format.getDefaultMIMEType();
		Charset charset = getCharset(mediaType, format.getCharset());
		if (format.hasCharset()) {
			contentType += "; charset=" + charset.name();
		}
		return contentType;
	}

	private Charset getCharset(MediaType m, Charset defCharset) {
		if (m == null)
			return defCharset;
		String name = m.getParameters().get("charset");
		if (name == null)
			return defCharset;
		return Charset.forName(name);
	}

	private RDFParserFactory getParserFactory(MediaType media) {
		RDFFormat format = getParserFormat(media);
		if (format == null)
			return null;
		return parsers.get(format);
	}

	private RDFFormat getParserFormat(MediaType media) {
		if (media == null || media.isWildcardType()
				&& media.isWildcardSubtype()
				|| APPLICATION_OCTET_STREAM_TYPE.equals(media)) {
			for (RDFFormat format : parsers.getKeys()) {
				if (parsers.get(format) != null)
					return format;
			}
			return null;
		}
		// FIXME FileFormat does not understand MIME parameters
		String mimeType = media.getType() + "/" + media.getSubtype();
		return parsers.getFileFormatForMIMEType(mimeType);
	}

	private RDFWriter getWriter(OutputStream out, Charset charset,
			RDFWriterFactory factory) {
		if (charset == null)
			return factory.getWriter(out);
		return factory.getWriter(new OutputStreamWriter(out, charset));
	}

	private RDFWriterFactory getWriterFactory(MediaType media) {
		RDFFormat format = getWriterFormat(media);
		if (format == null)
			return null;
		return writers.get(format);
	}

	private RDFFormat getWriterFormat(MediaType media) {
		if (media == null || media.isWildcardType()
				&& media.isWildcardSubtype()
				|| APPLICATION_OCTET_STREAM_TYPE.equals(media)) {
			for (RDFFormat format : writers.getKeys()) {
				if (writers.get(format) != null)
					return format;
			}
			return null;
		}
		// FIXME FileFormat does not understand MIME parameters
		String mimeType = media.getType() + "/" + media.getSubtype();
		return writers.getFileFormatForMIMEType(mimeType);
	}

}
