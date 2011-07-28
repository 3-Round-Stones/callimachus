Callimachus Project Installation Instructions

http://callimachusproject.org/

20 July 2010

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

This software is licensed as defined in the accompanying files
LICENSE.txt and LEGAL.txt.  These files may be found in the
installation directory.

-----------------
| prerequisites |
-----------------

You must have Java 6 installed and available.

The following platforms are supported:
* Linux i386
* Linux 64bit
* Mac on Intel
* Window XP
* Window Vista

If using a 64bit Linux host, you must create your own libjnotify.so file 
for the server to read runtime changes in the webapps directory.
- 1) Download http://sourceforge.net/projects/jnotify/files/jnotify/jnotify-0.93/
- 2) Extract jnotify-native-linux-0.93-src.zip
- 3) Edit the JDK include paths in Release/subdir.mk
- 4) Move the include statement for "unistd.h" up above "sys/time.h" in net_contentobjects_jnotify_linux_JNotify_linux.c
- 5) cd Release/ && make && cp libjnotify.so $CALLIMACHUS/lib/
More detailed instructions for creating your own libjnotify.so file can be found at
http://jnotify.sourceforge.net/

-----------------
| Configuration |
-----------------

An SMTP server is required to send an invite email and password and reset user's password.
The SMTP server can be configured in etc/mail.properties (not provided).
A typical file might contain the following block:

mail.transport.protocol = smtp
mail.from = system@example.com
mail.smtp.host = example.com
mail.smtp.port = 25
mail.smtp.auth = true
mail.user = system
mail.password = secret

For more options see:
 http://java.sun.com/javaee/5/docs/api/javax/mail/package-summary.html
 http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html


----------------
| Installation |
----------------

There are two ways to acquire and run Callimachus:  Download the ZIP archive 
or checkout the Subversion repository.

If you download a ZIP archive of a release from http://callimachusproject.org/, 
extract it into a new directory. Edit etc/callimachus.conf and change the
authority to the be the same as the hostname and port (if not port 80). Execute
a callimachus-start script located in the bin/ directory to start the server.
The server will serve files from the webapps directory that have a known file
extension.

On Mac and Linux run:
 $ chmod a+x bin/*
 $ bin/callimachus-start.sh

On Windows run:
 # bin/callimachus-start.bat

In some environments, Callimachus may log "An exception has occurred in the compiler...FilePermission" exceptions. This is 
often due to an issue in the embedded Java compiler and if so has no impact on the 
run time behaviour of Callimachus.

If you want to check out the source code, see the directions at:
  http://code.google.com/p/callimachus/source/checkout
Once you have the source code checked out, you can execute 'ant run' from a 
command line in the top directory to run the server.

Files with the extension .ttl, .ttl.gz, .rdf, or .rdf.gz will be loaded as 
graphs into the RDF store. Any RDF Schema or OWL ontologies in .ttl will be
compiled as defined in AliBaba (http://www.openrdf.org/).

Classes that are subclasses of calli:Viewable (and other calli-able classes) 
should use calli annotations, such as calli:view to identify XHTML+RDFa 
template files for display actions against individuals of the class. See the 
$CALLIMACHUS/webapps/callimachus/callimachus-ontology.ttl file for more details.

The calli Realm classes can be used to declare an authorization realms. Use 
the http:realm annotation to assign a realm to a message type or class. The 
default realm is </accounts>. See the callimachus-ontology.ttl file for 
details on how to declare them in the RDF store.

The end point "/sparql" accepts ready only application/sparql-query or a form 
and returns the result. The operation "?describe" can be appended to any IRI 
to receive an RDF bounded description of it. The path prefix "/diverted;" 
identifies percent encoded RDF IRI resources.

Additional documentation regarding usage and application development may
be found on the project's wiki at:
  http://code.google.com/p/callimachus/w/list

Share your experience or ask questions on the mailing list: callimachus-
discuss@googlegroups.com.  You can sign up at 
http://groups.google.com/group/callimachus-discuss.

