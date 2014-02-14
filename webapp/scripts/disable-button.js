// disabled-button.js
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

(function($){

$(document).ready(function() {
    $("button", document).click(flashButton);
});
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
    $("button", event.target).click(flashButton);
    if ($(event.target).is("button")) {
        $(event.target).click(flashButton);
    }
}

function flashButton(event) {
    var button = $(this);
    if (!button.is('.dropdown-toggle')) {
        // yield
        setTimeout(function() {
            if (!button.attr('disabled')) {
                button.attr('disabled', 'disabled');
                button.addClass("disabled");
                setTimeout(function() {
                    button.removeAttr('disabled');
                    button.removeClass("disabled");
                }, 5000);
                button.focus(function() {
                    button.removeAttr('disabled');
                    button.removeClass("disabled");
                    return true;
                });
            }
        }, 0);
    }
    return true;
}

})(window.jQuery);
