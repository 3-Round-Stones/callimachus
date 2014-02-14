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
package org.callimachusproject.xproc;

import net.sf.saxon.s9api.XdmNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

import com.xmlcalabash.core.XProcException;

public final class XProcErrorHandler implements ErrorHandler {
    private final Logger logger = LoggerFactory.getLogger(XProcErrorHandler.class);
    private XdmNode node;
	public XProcErrorHandler(XdmNode node) {
		super();
		this.node = node;
	}

	public void warning(CSSParseException e)
			throws CSSException {
		String msg = e.getURI() + "#" + e.getLineNumber() + ":"
				+ e.getColumnNumber() + " " + e.getMessage();
		logger.warn(msg, e);
	}

	public void error(CSSParseException e) throws CSSException {
		String msg = e.getURI() + "#" + e.getLineNumber() + ":"
				+ e.getColumnNumber() + " " + e.getMessage();
		logger.error(msg, e);
		throw XProcException.dynamicError(30, node,
				msg);
	}

	public void fatalError(CSSParseException e)
			throws CSSException {
		String msg = e.getURI() + "#" + e.getLineNumber() + ":"
				+ e.getColumnNumber() + " " + e.getMessage();
		logger.error(msg, e);
		throw XProcException.dynamicError(30, node,
				msg);
	}
}
