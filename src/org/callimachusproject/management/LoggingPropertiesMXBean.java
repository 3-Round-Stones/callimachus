package org.callimachusproject.management;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;

public interface LoggingPropertiesMXBean {

	Map<String, String> getLoggingProperties() throws IOException;

	void setLoggingProperties(Map<String, String> lines) throws IOException,
			MessagingException;

}
