// group.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/


jQuery(function($){

    $('#members').closest('[dropzone]').on("dragenter dragover dragleave", function(event){
        event.preventDefault();
        return false;
    }).on('drop', dropResourceURL.bind(this, $('#user-lookup').prop('href'), $('#members').selectize({
        load: resourceSearch.bind(this, $('#user-search').prop('href')),
        searchField: ['text', 'email', 'name'],
        create: createUser.bind(this, $('#user-lookup').prop('href'), '#members'),
        render: {
            option: renderOption,
            item: renderItem
        }
    }).prop('selectize')));

    function renderItem(data, escape){
        return '<div title="' + escape(data.value) + '" onclick="calli.selectResource(this, this.title)">' + escape(data.text) + '</div>';
    }

    function renderOption(data, escape){
        return [
            '<div><p>' + escape(data.text) + '</p>',
            '<p class="text-info">' + escape(data.email) + '</p>',
            data.comment ? ('<p class="text-info">' + escape(data.comment) + '</p>') : '',
            '<a class="text-muted" href="' + escape(data.value) + '" onclick="return false">' + escape(data.value) + '</a>',
            '</div>'
        ].join('\n');
    }

    function dropResourceURL(template, selectize, event) {
        event.preventDefault();
        var text = event.dataTransfer.getData('URL') || event.dataTransfer.getData('Text');
        if (!text) return calli.resolve();
        var iri = text.trim().replace(/\?.*/,'');
        resourceLookup(template, iri).then(function(data){
            if (!data) return;
            selectize.addOption(data);
            var value = selectize.getValue();
            if (value instanceof Array)
                selectize.setValue(value.concat(data.value));
            else
                selectize.setValue(data.value);
            return data;
        }).then(undefined, calli.error).then(calli.loading(event.target));
    }

    function resourceLookup(template, iri) {
        var url = template.replace('{iri}', encodeURIComponent(iri));
        return calli.getJSON(url).then(function(json){
            return json.results.bindings[0];
        }).then(function(bindings){
            if (!bindings) return;
            return {
                value: bindings.resource.value,
                text: bindings.label.value,
                email: bindings.email.value,
                comment: bindings.comment ? bindings.comment.value : ''
            };
        });
    }

    function resourceSearch(template, query, callback) {
        if (!query) return callback();
        var url = template.replace('{q}', encodeURIComponent(query));
        calli.getJSON(url).then(function(json){
            return json.results.bindings.map(function(bindings){
                return {
                    value: bindings.resource.value,
                    name: bindings.resource.value.replace(/.*\//,''),
                    text: bindings.label.value,
                    email: bindings.email.value,
                    comment: bindings.comment ? bindings.comment.value : ''
                };
            });
        }).then(callback, function(error){
            callback();
            calli.error(error);
        });
    }

    function createUser(template, selector, label, callback) {
        if (!label) return callback();
        var url = calli.getCallimachusUrl('/auth/invited-users/') + '?create=' + encodeURIComponent($('#invitedUser').prop('href')) + '#' + encodeURIComponent(label);
        calli.createResource(selector, url).then(function(resource){
            return calli.resolve().then(function(){
                if (!resource) return;
                else return resourceLookup(template, resource);
            }).then(function(data){
                if (data) return data;
                else if (resource) return {
                    value: resource,
                    text: resource.replace(/.*\//,'')
                };
            });
        }).then(callback, function(error){
            callback();
            calli.error(error);
        }).then(calli.loading(selector));
    }
});
