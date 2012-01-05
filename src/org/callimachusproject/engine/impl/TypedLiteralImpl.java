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
import org.callimachusproject.engine.model.TypedLiteral;

/**
 * A value as a string and its datatype.
 * 
 * @author James Leigh
 *
 */
public class TypedLiteralImpl extends TypedLiteral {
	private String label;
	private IRI datatype;

	public TypedLiteralImpl(String label, IRI datatype) {
		assert label != null;
		assert datatype != null;
		this.label = label;
		this.datatype = datatype;
	}

	public String getLabel() {
		return label;
	}

	public IRI getDatatype() {
		return datatype;
	}

	@Override
	public String stringValue() {
		return label;
	}

}
