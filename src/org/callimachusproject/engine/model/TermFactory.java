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
package org.callimachusproject.engine.model;

import org.callimachusproject.engine.impl.TermFactoryImpl;

/**
 * Factory class for RDF terms.
 * 
 * @author James Leigh
 *
 */
public abstract class TermFactory extends AbsoluteTermFactory {
	public static TermFactory newInstance(String systemId) {
		return new TermFactoryImpl(systemId);
	}

	public abstract String getSystemId();

	public abstract Reference base(String reference);

	public abstract String resolve(String reference);

	public abstract Reference reference(String reference);

	public abstract Reference prefix(String prefix, String reference);

	public abstract CURIE curie(String prefix, String reference);
}
