// editor/ckeditor.js

CKEDITOR_BASEPATH = 'ckeditor/';

jQuery(function($) {
    var jQuery = $;
    
    CKEDITOR.replace('editor', {
        
        skin: 'moono',
        resize_enabled: false,
        fullPage: true, // full html vs just body
        entities: false,
        startupOutlineBlocks: true,

        removeDialogTabs: 'dialog1:tab1;dialog2:tab1',

        coreStyles_bold: {
    		element: 'strong'
    	},
        
    	coreStyles_italic: {
    		element: 'em'
    	},
                
        toolbar: [
            { name: 'styles', items: [ '!Styles', 'Format', '!Font', '!FontSize' ] },
            { name: 'clipboard', items: [ 'Undo', 'Redo', '-', '!Cut', '!Copy', '!Paste'] },
        	{ name: 'editing', items: [ '!Find', '!Replace', '-', '!SelectAll' ] },
        	{ name: 'basicstyles', items: [ 'Bold', 'Italic', '!Underline', '!Strike', 'Subscript', 'Superscript', '-', '!RemoveFormat' ] },
        	{ name: 'paragraph', items: [ 'NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote', '!CreateDiv', '-', '!JustifyLeft', '!JustifyCenter', '!JustifyRight', '!JustifyBlock', '-', '!BidiLtr', '!BidiRtl' ] },
        	{ name: 'links', items: [ 'Link', 'Unlink', '?Anchor' ] },
        	{ name: 'insert', items: [ 'Image', '!Flash', 'Table', '!HorizontalRule', '!Smiley', '!SpecialChar', '!PageBreak', '!Iframe' ] },
            { name: 'tools', items: [ '!Maximize', '!ShowBlocks' ] },
            { name: 'paste', items: [ 'PasteText', 'PasteFromWord'] },
            
            { name: 'document', items: [ '!Source', '-', '!Save', '!NewPage', '!Preview', '!Print', '-', '!Templates' ] },
        	{ name: 'colors', items: [ '!TextColor', '!BGColor' ] },
        	{ name: 'others', items: [ '-' ] },
        	{ name: 'about', items: [ '!About' ] }
        ]

    });
    
    var editor = CKEDITOR.instances.editor;
    editor.xhtml = function() {
        var xhtml = this
            .getData()
            .replace(/>[\s]*</g, ">\n<")
        ;
        return xhtml;
    }    
    
    if (window.parent != window) {
        parent.postMessage('CONNECT calliEditorLoaded', '*');
    }
        
    var saved = null;
    window.onbeforeunload = function(event){
        event = event || window.event;
        if (!editor) return;
        var was = saved.split("\n");
        var is = editor.xhtml().split("\n");
        var diff = [];
        for (var i = 0, imax = was.length; i < imax; i++) {
            if (!is[i] || was[i] != is[i]) {
                diff.push(was[i]);
            }
        }
        if (diff.length) { // changed
            if (event) {
                event.returnValue = 'There are unsaved changes';
            }
            return 'There are unsaved changes';
        }
    };
    
    function resizeEditor() {
        try {clearTimeout(window.ckeditorTO)} catch (e) { }
        window.ckeditorTO = window.setTimeout(function() {
            try {
                var h = $(window).outerHeight();
                if (h != prevHeight) {
                    prevHeight = h;
                    editor.resize('100%', h, false);
                }
            } catch (e) {} 
        }, 20);    
    }
    var prevHeight = 0;
    $(window).on('resize', resizeEditor);
    resizeEditor();
    
    // messaging
    function handleMessage(header, body) {
        if (header.match(/^PUT text(\n|$)/)) {
            var m = header.match(/\nContent-Location:\s*(.*)(\n|$)/i);
            var systemId = m ? m[1] : null;
            if (header.match(/\nIf-None-Match: */) || !body) {
                if (!editor.getData()) {
                    editor.setData(body);
                    saved = editor.xhtml();
                }
                return true;
            } else {
                editor.setData(body);
                saved = editor.xhtml();
                return true;
            }
        } else if (header == 'GET text') {
            saved = editor.xhtml();
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
    
});
