package org.callimachusproject.engine.expressions;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.Location;

import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.model.AbsoluteTermFactory;

public class ExpressionFactory {
	private AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();

	public AbsoluteTermFactory getTermFactory() {
		return tf;
	}

	public void setTermFactory(AbsoluteTermFactory tf) {
		this.tf = tf;
	}

	public Expression parse(CharSequence text, NamespaceContext namespaces, Location location) throws RDFParseException {
		return new MarkupExpression(text, namespaces, location, tf);
	}

}
