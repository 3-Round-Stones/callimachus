package org.callimachusproject.engine.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.Location;

import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.TermOrigin;
import org.openrdf.model.Value;

public class ExpressionUtil {

	public static boolean isEmpty(CharSequence characterData,
			NamespaceContext namespaces, Location location)
			throws RDFParseException {
		if (characterData == null)
			return false;
		ExpressionFactory ef = new ExpressionFactory();
		return !ef.parse(characterData, namespaces, location)
				.isPatternPresent();
	}

	public static List<RDFEvent> pattern(AbsoluteTermFactory tf, Node subj,
			CharSequence value, TermOrigin origin, Location location,
			NamespaceContext namespaces) throws RDFParseException {
		if (subj == null)
			return Collections.emptyList();
		ExpressionFactory ef = new ExpressionFactory();
		return ef.parse(value, namespaces, location).pattern(subj, origin,
				location);
	}

	public static String substitute(String text, Location location,
			final Map<String, Value> assignments,
			final Map<String, TermOrigin> origins, NamespaceContext namespaces)
			throws RDFParseException {
		ExpressionFactory ef = new ExpressionFactory();
		return ef.parse(text, namespaces, location)
				.bind(new ExpressionResult() {

					@Override
					public String getVariable(String name, Location location) {
						if (assignments.containsKey(name))
							return assignments.get(name).stringValue();
						return "";
					}

					@Override
					public String getPropertyValue(String property,
							Location location) {
						String var = getVar(property, location, origins);
						Value val = assignments.get(var);
						if (val == null)
							return "";
						return val.stringValue();
					}
				}).toString();
	}

	private static String getVar(String property, Location location,
			Map<String, TermOrigin> origins) {
		for (String name : origins.keySet()) {
			if (origins.get(name).isAnonymous()
					&& origins.get(name).hasLocation(location)
					&& origins.get(name).propertyEquals(property))
				return name;
		}
		return null;
	}

}
