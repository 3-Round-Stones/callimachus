// submit-file.js
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

var calli = window.calli || (window.calli={});

calli.submitFile = function(event) {
    event.preventDefault();
    var form = $(calli.fixEvent(event).target).closest('form')[0];
    var action = calli.getFormAction(form);
    var enctype = form.getAttribute("enctype") || "application/octet-stream";
    var files = $(form).find('input[type="file"]').toArray().reduce(function(files, input) {
        return files.concat(input.files);
    }, []);
    return calli.putText(action, files[0], enctype).then(function(redirect){
        if (window.parent != window && parent.postMessage) {
            parent.postMessage('PUT src\n\n' + redirect, '*');
        }
        window.location.replace(redirect);
    }).then(undefined, calli.error);
};

})(jQuery);
