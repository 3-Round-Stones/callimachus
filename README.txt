Callimachus Project Installation Instructions

http://callimachusproject.org/

5 July 2010

----------------
| Introduction |
----------------

Thank you for your interest in the Callimachus Project.  Callimachus
aims to make Semantic Web applications (much) easier to create.
Because this is a community effort, you can help by submitting your
applications, upgrades and tools to the Callimachus Project site at
http://callimachusproject.org.

Please consider joining the mailing lists and participating in the
online community so we can serve you better.

Version-specific information is contained in the file RELEASENOTES.txt.
There are no release notes for the first release.

This software is licensed as defined in the accompanying files
LICENSE.txt and LEGAL.txt.  These files may be found in the
installation directory.

----------------
| Installation |
----------------

There are two ways to acquire and run Callimachus:  Download the ZIP archive 
or checkout the Subversion repository.

If you download a ZIP archive of a release from http://callimachusproject.org/, 
extract it into a new directory. Execute a callimachus-start script located 
in the bin/ directory to start the server. The server will serve files from 
the webapps directory that have a known file extension.

If you want to check out the source code, see the directions at:
  http://code.google.com/p/callimachus/source/checkout
Once you have the source code checked out, you can execute 'ant run' from a 
command line in the top directory to run the server.

NB:  If using a 64bit Linux host, you must create your own libjnotify.so file 
for the server to read runtime changes in the webapps directory. Instructions 
for creating your own libjnotify.so file can be found at
http://jnotify.sourceforge.net/

Files with the extension .ttl, .ttl.gz, .rdf, or .rdf.gz will be loaded as 
graphs into the RDF store. Any RDF Schema or OWL ontologies will be compiled 
as defined in AliBaba (http://www.openrdf.org/).

Classes that are subclasses of calli:Viewable (and other calli-able classes) 
should use calli annotations, such as calli:view to identify XML/RDFa 
template files for display actions against individuals of the class. See the 
callimachus-ontology.ttl file for details.

The calli Realm classes can be used to declare an authorization realms. Use 
the http:realm annotation to assign a realm to a message type or class. See 
the callimachus-ontology.ttl file for details on how to declare them in the 
RDF store.

The end point "/sparql" accepts ready only application/sparql-query or a form 
and returns the result. The operation "?describe" can be appended to any IRI 
to receive an RDF bounded description of it. The path prefix "/diverted;" 
identifies percent encoded RDF IRI resources.

Share your experience or ask questions on the mailing list: callimachus-
discuss@googlegroups.com.  You can sign up at 
http://groups.google.com/group/callimachus-discuss.

