// facebook.js

jQuery(function($){
    if (!$('#label').val()) $('#label').val('Facebook accounts').change();
    if (!$('#comment').val()) $('#comment').val('Sign in with your Facebook account').change();
    if ($('#secret-link').length) $('#add-secret').hide();
    $('[rel="calli:facebookSecret"]').each(function(){
        calli.getText(this.getAttribute('resource')).then(function(text){
            $('#secret').val(text);
        });
    });
    $('#secret').change(function(event){
        if (event.target.value) {
            var url = $('[rel="calli:facebookSecret"]').attr('resource');
            if (url) {
                calli.putText(url, event.target.value).then(undefined, calli.error);
            } else {
                calli.postText("/auth/secrets/?create", event.target.value).then(function(url){
                    $('#secret').before($('<span></span>', {
                        rel: "calli:facebookSecret",
                        resource: url
                    }));
                }, calli.error);
            }
        }
    });
});
