// error.js
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

(function($, jQuery){

var calli = window.calli || (window.calli={});

$(window).bind('message', function(event) {
    if ($('iframe').filter(function(){return this.contentWindow == event.originalEvent.source;}).length) {
        var msg = event.originalEvent.data;
        if (msg.indexOf('ERROR ') === 0) {
            if (msg.indexOf('\n\n') > 0) {
                var message = msg.substring('ERROR '.length, msg.indexOf('\n\n'));
                var stack = msg.substring(msg.indexOf('\n\n') + 2);
                calli.error(message, stack);
            } else {
                calli.error(msg.substring('ERROR '.length));
            }
        }
    }
});


// calli.error("message");
// calli.error("message", "stack");
// calli.error("message", "<html/>");
// calli.error(<span/>, "stack");
// calli.error(caught);
// calli.error({message:getter,stack:getter});
// calli.error({description:getter});
// calli.error(xhr);
window.calli.error = function(message, stack) {
    var e = jQuery.Event("error");
    if (typeof message == 'object') {
        if (message.status >= 400 && message.statusText) {
            return calli.error(message.statusText, message.responseText);
        } else if (message.status <= 100) {
            return calli.error("Could not connect to server, please try again later");
        }
        if (message.description) {
            e.message = asHtml(message.description);
        }
        if (message.name) {
            e.name = asHtml(message.name);
        }
        if (message.message) {
            e.message = asHtml(message.message);
        }
        if (message.stack) {
            e.stack = asHtml(message.stack);
        }
    }
    if (!e.message) {
        e.message = asHtml(message);
    }
    if (typeof message == 'string' && stack && stack.indexOf('<') === 0) {
        e.message = $(stack).find("h1").andSelf().filter("h1").html();
        e.stack = $(stack).find("pre").andSelf().filter("pre").html();
    } else if (stack) {
        e.stack = asHtml(stack);
    }
    if (e.message) {
        try {
            $(document).trigger(e);
        } catch (e) {
            setTimeout(function(){throw e;}, 0);
        }
    }
    var error;
    if (!message) {
        error = new Error();
    } else if (message instanceof Error) {
        error = message;
    } else if (typeof message == 'string') {
        error = new Error(message);
    } else if (message.nodeType) {
        error = new Error($('<p/>').append($(message).clone()).text());
    } else if (typeof message == 'function') {
        error = new Error(message.toSource());
    } else if (message.message) {
        error = new Error(message.message);
    } else {
        error = new Error(message.toString());
    }
    if (window.console && window.console.error) {
        console.error(message);
    }
    if (!e.isPropagationStopped() && parent != window && parent.postMessage) {
        if (stack) {
            parent.postMessage('ERROR ' + error.message + '\n\n' + stack, '*');
        } else {
            parent.postMessage('ERROR ' + error.message, '*');
        }
    }
    return calli.reject(error);
};

function asHtml(obj) {
    if (!obj) {
        return undefined;
    } else if (typeof obj == 'string') {
        return $('<p/>').text(obj).html();
    } else if (obj.nodeType) {
        return $('<p/>').append($(obj).clone()).html();
    } else if (typeof obj.toSource == 'function') {
        return $('<p/>').text(obj.toSource()).html();
    } else if (obj.message) {
        return $('<p/>').text(obj.message).html();
    } else {
        return $('<p/>').text(obj.toString()).html();
    }
}

})(jQuery, jQuery);

