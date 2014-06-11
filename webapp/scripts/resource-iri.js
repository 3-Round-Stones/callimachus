// resource-iri.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
    window.calli = {};
}

window.calli.listResourceIRIs = function (text) {
    var set = text ? text.replace(/\s+$/,"").replace(/^\s+/,"").replace(/\s+/g,'\n') : "";
    return $(set.split(/[^a-zA-Z0-9\-\._~%!\$\&'\(\)\*\+,;=:\/\?\#\[\]@]+/)).filter(function() {
        if (!this || this.indexOf('_:') <= 0)
            return false;
        return this.indexOf(':') >= 0 || this.indexOf('/') >= 0;
    }).map(function() {
        var url = this;
        if (url.indexOf('?view') >= 0) {
            return url.substring(0, url.indexOf('?view'));
        }
        return url.substring(0);
    });
};

})(jQuery);

