// common.js

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", initWiki, false)
	window.addEventListener("DOMContentLoaded", sortElements, false)
	window.addEventListener("DOMContentLoaded", changeDateLocale, false)
	window.addEventListener("DOMContentLoaded", inputPromptTitle, false)
	window.addEventListener("DOMContentLoaded", findAutoExpandTextArea, false)
	window.addEventListener("load", findAutoExpandTextArea, false)
	window.addEventListener("resize", findAutoExpandTextArea, false)
	window.addEventListener("change", targetAutoExpandTextArea, false)
	window.addEventListener("keyup", targetAutoExpandTextArea, false)
	window.addEventListener("paste", targetAutoExpandTextArea, false)
} else {
	window.attachEvent("onload", initWiki)
	window.attachEvent("onload", changeDateLocale)
	window.attachEvent("onload", sortElements)
	window.attachEvent("onload", inputPromptTitle)
	window.attachEvent("onload", findAutoExpandTextArea)
	window.attachEvent("onresize", findAutoExpandTextArea)
	document.attachEvent("onchange", targetAutoExpandTextArea)
	document.attachEvent("onkeyup", targetAutoExpandTextArea)
	document.attachEvent("onpaste", targetAutoExpandTextArea)
}

function initWiki() {
	if (window.Parse) {
		var creole = new Parse.Simple.Creole();
		var wikis = document.querySelectorAll("pre.wiki")
		for (var i = 0; i < wikis.length; i++) {
			var text = wikis[i].getAttribute("content") || wikis[i].textContent || wikis[i].innerText
			var div = document.createElement("div")
			var attrs = wikis[i].attributes
			for(var j=attrs.length-1; j>=0; j--) {
				div.setAttribute(attrs[j].name, attrs[j].value)
			}
			div.setAttribute("content", text)
			wikis[i].parentNode.replaceChild(div, wikis[i])
			creole.parse(div, text)
		}
	}
}

function sortElements() {
	var elements = document.querySelectorAll(".sorted")
	for (var e = 0; e < elements.length; e++) {
		var node = elements[e]
		var nodes = node.childNodes
		var list = [nodes.length]
		for (var i = 0; i < nodes.length; i++) {
			list[i] = nodes[i]
		}
		list.sort(function(a, b) {
			if (a.nodeType < b.nodeType) return -1
			if (a.nodeType > b.nodeType) return 1
			if (a.nodeType != 1) return 0
			var a1 = a.querySelectorAll(".asc")
			var a2 = b.querySelectorAll(".asc")
			if (a1.length && a2.length) {
				var s1 = a1[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				var s2 = a2[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				if (s1 < s2) return -1
				if (s1 > s2) return 1
			} else {
				if (a1.length > a2.length) return -1
				if (a1.length < a2.length) return 1
			}
			var d1 = a.querySelectorAll(".desc")
			var d2 = b.querySelectorAll(".desc")
			if (d1.length && d2.length) {
				var s1 = d1[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				var s2 = d2[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				if (s1 > s2) return -1
				if (s1 < s2) return 1
			} else {
				if (d1.length > d2.length) return -1
				if (d1.length < d2.length) return 1
			}
			return 0
		})
		for (var i = 0; i < nodes.length; i++) {
			node.appendChild(list[i])
		}
	}
}

function changeDateLocale() {
	function parseDateTime(text) {
        var timestamp = Date.parse(text)
        var struct;
        if (isNaN(timestamp) && (struct = /(?:(\d{4})-?(\d{2})-?(\d{2}))?(?:[T ]?(\d{2}):?(\d{2}):?(\d{2})(?:\.(\d{3,}))?)?(?:(Z)|([+\-])(\d{2})(?::?(\d{2}))?)?/.exec(text))) {
            var minutesOffset = 0
            if (struct[8] !== 'Z' && struct[9]) {
                minutesOffset = +struct[10] * 60 + (+struct[11])
                if (struct[9] === '+') {
                    minutesOffset = 0 - minutesOffset
                }
            }
			if (struct[1]) {
				if (struct[4] || struct[5] || struct[6]) {
					if (struct[8] || struct[9]) {
			        	timestamp = Date.UTC(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5] + minutesOffset, +struct[6], 0)
					} else {
			        	timestamp = new Date(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5], +struct[6], 0).getTime()
					}
				} else if (struct[8] || struct[9]) {
		        	timestamp = Date.UTC(+struct[1], +struct[2] - 1, +struct[3], 0, minutesOffset, 0, 0)
				} else {
		        	timestamp = new Date(+struct[1], +struct[2] - 1, +struct[3]).getTime()
				}
			} else {
				if (struct[4] || struct[5] || struct[6]) {
					if (struct[8] || struct[9]) {
			        	timestamp = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5] + minutesOffset, +struct[6], 0)
					} else {
			        	timestamp = new Date(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5], +struct[6], 0).getTime()
					}
				} else if (struct[8] || struct[9]) {
		        	timestamp = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate(), 0, minutesOffset, 0, 0)
				} else {
		        	timestamp = now.getTime()
				}
			}
		}
		return timestamp;
	}
	var now = new Date()
	var dates = document.querySelectorAll(".date-locale")
	for (var i = 0; i < dates.length; i++) {
		var text = dates[i].getAttribute("content") || dates[i].textContent || dates[i].innerText
		try {
			var timestamp = parseDateTime(text)
			if (!isNaN(timestamp)) {
				var date = new Date(timestamp)
				if ((date.getHours() > 0 || date.getMinutes() > 0) && /^\s*(\d{4})-?(\d{2})-?(\d{2})\s*$/.exec(text)) {
					date = new Date(parseDateTime(text + "T00:00:00"))
				}
				var month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][date.getMonth()]
				var locale = '';
				if (date.getMinutes() > 10) {
					locale = date.getHours() + ':' + date.getMinutes()
				} else if (date.getHours() > 0 || date.getMinutes() > 0) {
					locale = date.getHours() + ':0' + date.getMinutes()
				}
				if (locale == '' || date.getDate() != now.getDate() || date.getMonth() != now.getMonth() || date.getFullYear() != now.getFullYear()) {
					if (locale) {
						locale += ", "
					}
					locale += date.getDate() + ' ' + month
					if (date.getFullYear() != now.getFullYear()) {
						locale += ' ' + date.getFullYear()
					}
				}
				dates[i].setAttribute("content", text)
				dates[i].textContent = locale
				dates[i].innerText = locale
			}
		} catch (e) { }
	}
}

