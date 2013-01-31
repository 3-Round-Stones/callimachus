package org.callimachusproject.util;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.naming.NamingException;

public interface MailerMBean {

	String[] getMailProperties() throws IOException;

	void setMailProperties(String[] lines) throws IOException;

	void sendMessage(String html, String recipient) throws IOException,
			MessagingException, NamingException;

}