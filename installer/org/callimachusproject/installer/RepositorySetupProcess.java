package org.callimachusproject.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import javax.swing.JTextArea;

import org.callimachusproject.installer.helpers.Configure;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.util.AbstractUIProcessHandler;

public class RepositorySetupProcess {
	public void run(AbstractUIProcessHandler handler, String[] args) throws Exception {
		try {
			AutomatedInstallData adata = ConfigurationWriter.automatedInstallData;
			File dir = new File(adata.getInstallPath());
			printFile(new File(dir, "RELEASE_NOTES.txt"), handler);
			processData(adata);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			for (String line : sw.toString().split("\n")) {
				handler.logOutput(line, true);
			}
			handler.emitErrorAndBlockNext(e.toString(), e.getMessage());
		}
	}

	public void processData(AutomatedInstallData adata) throws Exception {
    	Configure configure = Configure.getInstance(adata.getInstallPath());

		if (configure.isConnected()) {
			String primary = getPrimaryOrigin(adata);

			setupAdminUser(adata, primary);

			configureOrigins(adata, primary);

			configure.disconnect();
		}
	}

	private void printFile(File file, AbstractUIProcessHandler handler)
			throws IOException {
		if (file.exists()) {
			JTextArea text = getTextArea(handler);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "UTF-8"));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					if (text == null) {
						handler.logOutput(line, false);
					} else {
						text.append(line + "\n");
					}
				}
			} finally {
				reader.close();
			}
		}
	}

	private JTextArea getTextArea(AbstractUIProcessHandler handler) {
		try {
			Field field = handler.getClass().getDeclaredField("outputPane");
			field.setAccessible(true);
			Object value = field.get(handler);
			if (value instanceof JTextArea)
				return (JTextArea) value;
		} catch (IllegalAccessException e) {
			// ignore
		} catch (SecurityException e) {
			// ignore
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getPrimaryOrigin(AutomatedInstallData adata) {
		return adata.getVariable("callimachus.PRIMARY_ORIGIN").split("\\s+")[0];
	}

	private void configureOrigins(AutomatedInstallData adata, String primary) throws Exception {
    	Configure configure = Configure.getInstance(adata.getInstallPath());
		String secondary = adata.getVariable("callimachus.SECONDARY_ORIGIN");
		for (String origin : secondary.split("\\s+")) {
			if (origin.length() > 0) {
				configure.createVirtualHost(origin, primary);
			}
		}
		String other = adata.getVariable("callimachus.OTHER_REALM");
		for (String realm : other.split("\\s+")) {
			if (realm.length() > 0) {
				configure.createRealm(realm, primary);
			}
		}
		if ("true".equals(adata.getVariable("callimachus.ALL_LOCAL"))) {
			configure.setResourcesAsLocalTo(primary);
		} else {
			configure.setResourcesAsLocalTo(null);
		}
	}

	private void setupAdminUser(AutomatedInstallData adata, String origin) throws Exception {
		String name = adata.getVariable("callimachus.fullname");
		String email = adata.getVariable("callimachus.email");
		String username = adata.getVariable("callimachus.username");
		String password = adata.getVariable("callimachus.password");
		if (username != null && username.length() > 0) {
        	Configure configure = Configure.getInstance(adata.getInstallPath());
			configure.createAdmin(name, email, username, password, origin);
		}
	}

}
