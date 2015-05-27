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
            this.setAttribute("resource", img[this.value]);
        });
    });
    $('#create').submit(function(event){
        var resource = calli.slugify($('#label').val());
        if ($('#direct').attr('checked')) {
            resource = resource.replace(/\/?$/,'/');
        }
        $('#endpoint').attr('resource',resource);
        calli.submitTurtle(event, resource);
    });

    function updateEndpoints(url, protocol) {
        if (protocol == 'autodetect') {
            return updateEndpoints(url, autodetect(url));
        } else if (protocol == 'sesame') {
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
        return protocol;
    }
    function autodetect(url) {
        var origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
        if (url.indexOf(origin) === 0) return 'callimachus';
        else if (url.match(/.*\/repositories\/.+/)) return 'sesame';
        else if (url.match(/https?:\/\/[^/]+\/[^/]+$/)) return 'stardog';
        else if (url.match(/https?:\/\/[^/]+(\/sparql)?\/$/i)) return 'virtuoso';
        else return 'sparql';
    }
});
