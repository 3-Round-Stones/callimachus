// load-editor.js
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

(function($) {

var waiting = null;
var calli = window.calli || (window.calli={});

calli.initEditor = function(event, text) {
    if (!waiting) {
        waiting = calli.wait();
    }
    event = calli.fixEvent(event);
    var iframe = $(event.target);
    var editor = iframe[0].contentWindow;
    var form = iframe.closest('form')[0];

    bindEditorEvents(editor);
    bindFormEvents(form, editor, false);
    setText(form, text, editor);
};

calli.loadEditor = function(event, url) {
    if (!waiting) {
        waiting = calli.wait();
    }
    event = calli.fixEvent(event);
    var iframe = $(event.target);
    var editor = iframe[0].contentWindow;
    var form = iframe.closest('form')[0];

    bindEditorEvents(editor, iframe);
    bindFormEvents(form, editor, true);
    loadText(form, url, editor);
};

$(window).bind('message', function(event) {
    var msg = event.originalEvent.data;
    if (msg.indexOf('OK\n\nGET text\nCallbackID: ') === 0) {
        var start = 'OK\n\nGET text\nCallbackID: '.length;
        var end = msg.indexOf('\n\n', start);
        var idx = msg.substring(start, end);
        var text = msg.substring(end + 2);
        var callback = sourceCallbacks[idx];
        if (callback) {
            delete sourceCallbacks[idx];
            callback(text);
        }
    } else if (msg.indexOf('OK\n\nPUT text') === 0) {
        if (waiting) {
            waiting.over();
            waiting = null;
        }
    }
});
var sourceCallbacks = [];
calli.readEditorText = function(editorWindow, callback) {
    var idx = sourceCallbacks.length;
    sourceCallbacks[idx] = callback;
    editorWindow.postMessage('GET text\nCallbackID: ' + idx, '*');
};

// bindEditorEvents
function bindEditorEvents(editor) {
    $(document).bind('calliOpenDialog', function(event) {
        if (editor && !event.isDefaultPrevented()) {
            editor.postMessage('PUT disabled\n\ntrue', '*');
        }
    });
    $(document).bind('calliCloseDialog', function(event) {
        if (!event.isDefaultPrevented()) {
            editor.postMessage('PUT disabled\n\nfalse', '*');
        }
    });
    $(window).bind('message', function(event) {
        if (event.originalEvent.source == editor) {
            var msg = event.originalEvent.data;
            if (msg.indexOf('PUT text\n\n') === 0) {
                var text = msg.substring('PUT text\n\n'.length);
                var se = $.Event("calliSave", {text: text});
                $(editor.frameElement).trigger(se);
            } else if (msg.indexOf('Error\n\n') === 0) {
                calli.error(msg.substring(msg.indexOf('\n\n', msg.indexOf('\n\n') + 2)));
            }
        }
    });
    $(window).bind('hashchange', onhashchange(editor));
}

function onhashchange(editor) {
    return function() {
        var hash = window.location.hash;
        if (hash && hash.length > 1 && hash.match(/^\d+(\.\d+)$/)) {
            editor.postMessage('PUT line.column\n\n' + hash.substring(1), '*');
        }  
    };
}

// bindFormEvents
var boundForms = {};
function bindFormEvents(form, editor, idempotent) {
    if (boundForms[form])
        return false;
    boundForms[form] = true;
    $(document).ajaxSend(function(event, XMLHttpRequest, ajaxOptions) {
        $(form).css('background-color', 'lightyellow');
    });
    $(document).ajaxSuccess(function(event, XMLHttpRequest, ajaxOptions) {
        $(form).css('background-color', 'inherit');
    });
    $(document).ajaxError(function(event, XMLHttpRequest, ajaxOptions) {
        $(form).css('background-color', '#FF9999');
        setTimeout(function() {
            $(form).css('background-color', 'inherit');
        }, 1000);
    });
    if (idempotent) {
        $(form).bind('calliSave', function(event) {
            saveFile(form, event.text);
        });
    }
    $(form).submit(function(event, onlyHandlers) {
        if (!onlyHandlers) {
            event.preventDefault();
            event.stopImmediatePropagation();
            $(this).triggerHandler(event.type, true);
        } else {
            setTimeout(function(){
                var resource = $(form).attr('about') || $(form).attr('resource');
                if ((idempotent || resource) && !event.isDefaultPrevented()) {
                    calli.readEditorText(editor, function(text) {
                        saveFile(form, text, function(xhr, cause) {
                            var event = $.Event("calliRedirect");
                            event.cause = cause;
                            event.resource = cause.resource;
                            var redirect = xhr.getResponseHeader('Location');
                            var url = calli.getFormAction(form);
                            if (url.indexOf('?') > 0) {
                                url = url.substring(0, url.indexOf('?'));
                            }
                            if (redirect) {
                                event.location = redirect + '?view';
                            } else if (resource) {
                                event.location = resource + '?view';
                            } else {
                                event.location = url + '?view';
                            }
                            $(form).trigger(event);
                            if (!event.isDefaultPrevented()) {
                                if (window.parent != window && parent.postMessage) {
                                    parent.postMessage('PUT src\n\n' + event.location, '*');
                                }
                                if (event.location.indexOf(url) === 0) {
                                    window.location.replace(event.location);
                                } else {
                                    window.location.href = event.location;
                                }
                            }
                        });
                    });
                }
            }, 0);
        }
    });
}

// saveFile
var saving = {};
function saveFile(form, text, callback) {
    var se = $.Event("calliSubmit");
    se.resource = $(form).attr('about') || $(form).attr('resource') || calli.getFormAction(form).replace(/\?.*/,'');
    se.payload = text;
    $(form).trigger(se);
    if (!se.isDefaultPrevented()) {
        if (saving[form]) return false;
        saving[form] = true;
        var method = form.getAttribute('method');
        var url = calli.getFormAction(form);
        $.ajax({
            type: method,
            url: url,
            contentType: form.getAttribute("enctype"),
            data: se.payload,
            dataType: "text",
            xhrFields: calli.withCredentials,
            beforeSend: function(xhr) {
                if (calli.lastModified(url) && method == 'PUT') {
                    xhr.setRequestHeader('If-Unmodified-Since', calli.lastModified(url));
                }
            },
            complete: function(xhr) {
                saving = false;
                if (xhr.status < 300 || xhr.status == 1223) {
                    if (xhr.status == 204 || xhr.status == 1223) {
                        calli.lastModified(url, new Date().toUTCString());
                    }
                    if (typeof callback == 'function') {
                        callback(xhr, se);
                    }
                }
            }
        });
    }
}

// setText
function setText(form, text, editor) {
    if (window.location.hash.indexOf('#!') === 0) {
        var url = resolve(window.location.hash.substring(2));
        jQuery.ajax({type: 'GET', url: url, xhrFields: calli.withCredentials, complete: function(xhr) {
            if (xhr.status == 200 || xhr.status == 304) {
                var text = xhr.responseText;
                editor.postMessage('PUT text\nIf-None-Match: *' +
                    '\nContent-Location: ' + url +
                    '\nContent-Type: '+ form.getAttribute("enctype") +
                    '\n\n' + text, '*');
            }
        }});
    } else if (text) {
        editor.postMessage('PUT text\nIf-None-Match: *' +
            '\nContent-Location: ' + window.location.href +
            '\nContent-Type: '+ form.getAttribute("enctype") +
            '\n\n' + text, '*');
    } else {
        editor.postMessage('PUT text\nIf-None-Match: *' +
            '\nContent-Location: ' + window.location.href +
            '\nContent-Type: '+ form.getAttribute("enctype") +
            '\n\n', '*');
    }
}

// loadText
function loadText(form, url, editor) {
    url = resolve(url);
    $.ajax({type: 'GET', dataType: "text", url: url, xhrFields: calli.withCredentials, complete: function(xhr) {
        if (xhr.status == 200 || xhr.status == 304) {
            calli.lastModified(url, xhr.getResponseHeader('Last-Modified'));
            editor.postMessage('PUT text\nContent-Location: '+ url +
                '\nContent-Type: '+ form.getAttribute("enctype") +
                '\n\n' + xhr.responseText, '*');
            onhashchange(editor)();
        }
    }});
}

function resolve(url) {
    if (document.baseURIObject && document.baseURIObject.resolve) {
        return document.baseURIObject.resolve(url);
    } else if (url.indexOf('http:') !== 0 && url.indexOf('https:') !== 0) {
        var a = document.createElement('a');
        a.setAttribute('href', url);
        if (a.href) {
            return a.href;
        }
    }
    return url;
}

})(jQuery);




