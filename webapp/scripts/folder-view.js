// folder-view.js

(function($, jQuery){
    try {
        if (window.sessionStorage) {
            sessionStorage.setItem("LastFolder", location.href);
            localStorage.setItem("LastFolder", location.href);
        }
    } catch (e) {
        // ignore
    }
    function createTextCell(text, url, type, src) {
        if (!src) {
            src = calli.getCallimachusUrl('images/rdf-icon.png');
        }
        var td = $('<td/>');
        td.addClass('ui-widget-content');
        td.addClass('filecell');
        var a = $('<a/>');
        if (url) {
            a.attr('href', url);
            a.addClass('view');
            if (type) {
                a.attr('type', type);
            }
        }
        if (text) {
            a.text(text);
        } else if (url) {
            a.text(url.replace(/.*\/(.)/, '$1'));
        } else {
            a.text('.');
        }
        if (src) {
            var img = $('<img/>');
            img.addClass('icon');
            img.attr('src', src);
            a.prepend(img);
        }
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
    function createPermissionCell(entry, local) {
        var td = $('<td/>');
        td.addClass('ui-widget-content');
        td.addClass('permission');
        if (!entry || !local)
            return td;
        var tags = entry.children('link[rel="http://callimachusproject.org/rdf/2009/framework#' + local + '"]');
        tags.each(function() {
            var uri = $(this).attr('href');
            var name = $(this).attr('title');
            var a = $('<a/>');
            if (uri) {
                a.attr('href', uri);
            }
            if (name) {
                a.text(name);
            } else if (uri) {
                a.text(uri);
            } else {
                a.text('.');
            }
            td.append(' ');
            td.append(a);
        });
        return td;
    }
    function reload() {
        var url = $('link[rel="contents"][type="application/atom+xml"]').attr('href');
        jQuery.ajax({
            url: url,
            processData: false,
            beforeSend: calli.withCredentials,
            complete: function(xhr) {
                var doc = xhr.responseXML;
                if (window.ActiveXObject && (!doc || !doc.childNodes.length)) {
                    var doc = new ActiveXObject("Microsoft.XMLDOM");
                    doc.async = false;
                    doc.loadXML(xhr.responseText);
                }
                if (doc) jQuery(function() {
                    var feed = $(doc.documentElement);
                    var totalResults = feed.children().filter(function(){return this.tagName=='openSearch:totalResults'}).text();
                    $('#totalResults').text(totalResults);
                    var tbody = $('<tbody/>');
                    tbody.attr('id', 'tfiles');
                    var totalEntries = 0;
                    feed.children('entry').each(function() {
                        var entry = $(this);
                        totalEntries++;
                        if (!entry.children('link[rel="contents"]').length) {
                            var tr = $('<tr/>');
                            var icon = entry.children('icon').text();
                            var title = entry.children('title').text();
                            var content = entry.children('content');
                            var src = content.attr('src');
                            var type = content.attr('type');
                            tr.append(createTextCell(title, src, type, icon));
                            tr.append(createTimeCell(entry.children('updated').text()));
                            tr.append(createPermissionCell(entry, 'reader'));
                            tr.append(createPermissionCell(entry, 'subscriber'));
                            tr.append(createPermissionCell(entry, 'contributor'));
                            tr.append(createPermissionCell(entry, 'editor'));
                            tr.append(createPermissionCell(entry, 'administrator'));
                            tbody.append(tr);
                        }
                    });
                    var box = $('#folder-box')[0];
                    var bottom = box.scrollTop > 0 && box.scrollTop >= box.scrollHeight - box.clientHeight;
                    $('#tfiles').replaceWith(tbody);
                    $('#totalEntries').text(totalEntries);
                    if (bottom) {
                        box.scrollTop = box.scrollHeight - box.clientHeight;
                    }
                });
            }
        });
    }
    reload();
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
        var classFile = $('#file-class-link').attr('href');
        var formData = new FormData();
        formData.append(file.name, file);
        var slug = calli.slugify(file.name.replace(/[-\s]+/g, '-'));
        jQuery.ajax({
            type:'POST',
            url:'?create=' + classFile + '&location=' + encodeURIComponent(slug),
            contentType:"multipart/form-data",
            processData:false,
            data:formData,
            beforeSend:function(xhr) {
                if (xhr.upload && xhr.upload.addEventListener) {
                    xhr.upload.addEventListener("progress", function(event) {
                        uploadProgress(event.loaded);
                    }, false);
                }
                calli.withCredentials(xhr);
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
        var rate = uploadedSize / (new Date().getTime() - queueStarted.getTime()) * 1000 / 5;
        estimate(uploadedSize, uploadedSize - queueCompleteSize + rate, rate);
    }
    function notifyProgressComplete() {
        uploadedSize = 0;
        $('#result-status').find('.ui-progressbar').remove();
        if (console && console.log) {
            console.log(new Date().toTimeString() + ' processed 100% of ' + queueTotalSize);
        }
    }
    jQuery(function(){
        if (window.FileReader) {
            $('#folder-box').bind('dragenter dragexit dragover', function(event) {
                $(this).addClass(event.type);
                event.stopPropagation();
                event.preventDefault();
            });
            $('#folder-box').bind('dragleave', function(event) {
                $(this).removeClass('dragenter dragover');
            });
            $('#folder-box').bind('drop', function(event) {
                $(this).removeClass('dragenter dragover');
                event.stopPropagation();
                event.preventDefault();
                var files = event.originalEvent.dataTransfer.files;
                for (var i = 0; i < files.length; i++) {
                    queue(files[i]);
                }
            });
        }
    });
})(jQuery, jQuery);
jQuery(function($){
    $('a.breadcrumb').addClass("view").trigger("DOMNodeInserted");
    if (!$('#tfolders').children().length) {
        $('#tfolders').remove();
    }
    var tooSmall = 500;
    var resized = function() {
        setTimeout(function(){
            var clientWidth = Math.round($('#folder-box').width());
            if (clientWidth < Math.round($('#table').outerWidth(true))) {
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

