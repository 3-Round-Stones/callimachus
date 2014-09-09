// permissions.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($) {
    var xhr = $.ajax({type:'GET', url:'?rdftype'});
    calli.resolve(xhr).then(calli.ready).then(function(text) {
        var list = text.split('\r\n');
        var calli = "http://callimachusproject.org/rdf/2009/framework#";
        if (text.indexOf('types/Creatable') < 0) {
            $('.creatable').hide();
        }
        if (jQuery.inArray('http://www.w3.org/2002/07/owl#Class', list) >= 0) {
            $('.class').show();
            $('.private').show();
        } else {
            $('.class').hide();
            var control = xhr.getResponseHeader('Cache-Control');
            if (control && control.indexOf('public') >= 0) {
                $('.private').hide();
            }
        }
        if (jQuery.inArray(calli + 'Composite', list) >= 0) {
            $('.composite').show();
        } else {
            $('.composite').hide();
        }
    }).then(undefined, calli.error);
})(jQuery);

jQuery(function($) {
    $('.party').filter(':empty').each(function(){
        $(this).text($(this).parent().attr('resource'));
    });

    var lookup = $('#party-lookup').prop('href');
    $('#reader,#subscriber,#author,#contributor,#editor,#administrator').each(function(){
        $(this).closest('[dropzone]').on("dragenter dragover dragleave", function(event){
            event.preventDefault();
            return false;
        }).on('drop', dropResourceURL.bind(this, lookup, $(this).selectize({
            load: resourceSearch.bind(this, $('#party-search').prop('href')),
            render: {
                option: renderOption,
                item: renderItem
            }
        })[0].selectize));
    });

    calli.all($('#party-fieldset option').toArray().map(function(option){
        return option.value;
    }).filter(function(value,i,values){
        return values.indexOf(value) == i;
    }).map(function(iri){
        var url = lookup.replace('{iri}', encodeURIComponent(iri));
        return calli.getJSON(url).then(function(json){
            return json.results.bindings[0];
        }).then(function(bindings){
            if (!bindings) return;
            return bindings.comment ? bindings.comment.value : '';
        }).then(function(content){
            var result = {};
            if (content)
                result[iri] = content;
            return result;
        });
    })).then(function(objects){
        return $.extend.apply($, objects);
    }).then(function(contents){
        $('#party-fieldset').popover({
            container: '#party-fieldset',
            selector: '.item',
            placement: 'bottom',
            content: function() {
                return contents[this.getAttribute('data-value')] || '';
            }
        });
    }).then(undefined, calli.error);

    var comparison = calli.copyResourceData('#form');
    $('#form').submit(function(event){
        event.preventDefault();
        var form = this;
        var btn = $(form).find('button[type="submit"]');
        btn.button('loading');
        return calli.resolve(form).then(function(form){
            return calli.copyResourceData(form);
        }).then(function(insertData){
            var action = calli.getFormAction(form);
            return calli.postUpdate(action, comparison, insertData);
        }).then(function(redirect){
            if (window.parent != window) {
                window.parent.postMessage('POST close', '*');
            }
            setTimeout(function(){
                window.location = $('body').attr('resource') + '?view';
            }, 500);
        }, function(error){
            btn.button('reset');
            return calli.error(error);
        });
    });

    function renderItem(data, escape){
        return [
            '<div',
            ' class="item"',
            ' title="' + escape(data.value) + '"',
            data.content ? ' data-content="' + escape(data.content) + '"' : '',
            '>' + escape(data.text) + '</div>'
        ].join('');
    }

    function renderOption(data, escape){
        return [
            '<div><p>' + escape(data.text) + '</p>',
            data.content ? ('<p class="text-info">' + escape(data.content) + '</p>') : '',
            '<a class="text-muted" href="' + escape(data.value) + '" onclick="return false">' + escape(data.value) + '</a>',
            '</div>'
        ].join('\n');
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
                content: bindings.comment ? bindings.comment.value : ''
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
                    content: bindings.comment ? bindings.comment.value : ''
                };
            });
        }).then(callback, function(error){
            callback();
            return calli.error(error);
        });
    }
});
