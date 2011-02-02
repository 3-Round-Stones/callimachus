// login-status.js

(function($){

if (window.sessionStorage && sessionStorage.getItem("Profile")) {
	$(document).ready(function(){loggedIn(sessionStorage.getItem("Profile"))});
} else {
	var options = {type: "GET",
		complete: function(req) {
			if (req.status < 300 ) {
				$(document).ready(function(){
					var doc = req.responseText;
					var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(doc);
					if (title) {
						if (window.sessionStorage) {
							sessionStorage.setItem("Profile", title[1]);
						}
						loggedIn(title[1]);
					} else {
						loggedIn("Profile");
					}
				})
			} else {
				$(document).ready(loggedOut);
			}
		},
		beforeSend: function(req) {
			try {
				req.withCredentials = true;
			} catch (e) {}
		}
	}
	options.url = "/accounts?authenticated";
	if (window.localStorage) {
		var auth = localStorage.getItem('Authorization');
		if (auth && auth.indexOf("Basic ") == 0) {
			var up = window.atob(auth.substring("Basic ".length));
			options.url = "/accounts?welcome";
			options.username = up.substring(0, up.indexOf(':'));
			options.password = up.substring(up.indexOf(':') + 1);
		} else if (auth && auth.indexOf("Credentials ") == 0) {
			var up = auth.substring("Credentials ".length);
			options.url = "/accounts?welcome";
			options.username = up.substring(0, up.indexOf(':'));
			options.password = up.substring(up.indexOf(':') + 1);
		}
	}
	window.jQuery.ajax(options);
}

function loggedIn(title) {
	$("#welcome-link").text(title);
	$(".authenticated").show();
	$("#logout-link").click(function(event) {
		var href = this.href;
		window.jQuery.ajax({ type: 'GET', url: href,
			username: 'logout', password: 'nil',
			complete: function() {
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

function loggedOut() {
	$(".authenticated").hide();
	$("#login-link").show();
	$("#login-link").click(function(event) {
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
	})
	if (window.localStorage && localStorage.getItem('Authorization')) {
		$("#login-link").click();
	}
}

function submitLoginForm(event) {
	var username = this.elements['username'].value;
	var password = this.elements['password'].value;
	window.jQuery.ajax({ type: 'GET', url: "/accounts?welcome",
		username: username,
		password: password,
		success: function(data) {
			if (window.sessionStorage) {
				var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(data);
				sessionStorage.setItem("Profile", title[1]);
			}
			location.reload();
		}
	})
	if (window.localStorage) {
		var auth = "Name " + username;
		if (this.elements['remember'].checked) {
			auth = username + ":" + password;
			if (window.btoa) {
				auth = "Basic " + window.btoa(auth);
			} else {
				auth = "Credentials " + auth;
			}
		}
		localStorage.setItem('Authorization', auth);
	}
	if (event.preventDefault) {
		event.preventDefault();
	}
	return false;
}

})(window.jQuery);

