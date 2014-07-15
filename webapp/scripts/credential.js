// credential.js
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

$('form[typeof~="calli:Credential"][enctype="text/turtle"]').submit(function(event){
    var form = this;
    event.preventDefault();
    var username = $('#username').val();
    var password = $('#password').val();
    var authority = $('#authority').val();
    var local = encodeURI(username) + '@' + authority;
    var btn = $(form).find('button[type="submit"]');
    btn.button('loading');
    $('#label').val(username + '@' + authority).change();
    return calli.resolve(form).then(function(form){
        form.setAttribute("resource", local);
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
        var url = local + '?password';
        var text = rstr2b64(str2rstr_utf8(password));
        return calli.postText(url, text).then(function(){
            return redirect;
        });
    }).then(function(redirect){
        window.location.replace(redirect);
    }, function(error){
        btn.button('reset');
        return calli.error(error);
    });
});

$('form[typeof~="calli:Credential"][enctype="application/sparql-update"]').each(function(){
    var form = this;
    var comparison = calli.copyResourceData(form);
    $(form).submit(function(event){
        event.preventDefault();
        var btn = $(form).find('button[type="submit"]');
        btn.button('loading');
        calli.resolve($('#password').val()).then(function(password){
            if (password) {
                var resource = $(form).attr('resource');
                var text = rstr2b64(str2rstr_utf8(password));
                return calli.postText(resource + '?password', text);
            } else {
                return calli.resolve();
            }
        }).then(function(){
            return calli.submitUpdate(comparison, event);
        }, function(error){
            btn.button('reset');
            return calli.error(error);
        });
    });
});

});
