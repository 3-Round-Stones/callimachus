
/* query-vizeditor-bar-dimple.js */

(function($) {
    
    var pLib = window.queryviz;
    var moduleName = 'bar-dimple';
    
    var lib = pLib[moduleName] = {
        
        hrefCols: {},
        
        dataFormat: 'd3.csv',

        getConfig: function() {
            var config = pLib.getConfig();
            config.options = {
                'x-axis': config.options['x-axis'],
                'y-axis': config.options['y-axis'],
                'color': config.options['color'],
                'color-axis': config.options['color-axis'],
                'padding-left': config.options['padding-left'],
                'padding-bottom': config.options['padding-bottom']
            }
            return config;
        },

        renderPreview: function() {
            lib.renderModule(pLib.getConfig());
        },
        
        renderModule: function(config) {
            // viz container and styling
            var container = $('#' + config.vizId);
            var containerHeight = (window == top) ? $(window).height() / 2 : $(window).height() - 30; // larger in iframe embeds
            container.width('100%').height(containerHeight).css('overflow', 'hidden').html('');
            // run the query and fetch the data
            var queryUrl = location.pathname;
            if (!queryUrl.match(/\.rq$/)) {// context is not the query view but the chart page, use config
                queryUrl = config.query;
            }
            queryUrl += '?results&' + $.param(config.params) + location.search.replace('?view', '&') + '&tqx=out:csv';
            d3.csv(queryUrl, function(errorXhr, data) {
                if (errorXhr) {
                    container.html('<div class="alert alert-error">Could not load data. ' + errorXhr.statusText + '</div>');
                }
                else {
                    var svg = dimple.newSvg(container[0], container.width(), container.height() - 10);
                    var viz = new dimple.chart(svg, data);
                    var x, y;
                    // axes
                    if (config.options['x-axis'] && config.options['x-axis'] != '-') {
                        x = viz.addCategoryAxis('x', config.options['x-axis']);
                    }
                    if (config.options['y-axis'] && config.options['y-axis'] != '-') {
                        y = viz.addMeasureAxis('y', config.options['y-axis']);
                    }
                    if (!x || !y) return container.html('Please specify the x and y axes.');
                    // color
                    if (config.options.color && config.options.color != '#ffffff') {
                        viz.defaultColors.unshift(new dimple.color(config.options.color));
                        if (config.options['color-axis']) {
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
                            viz.addColorAxis(config.options['y-axis'], [
                                tweakColor(config.options.color, 0.5), 
                                config.options.color, 
                                tweakColor(config.options.color, -0.5)
                            ]);
                        }
                    }
                    viz.setBounds(50, 10, container.width() - 60, container.height() - 20);
                    // padding left
                    if (config.options['padding-left']) {
                        viz.x += parseInt(config.options['padding-left']);
                        viz.width -= parseInt(config.options['padding-left']);
                    }
                    // padding bottom
                    if (config.options['padding-bottom']) {
                        viz.height -= parseInt(config.options['padding-bottom']);
                    }
                    // series
                    var series = viz.addSeries(null, dimple.plot.bar);
                    // draw
                    viz.draw();
                    // tweak axis labels
                    $(container).find('g.axis g.tick text').each(function() {
                        var label = $.trim($(this).text())
                            .replace(/^.+[\/\#]([^\/\#]+)/, '$1') // uri noise;
                            .replace(/([a-z])([A-Z])/g, '$1 $2')  // de-camelcase
                        ;
                        $(this).text(label.charAt(0).toUpperCase() + label.slice(1)); // uppercase first
                    });
                    container.hide().fadeIn(350);
                }
            });          
        },
        
        createOptionsPane: function() {
            pLib.loadData(function() {
                $('#calli-viz-editor .options-pane').each(function() {
                    $(this).append('<h3>Chart Options</h3>');
                    lib.createOptionsMarkup($(this));
                    pLib.createParamsMarkup($(this), pLib.config ? pLib.config.params : null);
                });
            });
        },
        
        createOptionsMarkup: function(container) {
            var columns = lib.getColumns();
            columns.unshift('-');
            $('<form class="calli-viz-options form-horizontal" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    var container = $(this);
                    pLib.createHiddenOption(container, 'module', moduleName);
                    pLib.createSelectOption(container, 'x-axis', 'X Axis', columns);
                    pLib.createSelectOption(container, 'y-axis', 'Y Axis', columns);
                    pLib.createTextOption(container, 'padding-left', 'Left space', 'Enter a pixel value'); 
                    pLib.createTextOption(container, 'padding-bottom', 'Bottom space', 'Enter a pixel value'); 
                    pLib.createColorOption(container, 'color', 'Color', 'Enter a hex value'); 
                    pLib.createCheckboxOption(container, 'color-axis', 'Graded colors');
                })
            ;
        },
        
        getColumns: function() {
            var result = [];
            $.each(pLib.data[0], function(index, val) {
                result.push(index);
            });
            return result;
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
