package org.callimachusproject.management;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class CalliKeyStore implements CalliKeyStoreMXBean {
	private static final int CERT_EXPIRE_DAYS = 31;
	private static final Map<String, String> defaultSystemProperties = new HashMap<String, String>();
	static {
		defaultSystemProperties.put("java.home", ".");
		defaultSystemProperties.put("javax.net.ssl.keyStore", ".keystore");
		defaultSystemProperties.put("javax.net.ssl.keyStorePassword", "changeit");
		defaultSystemProperties.put("javax.net.ssl.trustStore", "etc/truststore");
	}

	private final File baseDir;
	private final File etc;

	public CalliKeyStore(File baseDir) {
		this.baseDir = baseDir;
		this.etc = new File(baseDir, "etc");
	}

	public String toString() {
		return baseDir.toString();
	}

	public long getCertificateExperation() throws GeneralSecurityException,
			IOException {
		String alias = getKeyAlias();
		char[] password = getKeyStorePassword();
		return getCertificateExperation(CERT_EXPIRE_DAYS, alias, getKeyStore(),
				password);
	}

	public synchronized String exportCertificate() throws IOException,
			GeneralSecurityException {
		String alias = getKeyAlias();
		File file = new File(etc, alias + ".cer");
		if (file.isFile())
			return readString(file);
		return null;
	}

	public boolean isCertificateSigned() throws IOException,
			GeneralSecurityException {
		String alias = getKeyAlias();
		char[] password = getKeyStorePassword();
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		File file = new File(getKeyStore());
		if (!file.exists())
			return false;
		FileInputStream in = new FileInputStream(file);
		try {
			ks.load(in, password);
		} finally {
			in.close();
		}
		Certificate[] chain = ks.getCertificateChain(alias);
		return chain != null && chain.length > 1;
	}

	public synchronized String exportCertificateSigningRequest()
			throws IOException, GeneralSecurityException {
		String alias = getKeyAlias();
		File file = new File(etc, alias + ".csr");
		if (file.isFile())
			return readString(file);
		return null;
	}

	private String getKeyAlias() throws IOException, GeneralSecurityException {
		char[] password = getKeyStorePassword();
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		File file = new File(getKeyStore());
		if (!file.exists())
			return null;
		FileInputStream in = new FileInputStream(file);
		try {
			ks.load(in, password);
		} finally {
			in.close();
		}
		Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			if (ks.isKeyEntry(alias))
				return alias;
		}
		return null;
	}

	private char[] getKeyStorePassword() throws IOException {
		return getSystemProperty("javax.net.ssl.keyStorePassword")
				.toCharArray();
	}

	private String getKeyStore() throws IOException {
		return getSystemProperty("javax.net.ssl.keyStore");
	}

	private long getCertificateExperation(int days, String alias,
			String keystore, char[] password) throws GeneralSecurityException,
			IOException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		File file = new File(keystore);
		if (file.exists()) {
			FileInputStream in = new FileInputStream(file);
			try {
				ks.load(in, password);
			} finally {
				in.close();
			}
		} else {
			ks.load(null, password);
		}
		if (ks.isKeyEntry(alias)) {
			Certificate cert = ks.getCertificate(alias);
			if (cert instanceof X509Certificate) {
				return ((X509Certificate) cert).getNotAfter().getTime();
			}
		}
		return -1;
	}

	private void transfer(final InputStream in, final OutputStream out) {
		if (in != null) {
			try {
				try {
					int read;
					byte[] buf = new byte[1024];
					while ((read = in.read(buf)) >= 0) {
						out.write(buf, 0, read);
					}
				} finally {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String readString(File file) throws FileNotFoundException,
			UnsupportedEncodingException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		transfer(new FileInputStream(file), out);
		return new String(out.toByteArray());
	}

	private String getSystemProperty(String name) {
		String value = System.getProperty(name);
		if (value == null)
			return defaultSystemProperties.get(name);
		return value;
	}
}
