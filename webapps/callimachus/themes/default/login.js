// login.js

(function($){

$(document).bind("calliLogInPrompt", function() {
	$(document).ready(function() {
		if ($(document.documentElement).is(".noauth")) {
			login.call($("#header-login")[0]);
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
	if ($("#login-form").size() && $("#login-form").css('height') != '0px' ) {
		$("#login-form").css('height', '0px').css('opacity', '0');
		link.css('margin-left', null);
		link.css('margin-bottom', null);
		link.css('border-bottom-width', null);
		link.css('padding-bottom', null);
		link.css('top', null);
		$(document).trigger("calliLogout");
	} else {
		link.css('margin-left', '0px');
		link.css('margin-bottom', '0px');
		link.css('padding-bottom', '0.4em');
		link.css('border-bottom-width', '0px');
		link.css('top', '1px');
		function showForm(form) {
			form.css('opacity', '1').css('height', form.children('div').outerHeight() + 'px');
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
						form.css('overflow', "hidden");
						form.css('padding', "0");
						form.css('height', "0px").css('opacity', '0');
						form.css('-webkit-transition', 'height 1s, opacity 1s');
						form.css('-moz-transition', 'height 1s, opacity 1s');
						form.css('transition', 'height 1s, opacity 1s');
						link.after(form);
						form.submit(submitLoginForm);
						setTimeout(function(){showForm(form)});
					}
				}
			})
		}
	}
	if (event && event.preventDefault) {
		event.preventDefault();
	}
	return false;
}

function submitLoginForm(event) {
	try {
		var event = jQuery.Event("calliLogin");
		event.preventDefault();
		event.username = this.elements['username'].value;
		event.password = this.elements['password'].value;
		event.remember = this.elements['remember'].checked;
		$(document).trigger(event);
	} catch (e) {
		$(event.target).trigger("calliError", e.description ? e.description : e);
	}
	return false;
}

})(jQuery);

