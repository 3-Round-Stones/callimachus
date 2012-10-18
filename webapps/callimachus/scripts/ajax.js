// ajax.js


(function($,jQuery){

if (!window.calli) {
    window.calli = {};
}

window.calli.withCredentials = function(req) {
    try {
        req.withCredentials = true;
    } catch (e) {}
}

window.calli.getText = function(url, success) {
    return jQuery.ajax({
        type: 'GET',
        url: url,
        dataType: 'text',
        beforeSend: calli.withCredentials,
        success: success
    });
}

})(jQuery, jQuery);