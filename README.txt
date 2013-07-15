Callimachus Project Installation Instructions

http://callimachusproject.org/

----------------
| Introduction |
----------------

Thank you for your interest in the Callimachus Project.  This is the README
for Callimachus (Open Source), a Linked Data management system.

Callimachus aims to make Semantic Web applications (much) easier to create.
Because this is a community effort, you can help by submitting your
applications, upgrades and tools to the Callimachus Project site at
http://callimachusproject.org.

Documentation for Callimachus may be found at:
  http://callimachusproject.org/docs/

Please consider joining the mailing lists and participating in the
online community so we can serve you better.  See
http://callimachusproject.org for more details.

This software is licensed as defined in the accompanying files
LICENSE.txt and LEGAL.txt.  These files may be found in the
installation directory.

-----------------
| prerequisites |
-----------------

You must have at least Java JDK 7 installed and available with ECMAScript 5
support. Oracle introduced EMCAScript 5 support in Java™ SE 7 Update 1.

Callimachus is expected to work on the following server platforms:
* Linux i386
* Linux 64bit
* Mac on Intel
* Windows XP
* Windows Vista
* Windows 7

-----------------
| Configuration |
-----------------

An SMTP server is required to send an invite email and password and reset user's password.
The SMTP server can be configured in the file etc/mail.properties.
A typical file might contain the following block:

mail.transport.protocol = smtp
mail.from = system@example.com
mail.host = example.com
mail.smtp.port = 25
mail.smtp.auth = true
mail.smtp.starttls.enable = true
mail.smtp.starttls.required = true
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
extract it into a new directory. Copy etc/callimachus-defaults.conf to
etc/callimachus.conf; change the ORIGIN and PORT to the be the same as the
hostname and port (if not port 80). Be sure to remove the preceding comment
character ('#') in the .conf file.

To register a Callimachus daemon with the Linux system (to stop Callimachus
on machine shut down) run the bin/callimachus-install.sh script. The
Callimachus daemon is only available for Linux.

Execute a callimachus-setup script located in the bin/ directory to initialize
the repository. Open your Web browser to http://localhost:8080/ to access the
service. Execute a callimachus-stop script located in the bin/ directory to
stop the server. To start the server again use the provided start script.

On Linux server run (as root):
 # bin/callimachus-install.sh

On Linux or Mac desktop run:
 $ bin/callimachus-setup.sh

On Windows desktop run:
 # bin/callimachus-setup.bat

To monitor the activity of the server watch the log/callimachus.log.0 file for
log messages. Windows users may find the callimachus-log.bat script useful to
monitor the activity without locking the log file.


Alternatively, if you want to check out the source code, see the directions at:
  http://code.google.com/p/callimachus/source/checkout
Once you have the source code checked out, you will need to provide a username,
email and password. Create a build.properties file in the same directory as
build.xml. Put the variables "callimachus.username", "callimachus.email", and
"callimachus.password" followed by '=' and their value on their own line. Copy
src/callimachus-defaults.conf to etc/callimachus.conf and edit file as above.
You can then execute 'ant run' from a command line in this top directory to run
the server. If some files fail to download you may have to run "ant clean dist"
and run "ant run" again.


Additional documentation regarding usage and application development may
be found on the project's wiki at:
  http://callimachusproject.org/docs/1.1/getting-started-with-callimachus.docbook?view

Details of browser support may be found at:
  http://callimachusproject.org/docs/1.1/articles/browser-support.docbook?view

Share your experience or ask questions on the mailing list: callimachus-
discuss@googlegroups.com.  You can sign up at 
  http://groups.google.com/group/callimachus-discuss.

