// discussion.js

$(window).load(function(){
	$("#discussion-tab").each(function() {
		var tab = $(this);
		var url = this.href;
		jQuery.ajax({ type: 'GET', url: url,
			success: function(data) {
				var posts = $(".comment", data).parent().find(".datetime-locale").map(function(){ return $(this).text(); });
				if (posts.size()) {
					if (window.localStorage && localStorage.getItem(url)) {
						var seen = localStorage.getItem(url).split('|');
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
					tab.css('font-weight', "bold");
					tab.click(function() {
						if (window.localStorage) {
							localStorage.setItem(url, posts.get().join('|'));
						}
						return true;
					});
				}
			}
		})
	});
});
