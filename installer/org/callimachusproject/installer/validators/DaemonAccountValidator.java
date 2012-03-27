package org.callimachusproject.installer.validators;

import java.io.IOException;
import java.io.InputStream;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;

public class DaemonAccountValidator implements DataValidator {
	private String error;
	private String warning;

	public boolean getDefaultAnswer() {
		return false;
	}

	public synchronized String getErrorMessageId() {
		return error;
	}

	public synchronized String getWarningMessageId() {
		return warning;
	}

	public synchronized Status validateData(AutomatedInstallData adata) {
		try {

			String home = adata.getInstallPath();
			String group = adata.getVariable("callimachus.DAEMON_GROUP");
			String user = adata.getVariable("callimachus.DAEMON_USER");

			if ("true".equals(adata.getVariable("callimachus.daemon.autocreate"))) {
				if (!launch("groupadd", "-r", group)) {
					warning = "Could not create group " + group;
					return Status.WARNING;
				}
				if (!launch("useradd", "-d", home, "-g", group, "-r", user)) {
					warning = "Could not create group " + user;
					return Status.WARNING;
				}
				adata.setVariable("callimachus.daemon.autocreate", "false");
			} else {
				if (!launch("grep", group, "/etc/group")) {
					warning = "Group " + group + " not found";
					return Status.WARNING;
				}
				if (!launch("id", user)) {
					warning = "User " + user + " not found";
					return Status.WARNING;
				}
			}

			return Status.OK;
		} catch (IOException e) {
			e.printStackTrace();
			error = e.getMessage();
			return Status.ERROR;
		} catch (InterruptedException e) {
			// Restore the interrupted status
            Thread.currentThread().interrupt();
			error = e.toString();
            return Status.ERROR;
		}
	}

	private boolean launch(String... command) throws IOException, InterruptedException {
		Process p = exec(command);
		if (p == null)
			return false;
		InputStream in = p.getInputStream();
		try {
			int read;
			byte[] buf = new byte[1024];
			while ((read = in.read(buf)) >= 0) {
				System.err.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
		return p.waitFor() == 0;
	}

	private Process exec(String... command) throws IOException {
		ProcessBuilder process = new ProcessBuilder(command);
		process.redirectErrorStream(true);
		return process.start();
	}

}
