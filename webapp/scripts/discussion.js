// discussion.js

(function($, jQuery){

    function checkTab() {
        if (location.search == '?discussion') {
            var url = calli.getPageUrl();
            var posts = $(".comment").parent().find("time").map(function(){ return $(this).attr("content"); });
            try {
                if (posts.length) {
                    window.localStorage.setItem(url, posts.get().join(','));
                }
            } catch (e) { }
        }
        $("a[href='?discussion']:visible").each(function() {
            var tab = $(this);
            var url = this.href;
            jQuery.ajax({ type: 'GET', url: url,
                xhrFields: calli.withCredentials,
                success: function(doc) { handleDiscussion(tab, url, doc); }
            });
        });
    }
    function handleDiscussion(tab, url, data) {
        var posts = $(".comment", data).parent().find("time").map(function(){ return $(this).text(); });
        if (posts.length) {
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
                        tab.text(tab.text() + " (" + newer.size() + ")");
                    }
                } else {
                    tab.text(tab.text() + " (" + posts.size() + ")");
                }
            } catch(e) {
                tab.text(tab.text() + " (" + posts.size() + ")");
            }
            tab.css('font-weight', "bold");
        }
    }
    $(document).bind('calliLoggedIn', function(){setTimeout(checkTab,0)});

})(jQuery, jQuery);

