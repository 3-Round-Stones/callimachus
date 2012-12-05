// editor/ckeditor.js

CKEDITOR_BASEPATH = 'ckeditor/';

jQuery(function($) {
    var jQuery = $;
    
    CKEDITOR.replace('editor', {
        resize_enabled: false,                  // disable resize handle
        fullPage: true,                         // enable editing of complete documents
        basicEntities: true,                    // enable basic entities
        entities: false,                        // disable extended entities
        startupOutlineBlocks: true,             // activate visual blocks
        forcePasteAsPlainText: true,            // avoid paste mess
        fillEmptyBlocks: false,                 // avoid &nbsp; in empty table cells
        tabSpaces: false,                       // avoid &nbsp; when tab-key is pressed
        removeDialogTabs:                       // disable non-basic dialog tabs
            'link:target;link:advanced;' +
            'image:Link;image:advanced;' +
            'table:advanced;'
        ,
        coreStyles_bold: { element: 'strong' }, // convert bold to <strong>
    	coreStyles_italic: { element: 'em' },   // convert italic to <em>
                
        toolbar: [
            { name: 'styles', items: [ '!Styles', 'Format' ] },
            { name: 'clipboard', items: [ 'Undo', 'Redo'] },
        	{ name: 'basicstyles', items: [ 'Bold', 'Italic', '!Strike', 'Subscript', 'Superscript' ] },
        	{ name: 'paragraph', items: [ 'NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote', '-', '!JustifyLeft', '!JustifyCenter', '!JustifyRight' ] },
        	{ name: 'links', items: [ 'Link', 'Unlink', 'Anchor' ] },
        	{ name: 'insert', items: [ 'Image', 'Table' ] },
            { name: 'paste', items: [ '!PasteText', '!PasteFromWord'] }
        ],
        
        format_tags: 'p;h1;h2;h3;h4;h5;h6;pre'

    });
    
    var editor = CKEDITOR.instances.editor;
    var saved = null;
    
    // hide selected dialog UI elements
    editor.on('dialogShow', function(e) {
        var el = $(e.data._.element.$);
        
        // remove the advanced tab in table dialogs, in case of ignored config option
        el.find('a.cke_dialog_tab[title*="Table"]').siblings('a.cke_dialog_tab[title="Advanced"]').remove();
        
        // remove unsupported fields from "Table Properties" dialogs
        if (el.find('.cke_dialog_title').html().match(/Table Properties/)) {
            el.find('label').each(function() {
                if ($(this).html().match(/^(border size)$/i)) {
                    $(this).parents('tr').first().hide();// only the immediate parent
                }
            });
        }
        
        // remove unsupported fields from "Cell Properties" dialogs
        if (el.find('.cke_dialog_title').html().match(/Cell Properties/)) {
            el.find('label').each(function() {
                if ($(this).html().match(/^(width|height|word wrap|border color|background color)$/i)) {
                    $(this).parents('tr').first().hide();// only the immediate parent
                }
            });
        }
    });
    
    /**
     * Normalizes html for stable comparisons.
     */
     function normalize(html) {
        return html
            .replace(/^\s*/, '')    // no leading WS
            .replace(/\s*$/, '')    // no trailing WS
            .replace(/&nbsp;/g, '&#160;') // replace &nbsp; entities with xml equivalent
            .replace(new RegExp(unescape('%A0'), "g"), '&#160;') // replace unicode nbsp entities with xml equivalent
            .replace(/<title\/>/, '<title></title>') // fix empty title tag
            .replace(/<style[\s\S]+<\/style>/g, '') // no <style> tags
            .replace(/>\s*(<)/g, ">\n<")    // put tags on a new line
            .replace(/(.)\s*(<[^\/])/g, "$1\n$2")    // put opening tags on a new line
            .replace(/<([a-z0-9]+) ([^>]+)>/g, function(m, m1, m2) { // sorted attributes
                var attrs = (" " + m2).match(/\s+([a-z\:\_]+\=\"[^\"]*\")/g);
                if (!attrs || attrs.length == 1) return m; // no need to sort a single attribute
                attrs = attrs.sort(function(a, b) {
                    return (a == b ? 0 : (a < b ? -1 : 1));
                }) 
                return '<' + m1 + attrs.join('') + '>';
            })
        ;
     }
     
     /**
      * Creates a diff of two html snippets.
      */
     function getDiff(was, is) {
        was = was.split("\n");
        is = is.split("\n");
        for (var i = 0, imax = Math.max(was.length, is.length); i < imax; i++) {
            if (!was[i] && is[i]) {
                return { was: null, is: is[i] };
            }
            else if (!is[i] && was[i]) {
                return { was: was[i], is: null };
            }
            else if (was[i] == is[i]) {
                continue;
            }
            else {
                return { was: was[i], is: is[i] };
            }
        }
        return false;
     }     
     
    /**
     * Adds custom css to the editing interface, even when in fullPage mode.
     */ 
    function injectCss(html) {
        var styles = [
            'body { background-color: #f5f5f5; }',
            'body.cke_editable.cke_show_blocks > * { background-color: #fff; padding-bottom: 5px;}'
        ];
        return html.replace('</head>', '<style type="text/css">' + styles.join("\n") + "\n" + '</style></head>');
    }
    
    /**
     * Preprocesses input and sends it to the editor
     */ 
    editor.setXhtml = function(html, updateSavedVar) {
        html = injectCss(normalize(html));
        editor.setData(html);
        setTimeout(function() {
            if (updateSavedVar) {
                saved = editor.getXhtml();
            }
            resizeEditor();
        }, 100);
    }
    
    /**
     * Normalizes the output for stable comparisons.
     */ 
    editor.getXhtml = function() {
        return normalize(this.getData());
    }
    
    // warn the user before leaving a changed but unsaved article
    window.onbeforeunload = function(event){
        event = event || window.event;
        if (!editor) return;
        if (!editor.checkDirty()) return; // ckeditor didn't register changes at all
        var diff = getDiff(saved, editor.getXhtml()); // make sure the content has really changed
        if (diff) {
            if (event) {
                event.returnValue = 'There are unsaved changes';
            }
            return 'There are unsaved changes';
        }
    };
    
    /**
     * Maximizes the editor height.to fully fill the iframe.
     */ 
    function resizeEditor() {
        try {
            editor.resize('100%', $(window).outerHeight(), false);
        } catch (e) {
            setTimeout(resizeEditor, 250);
        } 
    }
    $(window).on('resize', resizeEditor);
    
    /**
     * Processes inter-window messages.
     */ 
    function handleMessage(header, body) {
        if (header.match(/^PUT text(\n|$)/)) {
            var m = header.match(/\nContent-Location:\s*(.*)(\n|$)/i);
            var systemId = m ? m[1] : null;
            if (header.match(/\nIf-None-Match: */) || !body) {
                if (!editor.getData()) {
                    editor.setXhtml(body, true);
                }
                return true;
            } else {
                editor.setXhtml(body, true);
                return true;
            }
        } else if (header == 'GET text') {
            saved = editor.getXhtml();
            return saved;
        } else if (header == 'PUT line.column') {
            return true;
        } else if (header == 'GET line.column') {
            return '';
        } else if (header == 'PUT disabled' && body) {
            return true;
        }
        return false; // Not Found
    };    
    
    // catch inter-window messages
    $(window).bind('message', function(event) {
        if (event.originalEvent.source == parent) {
            var msg = event.originalEvent.data;
            var header = msg;
            var body = null;
            if (msg.indexOf('\n\n') > 0) {
                header = msg.substring(0, msg.indexOf('\n\n'));
                body = msg.substring(msg.indexOf('\n\n') + 2);
            }
            try {
                var response = handleMessage(header, body);
                if (!response && typeof response == 'boolean') {
                    parent.postMessage('Not Found\n\n' + header, '*');
                } else if (response && typeof response != 'boolean') {
                    parent.postMessage('OK\n\n' + header + '\n\n' + response, '*');
                } else {
                    parent.postMessage('OK\n\n' + header, '*');
                }
            } catch (e) {
                calli.error(e);
            }
        }
    });
    
    // Tell the parent window we are ready
    if (window.parent != window) {
        parent.postMessage('CONNECT calliEditorLoaded', '*');
    }
    
});
