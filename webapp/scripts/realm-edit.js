// realm-edit.js
/*
   Copyright (c) 2015 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){
    $('#pattern').change(function(event){
        $('span.pattern').remove();
        var type = $('#type').prop('value');
        if (event.target.value && type) {
            $(event.target).parent().append($('<span/>', {
                "class": "pattern",
                property: type,
                content: event.target.value
            }));
        }
    });
    $('#type').change(function(){
        $('#pattern').change();
    });
    $('span.pattern[property]').each(function(){
        if (this.getAttribute('content')) {
            $('#pattern').val(this.getAttribute('content'));
            $('#type').val(this.getAttribute('property'));
        }
    });
    $('#type').change();
    $('#delete').click(function(event) {
        if (!confirm("Are you sure you want to delete this folder and all the contents of this folder?"))
            return false;
        calli.deleteText('?archive').then(function() {
            event.message = '';
            calli.deleteResource(event);
        }).then(undefined, calli.error);
    });
    $('#layout').on("dragenter dragover dragleave", function(event){
        event.preventDefault();
        return false;
    }).on("drop", function(event){
        event.preventDefault();
        var url = event.dataTransfer.getData('URL') || vent.dataTransfer.getData('Text');
        if (!url) return;
        var iri = url.trim().replace(/\?.*/,'');
        var label = decodeURI(iri.replace(/.*\//,'').replace(/\+/,' ').replace(/\.xq$/,''));
        $('#layout select').append($('<option/>', {value: iri}).text(label)).val(iri).change();
    });
    $('#layout select option').filter(function(){
        return !this.getAttribute('value');
    }).attr('value', function(){
        return this.getAttribute('resource');
    });
    var defaultLayout = $('#layout select').val() || $('#layout-sample').prop('href');
    $('#layout select').selectize({
        preload: "focus",
        load: function(query, callback) {
            var url = $('#realm-layout-list').prop('href');
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
        },
        create: function(label, callback) {
            calli.promptForNewResource($('body').attr('resource'), calli.slugify(label).replace(/(.xq)?$/,'.xq')).then(function(hash){
                if (!hash) return undefined;
                return calli.resolve(defaultLayout).then(function(url){
                    return calli.getText(url);
                }).then(function(text){
                    var action = hash.container + '?create=' + encodeURIComponent($('#XQuery').prop('href'));
                    return calli.postText(action, text, 'application/xquery', {Slug: hash.slug});
                }).then(function(resource){
                    return calli.createResource({target: $('#layout')[0]}, resource + '?edit');
                }).then(function(resource){
                    return resource && {
                        value: resource,
                        text: decodeURI(hash.slug.replace(/\+/,' ').replace(/\.xq$/, ''))
                    };
                });
            }).then(callback, function(error){
                callback();
                return calli.error(error);
            });
        }
    });
    $('#form').submit(calli.submitUpdate.bind(calli, calli.copyResourceData('#form')));
});