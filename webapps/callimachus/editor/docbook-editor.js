// docbook-editor.js

jQuery(function($) {
    var jQuery = $;
    $('.wym_box_0').wymeditor({
        html: '',
        lang: "en",
        initSkin: false,
        loadSkin: false,
        basePath: calli.getCallimachusUrl('editor/wymeditor/'),
        skinPath: calli.getCallimachusUrl('editor/wymeditor/'),
        jQueryPath: calli.getCallimachusUrl('scripts/jquery.js'),
        wymPath: calli.getCallimachusUrl('editor/wymeditor/jquery.wymeditor.js'),
        dialogFeatures: navigator.appName == 'Microsoft Internet Explorer' ? undefined : 'jQuery.dialog',
        dialogFeaturesPreview: navigator.appName == 'Microsoft Internet Explorer' ? undefined : 'jQuery.dialog'
    });
    $('#wym-iframe').one('load', function() {
        WYMeditor.INSTANCES[0].initIframe(this);

        var editor = jQuery.wymeditors(0);

        if (!$('#wym-iframe')[0].contentWindow.document.body) {
            setTimeout(function(){
                if (!$('body', $('#wym-iframe')[0].contentWindow.document).children().length) {
                    // IE need this called twice on first load
                    WYMeditor.INSTANCES[0].initIframe($('#wym-iframe')[0]);
                    // IE8 hides the iframe body on input, resizing it causes it to show again
                    editor = jQuery.wymeditors(0);
                    var iframe = $('#wym-iframe');
                    var resetHeight = function() {
                        iframe.css('height', iframe.height() + 'px').css('box-sizing', 'content-box');
                        setTimeout(setFullHeight, 300);
                    };
                    var setFullHeight = function() {
                        iframe.css('height', '100%').css('box-sizing', 'border-box');
                        setTimeout(resetHeight, 300);
                    };
                    setFullHeight();
                }
            }, 1000);
        }

        var saved = null;
        window.onbeforeunload = function(event){
            event = event || window.event;
            if (editor && editor.docbook() != saved) {
                if (event) {
                    event.returnValue = 'There are unsaved changes';
                }
                return 'There are unsaved changes';
            }
        };

        // messaging
        function handleMessage(header, body) {
            if (header == 'PUT src\nIf-None-Match: *' && body) {
                if (!editor.html()) {
                    editor.docbook(body);
                    saved = editor.docbook();
                }
                return true;
            } else if (header == 'PUT src' && body) {
                editor.docbook(body);
                saved = editor.docbook();
                return true;
            } else if (header == 'GET src') {
                saved = editor.docbook();
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
        if (window.parent != window) {
            parent.postMessage('CONNECT calliEditorLoaded', '*');
        }
    });
    $('#wym-iframe')[0].src = calli.getCallimachusUrl("editor/wymeditor/wymiframe.html");
});
