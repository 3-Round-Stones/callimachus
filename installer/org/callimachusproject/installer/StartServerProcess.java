package org.callimachusproject.installer;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.callimachusproject.installer.helpers.Configure;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.InstallerException;
import com.izforge.izpack.util.AbstractUIProcessHandler;

public class StartServerProcess {
	public void run(AbstractUIProcessHandler handler, String[] args) throws Exception {
		try {
			processData(ConfigurationWriter.automatedInstallData);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			for (String line : sw.toString().split("\n")) {
				handler.logOutput(line, true);
			}
			handler.emitErrorAndBlockNext(e.toString(), e.getMessage());
			throw e;
		}
	}

	public void processData(AutomatedInstallData adata) throws Exception {
    	Configure configure = Configure.getInstance(adata.getInstallPath());

		if ("true".equals(adata.getVariable("callimachus.startserver"))) {
			if (!configure.startServer()) {
				throw new InstallerException("The server could not be started.\n"
						+ " Please run this installer again and ensure the port and primary authority are correct.\n"
						+ " If the problem persists check the log file and seek help.");
			}
		}
	}

}
