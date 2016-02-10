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
package org.callimachusproject.webdriver;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SubjectTerm;

import junit.framework.AssertionFailedError;
import junit.framework.TestSuite;

import org.callimachusproject.util.MailProperties;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.DigestUserEdit;
import org.callimachusproject.webdriver.pages.GroupEdit;
import org.callimachusproject.webdriver.pages.Register;
import org.openqa.selenium.By;

/**
 * This test requires a etc/mail.properties file to be present.
 * <pre>
 * mail.store.protocol=imaps
 * mail.imaps.port=993
 * mail.transport.protocol=smtps
 * mail.smtps.port=465
 * mail.smtps.auth=true
 * mail.host=secure.emailsrvr.com
 * mail.from=callimachus@3roundstones.com
 * mail.user=callimachus@3roundstones.com
 * mail.password=Password1
 * </pre>
 * 
 * @author James Leigh
 * 
 */
public class DigestUserFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(DigestUserFunctionalTest.class);
	}

	public DigestUserFunctionalTest() {
		super();
	}

	public DigestUserFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testRegistration() throws Exception {
		String unique = UUID.randomUUID().toString();
		Properties props = MailProperties.getInstance().loadMailProperties();
		String email = props.getProperty("mail.from");
		assertNotNull(email);
		logger.info("Inviting {}", email);
		String fullname = "Test User " + getUniqueToken();
		page.open("/auth/groups/users").openEdit(GroupEdit.class)
				.openInviteUser(email).with(fullname, email).subject(unique)
				.invite().save();
		page.logout();
		String testuser = getUniqueToken() + "-test-user";
		logger.info("Login {}", testuser);
		String profile = page
				.open(getRegistrationUrlByEmailSubject(unique), Register.class)
				.with(testuser, "Password1", fullname, email).signup()
				.with(testuser, "Password1".toCharArray()).login()
				.openProfile(testuser).getCurrentUrl();
		page.logout();
		logger.info("Request password for {}", email);
		page.openLogin().forgotPassword().with(email).request();
		browser.waitUntilTextPresent(By.id("success"), "sent");
		page.openLogin().with(email, getPasswordByEmail()).login();
		page.logout();
		String username = getUsername();
		logger.info("Login {}", username);
		page.openLogin().with(username, getPassword()).login();
		page.open(profile).openEdit(DigestUserEdit.class).delete(fullname);
	}

	public String getRegistrationUrlByEmailSubject(String subject)
			throws Exception {
		String html = getEmailBySubject(subject, "text/plain");
		assertNotNull(html);
		Matcher m = Pattern.compile("(http://[^\"\\s]+?register[^\"\\s]*)")
				.matcher(html);
		assertTrue(m.find());
		return m.group(1).replace("&amp;", "&");
	}

	public String getPasswordByEmail() throws Exception {
		String html = getEmailBySubject("Password", "text/plain");
		assertNotNull(html);
		Matcher m = Pattern.compile("password \"([^\"]+)\"").matcher(html);
		assertTrue(m.find());
		return m.group(1);
	}

	private String getEmailBySubject(String subject, String contentType) throws Exception {
		Properties props = MailProperties.getInstance().loadMailProperties();
		Session session = Session.getDefaultInstance(props);
		Store store = session.getStore();
		store.connect(session.getProperty("mail.user"),
				session.getProperty("mail.password"));
	
		Folder folder = store.getFolder("INBOX");
		folder.open(Folder.READ_WRITE);
	
		try {
			long since = System.currentTimeMillis();
			for (int i = 1; i < 25; i++) {
				Message[] messages;
				if (subject == null) {
					messages = folder.getMessages();
				} else {
					messages = folder.search(new SubjectTerm(subject));
				}
				long seconds = (System.currentTimeMillis() - since) / 1000;
				if (messages.length == 0) {
					logger.info("Waiting for {} email for {}s", subject, seconds);
					browser.reload();
					Thread.sleep(i * 1000);
				} else {
					int m = messages.length;
					Message message = null;
					while (message == null && m > 0) {
						message = messages[--m]; // might be null
					}
					if (message == null) {
						logger.info("Waiting for {} email for {}s", subject, seconds);
						browser.reload();
						Thread.sleep(i * 1000);
					} else {
						message.setFlag(Flags.Flag.DELETED, true);
						return getPart(message, contentType);
					}
				}
			}
			fail("Could not find " + subject + " email");
            throw new AssertionFailedError();
		} finally {
			folder.close(true);
			store.close();
		}
	}

	private String getPart(Message message, String type) throws IOException,
			MessagingException {
		Object content = message.getContent();
		if (content instanceof Multipart) {
			Multipart mp = (Multipart) content;
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart bp = mp.getBodyPart(i);
				if (bp.getContentType().toLowerCase().contains(type)) {
					return (String) bp.getContent();
				}
			}
		} else if (content instanceof String) {
			return (String) content;
		}
		return null;
	}

}
