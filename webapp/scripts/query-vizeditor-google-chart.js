
/* query-vizeditor-google-chart.js */

(function($) {
    
    var pLib = window.queryviz;
    var moduleName = 'google-chart';
    
    var lib = pLib[moduleName] = {
        
        googleEditor: null,
        
        /**
         * Launches the chart editor with a clean chart wrapper and new container ID
         */ 
        openGoogleEditor: function() {
            var vizId = pLib.vizId;
            // query
            var query = window.location.href.replace(/^([^\?]+).*$/, '$1');
            var queryRelPath = pLib.detectQueryPath();
            var queryShortName = queryRelPath.replace(/^.+\/([^\/\.]+)\..+$/, '$1');
            // load the editor
            lib.loadGoogleEditor(function() {
                var config = pLib.config;
                if (!config || config.module != 'google-chart') {
                    config = {
                        chartType: "Table",
                        dataSourceUrl: queryRelPath + '?results',
                        options: {title: "Callimachus Chart", legend: "none"},
                        containerId: vizId
                    };
                }
                // use a clean wrapper
                lib.googleEditor.chartWrapper = new google.visualization.ChartWrapper(config);
                // reset the options widget
                $('#chart-options-container .chart-options')
                    .find('a.query').attr('href', query).end()
                    .find('.params').removeClass('active').html('').end()
                    .each(function() {
                        lib.googleEditor.openDialog(lib.googleEditor.chartWrapper, {dataSourceInput: this});
                        $('.google-visualization-charteditor-dialog .modal-dialog-title-text')
                            .replaceWith('<div class="viz-menu">Visualization: <menu class="visualizations dropdown"></menu></div>')
                        ;
                        pLib.buildVizMenu();
                        $('.google-visualization-charteditor-dialog .viz-menu a[data-module="' + moduleName + '"]').trigger('click');
                    })
                ;
                lib.injectParameters(query, null);
                lib.replaceButtons();
                lib.backupWidget();
                // update the preview
                lib.googleEditor.chartWrapper.draw();
            });
        },
        
        /**
         * Loads the Google charteditor code and the associated ChartWrapper, and triggers the query list builder.
         */ 
        loadGoogleEditor: function(cb) {
            if (!lib.googleEditor) {
                pLib.loadGooglePackage('charteditor', function() {
                    lib.googleEditor = new google.visualization.ChartEditor();
                    if (!$('#chart-options-container').length) {
                        lib.buildOptionsContainer(cb);
                    }
                });
            }
            else {
                if (cb) cb.call();
            }
        },
        
        /**
         * Builds and injects an options widget.
         */ 
        buildOptionsContainer: function(cb) {
            $('#chart-options-container').remove();
            $('body').append('\
                <div id="chart-options-container">\
                    <div class="chart-options">\
                        <a class="query" href=""></a>\
                        <div class="params"></div>\
                    </div>\
                </div>\
            ');
            $('.chart-options').on('change', '.params input', lib.onParameterChange);
            if (cb) cb.call();
        },
        
        /**
         * Replaces the Google editor buttons with controllable ones.
         */ 
        replaceButtons: function() {
            $('.google-visualization-charteditor-dialog .modal-dialog-buttons button[name="ok"]')
                .replaceWith('<a class="save-chart btn btn-info">Save settings...</a>')
            ;
            $('.google-visualization-charteditor-dialog .modal-dialog-buttons button[name="cancel"]')
                .replaceWith(' <a class="close-chart btn">Close</a>')
            ;
        },
        
        /**
         * Creates a clone of the options widget outside of the editor so that it doesn't get deleted.
         */ 
        backupWidget: function() {
            $('#google-visualization-charteditor-custom-data-input .chart-options').each(function() {
                $('#chart-options-container')
                    .html("")
                    .append($(this).clone(true, true))
                ;
            });
        },
        
        /**
         * Adds query parameter input fields (including values, if provided) to the chart options widget.
         */
        injectParameters: function(query, params) {
            var container = $('#google-visualization-charteditor-custom-data-input .chart-options .params');
            // reset params
            container.html("").removeClass('active');
            // fetch query source
            $.ajax({
                url: query,
                success: function(data) {
                    var m = data.match(/\$([a-z0-9_]+)/ig);
                    if (m) {
                        container.addClass('active');
                        var param, added= {}, value;
                        for (var i = 0, imax = m.length; i < imax; i++) {
                            param = m[i].replace('$', '');
                            if (added[param]) continue;
                            added[param] = true;
                            value = params && params[param] ? params[param] : '';
                            container.append('<input type="text" name="' + param + '" placeholder="Enter a ' + param + '…" value="' + value.replace(/"/g, '') + '"/>');
                        }
                    }
                }
            });
        },
        
        /**
         * Updates the chart wrapper if a parameter changes.
         */
        onParameterChange: function(e) {
            var container = $('#google-visualization-charteditor-custom-data-input .chart-options');
            var wrapper = lib.googleEditor.getChartWrapper();
            var containerId = wrapper.getContainerId();
            var query = container.find('.query').attr('href');
            var params = {};
            container.find('.params input').each(function() {
                var param = $(this).attr('name');
                var value = $(this).val();
                if (value.length) {
                    params[param] = value;
                }
            });
            wrapper.setDataSourceUrl(query + '?results&tqx=out:csv' + '&' + $.param(params));
            lib.googleEditor.chartWrapper = wrapper;
            lib.googleEditor.setChartWrapper(wrapper);
            lib.backupWidget();
            setTimeout(function() { e.target.focus();}, 500);// doesn't work if called immediately
        },
        
        /**
         * Activates custom chart editor buttons.
         */
        initActions: function() {
            $(document).on('click', 'a.btn.save-chart', lib.onSaveGoogleChart);
            $(document).on('click', 'a.btn.close-chart', lib.onCloseGoogleChartEditor);
        },
        
        /**
         * Closes the google chart editor
         */ 
        onCloseGoogleChartEditor: function(e) {
            e.preventDefault();
            lib.backupWidget();
            lib.googleEditor.closeDialog();
            pLib.selectedModule = null;
            window.location.replace('#');
            if (window.drawVisualization) {
                try {window.clearTimeout(window.renderVizTo)} catch(e) {};
                window.renderVizTo = window.setTimeout(function() {
                    drawVisualization(window.config);
                }, 250);
            }
        },
        
        /**
         * Saves the google chart to an xhtml page
         */ 
        onSaveGoogleChart: function(e) {
            lib.backupWidget();
            lib.googleEditor.chartWrapper = lib.googleEditor.getChartWrapper();
            var vizId = lib.googleEditor.chartWrapper.getContainerId();
            var chartType = lib.googleEditor.chartWrapper.getChartType();
            var slug = location.pathname.replace(/.*\//,'').replace(/\.rq$/, '.xhtml');
            // open save-as dialog
            pLib.getSaveTarget(slug, function(path, fname) {
                var title = document.title;
                var config = JSON.parse(lib.googleEditor.chartWrapper.toJSON());
                config.module = 'google-chart';
                var configStr = JSON.stringify(config, null, 4)
                    .replace(/("dataSourceUrl":\s*)"[^\"\?]*\?results[^\"]+"/, '$1 window.location.href.replace("?view","?results")')
                ;
                var content = [
                    '<?xml version="1.0" encoding="UTF-8" ?>',
                    '<html xmlns="http://www.w3.org/1999/xhtml"',
                    '   xmlns:xsd="http://www.w3.org/2001/XMLSchema#"',
                    '   xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"',
                    '   xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">',
                    '<head>',
                    '   <title>' + title + '</title>',
                        pLib.getVizHtmlHeadLinks(title),
                    '   <script type="text/javascript" src="https://www.google.com/jsapi"></script>',
                    '   <script type="text/javascript"><![CDATA[',
                    "       // load and render the chart",
                    "       var config = \n" + configStr + ";\n",
                    "       function drawVisualization(config) {",
                    "           google.load('visualization', '1.0', {callback: function() {",
                    "               // viz container and styling",
                    "               var container = $('#" + vizId + "');",
                    "               var containerHeight = (window == top) ? $(window).height() / 2 : $(window).height() - 30; // larger in iframe embeds",
                    "               container.width('100%').height(containerHeight).css('overflow', 'hidden').html('');",
                    "               // draw the chart",
                    "               google.visualization.drawChart(config);",
                    "           }});",
                    "       };",
                    "       drawVisualization(config);",
                    '    ]]></script>',
                    '</head>',
                    '<body>',
                    '   <div class="container">',
                    '      <div id="' + vizId + '"/>',
                    '   </div>',
                    '</body>',
                    '</html>'
                ].join("\n");
                pLib.saveFile(path, fname, content, 'application/xhtml+xml', function(data, status, xhr) {
                    var location = xhr.getResponseHeader('Location');
                    var buttonContainer = $('.google-visualization-charteditor-dialog .modal-dialog-buttons');
                    $('<span class="save-success">Saved chart to <a href="' + location + '?view">' + fname + '</a>.</span>')
                        .appendTo(buttonContainer);
                    ;
                    // add pointer to query
                    pLib.setQueryTemplate(location, buttonContainer);
                });
            });
        },
        
        run: function() {
            lib.openGoogleEditor();
        },
        
        /**
         * Initializes the module.
         */ 
    	init: function() {
            lib.initActions();
		}
	
	};
	
	$(lib.init);	
 	
})(jQuery);

