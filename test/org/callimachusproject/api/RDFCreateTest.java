package org.callimachusproject.api;

import java.io.IOException;
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

public class RDFCreateTest extends TestCase {
	
	private static Map<String, String[]> parameters = new LinkedHashMap<String, String[]>() {
        private static final long serialVersionUID = -4308917786147773821L;

        {
        	put("SKOSConcept", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        		    " prefix skos: <http://www.w3.org/2004/02/skos/core#> \n " + 
        			" INSERT DATA {  \n <concept> a skos:Concept, </callimachus/Concept> ;  \n" +
        			" skos:prefLabel \"concept\" . }"
        	});
        	
        	put("Folder", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <test/> a calli:Folder, </callimachus/Folder> ;  \n" +
        			" rdfs:label \"test\" . }"
        	});
        	
        	put("Group", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <testGroup/> a calli:Party, calli:Group, </callimachus/Group> ;  \n" +
        			" rdfs:label \"testGroup\" . }"
        	});
        	
        	put("Menu", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <menu/> a calli:Menu, </callimachus/Menu> ;  \n" +
        			" rdfs:label \"menu\" . }"
        	});
        	
        	put("Theme", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <theme/> a calli:Theme, </callimachus/Theme> ;  \n" +
        			" rdfs:label \"theme\" . }"
        	});
        	
        	put("User", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <user> a calli:Party, calli:User, </callimachus/User> ;  \n" +
        			" rdfs:label \"user\" . }"
        	});
        }
    };

	public static TestSuite suite() throws Exception{
        TestSuite suite = new TestSuite(RDFCreateTest.class.getName());
        for (String name : parameters.keySet()) {
            suite.addTest(new RDFCreateTest(name));
        }
        return suite;
    }
	
	private static TemporaryServer temporaryServer = TemporaryServerFactory.getInstance().createServer();
	private String query;

	public RDFCreateTest(String name) throws Exception {
		super(name);
		String [] args = parameters.get(name);
		query = args[0];
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
	
	private String getRDFContents() throws MalformedURLException, IOException,
			ProtocolException {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"describedby\"");
		int start = header.lastIndexOf("<", rel);
		int end = header.lastIndexOf(">", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	public void runTest() throws MalformedURLException, Exception {
		URL url = new java.net.URL(getRDFContents());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/sparql-update");
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write(("BASE <" + temporaryServer.getOrigin() + "/> \n" + query).getBytes());
		output.close();
		assertEquals(connection.getResponseMessage(), 201, connection.getResponseCode());
	}

}
