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
				var box = $('#box')[0];
				var bottom = box.scrollTop >= box.scrollHeight - box.clientHeight;
				$('#tfiles').replaceWith(tbody);
				$('#totalEntries').text(totalEntries);
				if (bottom) {
					box.scrollTop = box.scrollHeight - box.clientHeight;
				}
			});
		});
	}
	var queueStarted = null;
	var queueTotalSize = 0;
	var queueCompleteSize = 0;
	var upload_queue = [];
	function queue(file) {
		if (!queueStarted) {
			queueStarted = new Date();
		}
		queueTotalSize += file.size;
		upload_queue.push(function() {
			upload(file, function(){
				upload_queue.shift();
				queueCompleteSize += file.size;
				uploadProgress(0);
				if (upload_queue.length > 0) {
					upload_queue[0]();
				} else {
					queueStarted = null;
					queueTotalSize = 0;
					queueCompleteSize = 0;
					notifyProgressComplete();
				}
			});
		});
		if (upload_queue.length == 1) {
			upload_queue[0]();
		}
	}
	function upload(file, callback) {
		var formData = new FormData();
		formData.append(file.name, file);
		jQuery.ajax({
			type:'POST',
			url:'?create=/callimachus/File&location=' + encodeURI(file.name).replace(/%20/g, '-'),
			contentType:"multipart/form-data",
			processData:false,
			data:formData,
			beforeSend:function(xhr) {
				if (xhr.upload && xhr.upload.addEventListener) {
					xhr.upload.addEventListener("progress", function(event) {
						uploadProgress(event.loaded);
					}, false);
				}
			},
			success:function(data, textStatus) {
				reload();
			},
			complete:callback
		});
	}
	var uploadedSize = 0;
	function uploadProgress(complete, estimated) {
		var progress = $('#result-status').find('.ui-progressbar-value');
		if (!progress.length && queueCompleteSize + complete < queueTotalSize / 2) {
			var progress = $('<div/>');
			var progressbar = $('<div/>');
			progressbar.addClass("ui-progressbar ui-widget ui-widget-content ui-corner-all");
			progress.addClass("ui-progressbar-value ui-widget-header ui-corner-left");
			progressbar.append(progress);
			$('#result-status').append(progressbar);
		}
		// Set aside 25% for server processing time
		var x = (queueCompleteSize + complete * 7/8) / queueTotalSize;
		// Early stage upload just fills up buffers and don't contribute as much
		var percent = Math.round(Math.pow(x+(1-x)*0.03, 2) * 100);
		if (percent >= 100)
			return false;
		progress.css('width', percent + '%');
		if (!estimated) {
			uploadedSize = queueCompleteSize + complete;
			if (console && console.log) {
				console.log(new Date().toTimeString() + ' processed ' + percent + '% uploaded ' + (queueCompleteSize + complete) + ' of ' + queueTotalSize);
			}
			estimateProgress();
		}
		return true;
	}
	function estimateProgress() {
		var estimate = function(lastSize, complete, rate){
			setTimeout(function(){
				if (lastSize == uploadedSize) {
					if (uploadProgress(complete, true)) {
						estimate(lastSize, complete + rate, rate);
					}
				}
			}, 1000);
		};
		// animate progressbar every second while idle to simulate server processing
		var rate = uploadedSize / (new Date().getTime() - queueStarted.getTime()) * 1000 / 2;
		estimate(uploadedSize, uploadedSize - queueCompleteSize + rate, rate);
	}
	function notifyProgressComplete() {
		uploadedSize = 0;
		$('#result-status').find('.ui-progressbar').remove();
		if (console && console.log) {
			console.log(new Date().toTimeString() + ' processed 100% of ' + queueTotalSize);
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
					queue(files[i]);
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

