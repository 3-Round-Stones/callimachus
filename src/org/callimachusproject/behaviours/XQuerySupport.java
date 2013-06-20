package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;

import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.xml.XQueryValidator;
import org.openrdf.OpenRDFException;

public abstract class XQuerySupport implements CalliObject {

	public String[] getXQueryValidationErrors(InputStream queryStream)
			throws IOException, OpenRDFException {
		XQueryValidator validator = new XQueryValidator(this.toString(), this.getHttpClient());
		validator.parse(queryStream);
		return validator.getErrorMessages();
	}
}
