
/* query-vizeditor-table-bootstrap.js */

(function($) {
    
    var pLib = window.queryviz;
    var moduleName = 'table-bootstrap';
    
    var lib = pLib[moduleName] = {
    
        hrefCols: {},
        
        dataFormat: 'xml',
        
        getConfig: function() {
            var config = pLib.getConfig();
            config.options = {
                'striped': config.options['striped'],
                'bordered': config.options['bordered'],
                'condensed': config.options['condensed']
            }
            return config;
        },

        renderPreview: function() {
            lib.renderModule(pLib.getConfig());
        },
        
        renderModule: function(config) {
            var cls = 'table'
                + (config.options['bordered'] ? ' table-bordered' : '')
                + (config.options['striped'] ? ' table-striped' : '')
                + (config.options['condensed'] ? ' table-condensed' : '')
            ;
            // viz container and styling
            var container = $('#' + config.vizId);
            container.width('100%').css('height', 'auto').css('overflow', 'auto').html('');
            // data source
            var queryUrl = location.pathname;
            if (!queryUrl.match(/\.rq$/)) {// context is not the query view but the chart page, use config
                queryUrl = config.query;
            }
            queryUrl += '?results&' + $.param(config.params) + location.search.replace('?view', '&') + '&tqx=out:html';
            container.load(queryUrl, function() {
                container.find(' > table').addClass(cls);
            })
        },
        
        createOptionsPane: function() {
            $('#calli-viz-editor .options-pane').each(function() {
                $(this).append('<h3>Table Options</h3>');
                lib.createOptionsMarkup($(this));
                pLib.createParamsMarkup($(this), pLib.config ? pLib.config.params : null);
            });
        },
        
        createOptionsMarkup: function(container) {
            $('<form class="calli-viz-options form-horizontal" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    pLib.createHiddenOption($(this), 'module', moduleName);
                    pLib.createCheckboxOption($(this), 'striped', 'Striped');
                    pLib.createCheckboxOption($(this), 'bordered', 'Bordered');
                    pLib.createCheckboxOption($(this), 'condensed', 'Condensed');
                })
            ;
        },

        onOptionChange: function() {
            lib.renderPreview();
        },
        
        onParamChange: function() {
            pLib.resetData();
            lib.renderPreview();
        },
        
        run: function() {
            lib.createOptionsPane();
            lib.renderPreview();
        },
        
        /**
         * Initializes the module.
         */ 
		init: function() {
		}
	
	};
	
	$(lib.init);	
 	
})(jQuery);
