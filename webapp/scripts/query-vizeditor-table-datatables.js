// query-vizeditor-table-datatables.js
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
    var moduleName = 'table-datatables';
    
    var lib = pLib[moduleName] = {
        
        getConfig: function() {
            var config = pLib.getConfig();
            config.options = {
                'pagination': config.options['pagination'],
                'full-pagination': config.options['full-pagination']
            }
            return config;
        },
        
        renderPreview: function() {
            lib.renderModule(pLib.getConfig());
        },
        
        renderModule: function(config) {
            // viz container and styling
            var container = $('#' + config.vizId);
            container.width('100%').css('height', 'auto').css('overflow', 'auto').html('');         
            // data source
            var queryUrl = location.pathname;
            if (!queryUrl.match(/\.rq$/)) {// context is not the query view but the chart page, use config
                queryUrl = config.query;
            }
            queryUrl += '?results&' + $.param(config.params) + location.search.replace('?view', '&') + '&tqx=out:html';
            container.load(queryUrl + ' table', function() {
                container.find('table').dataTable( {
                    "bPaginate": config.options['pagination'] ? true : false,
                    "sPaginationType": config.options['full-pagination'] ? 'full_numbers' : 'two_button',
                    "bLengthChange": true
                });
            });
        },
        
        createOptionsPane: function() {
            $('#calli-viz-editor .options-pane').each(function() {
                lib.createOptionsMarkup($(this));
                pLib.createParamsMarkup($(this), pLib.config ? pLib.config.params : null);
            });
        },
        
        createOptionsMarkup: function(container) {
            $('<form role="form" class="calli-viz-options" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    pLib.createHiddenOption($(this), 'module', moduleName);
                    pLib.createCheckboxOption($(this), 'pagination', 'Pagination');
                    pLib.createCheckboxOption($(this), 'full-pagination', 'Advanced Pagination');
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
