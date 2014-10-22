// query-vizeditor.js
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
    
    if (!$.ajax) return; // don't initialize if jQuery is not defined
    
    var lib = window.queryviz = {
        
        modules: {
            'table': {
                'label': 'Tables',
                'subModules': {
                    'table-bootstrap': 'Table (Bootstrap)',
                    'table-datatables': 'Table (DataTables)',
                    'table-google': 'Table (Google Charts)'
                }
            },
            'axis': {
                'label': 'Axis Charts',
                'subModules': {
                    'bar-dimple': 'Bar Chart (Dimple)',
                    'line-dimple': 'Line Chart (Dimple)',
                    'area-dimple': 'Area Chart (Dimple)'
                }
            },
            'info': {
                'label': 'Infographics',
                'subModules': {
                    'pie-nvd3': 'Pie Chart (nvd3)',
                    'pie-google': 'Pie Chart (Google Charts)',
                    'google-chart': 'More... (Google Charts)'
                }
            }
        },
        selectedModule: null,
        editorInitialized: false,
        vizId: 'visualization',
        data: null,
        config: null,
        loaded: {},

        /**
         * Extends the Menu with an editor launcher
         */ 
        initMenu: function() {
            var onpopstate = function(event) {
                if ("#vizeditor" == window.location.hash && !$('#calli-viz-editor').length) {
                    lib.openEditor(event);
                }
            };
            $(window).bind('hashchange', onpopstate);
            onpopstate();
        },
        
        initEditor: function() {
            if (lib.editorInitialized) return;
            // editor position
        	$(window).on('resize', lib.positionEditor);
            // editor buttons
            $(document)
                .on('click', '#calli-viz-editor a.btn.save-viz', lib.saveVisualization)
                .on('click', '#calli-viz-editor a.close-editor', lib.closeEditor)
                .on('change', '#calli-viz-editor .options-pane input, #calli-viz-editor .options-pane select', lib.onOptionChange)
                .on('submit', '#calli-viz-editor form.calli-viz-options', lib.onOptionChange)
                .on('submit', '#calli-viz-editor form.calli-viz-params', lib.onParamChange)
                .on('queryviz.option-changed', '#calli-viz-editor', function(e) {lib[lib.selectedModule].onOptionChange.call(this, e)})
                .on('queryviz.param-changed', '#calli-viz-editor', function(e) {lib[lib.selectedModule].onParamChange.call(this, e)})
            ;
            lib.editorInitialized = true;
        },
        
        maxZIndex: function(self) {
            var result = 1;
            $('div, header, section, footer').each(function() {
                if ($(this).is(self)) return; // skip self
                var z = parseInt($(this).css("z-index"), 10);
                if (z >= result) {
                    result = z + 1;
                }
            });
            return result;
        },
        
        detectQueryPath: function() {
            var result = location.pathname;
            if (!result.match(/\.rq$/) && lib.config) {// context is not the query view, try config provided by embed code
                result = lib.config.query;
            }
            return result;
        },
        
        /**
         * Launches the visualization editor
         */ 
        openEditor: function(e) {
            if(e) e.preventDefault();
            lib.initEditor();
            // query
            var queryRelPath = lib.detectQueryPath();
            var queryShortName = queryRelPath.replace(/^.+\/([^\/\.]+)\..+$/, '$1');
            // dialog
            var html = [
                '<div id="calli-viz-editor" data-viz-id="' + lib.vizId + '" data-query="' + queryRelPath + '" class="panel panel-info">',
                    // menu
                    '<div class="viz-menu panel-heading">Visualization: <menu class="visualizations dropdown"></menu></div>',
                    // options pane
                    '<div class="options-pane panel-body"></div>',
                    // buttons
                    '<div class="form-group form-actions panel-footer">',
                        '<a type="button" class="save-viz btn btn-info">Save settings...</a>',
                        ' <a type="button" class="close-editor btn btn-default">Close</a>',
                    '</div>',
                '</div>'
            ].join("\n");
            $('body')
                // editor
                .find('#calli-viz-editor').remove().end()
                .append(html)
                // disable scrolling
                //.css('height', '100%')
                //.css('overflow', 'hidden')
                // make transparent until positioned
                .find('#calli-viz-editor')
                    .css('opacity', 0)
                    .end()
            ;
            lib.positionEditor();
            window.setTimeout(lib.positionEditor, 500);
            lib.buildVizMenu();
            if (lib.selectedModule) {
                $('#calli-viz-editor .viz-menu a[data-module="' + lib.selectedModule + '"]').trigger('click');
            }
            else if (lib.config && lib.config.module) {
                $('#calli-viz-editor .viz-menu a[data-module="' + lib.config.module + '"]').trigger('click');
            }
            else {
                $('#calli-viz-editor .viz-menu a[data-module]').first().trigger('click');
            }
        },
        
        positionEditor: function() {
            try { window.clearTimeout(lib.queryvizPositionTO) } catch(e) {}
            lib.queryvizPositionTO = window.setTimeout(function() {
                $('#calli-viz-editor-bg').css('z-index', lib.maxZIndex($('#calli-viz-editor-bg')));
            	$('#calli-viz-editor')
                    .css('z-index', lib.maxZIndex($('#calli-viz-editor')))
                    .css({
                        left: Math.max(8, ($(window).width() - $('#calli-viz-editor').width()) / 2),
                        top: Math.max(8, ($(window).height() - $('#calli-viz-editor').height()) / 2),
                        opacity: 1
                    })
                ;
            }, 100)
        },
        
        getConfig: function() {
            return {
                module: lib.selectedModule,
                vizId: lib.vizId,
                query: $('#calli-viz-editor').attr('data-query'),
                options: lib.getOptions(lib.vizId),
                params: lib.getParams()
            };
        },

        closeEditor: function(e) {
            if (e) e.preventDefault();
            $('body')
                .find('#calli-viz-editor').remove().end()
                .css('height', '100%')
                .css('overflow', 'auto')
            ;
            lib.selectedModule = null;
            window.location.replace('#');
        },
        
        buildVizMenu: function() {
            $('.viz-menu .visualizations').append([
                '<a class="dropdown-toggle" data-toggle="dropdown" href="#"><span class="selected-module"></span><b class="caret"></b></a>',
                '<ul role="menu" class="dropdown-menu"></ul>'
            ].join("\n"))
                .find(' > ul.dropdown-menu').each(function() {
                    var container = $(this);
                    var label;
                    for (var module in lib.modules) {
                        if (container.children().length) {
                            container.append('<li class="visualization divider"></li>');
                        }
                        if (typeof lib.modules[module] == 'string') {
                            label = lib.modules[module];
                            container.append([
                                '<li class="visualization ' + module + '-module">',
                                '   <a role="menuitem" data-module="' + module + '" href="#">' + label + '</a>',
                                '</li>'
                            ].join("\n"));
                        }
                        else {
                            label = lib.modules[module]['label'];
                            container.append('<li class="visualization ' + module + '-module dropdown-header">' + label + '</li>');
                            var subModules = lib.modules[module]['subModules'];
                            for (var subModule in subModules) {
                                label = subModules[subModule];
                                $(this).append([
                                    '<li class="visualization ' + subModule + '-module">',
                                    '   <a role="menuitem" data-module="' + subModule + '" href="#">' + label + '</a>',
                                    '</li>'
                                ].join("\n"));
                            }
                        }
                    }
                    // activate drop-down links and select the currently active or alternatively the first module in the list
                    container.on('click', 'a[data-module]', lib.runModule);
                })
            ;
        },
        
        /**
         * Loads a sub-module and calls the passed callback.
         */ 
        loadModule: function(module, cb) {
            if (!lib[module]) {
                lib.loadScript(calli.getCallimachusUrl('scripts/query-vizeditor-' + module + '.js'), cb)
            }
            else {
                if (cb) cb.call();
            }
        },
        
        runModule:function(e) {
            e.preventDefault();
            var prevModule = lib.selectedModule;
            var newModule = $(this).attr('data-module');
            var moduleLabel = $(this).html();
            // set the selected label
            $('.viz-menu .visualizations a.dropdown-toggle span.selected-module').html(moduleLabel);
            if (prevModule == newModule) return;
            $('#calli-viz-editor .options-pane').html('');
            lib.resetData();
            // handle switching between bespoke and google editor
            if (prevModule == 'google-chart') {
                lib['google-chart'].onCloseGoogleChartEditor(e);// close google editor
                lib.selectedModule = newModule;
                lib.openEditor(e);// re-open bespoke editor
            }
            if (newModule == 'google-chart') {
                lib.closeEditor(e);// close bespoke editor
            }
            // load and run the new module
            lib.selectedModule = newModule;
            lib.loadModule(newModule, function() {
                lib[newModule].run();
                lib.positionEditor();
            });
        },
        
        loadData: function(cb, force) {
            if (lib.data && !force) {
                if (cb) cb(lib.data);
            }
            else {
                var url = lib.detectQueryPath() + '?results' + location.search.replace(/^\?(view\&?)?/, '') + '&' + $.param(lib.getParams());
                var moduleLib = lib[lib.selectedModule];
                // xml
                if (moduleLib.dataFormat == 'xml') {
                    $.ajax({
                        url: url,
                        dataType: "xml",
                        success: function(data, status, xhr) {
                            lib.data = data;
                            if (cb) cb(lib.data, xhr);
                        },
                        error: function(xhr) {
                            if (cb) cb(null, xhr);
                        }
                    });
                }
                // d3:csv
                if (moduleLib.dataFormat == 'd3.csv') {
                    lib.loadD3(function() {
                        d3.csv(
                            url + '&tqx=out:csv', 
                            function(errorXhr, data) {
                                if (errorXhr) {
                                    if (cb) cb(null, errorXhr);
                                }
                                else {
                                    lib.data = data;
                                    if (cb) cb(data);
                                }
                            }
                        );
                    });
                }
            }
        },
        
        loadScript: function(url, cb) {
            if (lib.loaded[url]) {
                if (cb) cb.call();
            }
            else {
                $.ajax({
                    url: url,
                    dataType: "script",
                    cache: true,
                    success: function() {
                        lib.loaded[url] = true;
                        if (cb) cb.call();
                    }
                });
            }
        },
        
        resetData: function() {
            lib.data = null;
        },
        
        /**
         * D3 JS API is assumed to be loaded, just calls the passed callback.
         */ 
        loadD3: function(cb) {
            if (cb) cb.call();
        },
        
        /**
         * Loads the Google JS API and calls the passed callback.
         */ 
        loadGoogleApi: function(cb) {
            if (!window.google) {
                return lib.loadScript('//www.google.com/jsapi', cb);
            }
            if (cb) cb.call();
        },
        
        loadGooglePackage: function(packageName, cb) {
            lib.loadGoogleApi(function() {
                google.load('visualization', '1.0', {packages: [packageName], callback: cb});
            })
        },
        
        /**
         * Adds query parameter input fields (including values, if provided) to the chart options panel.
         */
        createParamsMarkup: function(container, params) {
            var query = lib.detectQueryPath();
            $('<form role="form" class="calli-viz-params" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    var container = $(this);
                    // fetch query source
                    calli.getText(query).then(function(data) {
                        var m = data.match(/\$([a-z0-9_]+)/ig);
                        if (m) {
                            container.append('<h3>Query Parameters</h3>');
                            var param, added = {}, value;
                            for (var i = 0, imax = m.length; i < imax; i++) {
                                param = m[i].replace('$', '');
                                if (added[param]) continue;
                                added[param] = true;
                                value = params && params[param] ? params[param] : '';
                                lib.createTextOption(container, param, param, 'Enter a ' + param + '…', value.replace(/"/g, ''));
                            }
                        }
                    });
                })
            ;
        },
        
        createTextOption: function(container, name, label, placeholder, value, info) {
            value = value || lib.getOption(name);
            $([
                '<div class="form-group">',
                '   <label for="options-' + name + '">' + label + '</label>',
                '   <div>',
                '       <input type="text" name="' +  name + '" id="options-' + name + '" placeholder="' + (placeholder || "") + '" value="' + (value || "") + '" class="form-control" />',
                        info ? '<p class="help-block">' + info + '</p>' : '',
                '   </div>',
                '</div>'
            ].join("\n"))
                .appendTo(container)
                .hide().fadeIn(500)
            ;
        },
        
        createColorOption: function(container, name, label, placeholder, value) {
            value = value || lib.getOption(name) || '#ffffff';
            $([
                '<div class="form-group">',
                '   <label for="options-' + name + '">' + label + '</label>',
                '   <div>',
                '       <input type="color" name="' +  name + '" id="options-' + name + '" placeholder="' + (placeholder || "") + '" value="' + (value || "") + '" class="form-control" />',
                '   </div>',
                '</div>'
            ].join("\n"))
                .appendTo(container)
                .hide().fadeIn(500)
            ;
        },
        
        createHiddenOption: function(container, name, value) {
            value = value || lib.getOption(name);
            $('<input type="hidden" name="' +  name + '" value="' + (value || "") + '" />')
                .appendTo(container)
            ;
        },
        
        createInfoOption: function(container, value) {
            $('<div class="info">' + value + '</div>')
                .appendTo(container)
            ;
        },
        
        createCheckboxOption: function(container, name, label) {
            var checked = lib.getOption(name) ? ' checked="checked"' : '';
            $([
                '<div class="form-group">',
                '   <div class="checkbox">',
                '       <label><input type="checkbox" name="' + name + '" value="true"' + checked +'> ' + label + '</label>',
                '   </div>',
                '</div>'
            ].join("\n"))
                .appendTo(container)
                .hide().fadeIn(500)
            ;
        },
        
        createSelectOption: function(container, name, label, entries) {
            var selected = lib.getOption(name);
            var html = [
                '<div class="form-group">',
                '   <label for="options-' + name + '">' + label + '</label>',
                '   <div>',
                '       <select name="' + name + '" class="form-control">'
            ];
            $.each(entries, function(index, val) {
                var selCode = (index == selected) || (val == selected) ? ' selected="selected"' : '';
                if (isNaN(index)) {
                    html.push('<option value="' + index + '"' + selCode + '>' + val + '</option>')
                }
                else {
                    html.push('<option value="' + val + '"' + selCode + '>' + val + '</option>')
                }
            });
            html.push('     </select>')
            html.push(' </div>');
            html.push('</div>');
            $(html.join("\n"))
                .appendTo(container)
                .hide().fadeIn(500)
            ;
        },
        
        onOptionChange: function(e) {
            e.preventDefault();
            try { window.clearTimeout(lib.changeTO); } catch(e) {};
            lib.changeTO = window.setTimeout(function() {
                $('#calli-viz-editor').trigger('queryviz.option-changed');
            }, 250);
        },
        
        getOptions: function(vizId) {
            var result = lib.config ? JSON.parse(JSON.stringify(lib.config.options)) : {};
            // try options form (overwrites config)
            if (vizId && $('#calli-viz-editor[data-viz-id="' + vizId + '"]').length) {
                var form = $('#calli-viz-editor[data-viz-id="' + vizId + '"] .options-pane form.calli-viz-options');
                var fields = form.serializeArray();
                $.each(fields, function(i, field) {
                    result[field.name] = field.value;
                });
                // support unchecked checkboxes/radios
                form.find('input[type="checkbox"], input[type="radio"]').not(':checked').each(function() {
                    result[$(this).attr('name')] = null;
                });
            }
            return result;
        },
        
        getOption: function(name, vizId) {
            var options = lib.getOptions(vizId);
            return options[name] || null;
        },
        
        onParamChange: function(e) {
            e.preventDefault();
            $('#calli-viz-editor').trigger('queryviz.param-changed');
        },
        
        getParams: function() {
            var fields = $('#calli-viz-editor .options-pane form.calli-viz-params').serializeArray();
            var result = {};
            $.each(fields, function(i, field) {
                if (field.value.length) {
                    result[field.name] = field.value;
                }
            });
            return result;
        },
        
        /**
         * Saves the visualization to an xhtml page
         */ 
        saveVisualization: function(e) {
            if (e) e.preventDefault();
            var content = lib.getPageHtml();
            // open save-as dialog
            var slug = location.pathname.replace(/.*\//,'').replace(/\.rq$/, '.xhtml');
            lib.getSaveTarget(slug, function(path, fname) {
                lib.saveFile(path, fname, content, 'application/xhtml+xml', function(data, status, xhr) {
                    var location = xhr.getResponseHeader('Location');
                    var href = location +'?view';
                    var buttonContainer = $('#calli-viz-editor .form-actions');
                    $('<span class="save-success">Saved chart to <a href="' + href + '">' + fname + '</a>.</span>')
                        .appendTo(buttonContainer)
                    ;
                    // add pointer to query
                    lib.setQueryTemplate(location, buttonContainer);
                });
            });
        },
        
        getPageHtml: function() {
            return [
                '<?xml version="1.0" encoding="UTF-8" ?>',
                '<html xmlns="http://www.w3.org/1999/xhtml"',
                '   xmlns:xsd="http://www.w3.org/2001/XMLSchema#"',
                '   xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"',
                '   xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">',
                '<head>',
                '   <title>' + document.title + '</title>',
                    lib.getVizHtmlHeadLinks(),
                '</head>',
                '<body>',
                '   <div class="container">',
                '      <div id="' + lib.vizId + '"/>',
                '   </div>',
                '   <script data-viz-id="' + lib.vizId + '" type="text/javascript">',
                '   //<![CDATA[',
                "       var config = \n" + JSON.stringify(lib[lib.selectedModule].getConfig(), null, 4) + ";\n",
                "       " + lib[lib.selectedModule].renderModule.toString().replace(/function\s*/, 'function drawVisualization') + ";",
                "       drawVisualization(config);",
                '   //]]>',
                '   </script>',
                '</body>',
                '</html>'
            ].join("\n");
        },
        
        getVizHtmlHeadLinks: function() {
            return [
                '   <link rel="edit-form" href="?edit" />',
                '   <link rel="comments" href="?discussion" />',
                '   <link rel="version-history" href="?history" />',
                '   <link rel="help" href="' + calli.getCallimachusUrl('../callimachus-reference#Named_query') + '" target="_blank" title="Help" />',
                '   <link href="#vizeditor" title="Edit visualization" />',
                '   <link rel="edit-media" href="?" title="Raw SPARQL file" type="application/sparql-query" />',
                '   <link href="?results" title="Results document" type="application/sparql-results+xml" />',
                '   <link rel="stylesheet" href="' + calli.getCallimachusUrl('../query-view.css') + '" type="text/css" />',
                '   <script type="text/javascript" src="' + calli.getCallimachusUrl('../query-view.js') + '"></script>'
            ].join("\n");
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
            calli.headText(path + fname).then(function() {
                if (confirm('Replace ' + fname + '?')) {
                    return calli.deleteText(path + fname).then(function() {
                        return calli.resolve($.ajax(request));
                    });
                }
            }, function() {
                return calli.resolve($.ajax(request));
            }).then(undefined, calli.error);
        },
        
        /**
         * Injects a @view template annotation into the query file.
         */
        setQueryTemplate: function(templateLocation, responseContainer) {
            var shortTemplateLocation = '/' + $('<a/>').attr('href', templateLocation)[0].pathname.replace(/^\//, '');// IE-safe
            calli.getText(location.pathname).then(function(data) {// retrieve current page
                return data
                    .replace(/(\s*)\#\s*\@view\s+[^\n\r]+[\r\n]+/, '$1')    // remove existing annotation
                    .replace(/(PREFIX|SELECT) /i, "# @view " +  shortTemplateLocation + "\n#\n$1 ") // inject annotation in front of query
                    .replace(/[\n\s]+\#\s*([\n\s]+\# \@view)/, '$1')    // remove empty comment line in front of annotation
                ;
            }).then(function(data){
                return calli.putText(location.pathname, data, 'application/sparql-query');
            }).then(function() {
                $('<span class="save-success">Updated the query\'s view template. Reloading...</span>')
                    .appendTo(responseContainer)
                    .delay(5000).fadeOut(2000, function() { 
                        $(this).remove();
                    })
                ;
                window.location.replace(window.location.search);
            },
            function() {
                $('<span class="save-error">Could not set view template</span>')
                    .appendTo(responseContainer)
                    .delay(5000).fadeOut(2000, function() { 
                        $(this).remove();
                    })
                ;
            });
        },
                
        /**
         * Lets the user choose a file location for saving a file
         */
        getSaveTarget: function(slug, callback) {
            var href = window.location.href;
            var src = [
                calli.getCallimachusUrl("pages/save-resource-as.html#"),
                encodeURIComponent(slug.replace(/!/g,'')),
                "!",
                href.substring(0, href.lastIndexOf('/', href.indexOf('?')) + 1),
                "?view"].join('');
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
                        dialog.postMessage('GET resource', '*');
                    } else if (event.data.indexOf('OK\n\nGET resource\n\n') == 0) {
                        var data = event.data;
                        var ns = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
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
         * Initializes the chart library.
         */ 
		init: function() {
            lib.initMenu();
		}
	
	};
	
	$(lib.init);	
 	
})(window.jQuery || function(){});
