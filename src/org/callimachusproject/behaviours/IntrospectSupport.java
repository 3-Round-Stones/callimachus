package org.callimachusproject.behaviours;

import java.beans.IntrospectionException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.callimachusproject.annotations.query;
import org.callimachusproject.server.helpers.ResourceInfo;
import org.callimachusproject.server.helpers.ResourceInfo.MethodInfo;
import org.callimachusproject.server.helpers.ResourceInfo.ParameterInfo;
import org.callimachusproject.server.helpers.ResourceInfo.PropertyInfo;
import org.callimachusproject.server.helpers.XHTMLInfoWriter;
import org.openrdf.model.URI;
import org.openrdf.repository.object.RDFObject;

public class IntrospectSupport {
	public ByteArrayOutputStream calliIntrospect(RDFObject target)
			throws XMLStreamException, FactoryConfigurationError,
			IntrospectionException {
		ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
		XHTMLInfoWriter out = new XHTMLInfoWriter(stream);

		ResourceInfo info = new ResourceInfo(target.getClass());
		Set<Class<?>> concepts = info.getConcepts();
		MethodInfo[] operations = info.getRemoteMethodDescriptors();
		PropertyInfo[] properties = info.getPropertyDescriptors();
		MethodInfo[] methods = info.getMethodDescriptors();
		String title = ((URI) target.getResource()).getLocalName()
				+ " Introspection";

		out.writeStartDocument(title);

		// table of content
		out.writeStartElement("div");
		out.writeAttribute("id", "sidebar");

		out.writeStartElement("aside");
		out.writeSubheading("Operations");
		out.writeStartElement("ul");
		for (MethodInfo m : operations) {
			out.writeStartElement("li");
			out.writeLink("#" + m.getRemoteName(), m.getRemoteName());
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul
		out.writeEndElement(); //aside

		out.writeStartElement("aside");
		out.writeSubheading("Properties");
		out.writeStartElement("ul");
		for (PropertyInfo p : properties) {
			out.writeStartElement("li");
			out.writeLink("#" + p.getName(), p.getName());
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul
		out.writeEndElement(); //aside

		out.writeStartElement("aside");
		out.writeSubheading("Methods");
		out.writeStartElement("ul");
		for (MethodInfo m : methods) {
			out.writeStartElement("li");
			out.writeLink("#" + m.getName(), m.getName());
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul
		out.writeEndElement(); //aside

		out.writeEndElement(); //div#sidebar

		// content
		out.writeTitle(title);

		out.writeHeading("Classes");
		writeConcepts(concepts, info, out);

		out.writeHeading("Operations");
		out.writeStartElement("ul");
		for (MethodInfo m : operations) {
			out.writeStartElement("li");
			writeRemoteMethodInfo(m, out);
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul

		out.writeEmptyElement("hr");

		out.writeHeading("Properties");
		out.writeStartElement("ul");
		for (PropertyInfo p : properties) {
			out.writeStartElement("li");
			writePropertyInfo(p, target, out);
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul

		out.writeEmptyElement("hr");

		out.writeHeading("Methods");
		out.writeStartElement("ul");
		for (MethodInfo m : methods) {
			out.writeStartElement("li");
			writeMethodInfo(m, out);
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul

		out.writeEndDocument();
		return stream;
	}

	private void writeConcepts(Set<Class<?>> classes, ResourceInfo info,
			XHTMLInfoWriter out) throws XMLStreamException {
		out.writeStartElement("ul");
		for (Class<?> cls : classes) {
			out.writeStartElement("li");
			Set<Class<?>> set = info.getSuperConcepts(cls);
			if (set.isEmpty()) {
				out.writeLink(info.getConceptIri(cls), info.getConceptName(cls));
			} else {
				out.writeStartElement("details");
				out.writeStartElement("summary");
				out.writeLink(info.getConceptIri(cls), info.getConceptName(cls));
				out.writeEndElement();
				writeConcepts(set, info, out);
				out.writeEndElement();
			}
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul
	}

	private void writeMethodInfo(MethodInfo info, XHTMLInfoWriter out) throws XMLStreamException {
		out.writeAnchor(info.getIRI(), info.getName());
		out.writeCharacters("(");
		ParameterInfo[] params = info.getParameterDescriptors();
		for (int i = 0; i < params.length; i++) {
			if (i > 0) {
				out.writeCharacters(", ");
			}
			out.writeLink(params[i].getIRI(), params[i].getType(), params[i].getName());
		}
		out.writeCharacters(")");
		out.writeStartElement("dl");
		for (int i = 0; i < params.length; i++) {
			out.writeDefinition("parameter type", params[i].getGenericType().toString());
		}
		out.writeDefinition("return type", info.getGenericReturnType().toString());
		out.writeEndElement(); //dl
	}

	private void writePropertyInfo(PropertyInfo info, Object target, XHTMLInfoWriter out)
			throws XMLStreamException {
		out.writeAnchor(info.getIRI(), info.getName());
		out.writeStartElement("dl");
		out.writeDefinition("return type", info.getGenericType().toString());
		try {
			Object value = info.getReadMethod().invoke(target);
			out.writeDefinition("has value", String.valueOf(value));
		} catch (Exception e) {
			out.writeDefinition("has error", e.toString());
		}
		out.writeEndElement(); //dl
	}

	private void writeRemoteMethodInfo(MethodInfo info, XHTMLInfoWriter out) throws XMLStreamException {
		out.writeAnchor(info.getIRI(), info.getRemoteName());
		String accept = null;
		List<ParameterInfo> parameters = new ArrayList<ParameterInfo>();
		List<ParameterInfo> headers = new ArrayList<ParameterInfo>();
		ParameterInfo[] params = info.getParameterDescriptors();
		for (int i = 0; i < params.length; i++) {
			if (params[i].getQuery() != null
					&& !params[i].getQuery().equals("*")) {
				parameters.add(params[i]);
			} else if (params[i].getHeader() != null) {
				headers.add(params[i]);
			} else {
				accept = params[i].getType();
			}
		}
		if (!parameters.isEmpty() && info.getMethod().isAnnotationPresent(query.class)) {
			out.writeCharacters("&");
		} else if (!parameters.isEmpty()) {
			out.writeCharacters("?");
		}
		for (int i = 0, n = parameters.size(); i < n; i++) {
			if (i > 0) {
				out.writeCharacters("&");
			}
			ParameterInfo p = parameters.get(i);
			out.writeCharacters(p.getQuery());
			out.writeCharacters("=");
			out.writeLink(p.getIRI(), p.getType(), "{" + p.getName() + "}");
		}
		for (int i = 0, n = headers.size(); i < n; i++) {
			out.writeCharacters(" ");
			ParameterInfo h = headers.get(i);
			out.writeCharacters(h.getHeader());
			out.writeCharacters(": ");
			out.writeLink(h.getIRI(), h.getType(), "{" + h.getName() + "}");
		}
		out.writeStartElement("dl");
		out.writeDefinition("accept", accept);
		out.writeDefinition("response type", info.getType());
		out.writeEndElement(); //dl
	}
}
