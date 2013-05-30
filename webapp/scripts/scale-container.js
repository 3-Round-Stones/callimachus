// scale-container.js

(function($) {
    
	var lib = {
        
        /**
         * Scales the sidebar's container to at least the height of the (absolutely positioned) sidebar.
         * Avoids the appearance of scrollbars in the container.
         */ 
        scaleContainer: function() {
            $('.sidebar').each(function() {
                var container = $(this).parents('.container');
                var currentHeight = parseInt(container.css('min-height'));
                var sidebarHeight = $(this).outerHeight(true) + $(this).position().top + 10;
                if (currentHeight < sidebarHeight) {
                    container.css('min-height', sidebarHeight);
                }
            })
        },
        
        init: function() {
            $(window).on('resize', lib.scaleContainer);
            $(document).on('DOMNodeInserted', lib.scaleContainer);
			lib.scaleContainer();
		}
	
	};
	
	$(lib.init);	
 	
})(jQuery);
