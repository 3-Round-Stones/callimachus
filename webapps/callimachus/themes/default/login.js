// login.js

(function($){

$(document).bind("calliLogInPrompt", function() {
	$(document).ready(function() {
		if ($(document.documentElement).is(".noauth")) {
			$("#header-login").submit();
		}
	});
});

$(document).bind("calliLoggedIn", function(event) {
	$(document).ready(function() {
		var title = event.title;
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

$(document).ready(function() {
	$("#header-login").submit(login);
});

function login(event) {
	var link = $(this);
	if (link.find('button').length) {
		link = link.find('button');
	}
	if ($("#login-form").size() && $("#login-form").css('display') != 'none' ) {
		$("#login-form").slideUp(function(){
			$("#login-form").hide();
			link.css('margin-left', null);
			link.css('margin-bottom', null);
			link.css('border-bottom-width', null);
			link.css('padding-bottom', null);
			link.css('top', null);
		});
		$(document).trigger("calliLogout");
	} else {
		link.css('margin-left', '0px');
		link.css('margin-bottom', '0px');
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
			window.jQuery.ajax({ type: 'GET', url: "/callimachus/themes/default/login.html",
				success: function(data) {
					if ($(document.documentElement).is(".noauth")) {
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

