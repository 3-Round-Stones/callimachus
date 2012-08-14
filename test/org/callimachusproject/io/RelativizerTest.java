package org.callimachusproject.io;

import static org.junit.Assert.*;

import java.net.URISyntaxException;

import org.junit.Test;

public class RelativizerTest {

	@Test
	public void testParentFile() throws URISyntaxException {
		assertRelative("../dir", "http://example.com/dir/dir/file", "http://example.com/dir/dir");
	}

	@Test
	public void testRootFile() throws URISyntaxException {
		assertRelative("/dir", "http://example.com/dir/dir", "http://example.com/dir");
	}

	@Test
	public void testFrag() throws URISyntaxException {
		assertRelative("#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag");
	}

	@Test
	public void testIdentity() throws URISyntaxException {
		assertRelative("", "http://example.com/dir/dir/file?qs", "http://example.com/dir/dir/file?qs");
	}

	@Test
	public void testOpaque() throws URISyntaxException {
		assertRelative("urn:test", "http://example.com/dir/dir/file?qs#frag", "urn:test");
	}

	@Test
	public void testFragment() throws URISyntaxException {
		assertRelative("#frag2", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag2");
	}

	@Test
	public void testQueryString() throws URISyntaxException {
		assertRelative("?qs2#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs2#frag");
	}

	@Test
	public void testDirectory() throws URISyntaxException {
		assertRelative(".", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/");
	}

	@Test
	public void testSameDirectory() throws URISyntaxException {
		assertRelative("file2?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file2?qs#frag");
	}

	@Test
	public void testNestedDirectory() throws URISyntaxException {
		assertRelative("nested/file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/nested/file?qs#frag");
	}

	@Test
	public void testParentDirectory() throws URISyntaxException {
		assertRelative("../file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/file?qs#frag");
	}

	@Test
	public void testOtherDirectory() throws URISyntaxException {
		assertRelative("../dir2/file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir2/file?qs#frag");
	}

	@Test
	public void testSameAuthority() throws URISyntaxException {
		assertRelative("/dir2/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir2/dir/file?qs#frag");
	}

	@Test
	public void testIdentityDir() throws URISyntaxException {
		assertRelative("", "http://example.com/dir/dir/", "http://example.com/dir/dir/");
	}

	@Test
	public void testOpaqueDir() throws URISyntaxException {
		assertRelative("urn:test", "http://example.com/dir/dir/", "urn:test");
	}

	@Test
	public void testFragmentDir() throws URISyntaxException {
		assertRelative("#frag2", "http://example.com/dir/dir/", "http://example.com/dir/dir/#frag2");
	}

	@Test
	public void testQueryStringDir() throws URISyntaxException {
		assertRelative("?qs2", "http://example.com/dir/dir/", "http://example.com/dir/dir/?qs2");
	}

	@Test
	public void testDirectoryDir() throws URISyntaxException {
		assertRelative("file", "http://example.com/dir/dir/", "http://example.com/dir/dir/file");
	}

	@Test
	public void testSameDirectoryDir() throws URISyntaxException {
		assertRelative("file2?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/dir/file2?qs#frag");
	}

	@Test
	public void testNestedDirectoryDir() throws URISyntaxException {
		assertRelative("nested/", "http://example.com/dir/dir/", "http://example.com/dir/dir/nested/");
	}

	@Test
	public void testNestedDirectoryFileDir() throws URISyntaxException {
		assertRelative("nested/file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/dir/nested/file?qs#frag");
	}

	@Test
	public void testParentDirectoryDir() throws URISyntaxException {
		assertRelative("../file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/file?qs#frag");
	}

	@Test
	public void testOtherDirectoryDir() throws URISyntaxException {
		assertRelative("../dir2/", "http://example.com/dir/dir/", "http://example.com/dir/dir2/");
	}

	@Test
	public void testOtherDirectoryFileDir() throws URISyntaxException {
		assertRelative("../dir2/file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/dir2/file?qs#frag");
	}

	@Test
	public void testSameAuthorityDir() throws URISyntaxException {
		assertRelative("/dir2/dir/file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir2/dir/file?qs#frag");
	}

	private void assertRelative(String relative, String base, String absolute) throws URISyntaxException {
		assertEquals(relative, new Relativizer(base).relativize(absolute));
	}

}
