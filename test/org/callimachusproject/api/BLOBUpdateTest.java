package org.callimachusproject.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BLOBUpdateTest extends TestCase {
	
	private static Map<String, String[]> parameters = new LinkedHashMap<String, String[]>() {
        private static final long serialVersionUID = -4308917786147773821L;

        {
        	put("article", new String[] { "article.docbook", "application/docbook+xml",
        					"<section id=\"ls\"> \n <title>LS command</title> \n " +
        					"<para>This command is a synonym for <link linkend=\"dir\"> <command>DIR</command></link> command. \n" +
        					"</para> \n </section>", //End original
        					"<section id=\"ls\"> \n <title>UPDATED LS command</title> \n " + //Begin update
        					"<para>This command (UPDATED) is a synonym for <link linkend=\"dir\"> <command>DIR</command></link> command. \n" +
        					"</para> \n </section>"
        	});
        	
        	put("font", new String[] { "font.woff", "application/font-woff",
        					"@font-face { \n font-family: GentiumTest; \n" +
        					"src: url(fonts/GenR102.woff) format(\"woff\")," +
        					"url(fonts/GenR102.ttf) format(\"truetype\"); }", //End original
        					"@font-face { \n font-family: GentiumTest; \n" + //Begin update
        					"src: url(fonts/GenR102.woff) format(\"woff\")," +
        					"url(fonts/GenR102.ttf) format(\"truetype\"); }"
        	});
        	
        	put("namedGraph", new String[] { "named.xml", "application/rdf+xml",
							"<rdf:RDF \n" +
        					"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n" +
        					"xmlns:dcterms=\"http://purl.org/dc/terms/\"> \n" +
        				    "<rdf:Description rdf:about=\"urn:x-states:New%20York\"> \n" +
        				    "<dcterms:alternative>NY</dcterms:alternative> \n" +
        				    "</rdf:Description> \n" +
        				    "</rdf:RDF>", //End original
        				    "<rdf:RDF \n" + //Begin update
        				    "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n" +
                			"xmlns:dcterms=\"http://purl.org/dc/terms/\"> \n" +
                			"<rdf:Description rdf:about=\"urn:x-states:New%20York\"> \n" +
                			"<dcterms:alternative>New York (UPDATED)</dcterms:alternative> \n" +
                			"</rdf:Description> \n" +
                			"</rdf:RDF>"
        	});
        	
        	put("namedQuery", new String[] { "query.sparql", "application/sparql-query",
							"SELECT ?title WHERE { \n" +
							"<http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title . \n" +
        					"}", //End original
        					"SELECT ?updatedTitle WHERE { \n" + //Begin update
							"<http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?updatedTitle . \n" +
        					"}" 
			});
        	
        	put("page", new String[] { "page.xhtml", "application/xhtml+xml",
							"<?xml version=\"1.0\" encoding=\"iso-8859-1\"?> <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \n " +
							"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia </title> </head> \n" +
							"<body> <p> Wikipedia is a great website. </p> </body> </html>", //End original
							"<?xml version=\"1.0\" encoding=\"iso-8859-1\"?> <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \n " + //Begin update
							"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
							"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
							"<head> <title> Wikipedia has been UPDATED </title> </head> \n" +
							"<body> <p> The UPDATED Wikipedia is a great website. </p> </body> </html>"
			});
        	
        	put("animatedGraphic", new String[] { "graphic.gif", "image/gif",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("photo", new String[] { "photo.jpeg", "image/jpeg",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("networkGraphic", new String[] { "network.png", "image/png",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("vectorGraphic", new String[] { "vector.svg", "image/svg+xml",
							"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">" +
							"<circle cx=\"100\" cy=\"50\" r=\"40\" stroke=\"black\"" +
							"stroke-width=\"2\" fill=\"red\" /> </svg> \n ", //End original
							"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">" + //Begin update
							"<circle cx=\"100\" cy=\"50\" r=\"40\" stroke=\"black\"" +
							"stroke-width=\"2\" fill=\"red\" /> </svg> \n "
			});
        	
        	put("iconGraphic", new String[] { "logo.image", "image/vnd.microsoft.icon",
							"binary", //End original 
							"UPDATED BINARY" //Begin update
			});
        	
        	put("style", new String[] { "style.css", "text/css",
							"hr {color:sienna;} \n" +
						    "p {margin-left:20px;} \n" +
						    "body {background-color:blue}", //End original
							"hr {color:black;} \n" + //Begin Update
						    "p {margin-left:0px;} \n" +
						    "body {background-color:black}"
			});
        	
            put("hypertext", new String[] { "file.html", "text/html",
                            "<html><head><title>Output Page</title></head><body></body></html>", //End original
                            "<html><head><title>Updated Page</title></head><body></body></html>" //Begin update
            });
            
            put("script", new String[] { "script.js", "text/javascript",
							"if (response == true) { \n" +
							"return true; \n" +
							"}", //End original 
							"if (response == UPDATED) { \n" + //Begin update
							"return true; \n" +
							"}"
			});
            
            put("text", new String[] { "file.txt", "text/plain",
            				"Body of a text document", //End original
            				"UPDATED body of a document" //Begin update
            });
            
            put("graphDocument", new String[] { "file.txt", "text/turtle",
							"@prefix foaf: <http://xmlns.com/foaf/0.1/> . \n" +
							"<http://example.org/joe#me> a foaf:Person . \n" +
							"<http://example.org/joe#me> foaf:homepage <http://example.org/joe/INDEX.html> . \n" +
							"<http://example.org/joe#me> foaf:mbox <mailto:joe@example.org> . \n" +
							"<http://example.org/joe#me> foaf:name \"Joe Lambda\" .", //End original
							"@prefix foaf: <http://xmlns.com/foaf/0.1/> . \n" + //Begin update
							"<http://example.org/joe#me> a foaf:Person . \n" +
							"<http://example.org/joe#me> foaf:homepage <http://example.org/joe/UPDATED/INDEX.html> . \n" +
							"<http://example.org/joe#me> foaf:mbox <mailto:joe@example.org> . \n" +
							"<http://example.org/joe#me> foaf:name \"UPDTAED Joe Lambda\" ."
							
            });
            
            put("transform", new String[] { "transform.xsl", "text/xsl",
							"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \n" +
						    "<xsl:template match=\"/\"> <html> <body> <h2>My CD Collection</h2> \n" +
						    "<table border=\"1\"> <tr bgcolor=\"#9acd32\"> <th>Title</th> \n" +
						    "<th>Artist</th>  </tr> </table> </body> </html> </xsl:template> \n" +
						    "</xsl:stylesheet>", //End original
						    "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \n" + //Begin update
						    "<xsl:template match=\"/\"> <html> <body> <h2>My CD Collection</h2> \n" +
						    "<table border=\"1\"> <tr bgcolor=\"#9acd32\"> <th>UPDATED Title</th> \n" +
						    "<th>UPDATED Artist</th>  </tr> </table> </body> </html> </xsl:template> \n" +
						    "</xsl:stylesheet>"
			});
        }
    };

    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite(BLOBUpdateTest.class.getName());
        for (String name : parameters.keySet()) {
            suite.addTest(new BLOBUpdateTest(name));
        }
        return suite;
    }

    private static TemporaryServer temporaryServer = TemporaryServer.newInstance();
	private String requestSlug;
	private String requestContentType;
	private String outputString;
	private String updateOutputString;

	public BLOBUpdateTest(String name) throws Exception {
		super(name);
		String [] args = parameters.get(name);
		requestSlug = args[0];
		requestContentType = args[1];
		outputString = args[2];
		updateOutputString = args[3];
	}


	public void setUp() throws Exception {
		super.setUp();
		temporaryServer.resume();
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication(temporaryServer.getUsername(), temporaryServer.getPassword()); 
		     }
		 });
	}

	public void tearDown() throws Exception {
		super.tearDown();
		temporaryServer.pause();
	}
	
	private String getCollection() throws Exception {
		String contents = getRelContents();
		
		URL contentsURL = new java.net.URL(contents);
		HttpURLConnection contentsConnection = (HttpURLConnection) contentsURL.openConnection();
		contentsConnection.setRequestMethod("GET");
		contentsConnection.setRequestProperty("ACCEPT", "application/atom+xml");
		assertEquals(contentsConnection.getResponseMessage(), 200, contentsConnection.getResponseCode());
		InputStream stream = contentsConnection.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		int app = text.indexOf("<app:collection");
		int start = text.indexOf("\"", app);
		int stop = text.indexOf("\"", start + 1);
		String result = text.substring(start + 1, stop);
		return result;
	}

	private String getRelContents() throws MalformedURLException, IOException,
			ProtocolException {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"contents\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	private String getLocation() throws Exception {
		URL url = new java.net.URL(getCollection());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Slug", requestSlug);
		connection.setRequestProperty("Content-Type", requestContentType);
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write(outputString.getBytes());
		output.close();
		assertEquals(connection.getResponseMessage(), 201, connection.getResponseCode());
		String header = connection.getHeaderField("Location");
		return header;
	}
	
	private String getEditMedia() throws Exception {
		URL url = new java.net.URL(getLocation());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"edit-media\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	public void runTest() throws Exception {
		URL url = new java.net.URL(getEditMedia());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("CONTENT-TYPE", requestContentType);
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write(updateOutputString.getBytes());
		output.close();
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
	}
}
