package org.callimachusproject.restapi;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.test.TemporaryServerTestCase;

public class RDFRetrieveTest extends TemporaryServerTestCase {

	private static Map<String, String[]> parameters = new LinkedHashMap<String, String[]>() {
        private static final long serialVersionUID = -4308917786147773821L;

        {
        	put("SKOSConcept", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        		    " prefix skos: <http://www.w3.org/2004/02/skos/core#> \n " + 
        			" INSERT DATA {  \n <read-concept> a skos:Concept, </callimachus/Concept> ;  \n" +
        			" skos:prefLabel \"concept\" . }",
        			"concept"
        	});
        	
        	put("Folder", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <read-test/> a calli:Folder, </callimachus/Folder> ;  \n" +
        			" rdfs:label \"test\" . }",
        			"test"
        	});
        	
        	put("Group", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <read-testGroup/> a calli:Party, calli:Group, </callimachus/Group> ;  \n" +
        			" rdfs:label \"testGroup\" . }",
        			"testGroup"
        	});
        	
        	put("Menu", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <read-menu/> a calli:Menu, </callimachus/Menu> ;  \n" +
        			" rdfs:label \"menu\" . }",
        			"menu"
        	});
        	
        	put("Theme", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <read-theme/> a calli:Theme, </callimachus/Theme> ;  \n" +
        			" rdfs:label \"theme\" . }",
        			"theme"
        	});
        	
        	put("User", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <read-user> a calli:Party, calli:User, </callimachus/User> ;  \n" +
        			" rdfs:label \"user\" . }",
        			"user"
        	});
        }
    };
    
	public static TestSuite suite() throws Exception{
        TestSuite suite = new TestSuite(RDFRetrieveTest.class.getName());
        for (String name : parameters.keySet()) {
            suite.addTest(new RDFRetrieveTest(name));
        }
        return suite;
    }
	
	private String query;
	private String compareText;

	public RDFRetrieveTest(String name) throws Exception {
		super(name);
		String [] args = parameters.get(name);
		query = args[0];
		compareText = args[1];
	}
	
	public void runTest() throws Exception {
		String update = "BASE <" + getHomeFolder() + "> \n" + query;
		String text = new String(getHomeFolder().link("describedby")
				.create("application/sparql-update", update.getBytes())
				.link("describedby").get("text/turtle"));
		assertTrue(text.contains(compareText));
	}
}