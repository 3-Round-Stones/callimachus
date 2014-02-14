// query-vizeditor-pie-nvd3.js
/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

(function($) {
    
    var pLib = window.queryviz;
    var moduleName = 'pie-nvd3';
    
    var lib = pLib[moduleName] = {
        
        dataFormat: 'd3.csv',
        
        getConfig: function() {
            var config = pLib.getConfig();
            config.options = {
                'x-axis': config.options['x-axis'],
                'y-axis': config.options['y-axis'],
                'labels': config.options['labels'],
                'legend': config.options['legend'],
                'donut': config.options['donut']
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
            nv.dev = false; // disable debug messages
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
                    var keyCol, valCol;
                    // axes
                    if (config.options['x-axis'] && config.options['x-axis'] != '-') {
                        keyCol = config.options['x-axis'];
                    }
                    if (config.options['y-axis'] && config.options['y-axis'] != '-') {
                        valCol = config.options['y-axis'];
                    }
                    if (!keyCol || !valCol) return container.html('Please specify the key and value columns.');
                    // add svg element
                    var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                    container[0].appendChild(svg);
                    nv.addGraph(function() {
                        var chart = nv.models.pieChart()
                            .x(function(row) { return row[keyCol]; })
                            .y(function(row) { return row[valCol]; })
                            .showLabels(config.options['labels'] ? true : false)
                            .showLegend(config.options['legend'] ? true : false)
                            .donut(config.options['donut'] ? true : false)
                        ;
                        d3.select(svg)
                            .datum(data)
                            .transition().duration(1200)
                            .call(chart)
                        ;
                        nv.utils.windowResize(chart.update);
                        container.hide().fadeIn(350);
                        return chart;
                    });
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
            $('<form role="form" class="calli-viz-options" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    var container = $(this);
                    pLib.createHiddenOption(container, 'module', moduleName);
                    pLib.createSelectOption(container, 'x-axis', 'Key column', columns);
                    pLib.createSelectOption(container, 'y-axis', 'Value column', columns);
                    pLib.createCheckboxOption(container, 'labels', 'Show labels');
                    pLib.createCheckboxOption(container, 'legend', 'Show legend');
                    pLib.createCheckboxOption(container, 'donut', 'Donut');
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
