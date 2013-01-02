// decode-html.js
/*
   Portions Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

window.calli = window.calli || {};

window.calli.decodeHtmlText = function(html, keepXmlEntities) {
    var text = $('<div />').html(html).text();
    if (keepXmlEntities) {
        text = text
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&apos;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }
    return text;
}

})(jQuery);
