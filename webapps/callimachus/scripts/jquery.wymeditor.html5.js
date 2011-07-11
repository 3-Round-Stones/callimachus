// jquery.wymeditor.html5.js

/*
 * Support HTML5 and RDFa attributes
 */

(function($) {
WYMeditor.XhtmlValidator._tags['a']['attributes']['rel'] = /^.+$/;
WYMeditor.XhtmlValidator._tags['a']['attributes']['rev'] = /^.+$/;
WYMeditor.XhtmlValidator._tags['time'] = {attributes: {datetime:  /^[\d+-WTZ:.]+$/, pubdate:  /^pubdate$/ }};
WYMeditor.XhtmlValidator._tags['input']['attributes']['type'] = /^(button|checkbox|color|date|datetime|datetime-local|email|file|hidden|image|month|number|password|radio|range|reset|search|submit|tel|text|time|url|week)$/;
WYMeditor.XhtmlValidator._attributes['core']['attributes'].push('dropzone', 'autofocus', 'placeholder');
WYMeditor.XhtmlValidator._attributes['core']['attributes'].push(
	'rel',
	'rev',
	'content',
	'href',
	'src',
	'about',
	'property',
	'resource',
	'datatype',
	'typeof');

})(jQuery);
