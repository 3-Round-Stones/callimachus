package org.callimachusproject.installer.validators;

import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;

public class MailServerValidator implements DataValidator {
	private MessagingException exception;

	public boolean getDefaultAnswer() {
		return true;
	}

	public String getErrorMessageId() {
		if (exception == null)
			return null;
		return exception.getMessage();
	}

	public String getWarningMessageId() {
		return "Run this installer again to setup a SMTP server";
	}

	public Status validateData(AutomatedInstallData adata) {
		try {

			Properties properties = new ConfigurationWriter()
					.getMailProperties(adata);
			String protocol = properties.getProperty("mail.transport.protocol");
			if (protocol == null)
				return Status.WARNING;

			Session session = Session.getInstance(properties);
			String user = session.getProperty("mail.user");
			String password = session.getProperty("mail.password");
			Transport tr = session.getTransport();
			try {
				tr.connect(user, password);
			} finally {
				tr.close();
			}

			return Status.OK;
		} catch (MessagingException e) {
			this.exception = e;
			e.printStackTrace();
			return Status.ERROR;
		} catch (IOException e) {
			e.printStackTrace();
			return Status.ERROR;
		}
	}

}
