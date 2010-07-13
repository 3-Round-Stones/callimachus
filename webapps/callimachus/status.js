/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

if (document.addEventListener) {
	document.addEventListener("DOMContentLoaded", initStatus, false);
}  else if (window.attachEvent) {
    window.attachEvent("onload", initStatus);
}

var formRequestCount = 0;
function initStatus() {
	$("form[about]").ajaxSend(function(event, xhr, options){
		if (!$(this).hasClass("wait")) {
			$(this).addClass("wait")
		}
		formRequestCount++
	})
	$("form[about]").ajaxComplete(function(event, xhr, options){
		formRequestCount--
		if (formRequestCount < 1) {
			$(this).removeClass("wait")
			formRequestCount = 0
		}
	})
}

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
		if (detail && detail.indexOf('<') == 0) {
			detail = $(detail).text()
		}
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
