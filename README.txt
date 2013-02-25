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

You must have Java 6 JDK (1.6.0_18+) installed and available.

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
hostname and port (if not port 80); add an initial user as instructed. Be sure
to remove the preceding comment character ('#') in the .conf file.

To register a Callimachus daemon with the Linux system (to stop Callimachus
on machine shut down) run the bin/callimachus-install.sh script. The
Callimachus daemon is only available for Linux.

Execute a callimachus-setup script located in the bin/ directory to initialize
the repository. The script will ask for a password for the initial user. Execute
a callimachus-start script located in the bin/ directory to start the server.

On Linux run:
 $ bin/callimachus-install.sh
 $ bin/callimachus-setup.sh
 $ bin/callimachus-start.sh

On Windows run:
 # bin/callimachus-setup.bat
 # bin/callimachus-start.bat

On Mac run:
 $ bin/callimachus-setup.sh
 $ bin/callimachus-start.sh

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
the server. If some files fail to download you may have to run "ant dist-clean"
and run "ant run" again.


Additional documentation regarding usage and application development may
be found on the project's wiki at:
  http://code.google.com/p/callimachus/wiki/GettingStarted

Details of browser support may be found at:
  http://callimachusproject.org/docs/1.0/articles/browser-support.docbook?view

Share your experience or ask questions on the mailing list: callimachus-
discuss@googlegroups.com.  You can sign up at 
  http://groups.google.com/group/callimachus-discuss.

