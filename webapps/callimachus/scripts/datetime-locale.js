// datetime-locale.js

(function($){

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", changeDateLocale, false)
} else {
	window.attachEvent("onload", changeDateLocale)
}

function parseDateTime(text, now) {
	if (/^\s*\d{4}\s*$/.exec(text))
		return NaN
    var timestamp = Date.parse(text)
	if (!isNaN(timestamp))
		return timestamp
    var struct = /(?:(\d{4})-?(\d{2})-?(\d{2}))?(?:[T ]?(\d{2}):?(\d{2}):?(\d{2})(?:\.(\d{3,}))?)?(?:(Z)|([+\-])(\d{2})(?::?(\d{2}))?)?/.exec(text)
	if (!struct)
		return NaN
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
	} else if (struct[4] || struct[5] || struct[6]) {
		if (struct[8] || struct[9]) {
        	timestamp = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5] + minutesOffset, +struct[6], 0)
		} else {
        	timestamp = new Date(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5], +struct[6], 0).getTime()
		}
	}	
	return timestamp;
}

function abbreviated(date, node) {
	var locale = '';
	if (node.is(".datetime-locale, .time-locale")) {
		var minutes;
		if (date.getMinutes() >= 10) {
			minutes = ':' + date.getMinutes()
		} else if (date.getHours() > 0 || date.getMinutes() > 0) {
			minutes = ':0' + date.getMinutes()
		}
		if (date.getHours() > 12) {
			locale = (date.getHours() - 12) + minutes + " pm"
		} else if (date.getHours() == 12) {
			locale = "12" + minutes + " pm"
		} else if (date.getHours() > 0) {
			locale = date.getHours() + minutes + " am"
		} else if (minutes) {
			locale = "12" + minutes + " am"
		}
	}
	if (node.is(".datetime-locale, .date-locale")) {
		if (locale == '' || date.getDate() != now.getDate() || date.getMonth() != now.getMonth() || date.getFullYear() != now.getFullYear()) {
			if (locale) {
				locale += ", "
			}
			var month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][date.getMonth()]
			locale += date.getDate() + ' ' + month
			if (date.getFullYear() != now.getFullYear()) {
				locale += ' ' + date.getFullYear()
			}
		}
	}
	return locale
}

function changeDateLocale() {
	var now = new Date()
	var dates = $(".datetime-locale, .date-locale, .time-locale")
	for (var i = 0; i < dates.length; i++) {
		var node = $(dates[i])
		var text = dates[i].getAttribute("content") || dates[i].textContent || dates[i].innerText
		try {
			var timestamp = parseDateTime(text, now)
			if (!isNaN(timestamp)) {
				var date = new Date(timestamp)
				if ((date.getHours() > 0 || date.getMinutes() > 0) && /^\s*(\d{4})-?(\d{2})-?(\d{2})\s*$/.exec(text)) {
					date = new Date(parseDateTime(text + "T00:00:00"))
				}
				var locale = ''
				if (node.is(".abbreviated")) {
					locale = abbreviated(date, node)
				} else if (node.is(".datetime-locale")) {
					locale = date.toLocaleString()
				} else if (node.is(".date-locale")) {
					locale = date.toLocaleDateString()
				} else if (node.is(".time-locale")) {
					locale = date.toLocaleTimeString()
				}
				dates[i].setAttribute("content", text)
				dates[i].textContent = locale
				dates[i].innerText = locale
			}
		} catch (e) { }
	}
}

})(window.jQuery)

