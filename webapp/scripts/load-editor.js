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

    bindEditorEvents(form, editor);
    return setText(form, text, editor);
};

calli.loadEditor = function(event, url) {
    event = calli.fixEvent(event);
    var iframe = $(event.target);
    var editor = iframe[0].contentWindow;
    var form = iframe.closest('form')[0];

    bindEditorEvents(form, editor);
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
            var resource = encodeURI(local).replace(/%25(\w\w)/g, '%$1').replace(/%20/g, '-');
            var url = action + "&resource=" + encodeURIComponent(local);
            return calli.createText(url, text, form.attr('enctype')).then(function(redirect){
                return redirect && redirect + '?view';
            });
        } else {
            return calli.putText(action, text, form.attr('enctype')).then(function(){
                return action.replace(/\?.*/,'') + "?view";
            });
        }
    }).then(function(redirect){
        if (redirect) {
            if (window.parent != window && parent.postMessage) {
                parent.postMessage('PUT src\n\n' + redirect, '*');
            }
            window.location.href = redirect;
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
            return calli.createText(url, text, form.attr('enctype'));
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
function bindEditorEvents(form, editor, idempotent) {
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
            if (msg.indexOf('PUT text\n\n') === 0 && form.getAttribute("method") == "PUT") {
                var text = msg.substring('PUT text\n\n'.length);
                var action = calli.getFormAction(form[0]);
                calli.putText(action, text, form.getAttribute("enctype"));
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

// setText
function setText(form, text, editor) {
    if (window.location.hash.indexOf('#!') === 0) {
        var url = window.location.hash.substring(2);
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




