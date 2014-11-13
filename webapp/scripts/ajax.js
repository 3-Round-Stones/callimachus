// ajax.js
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


(function($){

var calli = window.calli || (window.calli={});

calli.withCredentials = {
  withCredentials: true
};

calli.headText = function(url) {
    return ajax({
        type: 'HEAD',
        url: url,
        dataType: 'text',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    });
};

calli.getText = function(url, success) {
    return ajax({
        type: 'GET',
        url: url,
        dataType: 'text',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    }).then(success);
};

calli.postText = function(url, data, contentType, headers) {
    return ajax({
        type: "POST",
        url: url,
        dataType: "text",
        contentType: contentType || "text/plain",
        headers: headers,
        processData: false,
        data: data,
        xhrFields: {
            withCredentials: true
        }
    });
};

calli.updateText = function(url, data, contentType) {
    return ajax({
        type: "POST",
        url: url,
        dataType: "text",
        contentType: contentType || "text/plain",
        processData: false,
        data: data,
        xhrFields: {
            withCredentials: true
        },
        headers: {
            "If-Unmodified-Since": calli.lastModified(url)
        }
    });
};

calli.putText = function(url, data, contentType) {
    return ajax({
        type: "PUT",
        url: url,
        dataType: "text",
        contentType: contentType || "text/plain",
        processData: false,
        data: data,
        xhrFields: {
            withCredentials: true
        },
        headers: {
            "If-Unmodified-Since": calli.lastModified(url)
        }
    });
};

calli.deleteText = function(url) {
    return ajax({
        type: "DELETE",
        url: url,
        dataType: "text",
        xhrFields: {
            withCredentials: true
        },
        headers: {
            "If-Unmodified-Since": calli.lastModified(url)
        }
    });
};

calli.getJSON = function(url) {
    return ajax({
        type: 'GET',
        url: url,
        dataType: 'json',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    });
};

calli.getXML = function(url) {
    return ajax({
        type: 'GET',
        url: url,
        dataType: 'xml',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    });
};

function ajax(settings) {
    var xhr = $.ajax(settings);
    return calli.resolve(xhr).then(function(response){
        var location = xhr.getResponseHeader('Location');
        var content = xhr.getResponseHeader('Content-Location');
        var modified = xhr.getResponseHeader('Last-Modified');
        var now = modified || new Date().toUTCString();
        calli.lastModified(settings.url, now);
        if (location) {
            calli.lastModified(location, now);
        }
        if (content) {
            calli.lastModified(content, now);
        }
        return response;
    });
}

})(jQuery);
