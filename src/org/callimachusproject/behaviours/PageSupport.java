/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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

import static org.callimachusproject.util.PercentCodec.encode;

import java.io.IOException;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriClient;

/**
 * @author James Leigh
 * 
 */
public abstract class PageSupport implements CalliObject {

	/**
	 * Called from page.ttl query.ttl composite.ttl and editable.ttl
	 */
	public Template getTemplateFor(String uri) throws IOException,
			TemplateException, OpenRDFException {
		assert uri != null;
		String self = this.getResource().stringValue();
		String target = TermFactory.newInstance(self).resolve(uri);
		DetachedRealm realm = getCalliRepository().getRealm(target);
		if (realm == null) {
			realm = this.getRealm();
		}
		String url = self + "?layout&realm=" + encode(realm.toString());
		HttpUriClient hc = this.getHttpClient();
		return TemplateEngine.newInstance(hc).getTemplate(url);
	}
}
