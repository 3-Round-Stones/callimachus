/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.engine.impl;

import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.PlainLiteral;

/**
 * A string with an optional language.
 * 
 * @author James Leigh
 *
 */
public class PlainLiteralImpl extends PlainLiteral {
	private final String label;
	private final IRI datatype;
	private final String lang;

	public PlainLiteralImpl(String label, IRI string) {
		this(label, string, null);
	}

	public PlainLiteralImpl(String label, IRI langString, String lang) {
		assert label != null;
		assert langString != null;
		this.label = label;
		this.datatype = langString;
		this.lang = lang;
	}

	public String getLang() {
		return lang;
	}

	@Override
	public IRI getDatatype() {
		return datatype;
	}

	@Override
	public String stringValue() {
		return label;
	}

}
