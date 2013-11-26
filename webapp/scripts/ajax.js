// ajax.js


(function($,jQuery){

if (!window.calli) {
    window.calli = {};
}

window.calli.withCredentials = {
  withCredentials: true
};

window.calli.getText = function(url, success) {
    return jQuery.ajax({
        type: 'GET',
        url: url,
        dataType: 'text',
        xhrFields: calli.withCredentials,
        success: success
    });
}

})(jQuery, jQuery);
