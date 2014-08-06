// text-editor.js
/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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

    editor.commands.addCommand({
        name: 'save',
        bindKey: {
            win: 'Ctrl-S',
            mac: 'Command-S',
            sender: 'editor'
        },
        exec: function(env, args, request) {
            saved = editor.getSession().getValue();
            parent.postMessage('PUT text\n\n' + saved, '*');
        }
    });

    // messaging
    function handleMessage(header, body) {
        if (header.match(/^PUT text(\n|$)/)) {
            if (header.match(/\nIf-None-Match: */)) {
                if (!editor.getSession().getValue()) {
                    editor.insert(body);
                    saved = editor.getSession().getValue();
                }
                return true;
            } else {
                var row = editor.getSelectionRange().start.row;
                var col = editor.getSelectionRange().start.column;
                if (body != editor.getSession().getValue()) {
                    editor.getSession().setValue(body);
                    saved = editor.getSession().getValue();
                }
                if (row != editor.getSelectionRange().start.row) {
                    editor.gotoLine(row + 1, col);
                }
                return true;
            }
        } else if (header.match(/^GET text(\n|$)/)) {
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
    }

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
                } else if (response && typeof response != 'boolean' || typeof response == 'string') {
                    parent.postMessage('OK\n\n' + header + '\n\n' + response, '*');
                } else {
                    parent.postMessage('OK\n\n' + header, '*');
                }
            } catch (e) {
                window.console && window.console.error(e);
                parent.postMessage('Error\n\n' + header + '\n\n' + e, '*');
            }
        }
    });
});
