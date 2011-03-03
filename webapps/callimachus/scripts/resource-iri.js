/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
	window.calli = {};
}

window.calli.listResourceIRIs = function (text) {
	var set = text ? text.replace(/\s+$/,"").replace(/^\s+/,"").replace(/\s+/,'\n') : "";
	return $(set.split('\n')).filter(function() {
		if (this.indexOf('>') >= 0 || this.indexOf('<') >= 0) {
			return false;
		}
		if (this.indexOf(']') >= 0 || this.indexOf('[') >= 0) {
			return false;
		}
		if (this.indexOf('}') >= 0 || this.indexOf('{') >= 0) {
			return false;
		}
		if (this.indexOf('\n') >= 0 || this.indexOf('\r') >= 0) {
			return false;
		}
		if (this.indexOf(':') < 0 || this.indexOf('_:') >= 0) {
			return false;
		}
		return true;
	}).map(function() {
		var uri = this;
		if (uri.indexOf('/diverted;') >= 0) {
			uri = uri.substring(uri.indexOf('/diverted;') + '/diverted;'.length);
			if (uri.indexOf('?') >= 0) {
				uri = uri.substring(0, uri.indexOf('?'));
			}
			if (uri.indexOf('#') >= 0) {
				uri = uri.substring(0, uri.indexOf('#'));
			}
			uri = decodeURIComponent(uri);
		} else if (uri.indexOf('/callimachus/go?q=') >= 0) {
			uri = uri.substring(uri.indexOf('/callimachus/go?q=') + '/callimachus/go?q='.length);
			if (uri.indexOf('&') >= 0) {
				uri = uri.substring(0, uri.indexOf('&'));
			}
			if (uri.indexOf('#') >= 0) {
				uri = uri.substring(0, uri.indexOf('#'));
			}
			uri = decodeURIComponent(uri);
		} else if (uri.indexOf('?view') >= 0) {
			uri = uri.substring(0, uri.indexOf('?view'));
		} else if (uri.indexOf('?edit') >= 0) {
			uri = uri.substring(0, uri.indexOf('?edit'));
		} else if (uri.indexOf('?discussion') >= 0) {
			uri = uri.substring(0, uri.indexOf('?discussion'));
		} else if (uri.indexOf('?describe') >= 0) {
			uri = uri.substring(0, uri.indexOf('?describe'));
		} else if (uri.indexOf('?history') >= 0) {
			uri = uri.substring(0, uri.indexOf('?history'));
		}
		return uri;
	})
};

})(jQuery);

