// credential-create.js

jQuery(function($){

$('#created').val(new Date().toISOString()).change();
$('#modified').val($('#created').val()).change();

$('form[typeof~="calli:Credential"]').bind('calliRedirect', function(event){
    var password = $('#password').val();
    if (password && event.cause.type == 'calliSubmit') {
        event.preventDefault();
        $.ajax({
            type: 'POST',
            url: event.resource + '?password',
            contentType: 'text/plain',
            data: rstr2b64(str2rstr_utf8(password)),
            xhrFields: calli.withCredentials,
            dataType: "text",
            success: function(url) {
                window.location.replace(url);
            }
        });
    }
});

});
