// purl.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){
    var mode = getMode();   // get the subclass of PURL being created/edited

    // load PURL rules from the resource into the edit space
    $('#loadedRules input').each(function() {
        var purl = decodePurl($(this).attr('property'), $(this).val().trim());
        var rule = null;
        if (mode === 'Redirect' || mode === 'Purl') {
            // single rule inputs
            rule = $('#rules');
        } else {
            // multiple rule table row
            rule = $('#ruleTable>tfoot>tr').clone();
            $('#rules').append(rule);
        }
        rule.find('.requestMethod').val(purl.methods.split(','));
        rule.find('.requestPattern').val(purl.pattern);
        rule.find('.responseType').val(purl.response);
        rule.find('.purlTarget').val(purl.template);
        rule.find('.purlTarget').after($(this));
    });

    // load PURL rules from the resource into the view table
    $('#loadedRules p').each(function() {
        var purl = decodePurl($(this).attr('property'), $(this).attr('content').trim());
        $('#rules').append('<tr><td>'+purl.methods+'</td><td>'+purl.pattern+'</td><td>'+$(this).text()+'</td><td><a href="'+purl.template+'">'+purl.template+'</a></td></tr>');
    });

    // load a PURL rule from the resource into the view table for simple PURLs
    $('#loadedRule p').each(function() {
        var purl = decodePurl($(this).attr('property'), $(this).attr('content').trim());
        $('#rule').append(
            '<tr><th>Incoming request</th><td>'+purl.methods+'</td></tr>'+
            '<tr><th>Response target</th><td><a href="'+purl.template+'">'+purl.template+'</a></td></tr>'+
            '<tr><th>Status</th><td>'+$(this).text()+'</td></tr>'
        );
    });

    $('#form').each(function(){
        var form = this;
        var comparison = calli.copyResourceData(form);
        $(form).submit(function(event){
            event.preventDefault();
            var btn = $(form).find('button[type="submit"]');
            btn.button('loading');
            calli.resolve($(form).attr("enctype")).then(function(enctype){
                var errors = [];
                validate(mode, enctype, errors);
                if (errors.length !== 0) {
                    return calli.reject($('<span/>').append('<b>Validation errors:</b><br>'+errors.join('<br>'))[0]);
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
                calli.error(error);
            });
        });
    });
    
    // set up first row and any selects
    if (mode === 'Redirect' || mode === 'Purl') {
        setupSelects($('#rules'), mode);
    } else {
        // Create the initial row for a PURL rule
        if ($('#loadedRules').length === 0) {
            // create template rather than an edit
            addRule('#rules', mode);
        } else {
            setupSelects($('#rules'), mode);
        }
    }
});

function getMode() {
    var mode = $('#form').attr('typeof'); // only present in a create template
    if (mode) {
        mode = mode.split(':')[1];
    } else {
        mode = $('div[property="rdf:type"]:contains("http://callimachusproject.org/rdf/2009/framework")').text();
        mode = mode.split('#')[1];
    }
    return mode;
}

function decodePurl(responseType, target) {
    var purl = {};
    var res = target.match(/^((?:(?:GET|PUT|POST|PATCH|DELETE),?)*)\s*(?:(\S+)\s+)?\s*(.*)$/);
    purl.methods = (res[1] && res[1].length !== 0)? res[1] : null;
    purl.pattern = res[2] || '.*';
    purl.template = res[3] || null;
    purl.response = responseType;
    if (purl.methods === null) {
        if (responseType === 'calli:post') { 
            purl.methods = 'POST'; 
        } else if (responseType === 'calli:patch') { 
            purl.methods = 'PATCH';
        } else if (responseType === 'calli:put') { 
            purl.methods = 'PUT';
        } else if (responseType === 'calli:delete') { 
            purl.methods = 'DELETE';
        } else { 
            purl.methods = 'GET';
        }
    }
    return purl;
}

