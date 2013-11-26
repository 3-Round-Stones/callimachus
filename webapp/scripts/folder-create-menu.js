// folder-create-menu.js

jQuery(function($) {
    $('#create-menu').one('click', function() {
        $.ajax({
            type: 'GET',
            url: $('#create-menu-json')[0].href + encodeURIComponent(calli.getUserIri()),
            xhrFields: calli.withCredentials,
            dataType: 'json',
            success: function(data) {
                var ul = $('#create-menu-more');
                var section;
                $(data.rows).each(function(){
                    if (section != this[4]) {
                        if (section) {
                            ul.append('<li role="presentation" class="divider"></li>');
                        }
                        section = this[4];
                        var header = $('<li class="dropdown-header"></li>');
                        header.text(section);
                        ul.append(header);
                    }
                    var li = $('<li></li>');
                    var a = $('<a role="menuitem"></a>');
                    a.attr('href', this[0]);
                    a.text(this[1]);
                    if (this[2]) {
                        a.attr('title', this[2]);
                    }
                    if (this[3]) {
                        var img = $('<img class="icon"></img>');
                        img.attr('src', this[3]);
                        a.prepend(' ');
                        a.prepend(img);
                    }
                    li.append(a);
                    ul.append(li);
                });
            }
        });
        return true;
    });
});
