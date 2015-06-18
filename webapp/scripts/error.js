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
                flash(message, stack) || parent != window && parent.postMessage && parent.postMessage(msg);
            } else {
                flash(msg.substring('ERROR '.length)) || parent != window && parent.postMessage && parent.postMessage(msg);
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
calli.error = function(message, stack) {
    if (typeof message == 'object') {
        if (message.status >= 400 && message.statusText) {
            return calli.error(message.statusText, message.responseText);
        } else if (message.status <= 100) {
            return calli.error("Could not connect to server, please try again later");
        } else if (message.message) {
            return calli.error(message.message, stack || message.stack);
        } else if (message.name) {
            return calli.error(message.name, stack || message.stack);
        } else if (message.description) {
            return calli.error(message.description, stack || message.stack);
        }
    } 
    if (typeof message == 'function' && typeof message.toSource == 'function') {
        return calli.error(message.toSource(), stack);
    } else {
        var mhtml, shtml;
        if (stack && stack.indexOf('<') === 0) {
            mhtml = $(stack).find("h1.text-error").addBack().add($(stack).find("h1")).filter("h1").html();
            shtml = $(stack).find("pre.text-error").addBack().filter("pre").html();
        } else {
            mhtml = asHtml(message);
            shtml = stack && asHtml(stack);
        }
        if (window.console && window.console.error) {
            console.error(message);
        }
        if (!flash(mhtml, shtml)) {
            if (parent != window && parent.postMessage) {
                if (shtml) {
                    parent.postMessage('ERROR ' + mhtml + '\n\n' + shtml, '*');
                } else {
                    parent.postMessage('ERROR ' + mhtml, '*');
                }
            } else {
                return calli.reject(message);
            }
        }
    }
};

function asHtml(obj) {
    if (!obj) {
        return undefined;
    } else if (typeof obj == 'string') {
        return $('<p/>').text(obj).html();
    } else if (obj.nodeType) {
        return $('<p/>').append($(obj).clone()).html();
    } else {
        return $('<p/>').text(obj.toString()).html();
    }
}

var unloading = false;
$(window).bind('unload', function() {
    unloading = true;
}).bind('beforeunload', function(){
    unloading = true;
});

function flash(message, stack) {
    var msg = $("#calli-error");
    var template = $('#calli-error-template').children();
    if (!msg.length || !template.length)
        return false;
    var widget = template.clone();
    widget.append(message);
    if (stack) {
        var pre = $("<pre/>");
        pre.append(stack);
        pre.hide();
        var more = $('<a/>');
        more.text(" » ");
        more.click(function() {
            pre.toggle();
        });
        widget.append(more);
        widget.append(pre);
    }
    return setTimeout(function() {
        if (!unloading) {
            msg.append(widget);
            scroll(0,0);
        }
    }, 0);
}

})(jQuery, jQuery);

