Build Server

The aim of the build server is to automate testing under various browsers. This installation uses the Hudson Continuous Integration Server in conjunction with Ant build scripts to automate the build and test phases. The Integration server periodicaly polls the subversion code repository to check for updates; if the source has been revised the local copy is updated, built and then tested. Test reports are written to the respective test directories.

Ports used:
8000	Hudson Integration server
8080	Callimachus server
4444	Selenium (Remote Control) server

Dependencies:
Hudson Continous Integration server 1.396
Callimachus
Java JDK 1.6
Apache Ant 1.8.2
Subversion
Mozilla Firefox
Internet Explorer

Starting the Integration Server

cd c:\Program Files\Hudson-1.396
java -jar hudson.war --httpPort=8000

Ant
The ant scripts may be run independently of the integration server. The Callimachus compile and server start/stop scripts are defined in the main build.xml file. The test folder includes a build.xml file containing additional test scripts.

The test scripts download the necessary selenium dependencies (ant test-dependencies) that downloads the required selenium libraries to test/lib. It then removes any existing repositories and starts Callimachus. Once Callimachus has started it starts the Selenium (Remote Control) Server and points it to the appropriate test suite. Each subdirectory within test defines its own test suite. A test suite is defined in HTML (note that Internet Explorer prevents the use of the XHTML suffix) as used by the Selenium IDE. The test results are placed in these subdirectories; each browser type will produce separate results (e.g. test-results-firefox.html).

A test suite can be started from the command line with any of the following (run from the test directory):

ant test-accounts-firefox
ant test-accounts-explorer
ant test-accounts-chrome
ant test-skos-firefox

