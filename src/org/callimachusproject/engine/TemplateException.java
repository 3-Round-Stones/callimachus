package org.callimachusproject.engine;

public class TemplateException extends Exception {
	private static final long serialVersionUID = -5246565333133265356L;

	public TemplateException(Throwable cause) {
		super(cause);
	}

	public TemplateException(String message, Throwable cause) {
		super(message, cause);
	}

}
