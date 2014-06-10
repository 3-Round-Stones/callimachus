package org.callimachusproject.behaviours;

import java.io.IOException;

import javax.tools.FileObject;

import org.markdownj.MarkdownProcessor;

public abstract class MarkdownSupport implements FileObject {

	public String GetHTML() throws IOException {
		MarkdownProcessor markdown = new MarkdownProcessor();
		return markdown.markdown(this.getCharContent(true).toString());
	}

}
