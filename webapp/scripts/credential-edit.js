// credential-edit.js
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

$('<span></span>').attr("id", "modified")
    .attr("property", "dcterms:modified")
    .attr("datatype", "xsd:dateTime")
    .attr("content", new Date().toISOString())
    .appendTo('#form');

$('form[typeof~="calli:Credential"]').submit(function(event){
    var form = this;
    var resource = $(form).attr('resource');
    var password = $('#password').val();
    if (password) {
        event.preventDefault();
        $.ajax({
            type: 'POST',
            url: resource + '?password',
            contentType: 'text/plain',
            data: rstr2b64(str2rstr_utf8(password)),
            xhrFields: calli.withCredentials,
            dataType: "text",
            success: function(url) {
                $('#password').val('');
                $(form).submit();
            }
        });
    }
});

});
