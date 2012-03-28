package org.callimachusproject.installer;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.callimachusproject.installer.helpers.Configure;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.InstallerException;
import com.izforge.izpack.util.AbstractUIProcessHandler;

public class PrimeServerProcess {
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
		}
	}

	public void processData(AutomatedInstallData adata) throws Exception {
    	Configure configure = Configure.getInstance(adata.getInstallPath());
		String openbrowser = adata.getVariable("callimachus.openbrowser");
		if (configure.isWebBrowserSupported() && "true".equals(openbrowser)) {
			if (!configure.primeServer(getPrimaryOrigin(adata))) {
				throw new InstallerException(
						"The server could not be be reached.\n"
								+ " Please ensure the port and primary authority are correct.\n"
								+ " If the problem persists check the log file and seek help.\n"
								+ " The server is listening on port "
								+ adata.getVariable("callimachus.PORT")
								+ " for " + getPrimaryOrigin(adata));
			}
			configure.openWebBrowser(getPrimaryOrigin(adata) + "/");
		}
	}

	private String getPrimaryOrigin(AutomatedInstallData adata) {
		return adata.getVariable("callimachus.PRIMARY_ORIGIN").split("\\s+")[0];
	}

}
