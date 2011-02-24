package org.callimachusproject.behaviours;

import java.beans.IntrospectionException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.callimachusproject.helpers.ResourceInfo;
import org.callimachusproject.helpers.XHTMLInfoWriter;
import org.callimachusproject.helpers.ResourceInfo.MethodInfo;
import org.callimachusproject.helpers.ResourceInfo.ParameterInfo;
import org.callimachusproject.helpers.ResourceInfo.PropertyInfo;
import org.openrdf.http.object.annotations.query;
import org.openrdf.model.URI;
import org.openrdf.repository.object.RDFObject;

public class IntrospectSupport {
	public ByteArrayOutputStream calliIntrospect(RDFObject target)
			throws XMLStreamException, FactoryConfigurationError,
			IntrospectionException {
		ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
		XHTMLInfoWriter out = new XHTMLInfoWriter(stream);

		ResourceInfo info = new ResourceInfo(target.getClass());
		MethodInfo[] operations = info.getRemoteMethodDescriptors();
		PropertyInfo[] properties = info.getPropertyDescriptors();
		MethodInfo[] methods = info.getMethodDescriptors();

		out.writeStartDocument(((URI) target.getResource()).getLocalName()
				+ " Introspection");

		// table of content
		out.writeStartElement("ul");
		out.writeAttribute("class", "aside");

		out.writeStartElement("li");
		out.writeLabel("Operations");
		out.writeStartElement("ul");
		for (MethodInfo m : operations) {
			out.writeStartElement("li");
			out.writeLink("#" + m.getRemoteName(), m.getRemoteName());
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul
		out.writeEndElement(); //li

		out.writeStartElement("li");
		out.writeLabel("Properties");
		out.writeStartElement("ul");
		for (PropertyInfo p : properties) {
			out.writeStartElement("li");
			out.writeLink("#" + p.getName(), p.getName());
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul
		out.writeEndElement(); //li

		out.writeStartElement("li");
		out.writeLabel("Methods");
		out.writeStartElement("ul");
		for (MethodInfo m : methods) {
			out.writeStartElement("li");
			out.writeLink("#" + m.getName(), m.getName());
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul
		out.writeEndElement(); //li

		out.writeEndElement(); //ul

		// content
		out.writeStartElement("ul");
		for (MethodInfo m : operations) {
			out.writeStartElement("li");
			writeRemoteMethodInfo(m, out);
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul

		out.writeEmptyElement("hr");

		out.writeStartElement("ul");
		for (PropertyInfo p : properties) {
			out.writeStartElement("li");
			writePropertyInfo(p, target, out);
			out.writeEndElement(); //li
		}
		out.writeEndElement(); //ul

		out.writeEmptyElement("hr");

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
		out.writeStartElement("ul");
		for (int i = 0; i < params.length; i++) {
			out.writeListItem("parameter type", params[i].getGenericType().toString());
		}
		out.writeListItem("return type", info.getGenericReturnType().toString());
		out.writeEndElement(); //ul
	}

	private void writePropertyInfo(PropertyInfo info, Object target, XHTMLInfoWriter out)
			throws XMLStreamException {
		out.writeAnchor(info.getIRI(), info.getName());
		out.writeStartElement("ul");
		out.writeListItem("return type", info.getGenericType().toString());
		try {
			Object value = info.getReadMethod().invoke(target);
			out.writeListItem("has value", String.valueOf(value));
		} catch (Exception e) {
			out.writeListItem("has error", e.toString());
		}
		out.writeEndElement(); //ul
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
		out.writeStartElement("ul");
		out.writeListItem("accept", accept);
		out.writeListItem("response type", info.getType());
		out.writeEndElement(); //ul
	}
}
