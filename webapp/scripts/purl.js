// purl.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){
    $('#get textarea.pattern.hidden').remove();
    $('#get textarea.pattern[property]').each(function(){
        $('#type').val(this.getAttribute('property'));
    });
    $('#type').change(function(){
        $('#get textarea.pattern').attr('data-text-expression', this.value);
        $('#get textarea.pattern').attr('property', this.value);
    }).change();
    $('#form').each(function(){
        var form = this;
        var comparison = calli.copyResourceData(form);
        $(form).submit(function(event){
            event.preventDefault();
            var btn = $(form).find('button[type="submit"]');
            btn.button('loading');
            calli.resolve($(form).attr("enctype")).then(function(enctype){
                var getTextarea = $('#get').find('textarea');
                if (!getTextarea.val()) {
                    getTextarea.val('?view');
                }
                var action = calli.getFormAction(form);
                if (enctype == "text/turtle") {
                    return calli.resolve(form).then(function(form){
                        var local = $('#local').val();
                        var ns = window.location.pathname.replace(/\/?$/, '/');
                        var resource = ns + encodeURI(local).replace(/%25(\w\w)/g, '%$1').replace(/%20/g, '+');
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
                        return calli.postTurtle(action, data);
                    });
                } else {
                    return calli.resolve(form).then(calli.copyResourceData).then(function(insertData){
                        insertData.results.bindings.push({
                            s: {type:'uri', value: insertData.head.link[0]},
                            p: {type:'uri', value: 'http://purl.org/dc/terms/modified'},
                            o: {
                                type:'literal',
                                value: new Date().toISOString(),
                                datatype: "http://www.w3.org/2001/XMLSchema#dateTime"
                            }
                        });
                        return insertData;
                    }).then(function(insertData){
                        return calli.postUpdate(action, comparison, insertData);
                    });
    
                }
            }).then(function(redirect){
                if (window.parent != window && parent.postMessage) {
                    parent.postMessage('POST resource\n\n' + redirect, '*');
                }
                window.location.replace(redirect + '?view');
            }, function(error){
                btn.button('reset');
                return calli.error(error);
            });
        });
    });
});
