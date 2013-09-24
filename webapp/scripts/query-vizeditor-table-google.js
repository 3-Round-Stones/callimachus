
/* query-vizeditor-table-google.js */

(function($) {
    
    var pLib = window.queryviz;
    var moduleName = 'table-google';
    
    var lib = pLib[moduleName] = {
    
        getConfig: function() {
            var config = pLib.getConfig();
            config.options = {
                'sortable': config.options['sortable'],
                'striped': config.options['striped'],
                'show-row-number': config.options['show-row-number'],
                'page-size': config.options['page-size']
            }
            return config;
        },

        renderPreview: function() {
            lib.renderModule(pLib.getConfig());
        },
        
        renderModule: function(config) {
            $.getScript('//www.google.com/jsapi', function() {
                google.load('visualization', '1.0', {packages: ['table'], callback: function() {
                    // viz container and styling
                    var container = $('#' + config.vizId);
                    var containerHeight = (window == top) ? $(window).height() / 2 : $(window).height() - 30; // larger in iframe embeds
                    container.width('100%').height(containerHeight).css('overflow', 'hidden').html('');
                    // data source
                    var queryUrl = location.pathname;
                    if (!queryUrl.match(/\.rq$/)) {// context is not the query view but the chart page, use config
                        queryUrl = config.query;
                    }
                    queryUrl += '?results&' + $.param(config.params) + location.search.replace('?view', '&');
                    // chart options
                    var options = {
                        width: '100%',
                        allowHtml: true,
                        alternatingRowStyle: config.options['striped'] ? true : false,
                        sort: config.options['sortable'] ? 'enable' : 'disable',
                        page: config.options['page-size'] && config.options['page-size'] != '-' ? 'enable' : 'disable',
                        pageSize: config.options['page-size'] && config.options['page-size'] != '-' ?  1 * config.options['page-size'] : 10,
                        showRowNumber: config.options['show-row-number'] ? true : false
                    };
                    var chart = new google.visualization.ChartWrapper({
                        chartType: "Table",
                        containerId: config.vizId,
                        dataSourceUrl: queryUrl,
                        options: options
                    });
                    chart.draw();                
                }});
            });
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
                    pLib.createCheckboxOption($(this), 'sortable', 'Sortable');
                    pLib.createCheckboxOption($(this), 'striped', 'Striped');
                    pLib.createCheckboxOption($(this), 'show-row-number', 'Show row number');
                    pLib.createSelectOption($(this), 'page-size', 'Page size', {'-': 'Disable paging', '10': 10, '20': 20, '50': 50});
                })
            ;
        },

        onOptionChange: function() {
            lib.renderPreview();
        },
        
        onParamChange: function() {
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
