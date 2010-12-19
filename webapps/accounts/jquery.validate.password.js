/*
 * jQuery validate.password plug-in 1.0+
 *
 * http://bassistance.de/jquery-plugins/jquery-plugin-validate.password/
 *
 * Copyright (c) 2009 Jörn Zaefferer
 * Copyright (c) 2006 Steve Moitozo <god at zilla dot us>
 *
 * $Id$
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 */
(function($) {
		
	function rating(rate, message) {
		return {
			rate: rate,
			messageKey: message
		};
	}
	
	$.validator.passwordRating = function(passwd, username) {
		if (!passwd || passwd.length < 4)
			return rating(0, "too-short");
		if (username && passwd.length <= username.length * 2) {
			var p = passwd.toLowerCase();
			var u = username.toLowerCase();
			if (p.match(u) || u.match(p))
				return rating(0, "similar-to-username");
		}
		var lower = passwd.match(/[a-z]/);
		var upper = passwd.match(/.[A-Z]./);
		var numeric = passwd.match(/[0-9]/);
		var special = passwd.match(/[!@#$%\^\&*\(\)\-_+=]/);
		var other = passwd.match(/[^a-zA-Z0-9!@#$%\^\&*\(\)\-_+=]/);
		if(passwd.length > 14) {
			return rating(4, "strong");
		} else if (passwd.length > 8 && lower && upper && numeric && special && other) {
			return rating(4, "strong");
		} else if (passwd.length > 9) {
			return rating(3, "good");
		} else if (passwd.length > 5 && (lower || upper) && (numeric || special || other)) {
			return rating(3, "good");
		} else if (passwd.length > 5) {
			return rating(2, "weak");
		} else if ((lower || upper) && (numeric || special || other)) {
			return rating(2, "weak");
		} else {
			return rating(1, "very-weak");
		}
	}
	
	$.validator.passwordRating.messages = {
		"similar-to-username": "Too similar to username",
		"too-short": "Too short",
		"very-weak": "Very weak",
		"weak": "Weak",
		"good": "Good",
		"strong": "Strong"
	}
	
	$.validator.addMethod("password", function(value, element, usernameField) {
		// use untrimmed value
		var password = element.value,
		// get username for comparison, if specified
			username = $(typeof usernameField != "boolean" ? usernameField : []);
			
		var rating = $.validator.passwordRating(password, username.val());
		// update message for this field
		
		var meter = $(".password-meter", element.form);
		
		meter.find(".password-meter-bar").removeClass().addClass("password-meter-bar").addClass("password-meter-" + rating.messageKey);
		meter.find(".password-meter-message")
		.removeClass()
		.addClass("password-meter-message")
		.addClass("password-meter-message-" + rating.messageKey)
		.text($.validator.passwordRating.messages[rating.messageKey]);
		// display process bar instead of error message
		
		return rating.rate > 0;
	}, "");
	// manually add class rule, to make username param optional
	$.validator.classRuleSettings.password = { password: true };
	
})(jQuery);
