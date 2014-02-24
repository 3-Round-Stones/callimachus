// query-vizeditor-table-google.js
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
            $('<form role="form" class="calli-viz-options" action="#" method="post"></form>')
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
