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