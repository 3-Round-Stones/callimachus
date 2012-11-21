package org.callimachusproject.restapi;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.test.TemporaryServerTestCase;
import org.callimachusproject.test.WebResource;

public class RdfTest extends TemporaryServerTestCase {
	
	private static Map<String, String[]> parameters = new LinkedHashMap<String, String[]>() {
        private static final long serialVersionUID = -4308917786147773821L;

        {
        	put("SKOSConcept", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        		    " prefix skos: <http://www.w3.org/2004/02/skos/core#> \n " + 
        			" INSERT DATA {  \n <created-concept> a skos:Concept, </callimachus/Concept> ;  \n" +
        			" skos:prefLabel \"concept\" . }"
        	});
        	
        	put("Folder", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-test/> a calli:Folder, </callimachus/Folder> ;  \n" +
        			" rdfs:label \"test\" . }"
        	});
        	
        	put("Group", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-testGroup/> a calli:Party, calli:Group, </callimachus/Group> ;  \n" +
        			" rdfs:label \"testGroup\" . }"
        	});
        	
        	put("Menu", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-menu/> a calli:Menu, </callimachus/Menu> ;  \n" +
        			" rdfs:label \"menu\" . }"
        	});
        	
        	put("Theme", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-theme/> a calli:Theme, </callimachus/Theme> ;  \n" +
        			" rdfs:label \"theme\" . }"
        	});
        	
        	put("User", new String[] {
        			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        		    " prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        		    " prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
        			" INSERT DATA {  \n <created-user> a calli:Party, calli:User, </callimachus/User> ;  \n" +
        			" rdfs:label \"user\" . }"
        	});
        }
    };

	public static TestSuite suite() throws Exception{
        TestSuite suite = new TestSuite(RdfTest.class.getName());
        for (String name : parameters.keySet()) {
            suite.addTest(new RdfTest(name));
        }
        return suite;
    }
	
	private String query;

	public RdfTest(String name) throws Exception {
		super(name);
		String [] args = parameters.get(name);
		query = args[0];
	}
	
	public void runTest() throws Exception {
		String update = "BASE <" + getHomeFolder() + "> \n" + query;
		WebResource resource = getHomeFolder().link("describedby")
				.create("application/sparql-update", update.getBytes());
		resource.link("alternate", "text/html").get("text/html");
		resource.link("edit", "text/html").get("text/html");
		resource.ref("?discussion").get("text/html");
		resource.link("describedby", "text/turtle").get("text/turtle");
		resource.link("describedby", "application/rdf+xml").get("application/rdf+xml");
		resource.link("describedby", "text/html").get("text/html");
		resource.link("version-history", "text/html").get("text/html");
		resource.link("version-history", "application/atom+xml").get("application/atom+xml");
		resource.ref("?permissions").get("text/html");
		resource.ref("?rdftype").get("text/uri-list");
		resource.ref("?relatedchanges").get("text/html");
		resource.ref("?whatlinkshere").get("text/html");
		resource.ref("?introspect").get("text/html");
		resource.link("describedby").delete();
	}

}
