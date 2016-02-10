# Callimachus Project Release Notes

> http://callimachusproject.org/

10 February 2016

## REQUIREMENTS FOR CALLIMACHUS 1.5.0 (Open Source)

Callimachus works best on browsers that are closely tracking the development
of HTML5.  At the time of this writing, Callimachus works best on Chrome and
Firefox.  It is recommended that Callimachus users track updates to their
browsers for the best usage experience.

Chrome 48 has been thoroughly tested.
Firefox 44 has no known issues.
Safari 6.1 and 7 has known issues.
Internet Explorer 11 has known issues.

Details of browser support may be found at:
  http://callimachusproject.org/docs/1.2/articles/browser-support.docbook?view

The Callimachus server requires Java JDK 1.7 on the server to run.

## NEW IN VERSION 1.5.0

 * New Remote RDF Datasource resources provide Direct and Indirect Graph Store Protocol and navigable resources on top of (remote) SPARQL endpoints http://www.w3.org/TR/sparql11-http-rdf-update/
 * Support for text/turtle and application/ld+json result formats from SPARQL endpoints http://www.w3.org/TR/sparql11-protocol/
 * Support for application/sparql-update file hosting
 * SERVICE blocks now use Callimachus credentials for authentication http://callimachusproject.org/getting-started-with-callimachus#Home_Folder_Edit_Tab
 * Faster CAR folder import
 * Member of staff and power groups can now invite users to groups they are editors of
 * Folders can now define their own 404 handlers
 * New Persistent URL resources https://en.wikipedia.org/wiki/Persistent_uniform_resource_locator
   * Redirect resources simplify the PURL creation be providing only essential fields
   * PURL interface has been simplified to provide GET-only capabilities
   * Proxy resources proxy content and operations by a path prefix from the Web
   * Rewrite Rule allows complex redirection/handling of requests for resources of any path depth

## KNOWN ISSUES IN 1.5.0

 * When resource labels are updated, referencing resource may not display new
   label until they are modified as well or the callimachus-reset script is run.
 * Template variables are only bound if there is at least one triple binding after them.
 * On Windows systems with certain installations of Git you may see an error on startup e.g. 'couldn't find "#"'. If this happens, make sure that the System32 folder is before Git's tools folder on the path so that the correct version of "find" is found.

## HOWTO UPGRADE

Stop the server using the callimachus-stop script in the bin directory.
Remove the lib, bin, tmp, src directories.

Download and unzip the callimachus zip archive file to the install directory of
the Callimachus instance to be upgraded. Run the bin/callimachus-setup script to
upgrade the repositories directory. Use the "-K" flag to disabled automatic
backup.

Templates using the 1.3 syntax must be updated to use the new calli. functions
available in 1.4.0 and 1.5.0.
 * Links to <aClass?create> must be changed to `<aFolder?create=aClass>` syntax
 * Create forms must be changed to have `enctype="text/turtle"`
 * Create forms must now have
     `onsubmit="calli.submitTurtle(event,calli.slugify($('#label').val()))"`
 * Edit template body tags must be
     `<body resource="?this" onload="comparison=calli.copyResourceData('#form')">`
     Where "form" is the @id of the form tag.
 * Edit forms must have `onsubmit="calli.submitUpdate(comparison,event)"`
 * Both create and edit forms' input and textarea tags must include
     `onchange="calli.updateProperty(event, 'rdfs:label')"`
     Where rdfs:label is the datatype property of the field
 * `<select/>`, `type="checkbox"`, and `type="radio"` fields that are populated from
     the RDF store must be replaced with an RDF Named Query and included using
     `<xi:include href="select-query.rq?select" />` or another pragrma
     ?select, ?checkbox, ?checkbox-include, ?radio, ?radio-inline.
     The following (and selectEachResourceIn) copies RDFa data to a field:
     `$('[rel="dcterms:type"].hidden').each(calli.checkEachResourceIn('#type')).remove();`
 * View templates must replace `<pre class="wiki" property="rdfs:comment" />` with
     `<p property="rdfs:comment" />` if no wiki syntax is used or
```
    <pre property="rdfs:comment" />
    <script type="text/javascript">$(function($){
        $('pre[property]').replaceWith(calli.parseCreole);
    });</script>
```
 * Replace `<iframe class="flex"/>` with `calli.fillElement('iframe')`
 * Replace `<aside class="optional"/>` with `$('aside').filter(calli.isEmptyResource).remove();`
 * Remove class attributes from `<time/>` and use [calli.parseDateTime](http://callimachusproject.org/callimachus-reference#parseDateTime) to format
 * calliSubmit, calliRedirect and other calli* events must be replaced with [a function call](http://callimachusproject.org/callimachus-reference#JavaScript_Reference)
 * When [creating RDF resources using the Callimachus REST API](http://callimachusproject.org/callimachus-reference#RDF_Create), use text/turtle instead of sparql-update
 * Links with class "view" and "diverted" have no special behaviour
 

