// jquery.wymeditor.inithtml.js

/*
 * Allows WYM to be initialized with dynamic HTML string (downloaded separately).
 * The DOM iframe must have an id of 'wym-iframe'
 */
WYMeditor.editor.prototype.initHtml = function(html) {
	var wym = this;
	wym._wym._html = html;
	wym.initIframe($('#wym-iframe')[0]);
	$('#wym-iframe').trigger('load');
	setTimeout(function(){
		if (!$('body', $('#wym-iframe')[0].contentWindow.document).children().length) {
			// IE need this called twice on first load
			wym.initIframe($('#wym-iframe')[0]);
		}
	}, 1000);
};
