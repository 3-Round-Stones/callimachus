// scale-container.js


/**
 * Avoids the appearance of scrollbars in the container:
 * 1) Scales the sidebar's container to at least the height of the (absolutely positioned) sidebar.
 * 2) Adjusts the main container's height so that dropdown menus are always fully visible.
 * 
 */
(function($) {
    
	var lib = {
        
        timeout: null,
        container: null,
        
        getTargetHeight: function() {
            var newHeight = 0;
            var curHeight = parseInt(lib.container.css('min-height'));
            $('.sidebar, .dropdown.open > .dropdown-menu, .dropdown.open .dropdown-submenu > .dropdown-menu').each(function() {
                var h = $(this).outerHeight(true) + $(this).offset().top + 10;
                if (h >= newHeight) {
                    newHeight =  h;
                }
            });
            return newHeight;
        },
        
        scaleContainer: function() {
            try {window.clearTimeout(lib.timeout)} catch(e) {};
            lib.timeout = window.setTimeout(function() {// delay execution to avoid CPU stress on heavy mouse movement
                var curHeight = parseInt(lib.container.css('min-height'));
                var newHeight = lib.getTargetHeight();
                if (newHeight >= curHeight) {
                    lib.container.css('min-height', newHeight);
                }
                else {// smoothly move back to smaller height if possible
                    try {window.clearTimeout(lib.timeout)} catch(e) {};
                    lib.timeout = window.setTimeout(function() {
                        var newHeight = lib.getTargetHeight();// always use fresh targetHeight
                        lib.container.animate({'min-height': newHeight}, 500, lib.scaleContainer);
                    }, 1000);
                }
            }, 100);
        },
        
        init: function() {
            lib.container = $('.navbar + .container');
            $(window).on('resize', lib.scaleContainer);
            $(document).on('DOMNodeInserted', lib.scaleContainer);
            $(document).on('click', 'body, #create-menu, .dropdown, .dropdown-toggle', lib.scaleContainer);
            $(document).on('mouseover mouseout', '.dropdown-submenu', lib.scaleContainer);
			lib.scaleContainer();
		}
	
	};
	
	$(lib.init);	
 	
})(jQuery);
