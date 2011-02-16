Build Server
The aim of the build server is to automate testing under various browsers. This works reliably with Firefox, but IE and Chrome still need attention. There is an ant script in test that downloads the required selenium libraries to test/lib. Each test requires a fresh repository so the existing repository is automatically removed when a test is started. Each subdirectory within test defines its own test suite. The test suite is defined in XHTML as used by the Selenium IDE (this solution does not use JUnit). The test results are placed in these subdirectories; each browser type will produce separate results (e.g. test-results-firefox.html).

A test suite can be started from the command line with any of the following (run from the test directory):

ant test-accounts-firefox
ant test-accounts-explorer
ant test-accounts-chrome
ant test-skos-firefox
