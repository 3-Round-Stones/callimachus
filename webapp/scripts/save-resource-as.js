// save-resource-as.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
    window.calli = {};
}

window.calli.submitTurtleAs = function(event, fileName, create, folder) {
    var button = calli.fixEvent(event).target;
    var form = $(button).closest('form');
    var btn = $(button).filter('button');
    var resource = form.attr("about") || form.attr("resource") || '';
    var local = fileName || localPart(resource);
    btn.button('loading');
    return calli.promptForNewResource(folder, local).then(function(two){
        if (!two) return undefined;
        var url = two[0] + '?create=' + encodeURIComponent(create);
        var iri = two[0].replace(/\/?$/, '/') + two[1].replace(/%20/g, '+');
        form.attr("resource", iri);
        try {
            return calli.copyResourceData(form);
        } finally {
            if (resource) {
                form.attr("resource", resource);
            }
        }
    }).then(function(data){
        if (!data) return data;
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
        if (!data) return data;
        return calli.createTurtle(url, data);
    }).then(function(redirect){
        if (redirect) {
            window.location.replace(redirect);
        } else {
            btn.button('reset');
        }
    }, function(error){
        btn.button('reset');
        return calli.error(error);
    });
};

window.calli.promptForNewResource = function(container, localPart) {
    return calli.resolve().then(function(){
        return new Promise(function(resolve, reject){
            try {
                openSaveAsDialog(null, localPart, null, container, resolve);
            } catch(e){
                reject(e);
            }
        });
    });
};

window.calli.saveFormAs = function(event, fileName, create) {
    return calli.saveResourceAs(event, fileName, create);
};
var nestedSubmit = false;
window.calli.saveResourceAs = function(event, fileName, create, folder) {
    event = calli.fixEvent(event);
    var form = event.target;
    if(!$(form).is('form')) form = $(form).closest('form')[0];

    $(form).find("input").change(); // IE may not have called onchange before onsubmit
    var resource = $(form).attr('about') || $(form).attr('resource');
    if (event.type == 'submit') {
        if (fileName && !nestedSubmit) {
            // let's set the resource attribute and go
            var about = calli.listResourceIRIs(calli.getPageUrl())[0];
            if (about.indexOf('?') > 0) {
                about = about.substring(0, about.indexOf('?'));
            }
            var ns = null;
            if (resource) {
                // use the already chosen folder
                ns = resource.replace(/[^\/]*\/?$/, '');
                if (!ns) {
                    ns = about.replace(/[^\/]*$/, '');
                }
            } else if (about.lastIndexOf('/') != about.length - 1
                    && window.location.search.indexOf('?create=') == 0) {
                // creating nested resource
                ns = about + '/';
            } else if (window.location.search == '?create') {
                ns = null; // we have to prompt for a folder
            } else {
                // create resource in this same folder
                ns = about.replace(/[^\/]*$/, '');
            }
            if (ns) {
                var local = encodeURI(fileName.replace(/^\s+/,'').replace(/\s+$/,'').replace(/\s+/g, '+')).replace(/%25(\w\w)/g, '%$1');
                resource = ns + local;
                $(form).removeAttr('about');
                $(form).attr('resource', resource);
                overrideLocation(form, resource);
                return true;
            }
        } else if (resource) {
            // resource attribute ready set, let's go
            return true;
        }
    }
    // prompt for a new resource URI
    var label = fileName || findLabel(form) || localPart(resource);
    openSaveAsDialog(form, label, create, folder, function(twoPartArray) {
        if (!twoPartArray) return; // dialogue cancelled
        var ns = twoPartArray[0];
        var local = twoPartArray[1];
        if (fileName) {
            local = local.replace(/(%20|\-)+/g,'-');
        } else {
            local = local.replace(/%20/g,'+');
        }
        if (ns.lastIndexOf('/') != ns.length - 1) {
            ns += '/';
        }
        var resource = ns + local;
        $(form).removeAttr('about');
        $(form).attr('resource', resource);
        overrideLocation(form, resource);
        if (form.getAttribute("enctype") == "application/sparql-update") {
            form.setAttribute("enctype", "text/turtle");
        }
        try {
            nestedSubmit = true;
            $(form).submit(); // this time with a resource attribute
        } finally {
            nestedSubmit = false;
        }
    });
    $(form).removeAttr('about');
    $(form).removeAttr('resource');
    event.preventDefault();
    return false;
};

