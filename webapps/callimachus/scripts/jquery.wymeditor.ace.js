// jquery.wymeditor.ace.js

/*
 * Replaces the toggle textarea with a ace HTML edit iframe.
 * Additional iframe must have an id of 'ace-iframe'
 */
(function($){

function getNodeIndex(html, row, column) {
	var regex = '(:?[^\n]*\n){0,' + row + '}';
	if (column == 0) {
		regex = regex + '(:?[^<]|<[^/])*';
	} else {
		regex = regex + '.{0,' + column + '}';
	}
	var m = html.match(new RegExp(regex));
	if (m) {
		var body = m[0].replace(/[\s\S]*<body[^>]*>/, '');
		m = body.match(/<(:?[^<]|<\/)*/g);
		if (m) {
			var index = m.length - 1;
			return index;
		}
	}
	return -1;
}

function getSelectedNodeIndex(selected, body) {
	var index = jQuery.inArray(selected, $(body).find('*').get());
	return index;
}

function getNodeFromIndex(wym, index) {
	return $(wym._doc.body).find('*')[index];
}

function focusOnNodeIndex(wym, index) {
	var node = getNodeFromIndex(wym, index);
	if (node) {
		wym.setFocusToNode(node);
	}
}

var ace_port = null;
var outgoing = [];
var incoming = {};
function initAcePort(event) {
	if (!ace_port && event.originalEvent.data == "calliEditorPort" && $('#ace-iframe')[0].contentWindow == event.originalEvent.source) {
		ace_port = event.originalEvent.ports[0];
		if (outgoing.length) {
			$(outgoing).each(function() {
				ace_port.postMessage(this);
			});
		}
		ace_port.onmessage = function(event) {
			var msg = event.data;
			var header = msg;
			var body = null;
			if (msg.indexOf('\n') > 0) {
				header = msg.substring(0, msg.indexOf('\n'));
				body = msg.substring(msg.indexOf('\n') + 1);
			}
			if (incoming[header]) {
				$(incoming[header]).each(function() {
					this(body);
				});
				delete incoming[header];
			}
		};
		$(window).unbind('message', initAcePort);
	}
}
$(window).bind('message', initAcePort);

function postCallback(header, body, callback) {
	if (callback) {
		if (incoming[header]) {
			incoming[header].push(callback);
		} else {
			incoming[header] = [callback];
		}
	}
	var msg = header;
	if (body) {
		msg = msg + '\n' + body;
	}
	if (ace_port) {
		ace_port.postMessage(msg);
	} else {
		outgoing.push(msg);
	}
}

function getValue(callback) {
	postCallback('GET text', null, callback);
}

function setValue(value) {
	postCallback('POST text', value);
}

function gotoLine(line) {
	postCallback('POST line.column', '' + line + '.0');
}

function getLineColumn(callback) {
	postCallback('GET line.column', null, function(linecolumn) {
		var ar = linecolumn.split('.');
		callback({line: parseInt(ar[0]), column: parseInt(ar[1])});
	});
}

function insert(html) {
	postCallback('POST insert', html);
}

function gotoLineOfNodeIndex(html, index) {
	if (index >= 0) {
		var m = html.match(new RegExp('[\\s\\S]*<body[^>]*>(([^<]|</)*<[^<>/][^<>]*>){' + (index + 1) + '}'));
		if (m) {
			var line = m[0].match(/\n/g).length + 1;
			gotoLine(line);
		}
	}
}

WYMeditor.XhtmlSaxListener.prototype.afterParsing = function(xhtml)
{
  xhtml = this.replaceNamedEntities(xhtml);
  xhtml = this.joinRepeatedEntities(xhtml);
  //xhtml = this.removeEmptyTags(xhtml);
  xhtml = this.removeBrInPre(xhtml);
  return xhtml;
};

var _initHtml = WYMeditor.editor.prototype.initHtml;
WYMeditor.editor.prototype.initHtml = function(html) {
	var wym = this;
	var page = [null, '', html, ''];
	var m = html.match(/([\s\S]*<body[^>]*>)([\s\S]*)(<\/body>\s*<\/html>\s*)/);
	if (m) {
		page = m;
		html = page[2];
	}

	var innerHtml = wym.html;
	wym.html = function(html) {
		var editor = $('#ace-iframe');
		if(typeof html === 'string') {
			var m = html.match(/([\s\S]*<body[^>]*>)([\s\S]*)(<\/body>\s*<\/html>\s*)/);
			if (editor.is(':visible') && m) {
				setValue(html);
			}
			if (m) {
				page = m;
				html = page[2];
			}
			this.selected(function(selected) {
				var index = getSelectedNodeIndex(selected, wym._doc.body);
				innerHtml.call(wym, html);
				focusOnNodeIndex(wym, index);
			});
		} else {
			var editor = $('#ace-iframe');
			if (typeof html == 'function') {
				if (editor.is(':visible')) {
					getValue(function(value) {
						if (value) {
							page = value.match(/([\s\S]*<body[^>]*>)([\s\S]*)(<\/body>\s*<\/html>\s*)/);
							html(page[2]);
						}
					});
				} else {
					return innerHtml.call(this, html);
				}
			} else {
				return innerHtml.call(this);
			}
		}
	};

	var _xhtml = wym.xhtml;
	wym.xhtml = function(callback) {
		var editor = $('#ace-iframe');
		if (callback && editor.is(':visible')) {
			getValue(callback);
		} else {
			if (!callback) {
				callback = function(ret) {
					return ret;
				};
			}
			var body = _xhtml.call(this, function(body) {
				callback(page[1] + body + page[3]);
			});
			return page[1] + body + page[3];
		}
	}

	var _update = wym.update;
	wym.update = function() {
		var editor = $('#ace-iframe');
		if (editor.is(':visible')) {
			var html = page[1] + wym.parser.parse($(wym._doc.body).html()) + page[3];
			setValue(html);
		}
		return _update.call(this);
	};

	return _initHtml.call(this, html);
}

var _wymeditor = jQuery.fn.wymeditor;
jQuery.fn.wymeditor = function(options) {
	var _postInit = options.postInit;
	options.postInit = function(wym) {

		var _exec = wym.exec;
		wym.exec = function(cmd) {
			var editor = $('#ace-iframe');
			function switchBlock() {
				switch (cmd) {
				case 'ToggleHtml':
					if (editor.is(':visible')) {
						$('#wym-iframe').css('width', editor.css('width'));
						$('#wym-iframe').css('height', editor.css('height'));
						editor.hide();
						$('#wym-iframe').show();
					} else {
						wym.xhtml(function(html){
							setValue(html);
							wym.selected(function(selected) {
								var index = getSelectedNodeIndex(selected, wym._doc.body);
								gotoLineOfNodeIndex(html, index);
								editor.css('width', $('#wym-iframe').css('width'));
								editor.css('height', $('#wym-iframe').css('height'));
								$('#wym-iframe').hide();
								editor.show();
							});
						});
					}
				break;
				default:
					_exec.call(wym, cmd);
					wym.update();
					if (window.dialog) {
						window.dialog.bind("dialogbeforeclose", function(event, ui) {
							wym.update();
						});
					}
				}
			};
			if (editor.is(':visible')) {
				getValue(function(html) {
					getLineColumn(function(start) {
						var index = getNodeIndex(html, start.line - 1, start.column);
						wym.html(html);
						focusOnNodeIndex(wym, index);
						switchBlock();
					});
				});
			} else {
				switchBlock();
			}
		};

		var _setFocusToNode = wym.setFocusToNode;
		wym.setFocusToNode = function(node, toStart) {
			var editor = $('#ace-iframe');
			if (editor.is(':visible')) {
				wym.xhtml(function(html){
					setValue(html);
					var index = getSelectedNodeIndex(node, this._doc.body);
					gotoLineOfNodeIndex(html, index);
				});
			}
			try {
				return _setFocusToNode.call(this, node, toStart);
			} catch(e) {
				// hidden
				return null;
			}
		};

		var _selected = wym.selected;
		wym.selected = function(callback) {
			if (!callback) {
				callback = function(ret) {
					return ret;
				};
			}
			var editor = $('#ace-iframe');
			if (editor.is(':visible')) {
				try {
					var ret = _selected.call(this);
					if (ret)
						return callback(ret);
				} catch (e) {
					// no selection
				}
				getValue(function(html) {
					getLineColumn(function(start) {
						var index = getNodeIndex(html, start.line - 1, start.column);
						callback(getNodeFromIndex(wym, index));
					});
				});
				return ret;
			}
			return callback(_selected.call(this));
		};

		var _insert = wym.insert;
		wym.insert = function(code) {
			var editor = $('#ace-iframe');
			if (editor.is(':visible')) {
				insert(code);
				getValue(function(html) {
					getLineColumn(function(start) {
						var index = getNodeIndex(html, start.line - 1, start.column);
						wym.html(html);
						focusOnNodeIndex(wym, index);
					});
				});
				return true;
			}
			return _insert.call(this, code);
		};

		if (_postInit) {
			_postInit.call(this, wym);
		}
	};
	return _wymeditor.call(this, options);
}

})(jQuery);
