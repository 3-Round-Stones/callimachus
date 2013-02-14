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

import org.callimachusproject.util.SystemProperties;

public class CalliKeyStore implements CalliKeyStoreMXBean {
	private static final int CERT_EXPIRE_DAYS = 31;

	private final File cerDir;

	public CalliKeyStore(File cerDir) {
		this.cerDir = cerDir;
	}

	public String toString() {
		return cerDir.toString();
	}

	public long getCertificateExperation() throws GeneralSecurityException,
			IOException {
		String alias = getKeyAlias();
		char[] password = SystemProperties.getKeyStorePassword();
		return getCertificateExperation(CERT_EXPIRE_DAYS, alias, SystemProperties.getKeyStoreFile(),
				password);
	}

	public synchronized String exportCertificate() throws IOException,
			GeneralSecurityException {
		String alias = getKeyAlias();
		File file = new File(cerDir, alias + ".cer");
		if (file.isFile())
			return readString(file);
		return null;
	}

	public boolean isCertificateSigned() throws IOException,
			GeneralSecurityException {
		String alias = getKeyAlias();
		char[] password = SystemProperties.getKeyStorePassword();
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		File file = SystemProperties.getKeyStoreFile();
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
		File file = new File(cerDir, alias + ".csr");
		if (file.isFile())
			return readString(file);
		return null;
	}

	private String getKeyAlias() throws IOException, GeneralSecurityException {
		char[] password = SystemProperties.getKeyStorePassword();
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		File file = SystemProperties.getKeyStoreFile();
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

	private long getCertificateExperation(int days, String alias,
			File keystore, char[] password) throws GeneralSecurityException,
			IOException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		if (keystore.exists()) {
			FileInputStream in = new FileInputStream(keystore);
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
}
