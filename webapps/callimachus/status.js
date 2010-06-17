/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

function showRequest() {
	$("#message").empty();
	$('#message').trigger('status.calli', []);
}

function showSuccess() {
	$("#message").empty();
	$('#message').trigger('status.calli', []);
}

function showError(text, detail) {
	var msg = $("#message")
	if (msg.size()) {
		msg.empty()
		msg.text(text)
		msg.addClass("error")
		msg.focus()
		if (detail) {
			var pre = $("<pre/>")
			pre.text(detail)
			pre.hide()
			msg.append(pre)
			msg.click(function() {
				pre.toggle()
			})
		}
        msg.trigger('status.calli', [text, detail]);
	} else {
		alert(text);
	}
}
