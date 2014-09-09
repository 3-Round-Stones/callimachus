// class.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/


jQuery(function($){
    if ($('#subClassOf').find('[resource$="Composite"]').length) {
        $('#composite').attr('checked','checked');
    }
    $('#composite').click(function(){
        $('link[rel="composite"]').each(function(){
            var composite = $('#subClassOf').find('[resource$="' + this.href + '"]');
            if ($('#composite').is(':checked')) {
                if (!composite.length) {
                    var href = this.href;
                    var de = jQuery.Event('drop');
                    de.dataTransfer = {getData:function(){return href;}};
                    $('#subClassOf').trigger(de);
                }
            } else {
                composite.remove();
            }
        });
    });

    $('#authors').closest('[dropzone]').on("dragenter dragover dragleave", function(event){
        event.preventDefault();
        return false;
    }).on('drop', dropResourceURL.bind(this, $('#party-lookup').prop('href'), $('#authors').selectize({
        load: resourceSearch.bind(this, $('#party-search').prop('href')),
        render: {
            option: renderOption,
            item: renderItem
        }
    }).prop('selectize')));

    $('#subClassOf').closest('[dropzone]').on("dragenter dragover dragleave", function(event){
        event.preventDefault();
        return false;
    }).on('drop', dropResourceURL.bind(this, $('#class-lookup').prop('href'), $('#subClassOf').selectize({
        load: resourceSearch.bind(this, $('#class-search').prop('href')),
        render: {
            option: renderOption,
            item: renderItem
        }
    }).prop('selectize')));
    $('link[rel="super"]').each(function(){
        var href = this.href;
        var de = jQuery.Event('drop');
        de.dataTransfer = {getData:function(){return href;}};
        $('#subClassOf').trigger(de);
    });

    $('#equivalentClass').closest('[dropzone]').on("dragenter dragover dragleave", function(event){
        event.preventDefault();
        return false;
    }).on('drop', dropEquivalentClassURL.bind(this, $('#class-lookup').prop('href'), $('#equivalentClass').selectize({
        load: resourceSearch.bind(this, $('#class-search').prop('href')),
        render: {
            option: renderOption,
            item: renderItem
        }
    }).prop('selectize')));
    var equivalentURL = unescape(window.location.hash.substring(2));
    if (equivalentURL.length > 0) {
        var de = jQuery.Event('drop');
        de.dataTransfer = {getData:function(){return equivalentURL;}};
        $('#equivalentClass').trigger(de);
    }

    ['create', 'view', 'edit'].forEach(function(pragma){
        var selector = '#' + pragma;
        $(selector).closest('[dropzone]').on("dragenter dragover dragleave", function(event){
            event.preventDefault();
            return false;
        }).on('drop', dropResourceURL.bind(this, $('#page-lookup').prop('href'), $(selector).selectize({
            load: resourceSearch.bind(this, $('#page-search').prop('href')),
            create: createTemplate.bind(this, $('#sample-' + pragma).prop('href'), selector),
            render: {
                option: renderOption,
                item: renderEditItem
            }
        }).prop('selectize')));
    });

    function renderItem(data, escape){
        return '<div title="' + escape(data.value) + '" onclick="calli.selectResource(this, this.title)">' + escape(data.text) + '</div>';
    }

    function renderEditItem(data, escape){
        return '<div title="' + escape(data.value) + '" onclick="calli.selectResource(this, this.title + \'?edit\')">' + escape(data.text) + '</div>';
    }

    function renderOption(data, escape){
        return [
            '<div><p>' + escape(data.text) + '</p>',
            data.comment ? ('<p class="text-info">' + escape(data.comment) + '</p>') : '',
            '<a class="text-muted" href="' + escape(data.value) + '" onclick="return false">' + escape(data.value) + '</a>',
            '</div>'
        ].join('\n');
    }

    function dropEquivalentClassURL(template, selectize, event) {
        return dropResourceURL(template, selectize, event).then(function(data){
            if (data && !$('#label').val()) {
                $('#label').val(data.text);
                $('#label').change();
            }
            if (data && data.comment && !$('#comment').val()) {
                $('#comment').val(data.comment);
                $('#comment').change();
            }
        });
    }

    function dropResourceURL(template, selectize, event) {
        event.preventDefault();
        var text = event.dataTransfer.getData('URL') || event.dataTransfer.getData('Text');
        if (!text) return calli.resolve();
        var iri = text.trim().replace(/\?.*/,'');
        var url = template.replace('{iri}', encodeURIComponent(iri));
        return calli.getJSON(url).then(function(json){
            return json.results.bindings[0];
        }).then(function(bindings){
            if (!bindings) return;
            return {
                value: bindings.resource.value,
                text: bindings.label.value,
                comment: bindings.comment ? bindings.comment.value : ''
            };
        }).then(function(data){
            if (!data) return;
            selectize.addOption(data);
            var value = selectize.getValue() || [];
            selectize.setValue(value.concat(data.value));
            return data;
        }).then(undefined, calli.error);
    }

    function resourceSearch(template, query, callback) {
        if (!query) return callback();
        var url = template.replace('{q}', encodeURIComponent(query));
        calli.getJSON(url).then(function(json){
            return json.results.bindings.map(function(bindings){
                return {
                    value: bindings.resource.value,
                    text: bindings.label.value,
                    comment: bindings.comment ? bindings.comment.value : ''
                };
            });
        }).then(callback, function(error){
            callback();
            return calli.error(error);
        });
    }

    function createTemplate(template, selector, label, callback) {
        if (!label) return callback();
        var url = './?create=' + encodeURIComponent($('#page').prop('href')) + '#' + encodeURIComponent(label) + '!' + template;
        return calli.createResource(selector, url).then(function(resource){
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