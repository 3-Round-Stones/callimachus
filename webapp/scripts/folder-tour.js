// folder-tour.js
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

jQuery(function($){

    $('head').append($('<link/>', {
        rel: "stylesheet",
        type: "text/css",
        href: calli.getCallimachusUrl("assets/tripjs/trip.css")
    }));

    // Build page tour
    var tour = new Trip ([
        { content : "Welcome to the Folder view. This will become a very familiar screen as you start to use Callimachus more. Look around " +
            " to see all the functionality this page contains.", position : "screen-center" },
        { sel : $("#create-menu"), content : "Use the create menu to create resources within this folder. Folders can contain other folders. Try making a folder here." +
            "Articles allow you to quickly make a new Web page using a familiar editor." +
            "Read about all the file types you can create or upload in the documentation.",  position : "e" },
        { sel : $("#file-create"), content : "You can also use the upload button to upload existing resources from your computer.", position : "e" },
        { sel : $('[class="btn btn-default navbar-btn dropdown-toggle"]'), content : "The main menu can be used to export and import folder contents." +
            "You can export a Callimachus ARchive (CAR) file containing all resources from this folder and its children. " +
            "You can also import a CAR file to replace this folder's contents. This action deletes any current contents and replaces them. " +
            "Administrators may invite other users to this Callimachus instance.", position : "s" },
        { sel : $("thead"), content: "This table lists all resources contained within their folder along with the last time they " + 
            "were updated and their associated group permissions.", position : "n" }
    ], { 
        showNavigation: true,
        animation: 'fadeIn',
        showCloseBox : true,
        finishLabel: 'End tour',
        delay : -1, // Allows for manual control of tour
        backToTopWhenEnded: true
    });
    
    // Start tour
    tour.start();
});

