// iframe.js
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

(function($) {
    if (window.parent != window) {
        document.documentElement.className += " iframe";
        var src = null;
        var postSource = function() {
            if (window.location.search == '?view' && parent.postMessage) {
                var url = calli.getPageUrl();
                if (url != src) {
                    src = url;
                    parent.postMessage('POST resource\n\n' + url.replace(/\?.*$/,''), '*');
                }
            }
        }
        $(window).bind('popstate', postSource);
        $(window).bind('load', postSource);
    }
})(jQuery);