// validate the rules and purl definition
function validate(mode, enctype, errors) {
    /*
    General
        Body allowed for POST, PUT, PATCH
        Combination of regex and request methods must not overlap - not easy to test
    RewriteRule
        No further restriction
    Proxy
        1 rule per request method
    PURL
        Single rule
        Not partial
        No pattern matching
        No headers/body - use RewriteRule
    Redirect
        Single rule
        Not partial
        No cache control
        No pattern matching
        No headers/body - use RewriteRule

    PathSegment
        1 rule per response type
        No headers/body - use RewriteRule
    Partial PURL
        1 rule per response type
        No headers/body - use RewriteRule
    Folder
        1 rule per response type ???
        No headers/body - use RewriteRule
        
    Validation of target template, body, headers not yet implemented
    */
    var getMethodsFound = 0;
    // check the name on a create template
    if (enctype == "text/turtle") {
        var local = $('#local').val().trim();
        $('#local').val(local);
        if (local.charAt(local.length-1) === '/') {
            if (mode === 'Redirect' || mode === 'Purl') {
                errors.push('This type of PURL cannot end with / as that is only for PartialPURLs.');
            } else if (mode === 'PartialPURL') {
                // remove any redundant / at the end
                $('#local').val(local.replace(/\/*$/, ''));
            }
        }
    }
    
    // set up the hidden inputs to represent the full predicate & object pairs of the PURL
    $('#rules input:hidden').each(function(idx) {
        var rule = $(this).parents('.rule');
        var purl = validateRule(rule, idx, errors);

        // build full template for the rule if it is valid
        if (purl) {
            $(this).attr('property', purl.response);
            if (purl.methods && purl.methods.indexOf('GET') !== -1) {
                getMethodsFound++;
            }
            $(this).val(encodePurl(purl));
        }
    });
    
    if (!getMethodsFound && mode !== 'Redirect' && mode !== 'Purl') {
        // if there are no PURLs responding to GET requests create a redirect to a view of this PURL
        // what about those with pattern matching that don't resolve to this PURL?
        // feature turned off of simple PURLs but if needed we must ensure the special rule is not editable
        $('#form').append("<input type='hidden' property='calli:alternate' content='?view' value='?view'/>");
    }
}

function validateRule(rule, idx, errors) {
    var purl = {};
    var ruleError = false;
    // validate user entries
    purl.methods = rule.find('.requestMethod').val();
    if (purl.methods === null) {
        errors.push('Rule '+(idx+1)+': Request method is mandatory.');
        ruleError = true;
    }
    purl.pattern = rule.find('.requestPattern').val();
    if (purl.pattern) {
        purl.pattern = purl.pattern.trim();
    } else {
        // it is inly undefined if it is not on the form i.e. a Redirect or a Purl
        purl.pattern = '.*';
    }
    if (purl.pattern === null || purl.pattern === '') {
        errors.push('Rule '+(idx+1)+': Request pattern is mandatory.');
        ruleError = true;
    }
    purl.template = rule.find('.purlTarget').val() || null;
    if (purl.template) {
        purl.template = purl.template.trim();
    }
    if (purl.template === null || purl.template === '') {
        errors.push('Rule '+(idx+1)+': Response template is mandatory.');
        ruleError = true;
    }
    purl.response = rule.find('.responseType').val();
    if (purl.response === null || purl.response === '') {
        errors.push('Rule '+(idx+1)+': Response method/status is mandatory.');
        ruleError = true;
    }
    return ruleError ? null : purl;
}

function encodePurl(purl) {
    if (purl.methods && purl.methods.length === 1) {
        // only one rule so it may not be required
        if ((purl.methods[0] === 'POST' && purl.response === 'calli:post') ||
            (purl.methods[0] === 'PUT' && purl.response === 'calli:put') ||
            (purl.methods[0] === 'PATCH' && purl.response === 'calli:patch') ||
            (purl.methods[0] === 'DELETE' && purl.response === 'calli:delete') ||
            (purl.methods[0] === 'GET' && purl.response !== 'calli:post' && purl.response !== 'calli:put' && purl.response !== 'calli:patch' && purl.response !== 'calli:delete')) {
            // this is the default method for this response type so not required in the PURL
            purl.methods = null;
        }
    }
    if (purl.pattern === '.*' && purl.methods === null) {
        // .* is default so not required if method is default for the response type
        purl.pattern = null;
    }
    var encodedPurl = 
        (purl.methods?purl.methods.join(',')+' ':'') + 
        (purl.pattern?purl.pattern+' ':'') +
        purl.template;
    return encodedPurl;
}

function setupSelects(container, mode) {
    if (mode === 'Proxy') {
        var methodOptions = [
            { value: 'GET', text: 'GET' },
            { value: 'POST', text: 'POST' },
            { value: 'PUT', text: 'PUT' },
            { value: 'PATCH', text: 'PATCH' },
            { value: 'DELETE', text: 'DELETE' }
        ];
        container.find('select.requestMethod').selectize({
            options: methodOptions,
            onFocus: function() { 
                setRemainingOptions(this, methodOptions, $('#rules select.requestMethod'));
            },
            onInitialize: function() {
                if ($('#rules').children().length === 1) {
                    this.addItem('GET');
                }
            }
        });
    } else {
        container.find('select.requestMethod').selectize();
    }

    if (mode === 'PathSegment' || mode === 'Folder' || mode === 'PartialPURL') {
        var responseTypeOptions = [
            { value: 'calli:copy', text: 'Copy (200)' },
            { value: 'calli:canonical', text: 'Canonical (301)' },
            { value: 'calli:alternate', text: 'Alternate (302)' },
            { value: 'calli:describedby', text: 'Described by (303)' },
            { value: 'calli:resides', text: 'Resides (307)' },
            { value: 'calli:moved', text: 'Moved (308)' },
            { value: 'calli:missing', text: 'Missing (404)' },
            { value: 'calli:gone', text: 'Gone (410)' },
            { value: 'calli:illegal', text: 'Illegal (451)' },
            { value: 'calli:post', text: 'POST' },
            { value: 'calli:put', text: 'PUT' },
            { value: 'calli:patch', text: 'PATCH' },
            { value: 'calli:delete', text: 'DELETE' }
        ];
        container.find('select.responseType').selectize({
            options: responseTypeOptions,
            onFocus: function() { 
                setRemainingOptions(this, responseTypeOptions, $('#rules select.responseType'));
            },
            onInitialize: function() {
                this.addItem('calli:alternate');
            }
        });
    } else {
        container.find('select.responseType').selectize();
    }
    
}

// ensure the select only shows options that are still available
function setRemainingOptions(select, options, allSelects) {
    var items = select.items;
    select.addOption(options);
    allSelects.each(function( index ) {
        for (var opt in $(this).val()) {
            if (items.length === 0 || items.indexOf($(this).val()[opt]) == -1) {
                select.removeOption($(this).val()[opt]);
            }
        }
    });
    select.refreshOptions();
}

function addRule(container, mode) {
    if (!mode) {
        mode = getMode();
    }
    var row = $('#ruleTable>tfoot>tr').clone();
    row.find('.purlTarget').after('<input type="hidden" value="" property="calli:alternate"/>');
    $(container).append(row);
    if ($(container).children().length > 1) {
        $(container).children().first().find('.glyphicon-minus').show();
    }
    setupSelects($(container).children().last(), mode);
}

function deleteRule(event, container) {
    $(event.target.parentNode.parentNode).remove();
    if ($(container).children().length === 1) {
        $(container).find('.glyphicon-minus').hide();
    }
}
