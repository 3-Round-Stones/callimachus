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
package org.callimachusproject.behaviours;

import java.io.IOException;

import org.callimachusproject.concepts.Party;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;

public abstract class PartySupport implements Party, CalliObject {

	public void resetCache() throws OpenRDFException, IOException {
		this.getObjectConnection().commit();
		getCalliRepository().resetCache();
	}

}
