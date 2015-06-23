// remote-rdfsource.js

jQuery(function($) {
    var img = $('#protocol option').toArray().reduce(function(img, option){
        img[option.value] = option.getAttribute("resource");
        return img;
    }, {});
    var selectize = $('#protocol').selectize({
        render: {
            option: function(item, escape) {
                if (img[item.value])
                    return '<img src="' + escape(img[item.value]) + '" alt="' + escape(item.text) + '" />';
                else return '<span>' + escape(item.text) + '</span>';
            }
        }
    })[0].selectize;
    $('#url').change(function(event){
        var url = event.target.value;
        var protocol = $('#protocol').val();
        if (url) {
            selectize.setValue(updateEndpoints(url, protocol));
        }
    });
    $('#protocol').change(function(event){
        var url = $('#url').val();
        var protocol = $(event.target).val();
        if (protocol == 'separate') {
            $('#url').attr('disabled', "disabled");
            $('#separate').removeClass('hidden');
        } else {
            $('#url').removeAttr('disabled');
            $('#separate').addClass('hidden');
        }
        if (url) updateEndpoints(url, protocol);
        calli.updateResource(event, 'calli:endpointLogo');
        $('#protocol option').each(function(){
            this.setAttribute("resource", img[this.value] || img.sparql);
        });
    });
    $('#modification').change(function(event){
        if ($('#modification').prop("checked")) {
            $('#updateEndpoint').removeAttr("disabled");
            if (!$('#updateEndpoint').val()) {
                $('#updateEndpoint').val($('#queryEndpoint').val());
            }
        } else {
            $('#updateEndpoint').attr('disabled', "disabled").val('').change();
        }
    });
    $('#create').submit(function(event){
        var slug = calli.slugify($('#label').val());
        if ($('#direct').prop('checked')) {
            slug = slug.replace(/\/?$/,'/');
        }
        var ns = window.location.pathname.replace(/\/?$/, '/');
        var resource = ns + slug;
        $('#endpoint').attr('resource', resource);
        event.preventDefault();
        var form = event.target;
        calli.resolve(form).then(function(form){
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
            return calli.postTurtle(calli.getFormAction(form), data, slug);
        }).then(function(redirect){
            if ($('#protocol').val() != 'autodetect')
                return redirect;
            return calli.getJSON(redirect + '?describe').then(function(json){
                var server = json[0]["http://callimachusproject.org/rdf/2009/framework#endpointSoftware"][0]["@value"];
                if (server.indexOf('Apache-Coyote') >= 0 && $('#url').val().indexOf('/repositories/') >= 0) {
                    return 'sesame';
                } else if (server.indexOf('Stardog') >= 0) {
                    return 'stardog';
                } else if (server.indexOf('Virtuoso') >= 0) {
                    return 'virtuoso';
                } else if (server.indexOf('Callimachus') >= 0) {
                    return 'callimachus';
                } else {
                    return null;
                }
            }).then(function(protocol){
                if (!protocol) return redirect;
                $('#protocol').attr("resource", resource);
                var deleteData = calli.copyResourceData($('#protocol')[0]);
                selectize.setValue(protocol);
                var insertData = calli.copyResourceData($('#protocol')[0]);
                return calli.postUpdate(redirect + "?edit", deleteData, insertData);
            }).then(Promise.resolve.bind(Promise, redirect), function(error){
                if (console) console.log(error);
                return redirect;
            });
        }).then(function(redirect){
            if (window.parent != window && parent.postMessage) {
                parent.postMessage('POST resource\n\n' + redirect, '*');
            }
            window.location.replace(redirect);
        }, calli.loading(event.target, calli.error));
    });

    function updateEndpoints(url, protocol) {
        if (protocol == 'sesame') {
            $('#queryEndpoint').val(url).change();
            $('#updateEndpoint').val(url + '/statements').change();
        } else if (protocol == 'stardog') {
            $('#queryEndpoint').val(url.replace(/\/?$/, '/query')).change();
            $('#updateEndpoint').val(url.replace(/\/?$/, '/update')).change();
        } else if (protocol == 'virtuoso') {
            $('#queryEndpoint').val(url.replace(/(\/sparql)?\/?$/, '/sparql/')).change();
            $('#updateEndpoint').val(url.replace(/(\/sparql)?\/?$/, '/sparql/')).change();
        } else if (protocol != 'separate') {
            $('#queryEndpoint').val(url).change();
            $('#updateEndpoint').val(url).change();
        }
        $('#modification').change();
        return protocol;
    }
});
