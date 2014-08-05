// discussion.js
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

jQuery(function($){

    calli.getCurrentUserName().then(function(username){
        if (username) checkTab();
    });

    function checkTab() {
        if (location.search == '?discussion') {
            return calli.ready(calli.getPageUrl()).then(function(url){
                var posts = $(".comment").parent().find("time").map(function(){ return $(this).attr("content"); });
                try {
                    if (posts.length) {
                        window.localStorage.setItem(url, posts.get().join(','));
                    }
                } catch (e) { }
            });
        }
        $("a[href='?discussion']").filter(":visible").each(function() {
            var tab = $(this);
            var url = this.href;
            return calli.getText(url).then(function(doc) {
                return handleDiscussion(tab, url, doc);
            }, calli.error);
        });
    }

    function handleDiscussion(tab, url, data) {
        var posts = $(".comment", data).parent().find("time").map(function(){ return $(this).text(); });
        if (posts.length) {
            var span = $('<span class="badge pull-right"></span>');
            try {
                if (window.localStorage.getItem(url)) {
                    var seen = window.localStorage.getItem(url).split(',');
                    var newer = posts.filter(function(){
                        for (var i=0; i<seen.length; i++) {
                            if (seen[i] == this)
                                return false;
                        }
                        return true;
                    });
                    if (newer.size()) {
                        span.text(newer.size());
                        tab.prepend(span);
                    }
                } else {
                    span.text(posts.size());
                    tab.prepend(span);
                }
            } catch(e) {
                span.text(posts.size());
                tab.prepend(span);
            }
        }
    }

});

