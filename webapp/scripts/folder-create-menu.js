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
                    if (menu != this[4]) {
                        if (menu) {
                            ul.append('<li class="divider"></li>');
                            ul.append('<li class="dropdown-submenu"><a tabindex="-1" href="javascript:void(0)">More options</a><ul class="dropdown-menu"></ul></li>')
                            ul = ul.find('ul');
                        }
                        menu = this[4];
                        section = this[5];
                    } else if (section != this[5]) {
                        if (section) {
                            ul.append('<li class="divider"></li>');
                        }
                        section = this[5];
                    }
                    var li = $('<li></li>');
                    var a = $('<a></a>');
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
                $('#create-menu-more').find('.dropdown-submenu').each(function() {
                    var sup = $(this).siblings('li').length;
                    var sub = $(this).children('ul').children('li').length;
                    if (sup < sub) {
                        $(this).css('position','static');
                    } else {
                        $(this).parent('ul').addClass('dropup');
                    }
                });
            }
        });
        return true;
    });
});