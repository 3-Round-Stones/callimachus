// login.js

(function($){

$(document).ready(function(){
	$("#profile-link").text('');
	$(document).bind("calliLogin", function(event) {
		// remove bogus data
		if (window.sessionStorage) {
			sessionStorage.removeItem('Name');
			localStorage.removeItem('Authorization');
		}
		var options = {type: "GET", url: "/accounts?login",
			success: function(doc) {
				var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(doc);
				if (title) {
					if (window.sessionStorage) {
						// now logged in
						sessionStorage.setItem("Name", title[1]);
						localStorage.setItem("Name", title[1]);
					}
					if (!$("#profile-link").text()) {
						loggedIn(title[1]);
					}
				}
				if (window.localStorage) {
					var auth = "User " + event.username;
					if (event.remember) {
						auth = event.username + ":" + event.password;
						if (window.btoa) {
							auth = "Basic " + window.btoa(auth);
						} else {
							auth = "Credentials " + auth;
						}
					}
					localStorage.setItem('Authorization', auth);
				}
				if (!event.isDefaultPrevented() && event.location) {
					location = event.location;
				} else if (!event.isDefaultPrevented() && event.location) {
					location.reload(false);
				}
			},
			error: function(xhr, textStatus, errorThrown) {
				// Safari don't support spaces in ajax passwords
				if (xhr && xhr.status == 0) {
					// bring up browser login dialog
					location = "/accounts?login";
				}
			}
		};
		if (event.username) {
			options.username = event.username;
		}
		if (event.password) {
			options.password = event.password;
		} else {
			options.beforeSend = withCredentials;
		}
		try {
			jQuery.ajax(options);
		} catch (e) {
			// Opera don't support spaces in ajax passwords
			// bring up browser login dialog
			location = "/accounts?login";
		}
	});

	$(document).bind("calliLogout", function(event) {
		if (window.sessionStorage) {
			sessionStorage.removeItem('Name');
			localStorage.removeItem('Authorization');
			localStorage.removeItem('NotLoginCount');
			localStorage.removeItem('LoginCount');
			localStorage.removeItem('Name');
		}
		window.jQuery.ajax({ type: 'GET', url: "/accounts?logout",
			username: 'logout', password: 'nil',
			success: function(data) {
				location = "/";
			}
		});
	});

	// IE8 doesn't support event.key
	var oldName = localStorage.getItem('Name');
	var oldNotLoginCount = localStorage.getItem('NotLoginCount');
	var oldLoginCount = localStorage.getItem('LoginCount');
	$(window).bind('storage', handleStorageEvent);
	$(document).bind('storage', handleStorageEvent); // IE
	function handleStorageEvent(e) {
		var newName = localStorage.getItem('Name');
		var newNotLoginCount = localStorage.getItem('NotLoginCount');
		var newLoginCount = localStorage.getItem('LoginCount');
		if (newName != oldName) {
			oldName = newName;
			var currently = $("#profile-link").text();
			if (currently && !newName) {
				// now logged out
				sessionStorage.removeItem('Name');
				loggedOut();
				location = "/";
			} else if (!currently && newName) {
				// now logged in
				sessionStorage.setItem("Name", newName);
				loggedIn(newName);
			}
		}
		if (newNotLoginCount != oldNotLoginCount) {
			oldNotLoginCount = newNotLoginCount;
			var currently = $("#profile-link").text();
			if (currently && newNotLoginCount) {
				// a window is not sure if we are logged in
				localStorage.setItem("Name", currently);
				var count = localStorage.getItem('LoginCount');
				localStorage.setItem('LoginCount', count ? parseInt(count) + 1 : 1);
			} 
		}
		if (newLoginCount != oldLoginCount) {
			oldLoginCount = newLoginCount;
			var currently = $("#profile-link").text();
			if (!currently && newLoginCount) {
				// another window says we are logged in
				var value = localStorage.getItem("Name");
				sessionStorage.setItem("Name", value);
				loggedIn(value);
			} 
		}
		return true;
	}

	if (window.sessionStorage && sessionStorage.getItem("Name")) {
		// logged in already
		loggedIn(sessionStorage.getItem("Name"));
	} else {
		loggedOut();
		if (window.localStorage && localStorage.getItem('Authorization')) {
			// was logged in previously
			var auth = localStorage.getItem('Authorization');
			if (auth && auth.indexOf("Basic ") == 0) {
				// remember me Firefox, Chrome, Safari
				var up = window.atob(auth.substring("Basic ".length));
				var event = jQuery.Event("calliLogin");
				event.preventDefault(); // don't reload page
				event.username = up.substring(0, up.indexOf(':'));
				event.password = up.substring(up.indexOf(':') + 1);
				event.remember = true;
				$(document).trigger(event);
				return;
			} else if (auth && auth.indexOf("Credentials ") == 0) {
				// remember me IE
				var up = auth.substring("Credentials ".length);
				var event = jQuery.Event("calliLogin");
				event.preventDefault(); // don't reload page
				event.username = up.substring(0, up.indexOf(':'));
				event.password = up.substring(up.indexOf(':') + 1);
				event.remember = true;
				$(document).trigger(event);
				return;
			} else if (localStorage.getItem('Name')) {
				// was logged in before, prompt to log back in
				var count = localStorage.getItem('NotLoginCount');
				localStorage.setItem('NotLoginCount', count ? parseInt(count) + 1 : 1);
				setTimeout(function() {
					if ($("#login-link").is(":visible")) {
						$("#login-link").click();
					}
				}, 0); // check if already logged in
			}
		}
		// hasn't logged in using the login form; is this page protected?
		var xhr = jQuery.ajax({type: 'GET', url: location.href,
			beforeSend: withCredentials,
			success: function() {
				if (xhr.getResponseHeader("Authentication-Info")) { 
					var event = jQuery.Event("calliLogin");
					event.preventDefault(); // don't reload page
					$(document).trigger(event);
				} else if (!xhr.getAllResponseHeaders()) { // Opera sends empty response; try again w/o cache
					xhr = jQuery.ajax({type: 'GET', url: location.href,
						beforeSend: function(xhr) {
							xhr.setRequestHeader('Cache-Control', 'no-cache');
							withCredentials(xhr);
						},
						success: function() {
							if (xhr.getResponseHeader("Authentication-Info")) {
								var event = jQuery.Event("calliLogin");
								event.preventDefault(); // don't reload page
								$(document).trigger(event);
							}
						}
					});
				}
			}
		});
	}
});

function loggedIn(title) {
	$("#login-form").hide();
	$("#login-link").hide();
	$("#profile-link").text(title);
	$(".authenticated").css('display', null);
	$("#logout-link").click(function(event) {
		$(document).trigger(jQuery.Event("calliLogout"));
		if (event.preventDefault) {
			event.preventDefault();
		}
		return false;
	}) ;
	$(document).trigger("calliLoggedIn");
}

function loggedOut() {
	$(".authenticated").css('display', 'none');
	$("#login-link").show();
	$("#login-link").click(login);
	$(document).trigger("calliLoggedOut");
}

function login(event) {
	var link = $(this);
	if ($("#login-form").size() && $("#login-form").get(0).style.display != 'none' ) {
		$("#login-form").slideUp();
		$("#login-form").hide();
		link.css('border-bottom-width', '1px');
		link.css('padding-bottom', '0.2em');
		link.css('top', null);
		if (window.localStorage) {
			localStorage.removeItem('Authorization');
		}
	} else {
		link.css('padding-bottom', '0.4em');
		link.css('border-bottom-width', '0px');
		link.css('top', '1px');
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
					if ($("#login-link").is(":visible")) {
						var form = $(data).find("#login-form").andSelf().filter("#login-form");
						form.css('position', "absolute");
						form.css('display', "none");
						link.after(form);
						form.submit(submitLoginForm);
						showForm(form);
					}
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
		var event = jQuery.Event("calliLogin");
		event.username = this.elements['username'].value;
		event.password = this.elements['password'].value;
		event.remember = this.elements['remember'].checked;
		$(document).trigger(event);
	} catch (e) {
		$(event.target).trigger("calliError", e.description ? e.description : e);
	}
	if (event.preventDefault) {
		event.preventDefault();
	}
	return false;
}

function withCredentials(req) {
	try {
		req.withCredentials = true;
	} catch (e) {}
}

})(window.jQuery);

