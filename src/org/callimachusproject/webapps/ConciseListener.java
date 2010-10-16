package org.callimachusproject.webapps;

import java.io.PrintStream;

public class ConciseListener extends UploadListener {
	private PrintStream out;

	public ConciseListener(PrintStream out) {
		this.out = out;
	}

	@Override
	public void notifyError(String url, int code, String reason) {
		out.print("!");
	}

	@Override
	public void notifyNotModified(String url) {
		out.print(".");
	}

	@Override
	public void notifyReloaded() {
		out.println();
	}

	@Override
	public void notifyRemoved(String url) {
		out.print("~");
	}

	@Override
	public void notifyStarted() {
		out.println();
	}

	@Override
	public void notifyStarting() {
		out.print("Uploading Webapps");
	}

	@Override
	public void notifyUploaded() {
		out.print("^");
	}

}
