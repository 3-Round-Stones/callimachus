/*
 * Copyright (c) 2010, James Leigh Some rights reserved.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.NotImplemented;

/**
 * Provides methods to send emails to a configured SMTP server using the
 * properties in the system property java.mail.properties.
 * 
 * @author James Leigh
 * 
 */
public abstract class MailSubmitterSupport {
	private static final Pattern HTML_TITLE = Pattern
			.compile("<title>\\s*([^<]*)\\s*<.title>");

	public void sendMessage(String html, String recipient)
			throws IOException, MessagingException, NamingException {
		sendMessage(html, Collections.singleton(recipient));
	}

	public void sendMessage(String html, Set<String> recipients)
			throws IOException, MessagingException, NamingException {
		sendMessage(html, null, null, recipients);
	}

	public void sendMessage(String html, InputStream in, String mime,
			String recipient) throws IOException, MessagingException,
			NamingException {
		sendMessage(html, in, mime, Collections.singleton(recipient));
	}

	public void sendMessage(String html, InputStream in, String mime,
			Set<String> recipients) throws IOException, MessagingException,
			NamingException {
		if (recipients == null || recipients.isEmpty())
			throw new BadRequest("Missing to paramenter");
		Properties properties = new Properties();
		String javamail = System.getProperty("java.mail.properties");
		if (javamail != null) {
			InputStream stream = new FileInputStream(javamail);
			try {
				properties.load(stream);
			} finally {
				stream.close();
			}
		}
		Session session = Session.getInstance(properties);
		MimeMessage message = new MimeMessage(session);
		message.setFrom();
		message.setSentDate(new Date());
		if (recipients.size() == 1) {
			String to = recipients.iterator().next();
			message.addRecipients(RecipientType.TO, to);
		} else {
			for (String to : recipients) {
				message.addRecipients(RecipientType.BCC, to);
			}
		}
		Matcher m = HTML_TITLE.matcher(html);
		if (m.find()) {
			message.setSubject(m.group(1));
		}
		if (in == null) {
			message.setText(html, "UTF-8", "html");
		} else {
			MimeMultipart multipart = new MimeMultipart();
			MimeBodyPart part1 = new MimeBodyPart();
			part1.setText(html, "UTF-8", "html");
			multipart.addBodyPart(part1);
			MimeBodyPart part2 = new MimeBodyPart();
			ByteArrayDataSource source = new ByteArrayDataSource(in, mime);
			part2.setDataHandler(new DataHandler(source));
			part2.setDisposition("inline");
			multipart.addBodyPart(part2);
			message.setContent(multipart);
		}
		message.saveChanges();
		if (properties.containsKey("mail.transport.protocol")) {
			String user = session.getProperty("mail.user");
			String password = session.getProperty("mail.password");
			Transport tr = session.getTransport();
			try {
				tr.connect(user, password);
				tr.sendMessage(message, message.getAllRecipients());
			} finally {
				tr.close();
			}
		} else {
			properties.setProperty("mail.transport.protocol", "smtp");
			for (String to : recipients) {
				String domain = to.substring(to.indexOf('@') + 1);
				String host = findMailServer(domain);
				if (host == null)
					throw new NotImplemented("No Mail Server Configured");
				Transport tr = session.getTransport();
				try {
					tr.connect(host, 25, null, null);
					tr.sendMessage(message, message.getAllRecipients());
					return;
				} finally {
					tr.close();
				}
			}
		}
	}

	private String findMailServer(String domain) throws NamingException {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("java.naming.factory.initial",
				"com.sun.jndi.dns.DnsContextFactory");
		DirContext ictx = new InitialDirContext(env);
		Attributes attrs = ictx.getAttributes(domain, new String[] { "MX" });
		Enumeration e = attrs.getAll();
		if (e.hasMoreElements()) {
			Attribute a = (Attribute) e.nextElement();
			int size = a.size();
			if (size > 0) {
				String value = (String) a.get(0);
				if (value.indexOf(' ') >= 0)
					return value.substring(value.indexOf(' ') + 1);
				return value;
			}
		}
		return null;
	}
}
