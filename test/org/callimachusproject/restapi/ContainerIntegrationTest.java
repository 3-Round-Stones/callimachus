package org.callimachusproject.restapi;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import junit.framework.AssertionFailedError;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;

public class ContainerIntegrationTest extends
		TemporaryServerIntegrationTestCase {
	private static String CONTAINER_CLASS = cat(
			"@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"<Container> a </callimachus/1.4/types/Class>, owl:Class;",
			"		rdfs:label \"Container\";",
			"		owl:equivalentClass <http://www.w3.org/ns/ldp#Container>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Serviceable>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Composite>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Editable>;",
			"		calli:author </auth/groups/admin>.");
	private static String CONTAINER_RESOURCE = cat(
			"@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"<my-container> a <http://www.w3.org/ns/ldp#Container>; rdfs:label \"my container\".");
	private static String SOURCE_CLASS = cat(
			"@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"@prefix owl:<http://www.w3.org/2002/07/owl#>.",
			"@prefix calli:<http://callimachusproject.org/rdf/2009/framework#>.",
			"<RDFSource> a </callimachus/1.4/types/Class>, owl:Class;",
			"		rdfs:label \"Container\";",
			"		owl:equivalentClass <http://www.w3.org/ns/ldp#RDFSource>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Serviceable>;",
			"		rdfs:subClassOf </callimachus/1.4/types/Editable>;",
			"		calli:author </auth/groups/admin>.");
	private static String SOURCE_RESOURCE = cat(
			"@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>.",
			"<my-source> a <http://www.w3.org/ns/ldp#RDFSource>; rdfs:label \"my source\".");

	public ContainerIntegrationTest(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCreateAnyContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			getHomeFolder().rel("describedby")
					.create("text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateResourceContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateRDFSourceContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#RDFSource>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateContainerContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#Container>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateBasicContainerContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#BasicContainer>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} catch (AssertionFailedError e) {
			return;
		} finally {
			MyClass.delete();
		}
		fail();
	}

	public void testCreateDirectContainerContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#DirectContainer>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} catch (AssertionFailedError e) {
			return;
		} finally {
			MyClass.delete();
		}
		fail();
	}

	public void testCreateIndirectContainerContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#IndirectContainer>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateNonRDFSourceContainer() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						CONTAINER_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#NonRDFSource>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", CONTAINER_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateAnySource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			getHomeFolder().rel("describedby")
					.create("text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateResourceSource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateRDFSourceSource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#RDFSource>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	public void testCreateContainerSource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#Container>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} catch (AssertionFailedError e) {
			return;
		} finally {
			MyClass.delete();
		}
		fail();
	}

	public void testCreateBasicContainerSource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#BasicContainer>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} catch (AssertionFailedError e) {
			return;
		} finally {
			MyClass.delete();
		}
		fail();
	}

	public void testCreateDirectContainerSource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#DirectContainer>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} catch (AssertionFailedError e) {
			return;
		} finally {
			MyClass.delete();
		}
		fail();
	}

	public void testCreateIndirectContainerSource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#IndirectContainer>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} catch (AssertionFailedError e) {
			return;
		} finally {
			MyClass.delete();
		}
		fail();
	}

	public void testCreateNonRDFSourceSource() throws Exception {
		WebResource MyClass = waitForCompile(new Callable<WebResource>() {
			public WebResource call() throws Exception {
				return getHomeFolder().rel("describedby").create("text/turtle",
						SOURCE_CLASS.getBytes());
			}
		});
		try {
			Map<String, String> links = Collections
					.singletonMap("Link",
							"<http://www.w3.org/ns/ldp#NonRDFSource>;rel=\"type\"");
			getHomeFolder()
					.rel("describedby")
					.create(links, "text/turtle", SOURCE_RESOURCE.getBytes())
					.delete();
		} finally {
			MyClass.delete();
		}
	}

	private static String cat(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			sb.append(string).append("\n");
		}
		return sb.toString();
	}
}
