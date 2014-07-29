Callimachus Project Release Notes

http://callimachusproject.org/

29 July 2014

= REQUIREMENTS FOR CALLIMACHUS 1.4.0 (Open Source) =

Callimachus works best on browsers that are closely tracking the development
of HTML5.  At the time of this writing, Callimachus works best on Chrome and
Firefox.  It is recommended that Callimachus users track updates to their
browsers for the best usage experience.

Chrome 35 has been thoroughly tested.
Firefox 31 has no known issues.
Safari 6.1 and 7 has known issues.
Internet Explorer 11 has known issues.

Details of browser support may be found at:
  http://callimachusproject.org/docs/1.2/articles/browser-support.docbook?view

The Callimachus server requires Java JDK 1.7 on the server to run.

= NEW IN VERSION 1.4.0 =

 * .xsl and .xq files now have ?results pragma to execute without a pipeline
 * Markdown .md files can now be created and stored in Callimachus
 * SPARQL property paths can now be used within template expressions, such as
     <h1>{rdfs:label|foaf:name}</h1>
 * RDF Named Queries can now use ?results&xtq=out:sparql-json to return results
     in http://www.w3.org/TR/sparql11-results-json/
 * ?select, ?checkbox, and ?radio pragmas are now available to RDF Named Query
     for use as XInclude targets in templates
 * Selectize.js is now included per default in every Callimachus page
     http://brianreavis.github.io/selectize.js/
 * Many new calli. javascript functions to make it easier to build complicated
     Callimachus applications

= KNOWN ISSUES IN 1.4.0 =

 * All possible options in check boxes must have a label.
 * rdf:XMLLiteral on form create auto adds xmlns to elements.
 * When resource labels are updated, referencing resource may not display new
   label until they are modified as well or the callimachus-reset script is run.
 * Template variables are only bound if there is at least one triple binding after them.
 * Large folder imports will only succeed if importing into an empty folder.

= HOWTO UPGRADE =

Stop the server using the callimachus-stop script in the bin directory.
Remove the lib, bin, tmp, src directories.

Download and unzip the callimachus zip archive file to the install directory of
the Callimachus instance to be upgraded. Run the bin/callimachus-setup script to
upgrade the repositories directory. Use the "-K" flag to disabled automatic
backup.

Templates using the 1.3 syntax should be updated to use the new calli. functions
available in 1.4.0.
 * Create pages should not be accessed using the "Create a new ..." Class menu
     item, instead should be access from the folder create menu
 * Links to <aClass?create> should be changed to <aFolder?create=aClass> syntax
 * Save-As dialogue is now deprecated in create pages and should not be used
 * Create forms should be changed to have enctype="text/turtle"
 * Create forms should now have
     onsubmit="calli.submitTurtle(event,calli.slugify($('#label').val()))"
 * Edit template body tags should be
     <body resource="?this" onload="comparison=calli.copyResourceData('#form')">
     Where "form" is the @id of the form tag.
 * Edit forms should have onsubmit="calli.submitUpdate(comparison,event)"
 * Both create and edit forms' input and textarea tags should include
     onchange="calli.updateProperty(event, 'rdfs:label')"
     Where rdfs:label is the datatype property of the field
 * <select/>, type="checkbox", and type="radio" fields that are populated from
     the RDF store should be replaced with an RDF Named Query and included using
     <xi:include href="select-query.rq?select" /> or another pragrma
     ?select, ?checkbox, ?checkbox-include, ?radio, ?radio-inline.
     The following (and selectEachResourceIn) copies RDFa data to a field:
     $('[rel="dcterms:type"].hidden').each(calli.checkEachResourceIn('#type')).remove();
 * View templates should replace <pre class="wiki" property="rdfs:comment" /> with
     <p property="rdfs:comment" /> if no wiki syntax is used or
     <pre property="rdfs:comment" />
     <script type="text/javascript">$(function($){
         $('pre[property]').replaceWith(calli.parseCreole);
     });</script>
 * Replace <iframe class="flex"/> with calli.fillElement('iframe')
 * Replace <asibe class="optional"/> with $('aside').filter(calli.isEmptyResource).remove();
 * calliSubmit, calliRedirect and other calli* events should no longer be used

