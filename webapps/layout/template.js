// template.js

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", hideFluffIfInFrame, false)
	window.addEventListener("DOMContentLoaded", getCredentials, false)
} else {
	window.attachEvent("onload", hideFluffIfInFrame)
	window.attachEvent("onload", getCredentials)
}

function hideFluffIfInFrame() {
	try {
		if (window.frameElement) {
			hideSiblings(document.getElementById("content"))
			var asides = document.querySelectorAll(".aside")
			for (var i = 0; i < asides.length; i++) {
				asides[i].parentNode.removeChild(asides[i])
			}
		}
	} catch(e) {}
}

function printpage() {
	hideSiblings(document.getElementById("content"))
	print()
	setTimeout(function(){location.reload()}, 500)
}

function hideSiblings(node) {
	if (node && node.parentNode) {
		hideSiblings(node.parentNode)
		var siblings = node.parentNode.childNodes
		var i = siblings.length
		while (i--) {
			if (siblings[i].nodeType == 1 && siblings[i] != node) {
				siblings[i].style.display = "none"
			} else if (siblings[i].nodeType == 3 || siblings[i].nodeType == 4) {
				node.parentNode.removeChild(siblings[i])
			}
		}
		node.style.border = "none"
		node.style.margin = "0px"
	}
}

function getCredentials() {
	var auth = document.getElementById("authenticated-link").href
	var req = new XMLHttpRequest()
	req.open("GET", auth, true)
	req.withCredentials = true
	req.onreadystatechange = function() {
		if (req.readyState == 4 && req.status==200) {
			var doc = req.responseText
			var url = /<base[^>]*href="?([^" >]*)"?[^>"]*>/i.exec(doc)[1]
			var title = /<title[^>]*>([^<]*)<\/title>/i.exec(doc)[1]
			var link = document.getElementById("authenticated-link")
			link.textContent = title
			link.innerText = title
			var links = document.getElementById("authenticated-span").getElementsByTagName("a")
			for (var i=0; i<links.length; i++) {
				if (links[i].getAttribute("href").indexOf("?") == 0) {
					links[i].setAttribute("href", url + links[i].getAttribute("href"))
				}
			}
			document.getElementById("login-link").style.display = "none"
			document.getElementById("authenticated-span").style.display = "inline"
		} else if (req.readyState == 4) {
			document.getElementById("login-link").style.display = "inline"
			document.getElementById("authenticated-span").style.display = "none"
			var tabs = document.getElementById("tabs").childNodes;
			for (var i = 0; i < tabs.length; i++) {
				if (tabs[i].style) {
					tabs[i].style.display = "none"
				}
			}
		}
	}
	req.send(null)
}

$(document).ready(initStatus);

var formRequestCount = 0;
function initStatus() {
	$(window).unload(function (event) {
		$("form[about]").addClass("wait")
		formRequestCount++
	})
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

function showPageLoading() {
	$("form[about]").addClass("wait")
	formRequestCount++
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

