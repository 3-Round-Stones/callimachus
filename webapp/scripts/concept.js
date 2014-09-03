// concept.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){

    if (window.location.hash.substring(1) && !$('#label').val()){
        $('#label').val(window.location.hash.substring(1)).change()[0].select();
    }

    ['#related', '#narrower'].forEach(function(selector){
        $(selector).closest('[dropzone]').on("dragenter dragover dragleave", function(event){
            event.preventDefault();
            return false;
        }).on('drop', dropConcept.bind(this, $(selector).selectize({
            load: conceptSearch,
            create: conceptCreate.bind(this, selector)
        })[0].selectize));
    });

    function dropConcept(selectize, event) {
        event.preventDefault();
        var url = event.dataTransfer.getData('URL') || vent.dataTransfer.getData('Text');
        if (!url) return;
        var iri = url.trim().replace(/\?.*/,'');
        var label = iri.replace(/.*\//,'');
        selectize.addOption({text: label, value: iri});
        selectize.setValue(iri);
    }

    function conceptSearch(query, callback) {
        if (!query) return callback();
        var url = $('#concept-search').prop('href').replace('{q}', encodeURIComponent(query));
        calli.getJSON(url).then(function(json){
            return json.results.bindings.map(function(bindings){
                return {
                    value: bindings.resource.value,
                    text: bindings.label.value
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
        var url = folder + '?create=' + encodeURIComponent($('#type').prop('href')) + '#' + label;
        var resource = folder.replace(/\/?$/,'/') + calli.slugify(label);
        calli.headText(resource).then(function(){
            return resource; // already exists
        }, function(xhr) {
            if (xhr.status != 404) return calli.reject(xhr);
            return calli.createResource(selector, url);
        }).then(function(resource){
            return resource && {
                value: resource,
                text: resource.replace(/.*\//,'')
            };
        }).then(callback, function(error){
            callback();
            return calli.error(error);
        });
    }
});