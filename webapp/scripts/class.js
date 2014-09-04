// class.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/


jQuery(function($){
    $('#create').bind('drop', function(event) {
        calli.insertResource(event).then(function(){
            $('link[rel="group"]').each(function(){
                var href = this.href;
                var de = jQuery.Event('drop');
                de.dataTransfer = {getData:function(){return href;}};
                $('#authors').trigger(de);
            });
        });
    });
    $('#view').bind('drop', function(event) {
        calli.insertResource(event).then(function() {
            $('link[rel="viewable"]').each(function(){
                var href = this.href;
                var de = jQuery.Event('drop');
                de.dataTransfer = {getData:function(){return href;}};
                $('#subClassOf').trigger(de);
            });
        });
    });
    $('#edit').bind('drop', function(event) {
        calli.insertResource(event).then(function() {
            $('link[rel="editable"]').each(function(){
                var href = this.href;
                var de = jQuery.Event('drop');
                de.dataTransfer = {getData:function(){return href;}};
                $('#subClassOf').trigger(de);
            });
        });
    });
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

    $('#subClassOf').closest('[dropzone]').on("dragenter dragover dragleave", function(event){
        event.preventDefault();
        return false;
    }).on('drop', dropClassURL.bind(this, $('#subClassOf').selectize({
        load: classSearch,
        render: {
            option: renderClassOption,
            item: renderClassItem
        }
    })[0].selectize));
    $('link[rel="super"]').each(function(){
        var href = this.href;
        var de = jQuery.Event('drop');
        de.dataTransfer = {getData:function(){return href;}};
        $('#subClassOf').trigger(de);
    });

    $('#equivalentClass').closest('[dropzone]').on("dragenter dragover dragleave", function(event){
        event.preventDefault();
        return false;
    }).on('drop', dropEquivalentClassURL.bind(this, $('#equivalentClass').selectize({
        load: classSearch,
        render: {
            option: renderClassOption,
            item: renderClassItem
        }
    })[0].selectize));
    var equivalentURL = unescape(window.location.hash.substring(2));
    if (equivalentURL.length > 0) {
        var de = jQuery.Event('drop');
        de.dataTransfer = {getData:function(){return equivalentURL;}};
        $('#equivalentClass').trigger(de);
    }

    function renderClassItem(data, escape){
        return '<div title="' + escape(data.value) + '">' + escape(data.text) + '</div>';
    }

    function renderClassOption(data, escape){
        return [
            '<div><p>' + escape(data.text) + '</p>',
            data.comment ? ('<p class="text-info">' + escape(data.comment) + '</p>') : '',
            '<a class="text-muted" href="' + escape(data.value) + '" onclick="return false">' + escape(data.value) + '</a>',
            '</div>'
        ].join('\n');
    }

    function dropEquivalentClassURL(selectize, event) {
        return dropClassURL(selectize, event).then(function(data){
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

    function dropClassURL(selectize, event) {
        event.preventDefault();
        var text = event.dataTransfer.getData('URL') || event.dataTransfer.getData('Text');
        if (!text) return calli.resolve();
        var iri = text.trim().replace(/\?.*/,'');
        var url = $('#class-lookup').prop('href').replace('{iri}', encodeURIComponent(iri));
        return calli.getJSON(url).then(function(json){
            return json.results.bindings[0];
        }).then(function(bindings){
            if (!bindings) return;
            return {
                value: bindings.resource.value,
                text: bindings.label.value,
                comment: bindings.comment && bindings.comment.value
            };
        }).then(function(data){
            if (!data) return;
            selectize.addOption(data);
            var value = selectize.getValue() || [];
            selectize.setValue(value.concat(data.value));
            return data;
        }).then(undefined, calli.error);
    }

    function classSearch(query, callback) {
        if (!query) return callback();
        var url = $('#class-search').prop('href').replace('{q}', encodeURIComponent(query));
        calli.getJSON(url).then(function(json){
            return json.results.bindings.map(function(bindings){
                return {
                    value: bindings.resource.value,
                    text: bindings.label.value,
                    comment: bindings.comment.value
                };
            });
        }).then(callback, function(error){
            callback();
            return calli.error(error);
        });
    }
});