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
		if (username && (passwd.toLowerCase().match(username.toLowerCase()) || username.toLowerCase().match(passwd.toLowerCase()) ))
			return rating(0, "similar-to-username");

		var intScore   = 0
		
		// PASSWORD LENGTH
		if (passwd.length<5)                         // length 4 or less
		{
			intScore = (intScore+3)
		}
		else if (passwd.length>4 && passwd.length<8) // length between 5 and 7
		{
			intScore = (intScore+6)
		}
		else if (passwd.length>7 && passwd.length<16)// length between 8 and 15
		{
			intScore = (intScore+12)
		}
		else if (passwd.length>15)                    // length 16 or more
		{
			intScore = (intScore+18)
		}
		
		
		// LETTERS (Not exactly implemented as dictacted above because of my limited understanding of Regex)
		if (passwd.match(/[a-z]/))                              // [verified] at least one lower case letter
		{
			intScore = (intScore+1)
		}
		
		if (passwd.match(/[A-Z]/))                              // [verified] at least one upper case letter
		{
			intScore = (intScore+5)
		}
		
		// NUMBERS
		if (passwd.match(/\d+/))                                 // [verified] at least one number
		{
			intScore = (intScore+5)
		}
		
		if (passwd.match(/(.*[0-9].*[0-9].*[0-9])/))             // [verified] at least three numbers
		{
			intScore = (intScore+5)
		}
		
		
		// SPECIAL CHAR
		if (passwd.match(/.[^a-zA-Z0-9]/))            // [verified] at least one special character
		{
			intScore = (intScore+5)
		}
		
									 // [verified] at least two special characters
		if (passwd.match(/(.*[^a-zA-Z0-9].*[^a-zA-Z0-9])/))
		{
			intScore = (intScore+5)
		}
	
		
		// COMBOS
		if (passwd.match(/([a-z].*[A-Z])|([A-Z].*[a-z])/))        // [verified] both upper and lower case
		{
			intScore = (intScore+2)
		}

		if (passwd.match(/([a-zA-Z])/) && passwd.match(/([0-9])/)) // [verified] both letters and numbers
		{
			intScore = (intScore+2)
		}
 
									// [verified] letters, numbers, and special characters
		if (passwd.match(/([a-zA-Z0-9].*[^a-zA-Z0-9])|([^a-zA-Z0-9].*[a-zA-Z0-9])/))
		{
			intScore = (intScore+2)
		}
	
	
		if(intScore < 16)
		{
			return rating(1, "very-weak");
		}
		else if (intScore > 15 && intScore < 25)
		{
			return rating(2, "weak");
		}
		else if (intScore > 24 && intScore < 35)
		{
			return rating(3, "good");
		}
		else
		{
			return rating(4, "strong");
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
