package org.callimachusproject.util;

import java.io.IOException;

public interface MailPropertiesMXBean {

	String[] getMailProperties() throws IOException;

	void setMailProperties(String[] lines) throws IOException;

}