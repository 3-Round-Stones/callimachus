package org.callimachusproject.io;

import static org.junit.Assert.*;

import java.net.URISyntaxException;

import org.junit.Test;

public class RelativizerTest {

	@Test
	public void testParentFile() throws URISyntaxException {
		assertEquals("../dir", new Relativizer("http://example.com/dir/dir/file").relativize("http://example.com/dir/dir"));
	}

	@Test
	public void testRootFile() throws URISyntaxException {
		assertEquals("/dir", new Relativizer("http://example.com/dir/dir").relativize("http://example.com/dir"));
	}

	@Test
	public void testIdentity() throws URISyntaxException {
		assertEquals("", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/dir/file?qs#frag"));
	}

	@Test
	public void testOpaque() throws URISyntaxException {
		assertEquals("urn:test", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("urn:test"));
	}

	@Test
	public void testFragment() throws URISyntaxException {
		assertEquals("#frag2", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/dir/file?qs#frag2"));
	}

	@Test
	public void testQueryString() throws URISyntaxException {
		assertEquals("?qs2#frag", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/dir/file?qs2#frag"));
	}

	@Test
	public void testDirectory() throws URISyntaxException {
		assertEquals(".", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/dir/"));
	}

	@Test
	public void testSameDirectory() throws URISyntaxException {
		assertEquals("file2?qs#frag", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/dir/file2?qs#frag"));
	}

	@Test
	public void testNestedDirectory() throws URISyntaxException {
		assertEquals("nested/file?qs#frag", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/dir/nested/file?qs#frag"));
	}

	@Test
	public void testParentDirectory() throws URISyntaxException {
		assertEquals("../file?qs#frag", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/file?qs#frag"));
	}

	@Test
	public void testOtherDirectory() throws URISyntaxException {
		assertEquals("../dir2/file?qs#frag", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir/dir2/file?qs#frag"));
	}

	@Test
	public void testSameAuthority() throws URISyntaxException {
		assertEquals("/dir2/dir/file?qs#frag", new Relativizer("http://example.com/dir/dir/file?qs#frag").relativize("http://example.com/dir2/dir/file?qs#frag"));
	}

	@Test
	public void testIdentityDir() throws URISyntaxException {
		assertEquals("", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir/"));
	}

	@Test
	public void testOpaqueDir() throws URISyntaxException {
		assertEquals("urn:test", new Relativizer("http://example.com/dir/dir/").relativize("urn:test"));
	}

	@Test
	public void testFragmentDir() throws URISyntaxException {
		assertEquals("#frag2", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir/#frag2"));
	}

	@Test
	public void testQueryStringDir() throws URISyntaxException {
		assertEquals("?qs2", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir/?qs2"));
	}

	@Test
	public void testDirectoryDir() throws URISyntaxException {
		assertEquals("file", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir/file"));
	}

	@Test
	public void testSameDirectoryDir() throws URISyntaxException {
		assertEquals("file2?qs#frag", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir/file2?qs#frag"));
	}

	@Test
	public void testNestedDirectoryDir() throws URISyntaxException {
		assertEquals("nested/", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir/nested/"));
	}

	@Test
	public void testNestedDirectoryFileDir() throws URISyntaxException {
		assertEquals("nested/file?qs#frag", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir/nested/file?qs#frag"));
	}

	@Test
	public void testParentDirectoryDir() throws URISyntaxException {
		assertEquals("../file?qs#frag", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/file?qs#frag"));
	}

	@Test
	public void testOtherDirectoryDir() throws URISyntaxException {
		assertEquals("../dir2/", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir2/"));
	}

	@Test
	public void testOtherDirectoryFileDir() throws URISyntaxException {
		assertEquals("../dir2/file?qs#frag", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir/dir2/file?qs#frag"));
	}

	@Test
	public void testSameAuthorityDir() throws URISyntaxException {
		assertEquals("/dir2/dir/file?qs#frag", new Relativizer("http://example.com/dir/dir/").relativize("http://example.com/dir2/dir/file?qs#frag"));
	}

}
