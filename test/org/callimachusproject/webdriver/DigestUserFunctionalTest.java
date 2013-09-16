package org.callimachusproject.webdriver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
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
import org.callimachusproject.webdriver.pages.DigestUserEdit;
import org.callimachusproject.webdriver.pages.GroupEdit;
import org.callimachusproject.webdriver.pages.PhotoEdit;
import org.callimachusproject.webdriver.pages.SignIn;

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
				.openInviteUser().with(fullname, email).subject(unique)
				.invite().save();
		page.logout();
		String testuser = getUniqueToken() + "-test-user";
		logger.info("Login {}", testuser);
		String profile = page
				.open(getRegistrationUrlByEmailSubject(unique), SignIn.class)
				.registerWithDigest()
				.with(testuser, "Password1", fullname, email).signup()
				.with(testuser, "Password1".toCharArray()).login()
				.openProfile().getCurrentUrl();
		page.logout();
		String username = getUsername();
		logger.info("Login {}", username);
		page.openLogin().with(username, getPassword()).login();
		page.open(profile).openEdit(DigestUserEdit.class).delete(fullname);
	}

	public void testUserProfilePhoto() throws Exception {
		DigestUserEdit user = page.openProfile().openEdit(DigestUserEdit.class);
		File jpeg = textToImage("test-photo");
		logger.info("Upload profile photo {}", "test-photo.jpg");
		user.openPhotoUpload().selectFile(jpeg).uploadAs("test-photo.jpg");
		jpeg.delete();
		user.save().openDescribe().describe("foaf:depiction")
				.openEdit(PhotoEdit.class).delete("test-photo");
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
				Message[] messages;
				if (subject == null) {
					messages = folder.getMessages();
				} else {
					messages = folder.search(new SubjectTerm(subject));
				}
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

	/**
	 * creates a black-on-white image with the text on it
	 * (Courier New, normal, 10pt)
	 * @param str	string to be displayed (single line only!)
	 * @return	the image or null on error
	 * @throws IOException 
	 */
	private File textToImage(String str) throws IOException {
		// set font
		Font font = new Font("Courier New", Font.PLAIN, 12);
		// calc/guess bounding of text
		FontRenderContext frc = new FontRenderContext(null, true, false);
		TextLayout layout = new TextLayout(str, font, frc);
		Rectangle2D bounds = layout.getBounds();
		// create image
		int width = (int) bounds.getWidth() + (int) bounds.getX() + 4;
		int height = (int) bounds.getHeight() + 2;
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		// set complete background to white
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		// set font and colors
		g.setFont(font);
		g.setBackground(Color.WHITE);
		g.setColor(Color.BLACK);
		// render text
		float posX = 1;
		float posY = Math.abs((float) bounds.getY());
		g.drawString(str, posX, posY);
		// return file
	    File outputfile = File.createTempFile(str.replaceAll("\\W", "_"), ".jpeg");
	    ImageIO.write(img, "jpeg", outputfile);
	    return outputfile;
	}

}
