// datetime-locale.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
	var now = new Date();
	$(".datetime-locale, .date-locale, .time-locale", event.target).each(function(i, node) {
		changeDateLocale(node, now);
	})
	if ($(event.target).is(".datetime-locale, .date-locale, .time-locale")) {
		changeDateLocale(event.target, now);
	}
}

function changeDateLocale(node, now) {
	var node = $(node);
	if (!node.attr("content")) {
		var text = node.text();
		var timestamp = parseDateTime(text, now);
		if (!isNaN(timestamp)) {
			node.attr("content", text);
			var date = new Date(timestamp);
			if ((date.getHours() > 0 || date.getMinutes() > 0) && /^\s*(\d{4})-?(\d{2})-?(\d{2})\s*$/.exec(text)) {
				date = new Date(parseDateTime(text + "T00:00:00"));
			}
			if (node.is(".abbreviated")) {
				node.text(abbreviated(date, node, now));
			} else if (node.is(".datetime-locale")) {
				node.text(date.toLocaleString());
			} else if (node.is(".date-locale")) {
				node.text(date.toLocaleDateString());
			} else if (node.is(".time-locale")) {
				node.text(date.toLocaleTimeString());
			}
		}
	}
}

function parseDateTime(text, now) {
	if (/^\s*\d{4}\s*$/.exec(text))
		return NaN;
    var timestamp = Date.parse(text);
	if (!isNaN(timestamp))
		return timestamp;
    var struct = /(?:(\d{4})-?(\d{2})-?(\d{2}))?(?:[T ]?(\d{2}):?(\d{2}):?(\d{2})(?:\.(\d{3,}))?)?(?:(Z)|([+\-])(\d{2})(?::?(\d{2}))?)?/.exec(text);
	if (!struct)
		return NaN;
    var minutesOffset = 0;
    if (struct[8] !== 'Z' && struct[9]) {
        minutesOffset = +struct[10] * 60 + (+struct[11]);
        if (struct[9] === '+') {
            minutesOffset = 0 - minutesOffset;
        }
    }
	if (struct[1]) {
		if (struct[4] || struct[5] || struct[6]) {
			if (struct[8] || struct[9]) {
	        	timestamp = Date.UTC(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5] + minutesOffset, +struct[6], 0);
			} else {
	        	timestamp = new Date(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5], +struct[6], 0).getTime();
			}
		} else if (struct[8] || struct[9]) {
        	timestamp = Date.UTC(+struct[1], +struct[2] - 1, +struct[3], 0, minutesOffset, 0, 0);
		} else {
        	timestamp = new Date(+struct[1], +struct[2] - 1, +struct[3]).getTime();
		}
	} else if (struct[4] || struct[5] || struct[6]) {
		if (struct[8] || struct[9]) {
        	timestamp = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5] + minutesOffset, +struct[6], 0);
		} else {
        	timestamp = new Date(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5], +struct[6], 0).getTime();
		}
	}
	return timestamp;
}

function abbreviated(date, node, now) {
	var locale = '';
	if (node.is(".datetime-locale, .time-locale")) {
		var minutes;
		if (date.getMinutes() >= 10) {
			minutes = ':' + date.getMinutes();
		} else if (date.getHours() > 0 || date.getMinutes() > 0) {
			minutes = ':0' + date.getMinutes();
		}
		if (date.getHours() > 12) {
			locale = (date.getHours() - 12) + minutes + " pm";
		} else if (date.getHours() == 12) {
			locale = "12" + minutes + " pm";
		} else if (date.getHours() > 0) {
			locale = date.getHours() + minutes + " am";
		} else if (minutes) {
			locale = "12" + minutes + " am";
		}
	}
	if (node.is(".datetime-locale, .date-locale")) {
		if (locale == '' || date.getDate() != now.getDate() || date.getMonth() != now.getMonth() || date.getFullYear() != now.getFullYear()) {
			if (locale) {
				locale += ", ";
			}
			var month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][date.getMonth()];
			locale += date.getDate() + ' ' + month;
			if (date.getFullYear() != now.getFullYear()) {
				locale += ' ' + date.getFullYear();
			}
		}
	}
	return locale;
}

})(window.jQuery);

