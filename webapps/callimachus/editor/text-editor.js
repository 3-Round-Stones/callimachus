// text-editor.js

jQuery(function($) {
    var editor = ace.edit("editor");

    $('#editor').one('mouseenter', function() {
        try {
            editor.focus();
        } catch (e) {
            // ignore if disabled
        }
    });

    // loading
    function onhashchange() {
        if (location.hash && location.hash.length > 1) {
            var mode = location.hash.substring(1);
            if (mode) {
                var Mode = require("ace/mode/" + mode).Mode;
                if (Mode) {
                    editor.getSession().setMode(new Mode());
                }
            }
        }
    }
    $(window).bind('hashchange', onhashchange);
    onhashchange();

    var onresize = function() {
        setTimeout(function() {
            var pane = $('.ace_scroller')[0];
            if (pane.scrollWidth > pane.clientWidth && window.parent != window) {
                var width = pane.scrollWidth - pane.clientWidth + $(pane).outerWidth(true);
                width += $('.ace_gutter').outerWidth(true);
                width += 32; // scrollbar width
                $(pane).parents().each(function() {
                    width += $(this).outerWidth(true) - $(this).width();
                });
                parent.postMessage('PUT width\n\n' + width, '*');
            }
        }, 100);
    };
    $(window).bind('resize', onresize);

    var saved = null;
    window.onbeforeunload = function(event){
        event = event || window.event;
        if (editor && editor.getSession().getValue() != saved) {
            if (event) {
                event.returnValue = 'There are unsaved changes';
            }
            return 'There are unsaved changes';
        }
    };

    require('pilot/canon').addCommand({
        name: 'save',
        bindKey: {
            win: 'Ctrl-S',
            mac: 'Command-S',
            sender: 'editor'
        },
        exec: function(env, args, request) {
            saved = editor.getSession().getValue();
            parent.postMessage('PUT src\n\n' + saved, '*');
        }
    });

    // messaging
    function handleMessage(header, body) {
        if (header == 'PUT src\nIf-None-Match: *' && body) {
            if (!editor.getSession().getValue()) {
                editor.insert(body);
                saved = editor.getSession().getValue();
                onresize();
            }
            return true;
        } else if (header == 'PUT src' && body) {
            var row = editor.getSelectionRange().start.row;
            var col = editor.getSelectionRange().start.column;
            if (body != editor.getSession().getValue()) {
                editor.getSession().setValue(body);
                saved = editor.getSession().getValue();
                onresize();
            }
            if (row != editor.getSelectionRange().start.row) {
                editor.gotoLine(row + 1, col);
            }
            return true;
        } else if (header == 'GET src') {
            saved = editor.getSession().getValue();
            return saved;
        } else if (header == 'PUT line.column' && body) {
            var line = body;
            var column = null;
            if (line.indexOf('.')) {
                column = line.substring(line.indexOf('.') + 1);
                line = line.substring(0, line.indexOf('.'));
            }
            if (line && line != editor.getSelectionRange().start.row + 1) {
                editor.gotoLine(line, column);
            } else if (line && column && column != editor.getSelectionRange().start.column) {
                editor.gotoLine(line, column);
            }
            return true;
        } else if (header == 'GET line.column') {
            var start = editor.getSelectionRange().start;
            return '' + (1 + start.row) + '.' + start.column;
        } else if (header == 'PUT disabled' && body) {
            editor.textInput.getElement().disabled = body === 'true';
            return true;
        }
        return false; // Not Found
    };

    $(window).bind('message', function(event) {
        if (event.originalEvent.source == parent) {
            var msg = event.originalEvent.data;
            var header = msg;
            var body = null;
            if (msg.indexOf('\n\n') > 0) {
                header = msg.substring(0, msg.indexOf('\n\n'));
                body = msg.substring(msg.indexOf('\n\n') + 2);
            }
            try {
                var response = handleMessage(header, body);
                if (!response && typeof response == 'boolean') {
                    parent.postMessage('Not Found\n\n' + header, '*');
                } else if (response && typeof response != 'boolean') {
                    parent.postMessage('OK\n\n' + header + '\n\n' + response, '*');
                } else {
                    parent.postMessage('OK\n\n' + header, '*');
                }
            } catch (e) {
                calli.error(e);
            }
        }
    });
    if (window.parent != window) {
        parent.postMessage('CONNECT calliEditorLoaded', '*');
    }
});
