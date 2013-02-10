package org.callimachusproject.util;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;

public interface MailPropertiesMXBean {

	Map<String, String> getMailProperties() throws IOException;

	void setMailProperties(Map<String, String> lines) throws IOException,
			MessagingException;

}