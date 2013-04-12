// folder-create-menu.js

jQuery(function($) {
    $('#create-menu').one('click', function() {
        $.ajax({
            type: 'GET',
            url: $('#create-menu-json')[0].href,
            beforeSend: calli.withCredentials,
            dataType: 'json',
            success: function(data) {
                var ul = $('#create-menu-more');
                var menu;
                var section;
                $(data.rows).each(function(){
                    if (menu != this[3]) {
                        if (menu) {
                            ul.append('<li class="divider"></li>');
                            ul.append('<li class="dropdown-submenu dropup"><a tabindex="-1" href="javascript:void(0)">More options</a><ul class="dropdown-menu"></ul></li>')
                            ul = ul.find('ul');
                        }
                        menu = this[3];
                        section = this[4];
                    } else if (section != this[4]) {
                        if (section) {
                            ul.append('<li class="divider"></li>');
                        }
                        section = this[4];
                    }
                    var li = $('<li></li>');
                    var a = $('<a></a>');
                    a.attr('href', this[0]);
                    a.text(this[1]);
                    if (this[2]) {
                        var img = $('<img class="icon"></img>');
                        img.attr('src', this[2]);
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