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

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.util.AbstractUIProcessHandler;

public class DisplayReleaseNotesProcess {
	public void run(AbstractUIProcessHandler handler, String[] args) throws Exception {
		try {
			AutomatedInstallData adata = ConfigurationWriter.automatedInstallData;
			File dir = new File(adata.getInstallPath());
			printFile(new File(dir, "RELEASE_NOTES.txt"), handler);
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
			// console mode
		}
		return null;
	}

}
