// login.js

(function($){

$(document).bind("calliLogInPrompt", function() {
	$(document).ready(function() {
		if ($("#login-link").is(":visible")) {
			$("#login-link").click();
		}
	});
});

$(document).bind("calliLoggedIn", function(event) {
	$(document).ready(function() {
		var title = event.title;
		$("#login-form").hide();
		$("#login-link").hide();
		$("#profile-link").text(title);
		$("#logout-link").click(function(event) {
			$(document).trigger(jQuery.Event("calliLogout"));
			if (event.preventDefault) {
				event.preventDefault();
			}
			return false;
		});
	});
});

$(document).bind("calliLoggedOut", function() {
	$(document).ready(function() {
		$("#login-link").show();
		$("#login-link").click(login);
	});
});

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

})(jQuery);

