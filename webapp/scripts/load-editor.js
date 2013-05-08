// load-editor.js

(function($) {

var calli = window.calli || (window.calli={});

calli.initEditor = function(event, text) {
    event = calli.fixEvent(event);
    var iframe = $(event.target);
    var editor = iframe[0].contentWindow;
    var form = iframe.closest('form')[0];

    bindEditorEvents(editor);
    bindFormEvents(form, editor, false);
    setText(text, editor);
};

calli.loadEditor = function(event, url) {
    event = calli.fixEvent(event);
    var iframe = $(event.target);
    var editor = iframe[0].contentWindow;
    var form = iframe.closest('form')[0];

    bindEditorEvents(editor, iframe);
    bindFormEvents(form, editor, true);
    loadText(url, editor);
};

$(window).bind('message', function(event) {
    var msg = event.originalEvent.data;
    if (msg.indexOf('OK\n\nGET text\n\n') == 0) {
        var text = msg.substring('OK\n\nGET text\n\n'.length);
        var callbacks = sourceCallbacks[event.originalEvent.source];
        if (callbacks) {
            sourceCallbacks[event.originalEvent.source] = [];
            for (var i=0; i<callbacks.length; i++) {
                callbacks[i](text);
            }
        }
    }
});
var sourceCallbacks = {};
calli.readEditorText = function(editorWindow, callback) {
    if (!sourceCallbacks[editorWindow]) {
        sourceCallbacks[editorWindow] = [];
    }
    sourceCallbacks[editorWindow].push(callback);
    if (sourceCallbacks[editorWindow].length == 1) {
        editorWindow.postMessage('GET text', '*');
    }
};

// bindEditorEvents
var boundEditors = {};
function bindEditorEvents(editor) {
    if (boundEditors[editor])
        return false;
    boundEditors[editor] = true;
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
            if (msg.indexOf('PUT text\n\n') == 0) {
                var text = msg.substring('PUT text\n\n'.length);
                var se = $.Event("calliSave", {text: text});
                $(editor.frameElement).trigger(se);
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
};

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
    $(form).submit(function(event) {
        event.preventDefault();
        var resource = $(form).attr('about') || $(form).attr('resource');
        if (idempotent || resource) {
            calli.readEditorText(editor, function(text) {
                saveFile(form, text, function(xhr, cause) {
                    var event = $.Event("calliRedirect");
                    var redirect = xhr.getResponseHeader('Location');
                    var url = calli.getFormAction(form);
                    if (url.indexOf('?') > 0) {
                        url = url.substring(0, url.indexOf('?'));
                    }
                    if (redirect) {
                        event.resource = redirect;
                    } else if (resource) {
                        event.resource = resource;
                    } else {
                        event.resource = url;
                    }
                    event.cause = cause;
                    event.location = event.resource + '?view';
                    $(form).trigger(event);
                    if (!event.isDefaultPrevented()) {
                        if (window.parent != window && parent.postMessage) {
                            parent.postMessage('PUT src\n\n' + event.location, '*');
                        }
                        if (event.location.indexOf(url) == 0) {
                            window.location.replace(event.location);
                        } else {
                            window.location.href = event.location;
                        }
                    }
                });
            });
        }
        return false;
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
            beforeSend: function(xhr) {
                if (calli.etag(url) && method == 'PUT') {
                    xhr.setRequestHeader('If-Match', calli.etag(url));
                }
                calli.withCredentials(xhr);
            },
            complete: function(xhr) {
                saving = false;
                if (xhr.status < 300 || xhr.status == 1223) {
                    if (xhr.status == 204 || xhr.status == 1223) {
                        calli.etag(url, xhr.getResponseHeader('ETag'));
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
function setText(text, editor) {
    if (window.location.hash.indexOf('#!') == 0) {
        var url = resolve(window.location.hash.substring(2));
        jQuery.ajax({type: 'GET', url: url, beforeSend: calli.withCredentials, complete: function(xhr) {
            if (xhr.status == 200 || xhr.status == 304) {
                var text = xhr.responseText;
                editor.postMessage('PUT text\nIf-None-Match: *\nContent-Location: '
                    + url + '\n\n' + text, '*');
            }
        }});
    } else if (text) {
        editor.postMessage('PUT text\nIf-None-Match: *\nContent-Location: '
                    + window.location.href + '\n\n' + text, '*');
    } else {
        editor.postMessage('PUT text\nIf-None-Match: *\nContent-Location: '
                    + window.location.href + '\n\n', '*');
    }
}

// loadText
function loadText(url, editor) {
    url = resolve(url);
    $.ajax({type: 'GET', dataType: "text", url: url, beforeSend: calli.withCredentials, complete: function(xhr) {
        if (xhr.status == 200 || xhr.status == 304) {
            calli.etag(url, xhr.getResponseHeader('ETag'));
            editor.postMessage('PUT text\nContent-Location: '+ url +'\n\n' + xhr.responseText, '*');
            onhashchange(editor)();
        }
    }});
}

function resolve(url) {
    if (document.baseURIObject && document.baseURIObject.resolve) {
        return document.baseURIObject.resolve(url);
    } else if (url.indexOf('http:') != 0 && url.indexOf('https:') != 0) {
        var a = document.createElement('a');
        a.setAttribute('href', url);
        if (a.href) {
            return a.href;
        }
    }
    return url;
}

})(jQuery);




