Callimachus Project Installation Instructions

http://callimachusproject.org/

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
* Windows XP
* Windows Vista
* Windows 7

If using a 64bit Linux host and you want to use the webapps directory (not required),
you must create your own libjnotify.so file for the server to read
runtime changes in the webapps directory.
- 1) Download http://sourceforge.net/projects/jnotify/files/jnotify/jnotify-0.93/jnotify-lib-0.93.zip
- 2) Extract jnotify-native-linux-0.93-src.zip
- 3) Edit the JDK include paths in Release/subdir.mk
- 4) Move the include statement for "unistd.h" up above "sys/time.h" in net_contentobjects_jnotify_linux_JNotify_linux.c
- 5) cd Release && make
- 6) Edit etc/callimachus.conf
- 7) Change the LIB variable to point to the Release directory above
More detailed instructions for creating your own libjnotify.so file can be found at
http://jnotify.sourceforge.net/

-----------------
| Configuration |
-----------------

An SMTP server is required to send an invite email and password and reset user's password.
The SMTP server can be configured via the installer or in the file etc/mail.properties.
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

There are three ways to acquire and run Callimachus:  Download the installer
(preferred), download the ZIP archive or checkout the Subversion repository.

Callimachus has a graphical installer that is the primary way to
install and configure Callimachus.  The installer may be re-run any time
you need to change your Callimachus configuration.  You may also re-run the
installer to change the password for the initial user.

Execute the installer by running its JAR file:
[[
$ java -jar callimachus-setup-<version>.jar
]]

For headless installations on a Linux server, you can run the installer
in console mode.  You must redirect STDERR to a file to ensure that
error messages do not interfere with the display:
[[
java -jar callimachus-setup-<version>.jar -console 2> install.log
]]

If you download a ZIP archive of a release from http://callimachusproject.org/, 
extract it into a new directory. Edit etc/callimachus.conf and change the
ORIGIN to the be the same as the hostname and port (if not port 80). Execute
a callimachus-start script located in the bin/ directory to start the server.
The server will serve files from the webapps directory that have a known file
extension.

On Mac and Linux run:
 $ sh bin/callimachus-start.sh

On Windows run:
 # bin/callimachus-start.bat

In some environments, Callimachus may log "An exception has occurred in the compiler...FilePermission" exceptions. This is 
often due to an issue in the embedded Java compiler and if so has no impact on the 
run time behaviour of Callimachus.

If you want to check out the source code, see the directions at:
  http://code.google.com/p/callimachus/source/checkout
Once you have the source code checked out, you will need to provide a username
and password. Create a build.properties file in the same directory as build.xml.
Put the variables "callimachus.username" and "callimachus.password" followed by
'=' and their value. You can then execute 'ant run' from a 
command line in this top directory to run the server.

Files with the extension .ttl, .ttl.gz, .rdf, or .rdf.gz will be loaded as 
graphs into the RDF store. Any RDF Schema or OWL ontologies in .ttl will be
compiled as defined in AliBaba (http://www.openrdf.org/).

Classes that are subclasses of calli:Viewable (and other calli-able classes) 
should use calli annotations, such as calli:view to identify XHTML+RDFa 
template files for display actions against individuals of the class. See the 
$CALLIMACHUS/webapps/callimachus/callimachus-ontology.ttl file for more details.

The calli Realm classes can be used to declare an authorization realms. Use 
the calli:realm annotation to assign a realm to a message type or class. The 
default realm is </>. See the callimachus-ontology.ttl file for 
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

