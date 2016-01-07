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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.mail.MessagingException;
import javax.naming.NamingException;

import org.callimachusproject.concepts.User;
import org.callimachusproject.util.Mailer;

/**
 * Provides methods to send emails to a configured SMTP server using the
 * properties in the system property java.mail.properties.
 * 
 * @author James Leigh
 * 
 */
public abstract class MailSubmitterSupport implements User {

	public void sendMessage(String html, String name, String email) throws IOException,
			MessagingException, NamingException {
		String address = "\"" + name.replace('"', '\'') + "\" <" + email + ">";
		sendMessage(html, Collections.singleton(address));
	}

	public void sendMessage(String html, String recipient) throws IOException,
			MessagingException, NamingException {
		sendMessage(html, Collections.singleton(recipient));
	}

	public void sendMessage(String html, Set<String> recipients)
			throws IOException, MessagingException, NamingException {
		new Mailer(getCalliFullName(), getCalliEmail()).sendMessage(html, recipients);
	}
}
