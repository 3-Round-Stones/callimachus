// location.js
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

(function($,jQuery){

var calli = window.calli || (window.calli={});

calli.getPageUrl = function(ref) {
    // window.location.href needlessly encodes/decodes reserved characters in the URI path
    // https://bugs.webkit.org/show_bug.cgi?id=30225
    // https://bugzilla.mozilla.org/show_bug.cgi?id=1040285
    var path = location.pathname;
    if (path.match(/#|%27/))
        return location.href.replace(path, path.replace('#', '%23').replace('%27', "'"));
    return location.href;
};

calli.getCallimachusUrl = function(suffix) {
    var base = calli.baseURI;
    var home = base.substring(0, base.length - 1);
    while (home.lastIndexOf('/') === 0 || home.lastIndexOf('/') > home.indexOf('//') + 1) {
        home = home.substring(0, home.lastIndexOf('/'));
    }
    var url = base;
    if (typeof suffix == 'string' && suffix.indexOf('/') === 0) {
        url = home + suffix;
    } else if (typeof suffix == 'string') {
        url = base + suffix;
    }
    return url.replace(/\/\.\//g,'/').replace(/\/[^\/]+\/\.\.\//g, '/');
};

calli.getFormAction = function(form) {
    if (form.getAttribute("action"))
        return form.action;
    var url = calli.getPageUrl();
    if (url.indexOf('#') > 0)
        return url.substring(0, url.indexOf('#'));
    return url;
};

})(jQuery,jQuery);
