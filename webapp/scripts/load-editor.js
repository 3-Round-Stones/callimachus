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

var calli = window.calli || (window.calli={});

calli.initEditor = function(event, text) {
    event = calli.fixEvent(event);
    var iframe = $(event.target);
    var editor = iframe[0].contentWindow;
    var form = iframe.closest('form')[0];

    bindEditorEvents(editor);
    bindFormEvents(form, editor, false);
    return setText(form, text, editor);
};

calli.loadEditor = function(event, url) {
    event = calli.fixEvent(event);
    var iframe = $(event.target);
    var editor = iframe[0].contentWindow;
    var form = iframe.closest('form')[0];

    bindEditorEvents(editor, iframe);
    bindFormEvents(form, editor, true);
    return loadText(form, url, editor);
};

calli.submitEditor = function(event, local) {
    event.preventDefault();
    var form = $(calli.fixEvent(event).target).closest('form');
    var btn = form.find('button[type="submit"]');
    btn.button('loading');
    return calli.resolve(form).then(function(form){
        var editor = form.find('iframe')[0].contentWindow;
        return calli.readEditorText(editor);
    }).then(function(text) {
        var action = calli.getFormAction(form[0]);
        if (local) {
            var name = encodeURI(local).replace(/%25(\w\w)/g, '%$1').replace(/%20/g, '-');
            var resource = action.replace(/\/?([\?\#].*)?$/,'/') + name;
            var amp = action.indexOf('?') < 0 ? '?' : '&';
            var url = action + amp + "resource=" + encodeURIComponent(resource);
            return calli.postText(url, text, form.attr('enctype'));
        } else {
            return calli.putText(action, text, form.attr('enctype')).then(function(){
                return action.replace(/\?.*/,'');
            });
        }
    }).then(function(redirect){
        if (redirect) {
            if (window.parent != window && parent.postMessage) {
                parent.postMessage('POST resource\n\n' + redirect, '*');
            }
            window.location.href = redirect + '?view';
        } else {
            btn.button('reset');
        }
    }, function(error) {
        btn.button('reset');
        return calli.error(error);
    });
};

window.calli.submitEditorAs = function(event, local, create, folder) {
    event.preventDefault();
    var button = calli.fixEvent(event).target;
    var form = $(button).closest('form');
    var btn = $(button).filter('button');
    btn.button('loading');
    return calli.resolve(form).then(function(form){
        var editor = form.find('iframe')[0].contentWindow;
        return calli.readEditorText(editor);
    }).then(function(text) {
        return calli.promptForNewResource(folder, local).then(function(two){
            if (!two) return undefined;
            var action = two[0] + '?create=' + encodeURIComponent(create);
            var iri = two[0].replace(/\/?$/, '/') + two[1].replace(/%20/g, '+');
            var url = action + "&resource=" + encodeURIComponent(iri);
            return calli.postText(url, text, form.attr('enctype'));
        });
    }).then(function(redirect){
        return redirect && redirect + '?view';
    }).then(function(redirect){
        if (redirect) {
            window.location.href = redirect;
        } else {
            btn.button('reset');
        }
    }, function(error){
        btn.button('reset');
        return calli.error(error);
    });
};

var waiting = [];
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
        if (waiting.length) {
            waiting.shift()();
        }
    }
});
var sourceCallbacks = [];
calli.readEditorText = function(editorWindow, callback) {
    return calli.promise(function(callback){
        var idx = sourceCallbacks.length;
        sourceCallbacks[idx] = callback;
        editorWindow.postMessage('GET text\nCallbackID: ' + idx, '*');
    }).then(callback);
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
                calli.error(msg.substring(msg.indexOf('\n\n', msg.indexOf('\n\n') + 2) + 2));
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
    if (idempotent) {
        $(form).bind('calliSave', function(event) {
            saveFile(form, event.text);
        });
    }
    $(form).submit(function(event, onlyHandlers) {
        if (!onlyHandlers && !event.isDefaultPrevented()) {
            event.preventDefault();
            event.stopImmediatePropagation();
            $(this).triggerHandler(event.type, true);
        } else if (onlyHandlers) {
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
        calli.resolve($.ajax({
            type: method,
            url: url,
            contentType: form.getAttribute("enctype"),
            data: se.payload,
            dataType: "text",
            xhrFields: {withCredentials: true},
            beforeSend: function(xhr) {
                if (calli.lastModified(url) && method == 'PUT') {
                    xhr.setRequestHeader('If-Unmodified-Since', calli.lastModified(url));
                }
            },
            error: calli.error,
            complete: function(xhr) {
                saving = false;
                if (xhr.status < 300 || xhr.status == 1223) {
                    if (xhr.status == 204 || xhr.status == 1223) {
                        var now = xhr.getResponseHeader('Last-Modified');
                        var location = xhr.getResponseHeader('Location');
                        calli.lastModified(url, now);
                        if (location) {
                            calli.lastModified(location, now);
                        }
                    }
                    if (typeof callback == 'function') {
                        callback(xhr, se);
                    }
                }
            }
        }));
    }
}

// setText
function setText(form, text, editor) {
    if (window.location.hash.indexOf('!') > 0) {
        var url = window.location.hash.substring(window.location.hash.indexOf('!') + 1);
        return calli.getText(url).then(function(text){
            editor.postMessage('PUT text\nIf-None-Match: *' +
                '\nContent-Location: ' + url +
                '\nContent-Type: '+ form.getAttribute("enctype") +
                '\n\n' + text, '*');
            return calli.promise(function(callback){
                waiting.push(callback);
            });
        }).then(undefined, calli.error);
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
    return calli.promise(function(callback){
        waiting.push(callback);
    });
}

// loadText
function loadText(form, url, editor) {
    calli.getText(url).then(function(text){
        editor.postMessage('PUT text\nContent-Location: '+ url +
            '\nContent-Type: '+ form.getAttribute("enctype") +
            '\n\n' + text, '*');
        onhashchange(editor)();
        return calli.promise(function(callback){
            waiting.push(callback);
        });
    }).then(undefined, calli.error);
}

})(jQuery);




