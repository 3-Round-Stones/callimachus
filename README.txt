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

You must have Java 6 JDK (1.6.0_18+) installed and available.

The following platforms are supported:
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

There are two ways to acquire and run Callimachus:  Download the ZIP archive 
or checkout the Subversion repository.

If you download a ZIP archive of a release from http://callimachusproject.org/, 
extract it into a new directory. Copy etc/callimachus-defaults.conf to
etc/callimachus.conf; change the ORIGIN and PORT to the be the same as the
hostname and port (if not port 80); add an initial user as instructed.

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
Once you have the source code checked out, you will need to provide a username
and password. Create a build.properties file in the same directory as build.xml.
Put the variables "callimachus.username" and "callimachus.password" followed by
'=' and their value on their own line. You can then execute 'ant run' from a 
command line in this top directory to run the server.


Additional documentation regarding usage and application development may
be found on the project's wiki at:
  http://code.google.com/p/callimachus/wiki/GettingStarted


Share your experience or ask questions on the mailing list: callimachus-
discuss@googlegroups.com.  You can sign up at 
  http://groups.google.com/group/callimachus-discuss.

