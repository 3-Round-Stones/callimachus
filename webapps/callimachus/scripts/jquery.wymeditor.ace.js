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
		if (m)
			return m.length - 1;
	}
	return -1;
}

function getSelectedNodeIndex(selected, body) {
	return jQuery.inArray(selected, $(body).find('*').get());
}

function focusOnNodeIndex(wym, index) {
	var node = $(wym._doc.body).find('*')[index];
	if (node) {
		wym.setFocusToNode(node);
	}
}

function gotoLineOfNodeIndex(ed, html, index) {
	if (index >= 0) {
		var m = html.match(new RegExp('[\\s\\S]*<body[^>]*>[^<]*(<([^<]|</)*){' + index + '}'));
		if (m) {
			var line = m[0].match(/\n/g).length;
			ed.gotoLine(line);
		}
	}
}

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
				var ed = editor[0].contentWindow.editor;
				var value = ed.getSession().getValue();
				if (value != html) {
					var row = ed.getSelectionRange().start.row;
					ed.getSession().setValue(html);
					ed.gotoLine(row + 1);
				}
			}
			if (m) {
				page = m;
				html = page[2];
			}
			var index = getSelectedNodeIndex(this.selected(), this._doc.body);
			var ret = innerHtml.call(this, html);
			focusOnNodeIndex(this, index);
			return ret;
		} else {
			var editor = $('#ace-iframe');
			if (editor.is(':visible')) {
				var value = editor[0].contentWindow.editor.getSession().getValue();
				if (value) {
					page = value.match(/([\s\S]*<body[^>]*>)([\s\S]*)(<\/body>\s*<\/html>\s*)/);
					return page[2];
				}
			}
			return innerHtml.call(this, html);
		}
	};

	var _xhtml = wym.xhtml;
	wym.xhtml = function() {
		var body = _xhtml.call(this);
		return page[1] + body + page[3];
	}

	var _update = wym.update;
	wym.update = function() {
		var editor = $('#ace-iframe');
		if (editor.is(':visible')) {
			var html = page[1] + wym.parser.parse($(wym._doc.body).html()) + page[3];
			var ed = editor[0].contentWindow.editor;
			var value = ed.getSession().getValue();
			if (value != html) {
				var row = ed.getSelectionRange().start.row;
				ed.getSession().setValue(html);
				ed.gotoLine(row + 1);
			}
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
			if (editor.is(':visible')) {
				var ed = editor[0].contentWindow.editor;
				var html = ed.getSession().getValue();
				var start = ed.getSelectionRange().start;
				var index = getNodeIndex(html, start.row, start.column);
				wym.html(html);
				focusOnNodeIndex(this, index);
			}
			switch (cmd) {
			case 'ToggleHtml':
				if (editor.is(':visible')) {
					$('#wym-iframe').css('width', editor.css('width'));
					$('#wym-iframe').css('height', editor.css('height'));
					editor.hide();
					$('#wym-iframe').show();
				} else {
					editor.css('width', $('#wym-iframe').css('width'));
					editor.css('height', $('#wym-iframe').css('height'));
					$('#wym-iframe').hide();
					editor.show();
				}
				wym.update();
			break;
			default:
				var ret = _exec.call(wym, cmd);
				wym.update();
				if (window.dialog) {
					window.dialog.bind("dialogbeforeclose", function(event, ui) {
						wym.update();
						return true;
					});
				}
				return ret;
			}
		};

		var _setFocusToNode = wym.setFocusToNode;
		wym.setFocusToNode = function(node, toStart) {
			var editor = $('#ace-iframe');
			if (editor.is(':visible')) {
				var html = wym.xhtml();
				var ed = editor[0].contentWindow.editor;
				var value = ed.getSession().getValue();
				if (value != html) {
					ed.getSession().setValue(html);
				}
				var index = getSelectedNodeIndex(node, this._doc.body);
				gotoLineOfNodeIndex(ed, html, index);
			}
			return _setFocusToNode.call(this, node, toStart);
		};

		if (_postInit) {
			_postInit.call(this, wym);
		}
	};
	return _wymeditor.call(this, options);
}

})(jQuery);
