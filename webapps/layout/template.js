// template.js

(function($){

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
			var links = document.getElementById("authenticated-links").getElementsByTagName("a")
			for (var i=0; i<links.length; i++) {
				if (links[i].getAttribute("href").indexOf("?") == 0) {
					links[i].setAttribute("href", url + links[i].getAttribute("href"))
				}
			}
			$("#login-link").hide()
			$(".authenticated").show()
			$("#logout-link").click(function(event) {
				$.ajax({ type: 'GET', url: this.href,
					username: 'logout', password: 'nil',
					complete: function() {
						document.location = '/'
					}
				})
				if (event.preventDefault) {
					event.preventDefault()
				}
				return false
			})
		} else if (req.readyState == 4) {
			$("#login-link").show()
			$(".authenticated").hide()
		}
	}
	req.send(null)
}

var formRequestCount = 0;
$(document).ready(function() {
	$(window).unload(function (event) {
		$("body").addClass("wait")
		formRequestCount++
	})
	$("body").ajaxSend(function(event, xhr, options){
		if (!$(this).hasClass("wait")) {
			$(this).addClass("wait")
		}
		formRequestCount++
	})
	$("body").ajaxComplete(function(event, xhr, options){
		formRequestCount--
		if (formRequestCount < 1) {
			$(this).removeClass("wait")
			formRequestCount = 0
		}
	})
	var forms = $("form[about]")
	forms.bind("calli:redirect", function() {
		$("body").addClass("wait")
		formRequestCount++
	})
	forms.bind("calli:submit", function() {
		$("#error-widget").hide()
		$("#error-message").empty()
		$("#error-widget pre").remove()
	})
	forms.bind("calli:ok", function() {
		$("#error-widget").hide()
		$("#error-message").empty()
		$("#error-widget pre").remove()
	})
	forms.bind("calli:error", function(event, text, detail) {
		var msg = $("#error-message")
		if (msg.size()) {
			msg.empty()
			msg.text(text)
			$("#error-widget pre").remove()
			$("#error-widget").show()
			msg.focus()
			if (detail && detail.indexOf('<') == 0) {
				detail = $(detail).text()
			}
			if (detail) {
				var pre = $("<pre/>")
				pre.text(detail)
				pre.hide()
				$("#error-widget").append(pre)
				msg.click(function() {
					pre.toggle()
				})
			}
		} else {
			alert(text);
		}
	})
})

})(jQuery)

