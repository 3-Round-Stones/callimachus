// login.js

(function($){

$(document).ready(function(){
	if (window.sessionStorage && sessionStorage.getItem("Profile")) {
		// logged in already
		loggedIn(sessionStorage.getItem("Profile"));
	} else {
		$(".authenticated").hide();
		$("#login-link").show();
		$("#login-link").click(login);
		if (window.localStorage && localStorage.getItem('Authorization')) {
			// was logged in previously
			var options = {type: "GET", url: "/accounts?login",
				success: showProfile
			};
			var auth = localStorage.getItem('Authorization');
			if (auth && auth.indexOf("Basic ") == 0) {
				// remember me Firefox, Chrome, Safari
				var up = window.atob(auth.substring("Basic ".length));
				options.username = up.substring(0, up.indexOf(':'));
				options.password = up.substring(up.indexOf(':') + 1);
				window.jQuery.ajax(options);
				return;
			} else if (auth && auth.indexOf("Credentials ") == 0) {
				// remember me IE
				var up = auth.substring("Credentials ".length);
				options.username = up.substring(0, up.indexOf(':'));
				options.password = up.substring(up.indexOf(':') + 1);
				window.jQuery.ajax(options);
				return;
			} else if (auth && auth.indexOf("Name ") == 0) {
				// was logged in before, prompt to log back in
				$("#login-link").click();
			}
		}
		// hasn't logged in using the login form; is this page protected?
		var xhr = jQuery.ajax({type: 'GET', url: location.href,
			beforeSend: withCredentials,
			success: function() {
				if (xhr.getResponseHeader("Authentication-Info")) { 
					jQuery.ajax({type: "HEAD", url: "/accounts?login",
						beforeSend: withCredentials,
						success: showProfile
					});
				} else if (!xhr.getAllResponseHeaders()) { // Opera sends empty response; try again w/o cache
					xhr = jQuery.ajax({type: 'GET', url: location.href,
						headers: {'Cache-Control': "no-cache"},
						beforeSend: withCredentials,
						success: function() {
							if (xhr.getResponseHeader("Authentication-Info")) { 
								jQuery.ajax({type: "GET", url: "/accounts?login",
									beforeSend: withCredentials,
									success: showProfile
								});
							}
						}
					});
				}
			}
		});
	}
});

function withCredentials(req) {
	try {
		req.withCredentials = true;
	} catch (e) {}
}

function showProfile(doc) {
	var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(doc);
	if (title) {
		if (window.sessionStorage) {
			// now logged in
			sessionStorage.setItem("Profile", title[1]);
		}
		loggedIn(title[1]);
	} else { // Firefox may cache response without body
		loggedIn("Profile");
	}
}

function loggedIn(title) {
	$("#login-form").hide();
	$("#login-link").hide();
	$("#profile-link").text(title);
	$(".authenticated").show();
	$("#logout-link").click(function(event) {
		var href = this.href;
		window.jQuery.ajax({ type: 'GET', url: href,
			username: 'logout', password: 'nil',
			success: function() {
				location = href;
			}
		})
		if (window.localStorage) {
			localStorage.removeItem('Authorization');
		}
		if (window.sessionStorage) {
			sessionStorage.removeItem('Profile');
		}
		if (event.preventDefault) {
			event.preventDefault();
		}
		return false;
	}) 
}

function login(event) {
	var link = $(this);
	if ($("#login-form").size() && $("#login-form").get(0).style.display != 'none' ) {
		$("#login-form").slideUp();
		$("#login-form").hide();
		link.children(".ui-icon").removeClass('ui-icon-circle-arrow-n');
		link.children(".ui-icon").addClass('ui-icon-circle-arrow-s');
		link.removeClass('ui-corner-top');
		link.removeClass('ui-state-active');
		link.addClass('ui-corner-all');
		link.css('border-bottom-width', null);
		link.css('padding-bottom', '0.2em');
		link.css('top', null);
		if (window.localStorage) {
			localStorage.removeItem('Authorization');
		}
	} else {
		link.removeClass('ui-corner-all');
		link.addClass('ui-corner-top');
		link.addClass('ui-state-active');
		link.css('padding-bottom', '0.4em');
		link.css('border-bottom-width', '0px');
		link.css('top', '1px');
		link.children(".ui-icon").removeClass('ui-icon-circle-arrow-s');
		link.children(".ui-icon").addClass('ui-icon-circle-arrow-n');
		function showForm(form) {
			form.slideDown();
			form.show();
			$("input:first", form).focus();
		}
		if ($("#login-form").size()) {
			showForm($("#login-form"));
		} else {
			window.jQuery.ajax({ type: 'GET', url: "/layout/login.html",
				success: function(data) {
					var form = $(data).find("#login-form").andSelf().filter("#login-form");
					form.css('position', "absolute");
					form.css('display', "none");
					link.after(form);
					form.submit(submitLoginForm);
					showForm(form);
				}
			})
		}
	}
	if (event.preventDefault) {
		event.preventDefault();
	}
	return false;
}

function submitLoginForm(event) {
	try {
		var username = this.elements['username'].value;
		var password = this.elements['password'].value;
		var remember = this.elements['remember'].checked;
		window.jQuery.ajax({ type: 'GET', url: "/accounts?login",
			username: username, password: password,
			success: function(doc) {
				showProfile(doc);
				if (window.localStorage) {
					var auth = "Name " + username;
					if (remember) {
						auth = username + ":" + password;
						if (window.btoa) {
							auth = "Basic " + window.btoa(auth);
						} else {
							auth = "Credentials " + auth;
						}
					}
					localStorage.setItem('Authorization', auth);
				}
				location.reload(false);
			}
		});
	} catch (e) {
		$(event.target).trigger("calliError", e.description ? e.description : e);
	}
	if (event.preventDefault) {
		event.preventDefault();
	}
	return false;
}

})(window.jQuery);

