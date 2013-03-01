// put-file.js

jQuery(function($){

$('form[method="PUT"]').each(function(event){
    var form = this;
    var enctype = form.getAttribute("enctype");
    var fileInput = $(form).find('input[type="file"][accept*="' + enctype + '"]');
    if (!enctype || fileInput.length != 1)
        return;
    var etag = null;
    var xhr = $.ajax({
        type: 'HEAD',
        url: calli.getFormAction(form),
        beforeSend: calli.withCredentials,
        success: function() {
            etag = xhr.getResponseHeader('ETag');
        }
    });
    $(form).submit(function(){
        var form = this;
        var enctype = form.getAttribute("enctype");
        if (!enctype)
            return true;
        var fileInput = $(form).find('input[type="file"][accept*="' + enctype + '"]');
        if (fileInput.length != 1 || fileInput[0].files.length != 1)
            return true;
        var file = fileInput[0].files[0];
        var xhr = $.ajax({
            type: form.getAttribute("method"),
            url: calli.getFormAction(form),
            contentType: file.type ? file.type : enctype,
            processData: false,
            data: file,
            beforeSend: function(xhr) {
                if (etag) {
                    xhr.setRequestHeader('If-Match', etag);
                }
                calli.withCredentials(xhr);
            },
            success: function() {
                try {
                    var redirect = null;
                    if (xhr.getResponseHeader('Content-Type') == 'text/uri-list') {
                        redirect = xhr.responseText;
                    }
                    if (!redirect) {
                        redirect = calli.getPageUrl();
                        if (redirect.indexOf('?') > 0) {
                            redirect = redirect.substring(0, redirect.indexOf('?'));
                        }
                    }
                    redirect = redirect + "?view";
                    var event = $.Event("calliRedirect");
                    event.location = redirect;
                    $(form).trigger(event);
                    if (!event.isDefaultPrevented()) {
                        if (window.parent != window && parent.postMessage) {
                            parent.postMessage('PUT src\n\n' + event.location, '*');
                        }
                        window.location.replace(event.location);
                    }
                } catch(e) {
                    throw calli.error(e);
                }
            }
        });
        return false;
    });
});


});