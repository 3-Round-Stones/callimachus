
/* query-vizeditor-map-google.js */

(function($) {
    
    var pLib = window.queryviz;
    var moduleName = 'map-google';
    
    var lib = pLib[moduleName] = {
        
        getConfig: function() {
            var config = pLib.getConfig();
            config.options = {
                'display-mode': config.options['display-mode'],
                'region': config.options['region'],
                'resolution': config.options['resolution'],
                'color': config.options['color']
            }
            return config;
        },
        
        renderPreview: function() {
            lib.renderModule(pLib.getConfig());
        },
        
        renderModule: function(config) {
            $.getScript('//www.google.com/jsapi', function() {
                google.load('visualization', '1.0', {packages: ['geochart'], callback: function() {
                    // viz container and styling
                    var container = $('#' + config.vizId);
                    var containerHeight = (window == top) ? $(window).height() / 2 : $(window).height() - 30; // larger in iframe embeds
                    container.width('100%').height(containerHeight).css('overflow', 'hidden').html('');
                    // data source
                    var queryUrl = location.pathname;
                    if (!queryUrl.match(/\.rq$/)) {// context is not the query view but the chart page, use config
                        queryUrl = config.query;
                    }
                    queryUrl += '?results&' + $.param(config.params) + location.search.replace('?view', '&') + '&tqx=out:table';
                    // chart options
                    var options = {
                        width: '100%',
                        displayMode: config.options['display-mode'] || 'auto',
                        region: config.options['region'] || 'world',
                        resolution: config.options['resolution'] || 'countries'
                    };
                    // color
                    if (config.options.color && config.options.color != '#ffffff') {
                        var tweakColor = function(hex, factor) {
                            // hex2rgb
                            var parts = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
                            rgb = { r: parseInt(parts[1], 16), g: parseInt(parts[2], 16), b: parseInt(parts[3], 16) };
                            // alter
                            if (factor > 0) {
                                rgb.r += Math.floor((255 - rgb.r) * factor);
                                rgb.g += Math.floor((255 - rgb.g) * factor);
                                rgb.b += Math.floor((255 - rgb.b) * factor);
                            }
                            else {
                                rgb.r += Math.floor(rgb.r * factor);
                                rgb.g += Math.floor(rgb.g * factor);
                                rgb.b += Math.floor(rgb.b * factor);
                            }
                            // rgb2hex
                            return "#" + ((1 << 24) + (rgb.r << 16) + (rgb.g << 8) + rgb.b).toString(16).slice(1);
                        };
                        options.colorAxis = {colors: [
                            tweakColor(config.options.color, 0.5), 
                            config.options.color, 
                            tweakColor(config.options.color, -0.5)
                        ]};
                    }
                    var chart = new google.visualization.ChartWrapper({
                        chartType: "GeoChart",
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
                $(this).append('<h3>Chart Options</h3>');
                lib.createOptionsMarkup($(this));
                pLib.createParamsMarkup($(this), pLib.config ? pLib.config.params : null);
            });
        },
        
        createOptionsMarkup: function(container) {
            $('<form class="calli-viz-options form-horizontal" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    var container = $(this);
                    pLib.createHiddenOption(container, 'module', moduleName);
                    pLib.createSelectOption(container, 'display-mode', 'Display mode', ['auto', 'regions', 'markers']);
                    pLib.createColorOption(container, 'color', 'Base color', 'Enter a hex value');
                    pLib.createTextOption(container, 'region', 'Region', 'world or identifier', '', '<a href="https://developers.google.com/chart/interactive/docs/gallery/geochart#Data_Format" target="ext">Supported identifiers</a>');
                    pLib.createSelectOption(container, 'resolution', 'Resolution', ['countries', 'provinces', 'metros']);
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
