package org.callimachusproject.management;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface CalliKeyStoreMXBean {

	long getCertificateExperation() throws GeneralSecurityException,
			IOException;

	String exportCertificate() throws IOException, GeneralSecurityException;

	boolean isCertificateSigned() throws IOException, GeneralSecurityException;

	String exportCertificateSigningRequest() throws IOException,
			GeneralSecurityException;

}