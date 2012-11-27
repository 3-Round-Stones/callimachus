package org.callimachusproject.util;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StringUrlCodecTest extends TestCase {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAmazon() throws EncoderException, DecoderException, UnsupportedEncodingException {
		StringUrlCodec codec = new StringUrlCodec();
		String decoded = "http://www.amazon.com/Kindle-Wireless-Reading-Display-Globally/dp/B003FSUDM4/ref=amb_link_353259562_2?pf_rd_m=ATVPDKIK X0DER&pf_rd_s=center-10&pf_rd_r=11EYKTN682A79T370AM3&pf_rd_ t=201&pf_rd_p=1270985982&pf_rd_i=B002Y27P3M";
		String encoded = codec.encode(decoded);
		assertRoundTrip(decoded, codec);
	}

	@Test
	public void testWiki() throws EncoderException, DecoderException, UnsupportedEncodingException {
		StringUrlCodec codec = new StringUrlCodec();
		String decoded = "http://en.example.org/wiki/URL";
		String encoded = codec.encode(decoded);
		assertRoundTrip(decoded, codec);
	}

	@Test
	public void testInvalid() throws EncoderException, DecoderException, UnsupportedEncodingException {
		StringUrlCodec codec = new StringUrlCodec();
		String decoded = "this is an invalid URL, but who cares just encode it anyway";
		String encoded = codec.encode(decoded);
		assertRoundTrip(decoded, codec);
	}

	@Test
	public void testOpenID() throws EncoderException, DecoderException, UnsupportedEncodingException {
		StringUrlCodec codec = new StringUrlCodec("https:////?openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.mode=id_res&openid.op_endpoint=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fud&openid.response_nonce=--T%3A%3AZ_&openid.return_to=https%3A%2F%2F%2F%2F&openid.assoc_handle=&openid.signed=op_endpoint%2Cclaimed_id%2Cidentity%2Creturn_to%2Cresponse_nonce%2Cassoc_handle&openid.sig=%3D&openid.identity=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawn1Sq0QkK0tQr8VAs30GpEwr5H5trZCyHopenid.claimed_id=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3D");
		String decoded = "https://localhost/openid_verify?openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.mode=id_res&openid.op_endpoint=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fud&openid.response_nonce=2012-08-28T16%3A56%3A25Zezskld_oRHCIAA&openid.return_to=https%3A%2F%2Fsourceforge.net%2Faccount%2Fopenid_verify.php&openid.assoc_handle=AMlYA9X85sqV5ceJ-E2_t1S9DgFEtbAu5zm8xScfaG2pZM_Y5TUyUJcenDeqp4FT7BshPrEx&openid.signed=op_endpoint%2Cclaimed_id%2Cidentity%2Creturn_to%2Cresponse_nonce%2Cassoc_handle&openid.sig=I2ehuhV6XNU8IuV3HaFMhGTfDzLSYTLoaT0MYCIuQS0%3D&openid.identity=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawn1Sq0QkK0tQr8VAs30GpEwr5H5trZCyHk&openid.claimed_id=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawn1Sq0QkK0tQr8VAs30GpEwr5H5trZCyHk";
		String encoded = codec.encode(decoded);
		assertRoundTrip(decoded, codec);
	}

	@Test
	public void testDelim() throws EncoderException, DecoderException, UnsupportedEncodingException {
		StringUrlCodec codec = new StringUrlCodec("https://.org?_com&net=https%3A%3F%2C");
		String decoded = "https://localhost/openid_verify?openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.mode=id_res&openid.op_endpoint=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fud&openid.response_nonce=2012-08-28T16%3A56%3A25Zezskld_oRHCIAA&openid.return_to=https%3A%2F%2Fsourceforge.net%2Faccount%2Fopenid_verify.php&openid.assoc_handle=AMlYA9X85sqV5ceJ-E2_t1S9DgFEtbAu5zm8xScfaG2pZM_Y5TUyUJcenDeqp4FT7BshPrEx&openid.signed=op_endpoint%2Cclaimed_id%2Cidentity%2Creturn_to%2Cresponse_nonce%2Cassoc_handle&openid.sig=I2ehuhV6XNU8IuV3HaFMhGTfDzLSYTLoaT0MYCIuQS0%3D&openid.identity=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawn1Sq0QkK0tQr8VAs30GpEwr5H5trZCyHk&openid.claimed_id=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawn1Sq0QkK0tQr8VAs30GpEwr5H5trZCyHk";
		String encoded = codec.encode(decoded);
		assertRoundTrip(decoded, codec);
	}

	@Test
	public void testNet() throws EncoderException, DecoderException, UnsupportedEncodingException {
		StringUrlCodec codec = new StringUrlCodec("https://openid.net?_&openid=https%3A%3F%2C");
		String decoded = "https://localhost/openid_verify?openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.mode=id_res&openid.op_endpoint=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fud&openid.response_nonce=2012-08-28T16%3A56%3A25Zezskld_oRHCIAA&openid.return_to=https%3A%2F%2Fsourceforge.net%2Faccount%2Fopenid_verify.php&openid.assoc_handle=AMlYA9X85sqV5ceJ-E2_t1S9DgFEtbAu5zm8xScfaG2pZM_Y5TUyUJcenDeqp4FT7BshPrEx&openid.signed=op_endpoint%2Cclaimed_id%2Cidentity%2Creturn_to%2Cresponse_nonce%2Cassoc_handle&openid.sig=I2ehuhV6XNU8IuV3HaFMhGTfDzLSYTLoaT0MYCIuQS0%3D&openid.identity=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawn1Sq0QkK0tQr8VAs30GpEwr5H5trZCyHk&openid.claimed_id=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawn1Sq0QkK0tQr8VAs30GpEwr5H5trZCyHk";
		String encoded = codec.encode(decoded);
		assertRoundTrip(decoded, codec);
	}

	private void assertRoundTrip(String decoded, StringUrlCodec codec) throws EncoderException,
			DecoderException {
		String encoded = codec.encode(decoded);
		assertEquals(decoded, codec.decode(encoded));
	}

}
