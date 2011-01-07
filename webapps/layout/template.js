// template.js

(function($){

var options = {type: "GET",
	complete: function(req) {
		if (req.status < 300 ) {
			$(document).ready(function(){loggedIn(req)})
		} else {
			$(document).ready(loggedOut)
		}
	},
	beforeSend: function(req) {
		try {
			req.withCredentials = true
		} catch (e) {}
	}
}
options.url = "/accounts?authenticated"
if (window.localStorage) {
	var auth = localStorage.getItem('Authorization')
	if (auth && auth.indexOf("Basic ") == 0) {
		var up = window.atob(auth.substring("Basic ".length))
		options.url = "/accounts?welcome"
		options.username = up.substring(0, up.indexOf(':'))
		options.password = up.substring(up.indexOf(':') + 1)
	} else if (auth && auth.indexOf("Credentials ") == 0) {
		var up = auth.substring("Credentials ".length)
		options.url = "/accounts?welcome"
		options.username = up.substring(0, up.indexOf(':'))
		options.password = up.substring(up.indexOf(':') + 1)
	}
}
jQuery.ajax(options)

function loggedIn(req) {
	var doc = req.responseText
	var url = /about="([^" >]*)"/i.exec(doc)[1]
	var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(doc)[1]
	$("#welcome-link").text(title)
	var links = document.getElementById("authenticated-links").getElementsByTagName("a")
	for (var i=0; i<links.length; i++) {
		if (links[i].getAttribute("href").indexOf("?") == 0) {
			links[i].setAttribute("href", url + links[i].getAttribute("href"))
		}
	}
	$(".authenticated").show()
	$("#logout-link").click(function(event) {
		var href = this.href
		jQuery.ajax({ type: 'GET', url: href,
			username: 'logout', password: 'nil',
			complete: function() {
				document.location = href
			}
		})
		if (window.localStorage) {
			localStorage.removeItem('Authorization')
		}
		if (event.preventDefault) {
			event.preventDefault()
		}
		return false
	}) 
}

function loggedOut() {
	$(".authenticated").hide()
	$("#login-link").show()
	$("#login-link").click(function(event) {
		if ($("#login-form").get(0).style.display == 'none' ) {
			$(this).removeClass('ui-corner-all')
			$(this).addClass('ui-corner-top')
			$(this).addClass('ui-state-active')
			$(this).css('padding-bottom', '0.4em')
			$(this).css('border-bottom-width', '0px')
			$("#login-form").slideDown()
			$("#login-form").show()
			$(this).css('top', '1px')
			$(this).children(".ui-icon").removeClass('ui-icon-circle-arrow-s')
			$(this).children(".ui-icon").addClass('ui-icon-circle-arrow-n')
			$("#login-form input:first").focus()
		} else {
			$("#login-overlay").fadeOut()
			$("#login-form").slideUp()
			$("#login-form").hide()
			$(this).children(".ui-icon").removeClass('ui-icon-circle-arrow-n')
			$(this).children(".ui-icon").addClass('ui-icon-circle-arrow-s')
			$(this).removeClass('ui-corner-top')
			$(this).removeClass('ui-state-active')
			$(this).addClass('ui-corner-all')
			$(this).css('border-bottom-width', null)
			$(this).css('padding-bottom', '0.2em')
			$(this).css('top', null)
			if (window.localStorage) {
				localStorage.removeItem('Authorization')
			}
		}
		if (event.preventDefault) {
			event.preventDefault()
		}
		return false
	})
	$("#login-form").submit(function(event) {
		var username = this.elements['username'].value
		var password = this.elements['password'].value
		jQuery.ajax({ type: 'GET', url: $("#login-link").get(0).href,
			username: username,
			password: password,
			complete: function() {
				document.location.reload()
			}
		})
		if (window.localStorage) {
			var auth = "Name " + username
			if (this.elements['remember'].checked) {
				auth = username + ":" + password
				if (window.btoa) {
					auth = "Basic " + window.btoa(auth)
				} else {
					auth = "Credentials " + auth
				}
			}
			localStorage.setItem('Authorization', auth)
		}
		if (event.preventDefault) {
			event.preventDefault()
		}
		return false
	})
	if (window.localStorage && localStorage.getItem('Authorization')) {
		$("#login-overlay").fadeIn()
		$("#login-link").click()
	}
}

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", hideFluffIfInFrame, false)
} else {
	window.attachEvent("onload", hideFluffIfInFrame)
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

if (!window.calli) {
	window.calli = {}
}
window.calli.printpage = function() {
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
		node.style.background = "inherit"
	}
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
	forms.bind("calliRedirect", function() {
		$("body").addClass("wait")
		formRequestCount++
	})
	forms.bind("calliSubmit", function() {
		$("#error-widget").hide()
		$("#error-message").empty()
		$("#error-widget pre").remove()
	})
	forms.bind("calliOk", function() {
		$("#error-widget").hide()
		$("#error-message").empty()
		$("#error-widget pre").remove()
	})
	forms.bind("calliError", function(event, status, detail) {
		if (detail && detail.indexOf('<') == 0) {
			var html = $(detail);
			if (html.filter("title").length) {
				status = html.filter("title").text();
				detail = html.find("pre").text();
			}
		}
		var msg = $("#error-message")
		if (msg.size()) {
			msg.empty()
			msg.text(status)
			$("#error-widget pre").remove()
			$("#error-widget").show()
			msg.focus()
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
			alert(status);
		}
	})
})

})(jQuery)

