// invited-user.js
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
    var origin = window.location.protocol + '//' + window.location.host;
    $('#subject').val($('#subject').val().replace(/@@HOSTNAME@@/g, window.location.hostname));
    $('#body').val($('#body').val().replace(/@@REGISTRATION_URL@@/g, origin + '/?register').replace(/@@HOSTNAME@@/g, window.location.hostname));
    var messageChanged = false;
    $('#body').change(function(){
        messageChanged = true;
    });
    $('#label').on('change keyup blur', function() {
        if (!messageChanged) {
            var salutation = $(this).val() ? 'Hello ' + $(this).val() : 'Hello';
            $('#body').val($('#body').val().replace(/(^|[\r\n])Hello.*/, '$1' + salutation + ','));
        }
    });
    $('#email').change(function(){
        if (this.value.indexOf('<') >= 0) {
            var name = this.value.substring(0, this.value.indexOf('<')).replace(/^\s+/,'').replace(/\s+$/,'');
            if (name) {
                $('#label').val(name).change();
            }
            $(this).val(this.value.replace(/.*</, '').replace(/>.*/, '')).change();
        }
    });
    if (window.location.hash) {
        var decoded = decodeURIComponent(window.location.hash.substring(1));
        if (decoded.indexOf('@') > 0) {
            $('#email').val(decoded).change();
        } else {
            $('#label').val(decoded).change();
        }
    }
    $('#invite').click(function(event){
        if ($('#msgForm')[0].checkValidity()) {
            event.preventDefault();
            $('#rdfForm').find('[type="submit"]').click(); // interactively validate the constraints
        }
    });
    $('#rdfForm').submit(function(event){
        var form = this;
        event.preventDefault();
        var resource = calli.slugify($('#email').val());
        var btn = $('#invite');
        btn.button('loading');
        return calli.resolve(form).then(function(form){
            form.setAttribute("resource", resource);
            return calli.copyResourceData(form);
        }).then(function(data){
            data.results.bindings.push({
                s: {type:'uri', value: data.head.link[0]},
                p: {type:'uri', value: 'http://purl.org/dc/terms/created'},
                o: {
                    type:'literal',
                    value: new Date().toISOString(),
                    datatype: "http://www.w3.org/2001/XMLSchema#dateTime"
                }
            });
            return data;
        }).then(function(data){
            return calli.postTurtle(calli.getFormAction(form), data);
        }).then(function(redirect){
            $('#fullname').val($('#label').val());
            $('#msgForm').attr("action", resource + '?invite').submit();
        }).then(undefined, function(error){
            btn.button('reset');
            return calli.error(error);
        });
    });

});
