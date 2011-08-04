// jquery.wymeditor.containerclass.js

/*
 * Allow container arguments to include css classes
 */
(function($) {

var _container = WYMeditor.editor.prototype.container;
WYMeditor.editor.prototype.container = function(sType) {
	if (typeof sType == 'string' && sType.indexOf('.') >= 0 ) {
		if (sType.indexOf('.') > 0) {
			_container.call(this, sType.substring(0, sType.indexOf('.')));
		}
		this.container(function(container) {
			var css = sType.split('.');
			for (var i = 1; i < css.length; i++) {
				sType = $(container).toggleClass(css[i]);
			}
		});
	} else {
		return _container.call(this, sType);
	}
};

})(jQuery);
