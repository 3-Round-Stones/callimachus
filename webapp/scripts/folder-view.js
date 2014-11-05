// folder-view.js
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

(function($, jQuery){
    try {
        window.sessionStorage.setItem("LastFolder", location.href);
    } catch(e) {}
    try {
        window.localStorage.setItem("LastFolder", location.href);
    } catch(e) {}
    $(function($){
        $('#file-create').click(function(event) {
            var dialog = calli.openDialog(this.href, this.title, {
                onmessage: function(event) {
                    if (event.data.indexOf('POST resource\n') === 0) {
                        reload();
                        calli.closeDialog(dialog);
                    }
                }
            });
            event.preventDefault();
        });
    });
    function createTextCell(text, url, type, src) {
        if (!src) {
            src = calli.getCallimachusUrl('images/rdf_flyer.png');
        }
        var td = $('<td/>');
        td.addClass('filecell');
        var a = $('<a/>');
        if (url) {
            a.attr('href', url + '?view');
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
        td.addClass('timecell');
        if (text) {
            var time = $('<time/>');
            time.attr('datetime', text);
            time.text(calli.parseDateTime(text).toLocaleString());
            td.append(time);
        }
        return td;
    }
    function createPermissionCell(entry, local) {
        var td = $('<td/>');
        td.addClass('hidden-xs hidden-sm');
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
        return calli.getXML(url).then(calli.ready).then(function(doc) {
            var feed = $(doc.documentElement);
            var totalResults = feed.children().filter(function(){return this.tagName=='openSearch:totalResults';}).text();
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
            var checkForCompleteImg = function() {
                if ($(tbody).find('img').filter(function() { return !this.complete; }).length) {
                    return calli.sleep(500).then(checkForCompleteImg);
                }
            };
            return calli.sleep(100).then(checkForCompleteImg);
        }).then(undefined, calli.error);
    }
    reload();
    var queueStarted = null;
    var queueTotalSize = 0;
    var queueCompleteSize = 0;
    var upload_queue = 0;
    var uploading = calli.resolve();
    function queue(file) {
        if (!queueStarted) {
            queueStarted = new Date();
        }
        upload_queue++;
        queueTotalSize += file.size;
        var updateProgress = function(){
            upload_queue--;
            queueCompleteSize += file.size;
            uploadProgress(0);
            if (upload_queue < 1) {
                queueStarted = null;
                queueTotalSize = 0;
                queueCompleteSize = 0;
                notifyProgressComplete();
            }
        };
        var updateProgressAfterError = function(error) {
            updateProgress();
            return calli.reject(error);
        };
        var slug = calli.slugify(file.name.replace(/[\-\s]+/g, '-'));
        var xhr = $.ajax({
            type: 'HEAD',
            url: slug,
            dataType: "text"
        });
        return calli.resolve(xhr).then(function(){
            if (confirm(slug + " already exists. Do you want to replace it?")) {
                var contentType = file.type;
                if (!contentType || contentType.indexOf('/x-') > 0) {
                    contentType = xhr.getResponseHeader('Content-Type');
                }
                return uploading.then(function() {
                    return putFile(file, contentType, slug).then(updateProgress, updateProgressAfterError);
                });
            }
        }, function() {
            return uploading.then(function() {
                return postCreate(file, slug).then(updateProgress, updateProgressAfterError);
            });
        });
    }
    function postCreate(file, slug) {
        var classFile = $('#file-class-link').attr('href');
        var contentType = file.type && file.type.indexOf('/x-') < 0 && file.type || "application/octet-stream";
        return calli.resolve($.ajax({
            type: 'POST',
            url: '?create=' + encodeURIComponent(classFile),
            contentType: contentType,
            headers: {Slug: slug},
            processData: false,
            data: file,
            xhrFields: {withCredentials: true},
            xhr: function() {
                var xhr = $.ajaxSettings.xhr();
                if (xhr.upload && xhr.upload.addEventListener) {
                    xhr.upload.addEventListener("progress", function(event) {
                        uploadProgress(event.loaded);
                    }, false);
                } else if (console.log) {
                    console.log("Upload progress is not supported.");
                }
                return xhr;
            }
        })).then(reload);
    }
    function putFile(file, contentType, slug) {
        var classFile = $('#file-class-link').attr('href');
        return calli.resolve($.ajax({
            type:'PUT',
            url:slug,
            contentType:contentType,
            processData:false,
            data:file,
            xhrFields: {withCredentials: true},
            xhr: function() {
                var xhr = $.ajaxSettings.xhr();
                if (xhr.upload && xhr.upload.addEventListener) {
                    xhr.upload.addEventListener("progress", function(event) {
                        uploadProgress(event.loaded);
                    }, false);
                } else if (console.log) {
                    console.log("Upload progress is not supported.");
                }
                return xhr;
            }
        })).then(reload);
    }
    var uploadedSize = 0;
    function uploadProgress(complete, estimated) {
        var progressbar = $('#result-status').find('.progress-bar');
        if (!progressbar.length && queueCompleteSize + complete < queueTotalSize / 2) {
            var progress = $('<div/>');
            progressbar = $('<div/>');
            var span = $('<span></span>');
            span.addClass("sr-only");
            progressbar.append(span);
            progressbar.addClass("progress-bar");
            progress.addClass("progress");
            progress.append(progressbar);
            $('#result-status').append(progress);
        }
        // Set aside 25% for server processing time
        var x = (queueCompleteSize + complete * 7/8) / queueTotalSize;
        // Early stage upload just fills up buffers and don't contribute as much
        var percent = Math.round(Math.pow(x+(1-x)*0.03, 2) * 100);
        if (percent >= 100)
            return false;
        progressbar.css('width', percent + '%');
        progressbar.find('span').text(percent + '%');
        if (estimated) {
            progressbar.closest('.progress').addClass('progress-striped active');
        } else {
            progressbar.closest('.progress').removeClass('progress-striped active');
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
            }, 2000);
        };
        // animate progressbar every second while idle to simulate server processing
        if (queueStarted) {
            var rate = uploadedSize / (new Date().getTime() - queueStarted.getTime()) * 1000 / 5;
            estimate(uploadedSize, uploadedSize - queueCompleteSize + rate, rate);
        }
    }
    function notifyProgressComplete() {
        uploadedSize = 0;
        $('#result-status').find('.progress').remove();
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
                calli.resolve().then(function(){
                    return calli.all(Array.prototype.map.call(files, queue));
                }).then(undefined, calli.error);
            });
        }
    });
})(jQuery, jQuery);
jQuery(function($){
    if (!$('#tfolders').children().length) {
        $('#tfolders').remove();
    }
});

