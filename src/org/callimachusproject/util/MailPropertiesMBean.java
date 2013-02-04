package org.callimachusproject.util;

import java.io.IOException;

public interface MailPropertiesMBean {

	String[] getMailProperties() throws IOException;

	void setMailProperties(String[] lines) throws IOException;

}