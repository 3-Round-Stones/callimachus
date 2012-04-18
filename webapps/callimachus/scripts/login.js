// login.js

(function($, jQuery){

$(document).ready(function() {
	$("#logout-link").click(function(event) {
		$(document).trigger(jQuery.Event("calliLogout"));
		if (event.preventDefault) {
			event.preventDefault();
		}
		return false;
	});
});

var loginForm;

$(document).bind("calliLoggedIn", function(event) {
	$(document).ready(function() {
		var title = event.title;
		$("#profile-link").text(title);
		if (loginForm) {
			$(loginForm).remove();
			$("#login-form").remove();
			loginForm = null;
		}
	});
});

$(document).bind("calliLoggedOut", function(event) {
	$(document).ready(function() {
		if (!loginForm) {
			loginForm = createLoginButton();
		}
	});
});

$(document).bind("calliLogInPrompt", function() {
	$(document).ready(function() {
		if (loginForm) {
			login.call(loginForm);
		}
	});
});

function createLoginButton() {
	if ($('#login').length) {
		var loginForm = $('<form />')[0];
		$(loginForm).attr('action', $('#profile-link')[0].getAttribute('href'));
		$(loginForm).css('display', "inline-block");
		var button = $('<button type="submit" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-primary" style="margin:0px" />');
		var icon = $('<span class="ui-button-icon-primary ui-icon ui-icon-circle-arrow-s" style="position:absolute;top:50%;margin-top:-8px;height:16px;width:16px;"> </span>');
		var text = $('<span class="ui-button-text" style="padding-left: 22px;">Login</span>');
		button.append(icon);
		button.append(text);
		$(loginForm).append(button);
		$('#login').append(loginForm);
		$(loginForm).submit(login);
		return loginForm;
	}
	return null;
}

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
			setTimeout(function(){form.css('filter','none')}, 1000); // workaround for IE8 transition polyfill
			$("input:first", form).focus();
		}
		if ($("#login-form").length) {
			showForm($("#login-form"));
		} else {
			jQuery.ajax({ type: 'GET', url: calli.getCallimachusUrl("pages/login.html"),
				beforeSend: withCredentials,
				success: function(data) {
					if ($(document.documentElement).is(".noauth")) {
						var form = $(data).find("#login-form").andSelf().filter("#login-form");
						form.css('position', "absolute");
						form.css('top', link.offset().top + link.outerHeight(false));
						var clientWidth = document.documentElement.clientWidth;
						if (clientWidth / 2 < link.offset().left) {
							form.css('right', clientWidth - link.offset().left - link.outerWidth(false));
						} else {
							form.css('left', link.offset().left);
						}
						form.css('overflow', "hidden");
						form.css('padding', "0");
						form.css('height', "0px").css('opacity', '0');
						form.css('-webkit-transition', 'height 1s, opacity 1s');
						form.css('-moz-transition', 'height 1s, opacity 1s');
						form.css('transition', 'height 1s, opacity 1s');
						$('body').append(form);
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
		throw calli.error(e);
	}
	return false;
}

function withCredentials(req) {
	try {
		req.withCredentials = true;
	} catch (e) {}
}

})(jQuery, jQuery);

