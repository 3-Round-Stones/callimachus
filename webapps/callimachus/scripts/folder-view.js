// folder-view.js

(function($){
	try {
		if (window.sessionStorage) {
			sessionStorage.setItem("LastFolder", location.href);
			localStorage.setItem("LastFolder", location.href);
		}
	} catch (e) {
		// ignore
	}
	function createTextCell(text, url, src) {
		if (!src) {
			src = '/callimachus/images/rdf-icon.png';
		}
		var td = $('<td/>');
		td.addClass('ui-widget-content');
		td.addClass('filecell');
		var a = $('<a/>');
		if (url) {
			a.attr('href', url);
			a.addClass('view');
		}
		a.text(text);
		var img = $('<img/>');
		img.addClass('icon');
		img.attr('src', src);
		a.prepend(img);
		td.append(a);
		return td;
	}
	function createTimeCell(text) {
		var td = $('<td/>');
		td.addClass('ui-widget-content');
		td.addClass('timecell');
		if (text) {
			var time = $('<time/>');
			time.addClass("abbreviated");
			time.text(text);
			td.append(time);
		}
		return td;
	}
	function createPermissionCell(entry, tagName) {
		var td = $('<td/>');
		td.addClass('ui-widget-content');
		td.addClass('permission');
		var tags = entry.children().filter(function(){return this.tagName==tagName});
		tags.each(function() {
			var uri = $(this).children('uri').text();
			var name = $(this).children('name').text();
			var a = $('<a/>');
			if (uri) {
				a.attr('href', uri);
			}
			if (name) {
				a.text(name);
			} else {
				a.text(uri);
			}
			td.append(' ');
			td.append(a);
		});
		return td;
	}
	function reload() {
		var url = $('link[type="application/atom+xml"]').attr('href');
		jQuery.get(url, function(doc) {
			jQuery(function() {
				var feed = $(doc).children('feed');
				var totalResults = feed.children().filter(function(){return this.tagName=='openSearch:totalResults'}).text();
				$('#totalResults').text(totalResults);
				var tbody = $('<tbody/>');
				tbody.attr('id', 'tfiles');
				var totalEntries = 0;
				feed.children('entry').each(function() {
					var entry = $(this);
					totalEntries++;
					if (!entry.children('link[type="application/atom+xml"]').length) {
						var tr = $('<tr/>');
						var icon = entry.children('icon').text();
						var title = entry.children('title').text();
						var link = entry.children('link').attr('href');
						tr.append(createTextCell(title, link, icon));
						tr.append(createTimeCell(entry.children('updated').text()));
						tr.append(createPermissionCell(entry, 'calli:reader'));
						tr.append(createPermissionCell(entry, 'contributor'));
						tr.append(createPermissionCell(entry, 'calli:editor'));
						tr.append(createPermissionCell(entry, 'calli:administrator'));
						tbody.append(tr);
					}
				});
				$('#tfiles').replaceWith(tbody);
				$('#totalEntries').text(totalEntries);
			});
		});
	}
	function upload(name, type, payload, callback) {
		jQuery.ajax({
			type:'POST',
			url:'?create=/callimachus/File',
			contentType:type,
			processData:false,
			data:payload,
			beforeSend:function(xhr) {
				xhr.setRequestHeader('Location', encodeURI(name).replace(/%20/g, '-'));
			},
			success:function(data, textStatus) {
				reload();
			},
			complete:callback
		});
	}
	var upload_queue = [];
	function queue(name, type, binary) {
		upload_queue.push(function() {
			upload(name, type, binary, function(){
				upload_queue.shift();
				if (upload_queue.length > 0) {
					upload_queue[0]();
				}
			});
		});
		if (upload_queue.length == 1) {
			upload_queue[0]();
		}
	}
	reload();
	jQuery(function(){
		if (window.FileReader && (window.BlobBuilder || window.MozBlobBuilder || window.WebKitBlobBuilder)) {
			$('#box').bind('dragenter dragexit dragover', function(event) {
				event.stopPropagation();
				event.preventDefault();
			});
			$('#box').bind('drop', function(event) {
				event.stopPropagation();
				event.preventDefault();
				var files = event.originalEvent.dataTransfer.files;
				for (var i = 0; i < files.length; i++) {
					(function(file){
						var reader = new FileReader();
						reader.onload = function(event) {
							var datastr = event.target.result;
							var bb = new (window.BlobBuilder || window.MozBlobBuilder || window.WebKitBlobBuilder)();
							var data = new ArrayBuffer(datastr.length);
							var ui8a = new Uint8Array(data, 0);
							for (var i=0; i<datastr.length; i++) {
								ui8a[i] = (datastr.charCodeAt(i) & 0xff);
							}
							bb.append(data);
							queue(file.name, file.type, bb.getBlob());
						};
						reader.readAsBinaryString(file);
					})(files[i]);
				}
			});
		}
	});
})(jQuery);
jQuery(function($){
	var tooSmall = 500;
	var resized = function() {
		setTimeout(function(){
			var clientWidth = $('#box').width();
			if (clientWidth < $('#table').width()) {
				tooSmall = clientWidth;
				$('#table').addClass('small');
			} else if (clientWidth > tooSmall) {
				$('#table').removeClass('small');
				setTimeout(resized, 500);
			}
		}, 0);
	}
	$(window).bind('resize', resized);
	resized();
});

