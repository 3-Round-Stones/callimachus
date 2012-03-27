package org.callimachusproject.installer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import com.izforge.izpack.panels.ProcessingClient;
import com.izforge.izpack.panels.Validator;

public class PortAvailabilityValidator implements Validator {

	private static final int MAX_PORT = 49151;

	public boolean validate(int port) {
		if (port < 1 || port > MAX_PORT)
			return false;
		try {
			ServerSocket ss = new ServerSocket(port);
			try {
				ss.setReuseAddress(true);
				DatagramSocket ds = new DatagramSocket(port);
				try {
					ds.setReuseAddress(true);
					return true;
				} finally {
					ds.close();
				}
			} finally {
				ss.close();
			}
		} catch (IOException e) {
			return false;
		}
	}

	public boolean validate(String port) {
		if (port == null)
			return false;
		String[] ports = port.trim().split("\\s+");
		if (ports.length < 1)
			return false;
		for (String p : ports) {
			try {
				if (!validate(Integer.parseInt(p)))
					return false;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean validate(ProcessingClient client) {
		return validate(client.getText());
	}

}
