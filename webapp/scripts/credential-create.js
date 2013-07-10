// credential-create.js

jQuery(function($){

$('form[typeof~="calli:Credential"]').bind('calliRedirect', function(event){
    var password = $('#password').val();
    if (password && event.cause.type == 'calliSubmit') {
        event.preventDefault();
        $.ajax({
            type: 'POST',
            url: event.resource + '?password',
            contentType: 'text/plain',
            data: rstr2b64(str2rstr_utf8(password)),
            beforeSend: calli.withCredentials,
            dataType: "text",
            success: function(url) {
                window.location.replace(url);
            }
        });
    }
});

});