/************************************ DATADRIVEN EXTENSION START ********************************************/
/*
NAME:
	datadriven
	
	Licensed under Apache License v2
	http://www.apache.org/licenses/LICENSE-2.0

PURPOSE:
	Basic data driven testing.
	
	Full documentation at http://wiki.openqa.org/display/SEL/datadriven
	
	
EXAMPLE USE:
	The structure of your data driven test case will be;
	-------------------------------------------
	COMMAND       |TARGET          |VALUE
	-------------------------------------------
	loadTestData  |<file path>     |
	while         |!testdata.EOF() |
	testcommand1  |                |
	testcommand...|                |
	testcommandn  |                |
	endWhile      |                |
	-------------------------------------------

AUTHOR:
	Jonathan McBrien
	jonathan@mcbrien.org
	2008-10-22: v0.1:	Initial version.
	2009-01-16: v0.2:	Updated for Firefox 3.
				xmlTestData.prototype.load now uses the include extension's getIncludeDocumentBySynchronRequest method for better portability.
				(Why reinvent the wheel? :) - with appreciation to the include extension's authors.)
*/

var XMLserialize = function(node) {
	if (typeof XMLSerializer != "undefined")
		return (new XMLSerializer()).serializeToString(node) ;
	else if (node.xml) return node.xml;
	else throw "XMLserialize is not supported or can't serialize " + node;
}

function xmlTestData() {
	this.xmlDoc = null;
	this.testdata = null;
	this.index = null;
}


xmlTestData.prototype.load = function(xmlloc) {
	var xmlHttpReq = IncludeCommand.getIncludeDocumentBySynchronRequest(xmlloc);
	this.xmlDoc = xmlHttpReq.responseXML;
		
	this.index = -1;
	this.testdata = this.xmlDoc.getElementsByTagName("test");

	if (this.testdata == null || this.testdata.length == 0) {
		throw new Error("Test data couldn't be loaded or test data was empty.");
	}
}

xmlTestData.prototype.EOF = function() {
	if (this.index != null && this.index < this.testdata.length - 1) return false;
	return true;
}

xmlTestData.prototype.more = function() {
	return !this.EOF();
}

xmlTestData.prototype.store = function() {
	LOG.info(XMLserialize(this.testdata[this.index]));	// Log should anything go wrong while testing with this data.

	if (this.testdata[this.index].attributes.length != this.testdata[0].attributes.length) {
		LOG.error("Inconsistent attribute length in test data.");
		return;
	}
	
	for (i=0; i<this.testdata[this.index].attributes.length; i++){
		if (null == this.testdata[0].getAttribute(this.testdata[this.index].attributes[i].nodeName)) {
			LOG.error("Inconsistent attribute names in test data.");
			return;
		}

		selenium.doStore(this.testdata[this.index].attributes[i].nodeValue, this.testdata[this.index].attributes[i].nodeName);
	}
}

xmlTestData.prototype.next = function() {
	if (this.EOF()) {
		LOG.error("No test data.");
		return;
	}

	this.index++;
	
	this.store();
}

Selenium.prototype.testdataStack = [];
Selenium.prototype.testdata = null;

Selenium.prototype.doLoadTestData = function(xmlloc) {
	if (this.testdata) {
	    this.testdataStack.push(this.testdata);
	}
	this.testdata = new xmlTestData();
	this.testdata.load(xmlloc);
};

Selenium.prototype.doNextTestData = function() {
	this.testdata.next();
};

Selenium.prototype.doUnloadTestData = function() {
	if (this.testdataStack.length) {
    	this.testdata = this.testdataStack.pop();
	    this.testdata.store();
	}
};

/************************************ DATADRIVEN EXTENSION END **********************************************/
