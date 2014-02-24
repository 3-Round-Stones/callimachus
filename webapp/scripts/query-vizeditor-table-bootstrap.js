// query-vizeditor-table-bootstrap.js
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
                + (config.options['hover'] ? ' table-hover' : '')
                + (config.options['condensed'] ? ' table-condensed' : '')
            ;
            // viz container and styling
            var container = $('#' + config.vizId);
            // data source
            var queryUrl = location.pathname;
            if (!queryUrl.match(/\.rq$/)) {// context is not the query view but the chart page, use config
                queryUrl = config.query;
            }
            queryUrl += '?results&' + $.param(config.params) + location.search.replace('?view', '&') + '&tqx=out:html';
            container.load(queryUrl + ' table', function() {
                $(this).find('table').addClass(cls);
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
            $('<form rol="form" class="calli-viz-options" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    pLib.createHiddenOption($(this), 'module', moduleName);
                    pLib.createCheckboxOption($(this), 'striped', 'Striped');
                    pLib.createCheckboxOption($(this), 'hover', 'Hover');
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
