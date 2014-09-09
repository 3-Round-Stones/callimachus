// concept.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){

    if (window.location.hash.substring(1) && !$('#label').val()){
        $('#label').val(decodeURIComponent(window.location.hash.substring(1))).change()[0].select();
    }

    ['#related', '#narrower'].forEach(function(selector){
        $(selector).closest('[dropzone]').on("dragenter dragover dragleave", function(event){
            event.preventDefault();
            return false;
        }).on('drop', dropConcept.bind(this, $(selector).selectize({
            load: conceptSearch,
            create: window.parent == window && conceptCreate.bind(this, selector),
            render: {
                option: renderConceptOption,
                item: renderConceptItem
            }
        })[0].selectize));
    });

    function renderConceptItem(data, escape){
        return '<div title="' + escape(data.value) + '" onclick="calli.createResource(this, this.title + \'?edit\').then(undefined, calli.error)">' + escape(data.text) + '</div>';
    }

    function renderConceptOption(data, escape){
        return [
            '<div><p>' + escape(data.text) + '</p>',
            data.definition ? ('<p class="text-info">' + escape(data.definition) + '</p>') : '',
            '</div>'
        ].join('\n');
    }

    function dropConcept(selectize, event) {
        event.preventDefault();
        var text = event.dataTransfer.getData('URL') || event.dataTransfer.getData('Text');
        if (!text) return calli.resolve();
        var iri = text.trim().replace(/\?.*/,'');
        return lookupConcept(iri).then(function(data){
            if (!data) return;
            selectize.addOption(data);
            var value = selectize.getValue() || [];
            selectize.setValue(value.concat(data.value));
            return data;
        }).then(undefined, calli.error);
    }

    function lookupConcept(iri) {
        var url = $('#concept-lookup').prop('href').replace('{iri}', encodeURIComponent(iri));
        return calli.getJSON(url).then(function(json){
            return json.results.bindings[0];
        }).then(function(bindings){
            if (!bindings) return;
            return {
                value: bindings.resource.value,
                text: bindings.label.value,
                definition: bindings.definition && bindings.definition.value
            };
        });
    }

    function conceptSearch(query, callback) {
        if (!query) return callback();
        var url = $('#concept-search').prop('href').replace('{q}', encodeURIComponent(query));
        calli.getJSON(url).then(function(json){
            return json.results.bindings.map(function(bindings){
                return {
                    value: bindings.resource.value,
                    text: bindings.label.value,
                    definition: bindings.definition && bindings.definition.value
                };
            });
        }).then(callback, function(error){
            callback();
            return calli.error(error);
        });
    }

    function conceptCreate(selector, label, callback) {
        if (!label) return callback();
        var folder = $('#folder').prop('href') || window.location.pathname;
        var url = folder + '?create=' + encodeURIComponent($('#type').prop('href')) + '#' + encodeURIComponent(label);
        var resource = folder.replace(/\/?$/,'/') + calli.slugify(label);
        calli.headText(resource).then(function(){
            return resource; // already exists
        }, function(xhr) {
            if (xhr.status != 404) return calli.reject(xhr);
            return calli.createResource(selector, url);
        }).then(function(resource){
            return resource && lookupConcept(resource);
        }).then(callback, function(error){
            callback();
            return calli.error(error);
        });
    }
});
