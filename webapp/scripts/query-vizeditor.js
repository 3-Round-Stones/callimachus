
/* query-vizeditor.js */

(function($) {
    
	var lib = window.charts = {
        
        googleEditor: null,
        chartContainerIdPrefix: 'chart-',
        chartConfigs: {calli: {}},
        savedCharts: {},

        /**
         * Lets the user choose a file location for saving a file
         */
        getSaveTarget: function(slug, callback) {
            var src = calli.getCallimachusUrl("pages/save-resource-as.html#");
            src += encodeURIComponent(slug.replace(/!/g,''));
            var dialog = window.calli.openDialog(src, 'Save Settings', {
                buttons: {
                    "Save": function() {
                        dialog.postMessage('GET label', '*');
                    },
                    "Cancel": function() {
                        calli.closeDialog(dialog);
                    }
                },
                onmessage: function(event) {
                    if (event.data == 'POST save') {
                        dialog.postMessage('OK\n\n' + event.data, '*');
                        dialog.postMessage('GET label', '*');
                    } else if (event.data.indexOf('OK\n\nGET label\n\n') == 0) {
                        var data = event.data;
                        label = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                        dialog.postMessage('GET url', '*');
                    } else if (event.data.indexOf('OK\n\nGET url\n\n') == 0) {
                        var data = event.data;
                        var src = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                        if (src.indexOf('?') >= 0) {
                            src = src.substring(0, src.indexOf('?'));
                        }
                        var ns = src.replace(/\?.*/,'');
                        if (ns.lastIndexOf('/') != ns.length - 1) {
                            ns += '/';
                        }
                        var local = encodeURI(label).replace(/%20/g,'+');
                        callback(ns, local);
                        calli.closeDialog(dialog);
                    }
                }
            });
            return dialog;
        },
        
        /**
         * Returns an unused HTML id for newly created charts.
         */
        getNextChartContainerId: function() {
            var i = 1;
            while ($('#' + lib.chartContainerIdPrefix + i).length) {
                i++;
            }
            return lib.chartContainerIdPrefix + i;
        },

        /**
         * Loads the Google JS API and calls the passed callback.
         */ 
        loadGoogleApi: function(cb) {
            if (!window.google) {
                $.ajax({
                    url: '//www.google.com/jsapi',
                    dataType: "script",
                    cache: true,
                    success: function() {
                        if (cb) cb.call();
                    }
                });
            }
            else {
                if (cb) cb.call();
            }
        },
        
        /**
         * Loads the Google charteditor code, the associated ChartWrapper, CSS tweaks, and triggers the query list builder.
         */ 
        loadGoogleEditor: function(cb) {
            if (!lib.googleEditor) {
                // editor
                lib.loadGoogleApi(function() {
                    google.load('visualization', '1.0', {packages: ['charteditor'], callback: function() {
                        lib.googleEditor = new google.visualization.ChartEditor();
                        if (!$('#chart-options-container').length) {
                            lib.buildOptionsContainer(cb);
                        }
                    }});
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
            cb.call();
        },
        
        /**
         * Launches the chart editor with a clean chart wrapper and new container ID
         */ 
        initMenu: function() {
            var containerId = lib.getNextChartContainerId();
            // html containers
            if (!$('#' + containerId).length) {
                $('.tab-content').append('<div id="' + containerId + '"></div>');
            }
            // query
            var query = window.location.href.replace(/^([^\?]+).*$/, '$1');
            var queryRelPath = location.pathname;
            // load the editor
            lib.loadGoogleEditor(function() {
                // use a clean wrapper
                lib.googleEditor.chartWrapper = new google.visualization.ChartWrapper({
                    chartType: "Table",
                    dataSourceUrl: queryRelPath + '?results&tqx=out:csv',
                    options: {title: "Callimachus Chart", legend: "none"},
                    containerId: containerId
                });
                // reset the options widget
                $('#chart-options-container .chart-options')
                    .find('a.query').attr('href', query).end()
                    .find('.params').removeClass('active').html('').end()
                    .each(function() {
                        lib.googleEditor.openDialog(lib.googleEditor.chartWrapper, {dataSourceInput: this});
                    })
                ;
                lib.chartConfigs['calli'][containerId] = {query: query, params: {}};
                lib.injectParameters(query, null);
                lib.replaceButtons();
                lib.backupWidget();
            });
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
         * Activates custom chart editor buttons.
         */
        initActions: function() {
            $(document).on('click', 'a.btn.save-chart', lib.onSaveGoogleChart);
            $(document).on('click', 'a.btn.close-chart', lib.onCloseGoogleChart);
        },
        
        /**
         * Closes the google chart editor
         */ 
        onCloseGoogleChart: function(e) {
            e.preventDefault();
            lib.backupWidget();
            lib.googleEditor.closeDialog();
        },        
        
        /**
         * Saves the google chart to an xhtml page
         */ 
        onSaveGoogleChart: function(e) {
            lib.backupWidget();
            lib.googleEditor.chartWrapper = lib.googleEditor.getChartWrapper();
            var containerId = lib.googleEditor.chartWrapper.getContainerId();
            var chartType = lib.googleEditor.chartWrapper.getChartType();
            var slug = location.pathname.replace(/.*\//,'').replace(/\.rq$/, '.xhtml');
            // open save-as dialog
            lib.getSaveTarget(slug, function(path, fname) {
                var title = document.title;
                // config with support for query string parameters and just ?results as URI for re-usability by other queries
                var indent = function(v,i,a){
                    var indent = a.reduce(function(indent,v,j){
                        var m = v.match(/\{|\[/g);
                        return indent + (j<i && m ? m.length : 0);
                    }, 0);
                    var outdent = a.reduce(function(outdent,v,j){
                        var m = v.match(/\}|\]/g);
                        return outdent + (j<i && m ? m.length : 0);
                    }, 0);
                    if (v.indexOf('}') == 0) {
                        outdent++;
                    }
                    return '\n' + new Array(8 + 4 * (indent - outdent)).join(' ') + v;
                };
                var config = lib.googleEditor.chartWrapper.toJSON()
                    .split(/(?=\})/g).map(indent).join('')
                    .split(/,(?=")/g).map(indent).join(',')
                    .split(/\{(?!\})/g).map(function(v,i,a){return v+(i<a.length-1?'{':'')}).map(indent).join('')
                    .replace(/("dataSourceUrl":\s*)"[^\"\?]*\?results[^\"]+"/, '$1 window.location.href.replace("?view","?results")')
                    .trim()
                ;
                var content = '' + 
                    '<?xml version="1.0" encoding="UTF-8" ?>\n' +
                    '<html xmlns="http://www.w3.org/1999/xhtml"\n' +
                    '    xmlns:xsd="http://www.w3.org/2001/XMLSchema#"\n' +
                    '    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"\n' +
                    '    xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">\n' +
                    '<head>\n' +
                    '    <title>' + title + '</title>\n' +
                    '    <link rel="edit-form" href="?edit" />\n' +
                    '    <link rel="comments" href="?discussion" />\n' +
                    '    <link rel="version-history" href="?history" />\n' +
                    '    <link href="?vizeditor" title="Edit visualization" />\n' +
                    '    <link rel="edit-media" href="?" title="Raw SPARQL file" type="application/sparql-query" />\n' +
                    '    <link href="?results" title="Results document" type="application/sparql-results+xml" />\n' +
                    '    <script type="text/javascript" src="https://www.google.com/jsapi"></script>\n' +
                    '    <script type="text/javascript"><![CDATA[\n' +
                    '      google.load("visualization", "1.0");\n' +
                    '      google.setOnLoadCallback(function() {\n' +
                    '        google.visualization.drawChart(' + config + ');\n' +
                    '      });\n' +
                    '    ]]></script>\n' +
                    '</head>\n' +
                    '<body>\n' +
                    '    <div id="' + containerId + '" class="flex" />\n' +
                    '</body>\n' +
                    '</html>' + "\n" + 
                '';
                lib.saveFile(path, fname, content, 'application/xhtml+xml', function(data, status, xhr) {
                    var location = xhr.getResponseHeader('Location');
                    $('<span class="save-success">Saved chart to <a href="' + location + '?view">' + fname + '</a>.</span>')
                        .appendTo('.google-visualization-charteditor-dialog .modal-dialog-buttons')
                        .delay(5000).fadeOut(2000, function() { 
                            $(this).remove();
                        })
                    ;
                    // add pointer to query
                    lib.setQueryTemplate(containerId, location);
                });
            });
        },
        
        /**
         * Saves a file to the file system and asks for confirmation if the file exists already.
         */ 
        saveFile: function(path, fname, content, mimetype, callback) {
            var request = {
                type: "POST",
                url: path + '?contents',
                data: content,
                contentType: mimetype,
                headers: { 'slug': fname },
                success: callback
            }
            // check existence
            $.ajax({
                url: path + fname,
                global: false,
                success: function() {
                    if (confirm('Replace ' + fname + '?')) {
                        $.ajax({
                            type: 'DELETE',
                            url: path + fname + '?',
                            success: function() {
                                $.ajax(request);
                            }
                        })
                    }
                },
                error: function() {
                    $.ajax(request);
                }
            })
        },
        
        /**
         * Injects a @view template annotation into the query file.
         */
        setQueryTemplate: function(containerId, templateLocation) {
            var shortTemplateLocation = $('a').attr('href', templateLocation)[0].pathname;
            $.ajax({// retrieve current page
                url: location.pathname,
                dataType: 'text',
                success: function(data) {
                    data = data
                        .replace(/(\s*)\#\s*\@view\s+[^\n\r]+[\r\n]+/, '$1')    // remove existing annotation
                        .replace(/(PREFIX|SELECT) /i, "# @view " +  shortTemplateLocation + "\n#\n$1 ") // inject annotation in front of query
                        .replace(/[\n\s]+\#\s*([\n\s]+\# \@view)/, '$1')    // remove empty comment line in front of annotation
                    ;
                    $.ajax({// update
                        url: location.pathname + '?',
                        type: 'PUT',
                        data: data,
                        contentType: 'application/sparql-query',
                        success: function() {
                            $('<span class="save-success">Updated the query\'s view template.</span>')
                                .appendTo('.google-visualization-charteditor-dialog .modal-dialog-buttons')
                                .delay(5000).fadeOut(2000, function() { 
                                    $(this).remove();
                                })
                            ;
                            window.location = window.location.href.replace(/\?\w+/,'?view');
                        },
                        error: function() {
                            $('<span class="save-error">Could not set view template</span>')
                                .appendTo('.google-visualization-charteditor-dialog .modal-dialog-buttons')
                                .delay(5000).fadeOut(2000, function() { 
                                    $(this).remove();
                                })
                            ;
                        }
                    });
                }
            });
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
            lib.chartConfigs['calli'][containerId]['params'] = params;
            lib.googleEditor.chartWrapper = wrapper;
            lib.googleEditor.setChartWrapper(wrapper);
            lib.backupWidget();
            setTimeout(function() { e.target.focus();}, 500);// doesn't work if called immediately
        },
        
        /**
         * Returns a streamlined chart configuration object for the given (optional) chart id, optionally as JSON string.
         */
        getChartConfig: function(containerId, asString) {
            var compact = function(obj) {
                var result = null, subObj, type, hasKeys;
                for (var key in obj) {
                    subObj = obj[key];
                    type = typeof subObj;
                    // custom skippable props
                    if (key.match(/^(colors)$/)) subObj = null;
                    if (key.match(/^(useFormatFromData|isDefaultVisualization)$/) && subObj === true) subObj = null;
                    
                    if (type == 'object' && subObj !== null) {
                        subObj = compact(subObj);
                    }
                    if (subObj !== null) {
                        if (!result) {
                            result = key.match(/^[0-9]+$/) ? [] : {};
                        }
                        result[key] = subObj;
                    }
                }
                return result;
            }
            var config = null;
            if (containerId && lib.chartConfigs[containerId]) {
                config = lib.chartConfigs[containerId];
            }
            else if (lib.googleEditor) {
                config = $.parseJSON(lib.googleEditor.chartWrapper.toJSON());
                containerId = lib.googleEditor.chartWrapper.getContainerId();
            }
            config['calli'] = lib.chartConfigs['calli'][containerId] || {};
            config = compact(config);
            return asString ? JSON.stringify(config, null, 2) : config;
        },
        
        /**
         * Renders a chart. Called by the embed code in the page.
         */
        render: function(config) {
            var containerId = config.containerId;
            lib.chartConfigs['calli'][containerId] = config['calli'];
            delete config['calli'];
            lib.chartConfigs[containerId] = config;
            if (!$('#' + containerId).length) {
                $('script.chart[data-chart="' + containerId + '"]').after('<div id="' + containerId +'"></div>');
            }
            lib.savedCharts[containerId] = true;
            // adjust the dataSourceURL if it is relative and the context is not the query
            if (config['dataSourceUrl'].match(/^\?results/) && !location.pathname.match(/\.rq$/)) {
                config['dataSourceUrl'] = lib.chartConfigs['calli'][containerId]['query'] + config['dataSourceUrl'];
            }
            lib.loadGoogleApi(function() {
                google.load("visualization", "1.0", { callback:function() {
                    lib.prepareChartContainer(containerId);
                    (new google.visualization.ChartWrapper(config)).draw();
                }});
            });
        },
        
        /**
         * Styles the chart container
         */ 
        prepareChartContainer: function(containerId) {
            var isIframe = (window != top);
            var h = isIframe ? $(window).height() - 30 : '400'; // detect iframe embed
            $('#' + containerId)
                .height(h)
                .width('100%')
                .css('overflow', 'hidden') // avoid scrollbars
                .html("")
            ;
        },
        
        /**
         * Initializes the chart library.
         */ 
		init: function() {
            lib.initMenu();
            lib.initActions();
		}
	
	};
	
	$(lib.init);	
 	
})(jQuery);
