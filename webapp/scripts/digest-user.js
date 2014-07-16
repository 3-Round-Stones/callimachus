// digest-user.js
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

jQuery(function($){
    var form = document.getElementById('form');
    var uri = $(form).attr("resource");
    var credential = calli.getText("/?profile").then(function(doc) {
        return (/resource="([^" >]*)"/i).exec(doc)[1];
    });
    $(form).submit(calli.submitUpdate.bind(calli,calli.copyResourceData(form)));
    $('#cancel').click(function(event){
        window.location.replace('?view');
    });
    $('#delete').click(function(event){
        if (confirm("Are you sure you want to delete " + document.title + "?")) {
            var action = calli.getFormAction(form);
            calli.deleteText(action).then(function(){
                return credential.then(function(credential){
                    if (credential && credential == uri) {
                        // need to log user out gracefully since they deleted themselves
                        var e = jQuery.Event("calliLogout");
                        e.location = '/';
                        $(document).trigger(e);
                    } else {
                        window.location.replace('./');
                    }
                });
            }).catch(calli.error);
        }
    });
});