function findLabel(form) {
    var field = $($(form).find('input:not(:checkbox,:disabled,:button,:password,:radio)')[0]);
    var input = field.val();
    if (input) {
        var onchange = function() {
            if (input != $(field).val()) {
                // restore the resource attribute when this field changes
                if (resource) {
                    $(form).removeAttr('about');
                    $(form).attr('resource', resource);
                } else {
                    $(form).removeAttr('about');
                    $(form).removeAttr('resource');
                }
                field.unbind('change', onchange);
            }
        };
        field.bind('change', onchange);
    }
    return calli.slugify(input);
}

function localPart(resource) {
    if (resource)
        return resource.replace(/.*\/(.+)/, '$1');
    return null;
}

function openSaveAsDialog(form, label, create, folder, callback) {
    var src = calli.getCallimachusUrl("pages/save-resource-as.html#");
    if (label) {
        src += encodeURIComponent(label.replace(/!/g,''));
    }
    if (folder) {
        src += '!' + folder + '?view';
    } else if (location.search.search(/\?create=/) === 0) {
        var page = calli.getPageUrl();
        src += '!' + page.substring(0, page.indexOf('?')) + '?view';
    } else {
        try {
            if (window.sessionStorage.getItem("LastFolder")) {
                src += '!' + window.sessionStorage.getItem("LastFolder");
            } else if (window.localStorage.setItem("LastFolder")) {
                src += '!' + window.localStorage.setItem("LastFolder");
            }
        } catch (e) {
            // ignore
        }
    }
    var called = false;
    var dialog = window.calli.openDialog(src, 'Save As...', {
        buttons: {
            "Save": function() {
                dialog.postMessage('GET label', '*');
            },
            "Cancel": function() {
                calli.closeDialog(dialog);
            }
        },
        onmessage: function(event) {
            var data = event.data;
            if (data == 'POST save') {
                dialog.postMessage('OK\n\n' + event.data, '*');
                dialog.postMessage('GET label', '*');
            } else if (data.indexOf('OK\n\nGET label\n\n') === 0) {
                label = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                dialog.postMessage('GET url', '*');
            } else if (data.indexOf('OK\n\nGET url\n\n') === 0) {
                var src = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                if (src.indexOf('?') >= 0) {
                    src = src.substring(0, src.indexOf('?'));
                }
                var ns = src.replace(/\?.*/,'');
                var local = encodeURI(label).replace(/%25(\w\w)/g, '%$1');
                form && updateFormAction(form, src, create);
                called = true;
                callback([ns, local]);
                calli.closeDialog(dialog);
            }
        },
        onclose: function() {
            if (!called){
                called = true;
                callback();
            }
        }
    });
    return dialog;
}

function updateFormAction(form, target, create) {
    var action = calli.getFormAction(form);
    var m;
    if (create) {
        form.setAttribute("method", "POST");
        form.action = target + '?create=' + encodeURIComponent(create);
    } else if (m = action.match(/^([^\?]*)\?create(&.*)?$/)) {
        action = target + '?create=';
        if (create) {
            action += encodeURIComponent(create);
        } else if (m[1]) {
            action += encodeURIComponent(calli.listResourceIRIs(m[1])[0]);
        } else {
            action += encodeURIComponent(calli.listResourceIRIs(location.pathname)[0]);
        }
        if (m[2]) {
            action += m[2];
        }
        form.setAttribute("method", "POST");
        form.action = action;
    } else if (m = action.match(/^([^\?]*)(\?create=[^&]+)(&.*)?$/)) {
        action = target + m[2];
        if (m[3]) {
            action += m[3];
        }
        form.setAttribute("method", "POST");
        form.action = action;
    }
}

function overrideLocation(form, uri) {
    var action = calli.getFormAction(form);
    if (action.indexOf('&resource=') > 0) {
        var m = action.match(/^(.*&resource=)([^&=]*)(.*)$/);
        form.action = m[1] + encodeURIComponent(uri) + m[3];
    } else if (action.indexOf('?create') >= 0) {
        form.action = action + '&resource=' + encodeURIComponent(uri);
    }
    if (action.indexOf('?create') >= 0 && action.indexOf('&intermediate=') < 0 && isIntermidate(action)) {
        form.action += '&intermediate=true';
    }
}

function isIntermidate(url) {
    if (window.parent != window) {
        try {
            var childUrl = url;
            if (childUrl.indexOf('?create') > 0) {
                childUrl = childUrl.substring(0, childUrl.indexOf('?'));
                var parentUrl = window.parent.location.href;
                if (parentUrl.indexOf('?edit') > 0) {
                    parentUrl = parentUrl.substring(0, parentUrl.indexOf('?'));
                    if (parentUrl == childUrl) {
                        // they are creating a component in a dialog from an edit form
                        return true;
                    }
                }
            }
        } catch (e) {
            // I guess not
        }
    }
    return false;
}

})(jQuery);