function findAutoExpandTextArea() {
	var areas = document.querySelectorAll(".auto-expand")
	for (var i = 0; i < areas.length; i++) {
		if (areas[i].type == "textarea" || areas[i].type == "text") {
			expandTextArea(areas[i])
		}
	}
}

function targetAutoExpandTextArea(event) {
	var target = null
	if (event.target && event.target.type == "textarea") {
		target = event.target
	} else if (event.srcElement && event.srcElement.type == "textarea") {
		target = event.srcElement
	} else if (event.target && event.target.type == "text") {
		target = event.target
	} else if (event.srcElement && event.srcElement.type == "text") {
		target = event.srcElement
	}
	if (target && target.className.match(/\bauto-expand\b/)) {
		expandTextArea(target)
	}
}

function expandTextArea(area) {
	var clientWidth = document.documentElement.clientWidth
	var clientHeight = window.innerHeight
	var trail = area
	while (trail) {
		clientWidth -= trail.offsetLeft
		trail = trail.offsetParent
	}
	var width = area.cols || area.size
	var height = area.rows
	var maxCols = Math.floor(clientWidth / area.offsetWidth * width - 3)
	var maxRows = Math.floor(clientHeight / area.offsetHeight * height - 3)
	if (!maxCols || maxCols < 2) {
		maxCols = 96
	}
	if (!maxRows || maxRows < 2) {
		maxRows = 43
	}
	var lines = area.value.split("\n")
	var cols = 20
	var rows = Math.max(1, lines.length)
	for (var i = 0; i < lines.length; i++) {
		var len = lines[i].replace("\t", "        ").length
		if (cols < len) {
			cols = len
		}
		rows += Math.floor(len / maxCols)
	}
	if (area.type == "textarea") {
		area.cols = Math.min(maxCols, cols)
		area.rows = Math.min(maxRows, rows + 1)
	} else {
		area.size = Math.min(maxCols, cols)
	}
}

function inputPromptTitle() {
	function initInputPromptTitle(input, title) {
		var promptSpan = document.createElement("span")
		promptSpan.setAttribute("style", "position: absolute; font-style: italic; color: #aaa; margin: 0.2em 0 0 0.5em;")
		promptSpan.setAttribute('id', 'input-prompt-' + i)
		promptSpan.setAttribute("title", title)
		promptSpan.textContent = title
		promptSpan.innerText = title
		promptSpan.onclick = function() {
			promptSpan.style.display = "none"
			input.focus()
		}
		if(input.value != '') {
			promptSpan.style.display = "none"
		}
		input.parentNode.insertBefore(promptSpan, input)
		input.onfocus = function() {
			promptSpan.style.display = "none"
		}
		input.onblur = function() {
			if(input.value == '') {
				promptSpan.style.display = "inline"
			}
		}
	}
	var inputs = document.getElementsByTagName("input")
	for (var i = 0; i < inputs.length; i++) {
		var title = inputs[i].getAttribute("title")
		if (title) {
			initInputPromptTitle(inputs[i], title)
		}
	}
}
