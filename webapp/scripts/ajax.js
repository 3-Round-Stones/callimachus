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

var calli = window.calli = window.calli || {};

calli.withCredentials = {
  withCredentials: true
};

calli.headText = function(url) {
    return calli.resolve($.ajax({
        type: 'HEAD',
        url: url,
        dataType: 'text',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    })).then(function(response){
        calli.lastModified(url, new Date().toUTCString());
        return response;
    });
};

calli.getText = function(url, success) {
    return calli.resolve($.ajax({
        type: 'GET',
        url: url,
        dataType: 'text',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    })).then(function(response){
        calli.lastModified(url, new Date().toUTCString());
        return response;
    }).then(success);
};

calli.postText = function(url, data, contentType) {
    return calli.resolve($.ajax({
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
    })).then(function(response){
        calli.lastModified(url, new Date().toUTCString());
        return response;
    });
};

calli.putText = function(url, data, contentType) {
    return calli.resolve($.ajax({
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
    })).then(function(response){
        calli.lastModified(url, new Date().toUTCString());
        return response;
    });
};

calli.deleteText = function(url) {
    return calli.resolve($.ajax({
        type: "DELETE",
        url: url,
        dataType: "text",
        xhrFields: {
            withCredentials: true
        },
        headers: {
            "If-Unmodified-Since": calli.lastModified(url)
        }
    }));
};

calli.getJSON = function(url) {
    return calli.resolve($.ajax({
        type: 'GET',
        url: url,
        dataType: 'json',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    })).then(function(response){
        calli.lastModified(url, new Date().toUTCString());
        return response;
    });
};

calli.getXML = function(url) {
    return calli.resolve($.ajax({
        type: 'GET',
        url: url,
        dataType: 'xml',
        cache: true,
        xhrFields: {
          withCredentials: true
        }
    })).then(function(response){
        calli.lastModified(url, new Date().toUTCString());
        return response;
    });
};

})(jQuery);
