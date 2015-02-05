// last-modified.js
/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

(function($){

var calli = window.calli || (window.calli={});

calli.lastModified = function(url, value) {
    var uri = url;
    if (!uri || uri.indexOf('http') !== 0) {
        if (document.baseURIObject && document.baseURIObject.resolve) {
            uri = document.baseURIObject.resolve(uri || '');
        } else {
            var a = document.createElement('a');
            a.setAttribute('href', uri || '');
            if (a.href) {
                uri = a.href;
            }
        }
        if (window.location.pathname.indexOf('%27') > 0) {
            // https://bugzilla.mozilla.org/show_bug.cgi?id=1040285
            uri = uri.replace('%27', "'");
        }
    }
    if (uri.indexOf('?') > 0) {
        uri = uri.substring(0, uri.indexOf('?'));
    }
    var last = window.sessionStorage.getItem(uri + " Last-Modified");
    if (value) {
        if (last && Date.parse(last) >= Date.parse(value))
            return last;
        window.sessionStorage.setItem(uri + " Last-Modified", value);
        return value;
    } else {
        if (last) return last;
        return undefined;
    }
};
if (window.sessionStorage) {
    if (bug4363()) {
        calli.lastModified(calli.getPageUrl(), new Date(document.lastModified + ' GMT').toUTCString());
    } else {
        calli.lastModified(calli.getPageUrl(), new Date(document.lastModified).toUTCString());
    }
} else {
    calli.lastModified = function(){return undefined;};
}

// https://bugs.webkit.org/show_bug.cgi?id=4363
function bug4363() {
    if (navigator.userAgent.indexOf('Safari') < 0)
        return false;
    if (navigator.userAgent.indexOf('Chrom') < 0)
        return true; // Apple has not fixed this as of Safari 7.0
    try {
        var chrome = navigator.userAgent.match(/Chrome\/(3\d)\b/);
        return chrome && parseInt(chrome[1],10) < 38; // Google fixed this in Chrome 38
    } catch(e) {
        return false;
    }
}

})(jQuery);
