Build Server
------------
The aim of the build server is to automate testing under various browsers. This installation uses the Hudson Continuous Integration Server in conjunction with Ant build scripts to automate the build and test phases. The Integration server periodicaly polls the subversion code repository to check for updates; if the source has been revised the local copy is updated, built and then tested. 

Test reports are written to the respective test directories. To view test reports click on the 'Callimachus' project on the Hudson dashboard, then open 'Workspace'> 'callimachus' > 'test'. Open the relevant test folder (e.g. 'accounts') then click on the name (not 'view') of the appropriate test results (e.g. test-result-firefox.html).

Ports used:
25	James mail server
80	Hudson Integration server
4444	Selenium (Remote Control) server
8080	Callimachus server

Dependencies:
Hudson Continous Integration server 1.396 <http://hudson-ci.org/>
Apache James Mail Server <http://james.apache.org/>
Java JDK 1.6
Apache Ant 1.8.2 <http://ant.apache.org/>
Subversion <http://subversion.tigris.org/>
Mozilla Firefox
Internet Explorer

Callimachus and selenium-server-standalone-2.0b2.jar are installed during the build process.

Starting the Mail Server
------------------------
The ant test scripts update callimachus/etc/mail.properties to point to a local mail server. This should be running in the background:
As Administrator:

cd /Program Files/james-2.3.2/bin
run.bat
OR sudo bin/run.sh

The mail server can be stopped with a CTL-C. Once the mail server has been run once, a configuration file may be found in:
JAMES_HOME/apps/james/SAR-INF/config.xml

This should be modified to :

To drop incoming messages find the line <processor name="root"> and append the following mailet:
<mailet match="All" class="Null"/>

Starting the Integration Server
-------------------------------
Before you start the server create a workspace directory in c:\Workspace

cd c:\Program Files\Hudson-1.396
java -Dmail.smtp.starttls.enable="true" -jar hudson.war --httpPort=80


Configuring the Integration Server
----------------------------------
Open the Hudson Dashboard at http://localhost:8000
From the Hudson Dashboard : Manage Hudson > Configure System

JDK 
Name: JDK1.6
JAVA_HOME: C:\Program Files\Java\jdk1.6.0_23

Ant
Name: Ant1.8.2
ANT_HOME: C:\Program Files\apache-ant-1.8.2

Subversion
Subversion Workspace Version: 1.6
C:\Program Files\Subversion\bin

Create a new Job
Configuration:
Project Name: Callimachus

Advanced Project Options:
(click Advanced)
Check 'Use Custom Workspace'
Directory: C:\Workspace

Source Code Management
Select Subversion
Repository URL: https://callimachus.googlecode.com/svn/trunk
Local module: callimachus

Select 'Use Update'

Build Triggers
Select 'Poll SCM'
Schedule: @hourly

Build
Add Ant build steps:

Ant Version: Ant1.8.2
Targets: compile

Ant Version: Ant 1.8.2
(click 'Advanced')
Targets: test-accounts-firefox
Build File: test/build.xml

etc. e.g. Also for 'test-accounts-explorer', 'test-skos-firefox', ...


Ant
---
The ant scripts may be run independently of the integration server. The Callimachus compile and server start/stop scripts are defined in the main build.xml file. The test folder includes a build.xml file containing additional test scripts.

The test scripts download the necessary selenium dependencies (ant test-dependencies) that downloads the required selenium libraries to test/lib. It then removes any existing repositories and starts Callimachus. Once Callimachus has started it starts the Selenium (Remote Control) Server and points it to the appropriate test suite. Each subdirectory within test defines its own test suite. A test suite is defined in HTML (note that Internet Explorer prevents the use of the XHTML suffix) as used by the Selenium IDE. The test results are placed in these subdirectories; each browser type will produce separate results (e.g. test-results-firefox.html).

A test suite can be started from the command line with any of the following (run from the test directory):

ant test-accounts-firefox
ant test-accounts-explorer
ant test-accounts-chrome
ant test-skos-firefox

Alternatively, a test may be run providing explicit parameters for suite, browser, and results on the command line:

ant -Dsuite=/callimachus/test/directory/directory.html -Dbrowser=*firefox -Dresults=/callimachus/test/directory/test-result-firefox.html test

RDFa Conformance Tests
----------------------

The ant scripts include tests for RDFa conformance (RDFaParser). This downloads the RDFa conformance test-suite from <http://rdfa.digitalbazaar.com/test-suite/> parses the XHTML including the RDFa, and evaluates the sparql against the output. The results are written to 'test/RDFa/rdfa-conformance-test-results.txt'.

ant test-rdfa-conformance

