/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
