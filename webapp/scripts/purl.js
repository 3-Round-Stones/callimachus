// purl.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/
jQuery(function($){
    var mode = getMode();   // get the subclass of PURL being created/edited
    
    var actionTypeOptions = [
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
    
    var redirectActions = {
        '301': actionTypeOptions[1],
        '302': actionTypeOptions[2]
    };
    
    var purlActions = {
        '200': actionTypeOptions[0],
        '301': actionTypeOptions[1],
        '302': actionTypeOptions[2],
        '303': actionTypeOptions[3],
        '307': actionTypeOptions[4],
        '308': actionTypeOptions[5],
        '404': actionTypeOptions[6],
        '410': actionTypeOptions[7],
        '451': actionTypeOptions[8]
    };

    // check for limited action types in the query string
    var params = parseQueryString();
    var allowedActions = [];
    if ((mode === 'Redirect' || mode === 'Purl') && (params.action !== undefined || params.actions !== undefined)) {
        var val = params.actions || params.action;
        if (typeof val === 'string') {
            val = val.split(',');
        }
        var list = (mode === 'Redirect') ? redirectActions : purlActions;
        for (var i in val) {
            if (val[i] in list) {
                allowedActions.push(list[val[i]]);
            }
        }
        removeDefinitions(allowedActions);
    }

    if (mode === 'Proxy') {
        // set up collapsible panels in Proxy templates
        $(".collapse").on("hide.bs.collapse", function(){
            $(this).parent().find('.panel-heading a').removeClass('glyphicon-minus').addClass('glyphicon-plus');
            $(this).find('input[type="hidden"]').removeAttr('property');
        });
        $(".collapse").on("show.bs.collapse", function(){
            $(this).parent().find('.panel-heading a').removeClass('glyphicon-plus').addClass('glyphicon-minus');
        });
    }

    $('#form').each(function(){
        var form = this;
        var comparison = calli.copyResourceData(form);
        $(form).submit(function(event){
            event.preventDefault();
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
            }, calli.loading(event.target, calli.error));
        });
    });
    
    // load PURL rules from the resource into the edit space
    $('#loadedRules input').each(function() {
        var purl = decodePurl($(this).attr('property'), $(this).val().trim());
        var rule = null;
        if (mode === 'Redirect' || mode === 'Purl') {
            // single rule inputs
            rule = $('#rules');
        } else if (mode === 'Proxy') {
            if (purl.action === 'calli:alternate') {
                // ignore the calli:alternate rule that is a special view case when there is no GET
                $(this).remove();
                return;
            }
            rule = $('#rules').find('div.rule[data-method="'+purl.methods+'"]');
            if (rule.length === 0) {
                $(this).remove();
                return;
            }
            rule.find('.panel-body').collapse('show');
            rule.find('input[type="hidden"]').remove(); // remove dummy rule
        } else {
            // multiple rule table row
            rule = $('#blankRule>div').clone();
            $('#rules').append(rule);
        }
        rule.find('.requestMethod').val(purl.methods.split(','));
        rule.find('.requestPattern').val(purl.pattern);
        rule.find('.actionType').val(purl.action);
        rule.find('.purlTarget').val(purl.template);
        rule.find('.purlTarget').after($(this));
    });

    // load PURL rules from the resource into the view table
    $('#loadedRules p').each(function() {
        var purl = decodePurl($(this).attr('property'), $(this).attr('content').trim());
        if (mode !== 'Proxy' || purl.action !== 'calli:alternate') {
            // ignore the alternate rule as itis just for viewing when there is no GET rule
            $('#rules').append('<tr><td>'+purl.methods+'</td><td>'+(purl.pattern?purl.pattern:'')+'</td><td>'+$(this).text()+'</td><td><a href="'+purl.template+'">'+purl.template+'</a></td></tr>');
        }
    });

    // load a PURL rule from the resource into the view table for simple PURLs
    $('#loadedRule p').each(function() {
        var purl = decodePurl($(this).attr('property'), $(this).attr('content').trim());
        $('#rule').append(
            '<tr><th>Incoming request</th><td>'+purl.methods+'</td></tr>'+
            '<tr><th>Action target</th><td><a href="'+purl.template+'">'+purl.template+'</a></td></tr>'+
            '<tr><th>Status</th><td>'+$(this).text()+'</td></tr>'
        );
    });

    // set up first row and any selects
    if (mode === 'Redirect' || mode === 'Purl') {
        setupRuleSelects($('#rules'), mode, allowedActions);
    } else if (mode === 'Proxy') {
        setupRuleSelects($('#rules'), mode);
    } else {
        if ($('#loadedRules').length === 0) {
            // Create the initial row for a PURL rule in a create template
            addRule('#rules', mode);
            $('#rules a.glyphicon-minus').hide();
        } else {
            setupRuleSelects($('#rules'), mode);
        }
    }

    if (mode === 'Purl' || mode === 'Proxy' || mode === 'RewriteRule') {
        var selectionValue = $('#cache>option:selected').val();
        if (selectionValue && $('#cache>option[value="'+selectionValue+'"]').length === 2) {
            $('#cache>option:selected').remove();
            $('#cache').val(selectionValue);
        }
        $('#cache').selectize({
            allowEmptyOption:true,
            create:true
        });
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

function decodePurl(actionType, target) {
    var purl = {};
    var res = target.match(/^((?:(?:GET|PUT|POST|PATCH|DELETE),?)*)\s*(?:(\S+)\s+)?\s*(.*)$/);
    purl.methods = (res[1] && res[1].length !== 0)? res[1] : null;
    purl.pattern = res[2];
    purl.template = res[3] || null;
    purl.action = actionType;
    if (purl.methods === null) {
        if (actionType === 'calli:post') { 
            purl.methods = 'POST'; 
        } else if (actionType === 'calli:patch') { 
            purl.methods = 'PATCH';
        } else if (actionType === 'calli:put') { 
            purl.methods = 'PUT';
        } else if (actionType === 'calli:delete') { 
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
        Simplified Cache-Control
    PURL
        Single rule
        Not partial
        Simplified Cache-Control
        No pattern matching
        No headers/body - use RewriteRule
    Redirect
        Single rule
        Not partial
        No cache control
        No pattern matching
        No headers/body - use RewriteRule

    Folder - TBD
        1 rule per action type ???
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
                errors.push('This type of PURL cannot end with / as they are for simple redirections');
            }
        }
    }
    
    // set up the hidden inputs to represent the full predicate & object pairs of the PURL
    var ruleCount = 0;
    $('#rules input[type="hidden"]').each(function(idx) {
        var rule = $(this).parents('.rule');
        if (mode !== 'Proxy' || rule.find('.panel-body').hasClass("in")) {
            // for a proxy, ignore collapsed panels
            var purl = validateRule(rule, idx, errors, mode);

            // build full template for the rule if it is valid
            if (purl) {
                ruleCount++;
                $(this).attr('property', purl.action);
                if (purl.methods && purl.methods.indexOf('GET') !== -1) {
                    getMethodsFound++;
                }
                $(this).val(encodePurl(purl, mode));
            }
        }
        
    });
    if (ruleCount === 0 && mode === 'Proxy') {
        errors.push('Please define at least one handler');
    }
    if (ruleCount !== 0 && errors.length === 0 && !getMethodsFound && mode !== 'Redirect' && mode !== 'Purl') {
        // if there are no valid PURLs responding to GET requests create a redirect to a view of this PURL
        // what about those with pattern matching that don't resolve to this PURL?
        $('#form').append("<input type='hidden' property='calli:alternate' content='?view' value='?view'/>");
    }
}

function validateRule(rule, idx, errors, mode) {
    var purl = {};
    var ruleError = false;
    // validate user entries
    if (mode === 'Redirect' || mode === 'Purl') {
        // this is a fixed method
        purl.methods = ['GET'];
    } else if (mode === 'Proxy') {
        purl.methods = [rule.attr('data-method')];
    } else {
        purl.methods = rule.find('.requestMethod').val();
    }
    if (purl.methods === null) {
        errors.push('Rule '+(idx+1)+': Request method is mandatory.');
        ruleError = true;
    }
    purl.pattern = rule.find('.requestPattern').val();
    if (purl.pattern) {
        purl.pattern = purl.pattern.trim();
    }
    purl.template = rule.find('.purlTarget').val() || null;
    if (purl.template) {
        purl.template = purl.template.trim();
    }
    if (purl.template === null || purl.template === '') {
        errors.push('Rule '+(idx+1)+': Target template is mandatory.');
        ruleError = true;
    }
    var actionElem = rule.find('.actionType');
    purl.action = actionElem.val() || actionElem.attr('data-value');
    if (purl.action === null || purl.action === '') {
        errors.push('Rule '+(idx+1)+': Action method/status is mandatory.');
        ruleError = true;
    }
    if (purl.pattern === null || purl.pattern === '' && patternRequired(purl)) {
        errors.push('Rule '+(idx+1)+': Request pattern is mandatory.');
        ruleError = true;
    }
    return ruleError ? null : purl;
}

function patternRequired(purl) {
    // pattern is required when a method is given or the request method doesn't match the action
    return (purl.methods.length > 1) || !(
        (purl.methods[0] === 'GET' && (
            purl.action === 'calli:copy' || purl.action === 'calli:canonical' || purl.action === 'calli:alternate' || 
            purl.action === 'calli:describedby' || purl.action === 'calli:resides' || purl.action === 'calli:moved' ||
            purl.action === 'calli:missing' || purl.action === 'calli:gone' || purl.action === 'calli:illegal')) ||
        (purl.methods[0] === 'POST' && purl.action === 'calli:post') ||
        (purl.methods[0] === 'PUT' && purl.action === 'calli:put') ||
        (purl.methods[0] === 'PATCH' && purl.action === 'calli:patch') ||
        (purl.methods[0] === 'DELETE' && purl.action === 'calli:delete')
    );
}

function encodePurl(purl, mode) {
    if (mode !== 'Redirect' && mode !== 'Purl') {
        // simplify some classes of PURL
        if (purl.methods && purl.methods.length === 1) {
            // only one rule so it may not be required
            if ((purl.methods[0] === 'POST' && purl.action === 'calli:post') ||
                (purl.methods[0] === 'PUT' && purl.action === 'calli:put') ||
                (purl.methods[0] === 'PATCH' && purl.action === 'calli:patch') ||
                (purl.methods[0] === 'DELETE' && purl.action === 'calli:delete') ||
                (purl.methods[0] === 'GET' && purl.action !== 'calli:post' && purl.action !== 'calli:put' && purl.action !== 'calli:patch' && purl.action !== 'calli:delete')) {
                // this is the default method for this action type so not required in the PURL
                purl.methods = null;
            }
        }
        if (purl.pattern === '.*' && purl.methods === null) {
            // .* is default so not required if method is default for the action type
            purl.pattern = null;
        }
    } else {
        // Method is GET only so pattern is also required
        purl.pattern = '.*';
    }
    var encodedPurl = 
        (purl.methods?purl.methods.join(',')+' ':'') + 
        (purl.pattern?purl.pattern+' ':'') +
        purl.template;
    return encodedPurl;
}

function setupRuleSelects(container, mode, allowedActions) {
    if (mode === 'RewriteRule') {
        container.find('select.requestMethod').selectize();
    }

    if (mode === 'Folder') {
        /*
        container.find('select.actionType').selectize({
            options: actionTypeOptions, // IS THIS NEEDED?
            onFocus: function() { 
                setRemainingOptions(this, actionTypeOptions, $('#rules select.actionType'));
            },
            onInitialize: function() {
                this.addItem('calli:alternate');
            }
        });
        */
    } else {
        if ((mode === 'Redirect' || mode === 'Purl') && allowedActions.length !== 0) {
            if (allowedActions.length === 1) {
                container.find('select.actionType').replaceWith(
                    "<input type='hidden' data-value='"+allowedActions[0].value+"'/>"
                );
                $('#redirectStatus').hide();
            } else {
                var select = container.find('select.actionType').selectize()[0].selectize;
                select.clearOptions();
                select.addOption(allowedActions);
                select.refreshOptions(false);
                if (select.getOption('calli:alternate').length !== 0) {
                    select.addItem('calli:alternate', false);
                } else {
                    select.addItem(allowedActions[0].value, false);
                }
            }
        } else {
            container.find('select.actionType').selectize();
        }
    }
}

function addRule(container, mode) {
    if (!mode) {
        mode = getMode();
    }
    var row = $('#blankRule>div').clone();
    row.find('.purlTarget').after('<input type="hidden" value="" property="calli:alternate"/>');
    $(container).append(row);
    if ($(container).find('.rule').length > 1) {
        $(container).find('.rule').first().find('.glyphicon-minus').show();
    }
    setupRuleSelects($(container).children().last(), mode);
}

function deleteRule(event, container) {
    $(event.target).parents('.rule').remove();
    if ($(container).find('.rule').length === 1) {
        $(container).find('.glyphicon-minus').hide();
    }
}

function parseQueryString() {
    var str = window.location.search;
    if (typeof str !== "string" || str.length === 0) return {};
    var s = str.split("&");
    var s_length = s.length;
    var bit, query = {}, first, second;
    for(var i = 0; i < s_length; i++) {
        bit = s[i].split("=");
        first = decodeURIComponent(bit[0]);
        if (first.length === 0) continue;
        second = decodeURIComponent(bit[1]);
        if(typeof query[first] == "undefined") query[first] = second;
        else if(query[first] instanceof Array) query[first].push(second);
        else query[first] = [query[first], second];
    }
    return query;
}

function removeDefinitions(allowedActions) {
    if (allowedActions.length !== 0) {
        $('#actionDefinitions>dt').each(function() {
            var found = false;
            var action = $(this).text();
            for (var i in allowedActions) {
                if (allowedActions[i].text === action) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                $(this).hide();
                $(this).next().hide();
            }
        });
    }
}