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

import junit.framework.TestSuite;

import org.callimachusproject.util.MailProperties;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.GroupEdit;
import org.callimachusproject.webdriver.pages.Register;

/**
 * This test requires a etc/mail.properties file to be present.
 * <pre>
 * mail.store.protocol=pop3s
 * mail.pop3s.port=995
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
public class RegistrationFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(RegistrationFunctionalTest.class);
	}

	public RegistrationFunctionalTest() {
		super();
	}

	public RegistrationFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testRegistration() throws Exception {
		String unique = UUID.randomUUID().toString();
		Properties props = MailProperties.getInstance().loadMailProperties();
		String email = props.getProperty("mail.from");
		assertNotNull(email);
		logger.info("Inviting {}", email);
		page.open("/auth/groups/users").openEdit(GroupEdit.class)
				.openInviteUser().with("Test User", email).subject(unique)
				.invite().save();
		page.logout();
		logger.info("Login {}", "test-user");
		page.open(getRegistrationUrlByEmailSubject(unique), Register.class)
				.with("test-user", "Password1", "Test User", email).signup()
				.with("test-user", "Password1".toCharArray()).login();
		page.logout();
		String username = getUsername();
		logger.info("Login {}", username);
		page.openLogin().with(username, getPassword()).login();
	}

	private String getRegistrationUrlByEmailSubject(String subject)
			throws Exception {
		String html = getEmailBySubject(subject, "text/plain");
		assertNotNull(html);
		Matcher m = Pattern.compile("(http://[^\"\\s]+?register[^\"\\s]*)")
				.matcher(html);
		assertTrue(m.find());
		return m.group(1).replace("&amp;", "&");
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
			for (int i = 0; i < 60; i++) {
				Message[] messages = folder.search(new SubjectTerm(subject));
				if (messages.length == 0) {
					Thread.sleep(1000);
				} else {
					Message message = messages[0];
					message.setFlag(Flags.Flag.DELETED, true);
					return getPart(message, contentType);
				}
			}
			return null;
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
