<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="xhtml"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <xsl:import href="graph.xsl" />
    <xsl:param name="target" />
    <xsl:template match="/">
        <html>
            <head>
                <title>
                    <xsl:call-template name="resource">
                        <xsl:with-param name="iri" select="$target"/>
                    </xsl:call-template>
                </title>
                <style>
                    ul.properties { margin-top: 0px; }
                    li.triple { list-style-type: none }
                    .plain { font-size: large; }
                    .describe { font-size: xx-small; }
                    .bnode, .plain { font-family: monospace; white-space: pre-wrap; }
                    .typed { color: magenta; }
                    .datatype, .language { color: gray; }
                    .predicate { color: darkgreen; }
                </style>
                <script type="text/javascript">
                // <![CDATA[
                jQuery(function($) {
                    var index = {};
                    $($('#results').children('div[resource]').get()).each(function() {
                        var first = index[this.getAttribute('resource')];
                        if (first && $(first).children('ul')) {
                            $(this).children('ul').contents().appendTo($(first).children('ul'));
                            $(this).remove();
                        } else {
                            index[this.getAttribute('resource')] = this;
                        }
                    });
                    $('.describe a').mousedown(function() {
                        if (this.getAttribute('resource'))
                            return true;
                        var resource = this.href;
                        this.setAttribute('resource', resource);
                        var hash = resource.indexOf('#');
                        if (hash < 0) {
                            this.href = window.calli.diverted(resource, 'describe');
                        } else {
                            var uri = resource.substring(0, hash);
                            var frag = resource.substring(hash);
                            this.href = window.calli.diverted(uri, 'describe') + frag;
                        }
                        return true;
                    });
                    $('#rdfxml').click(function(event) {
                        event.preventDefault();
                        var req = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
                        req.open('GET', '?describe', true);
                        req.setRequestHeader("Accept", "application/rdf+xml");
                        req.onreadystatechange = function () {
                            if (req.readyState != 4) return;
                            if (req.status == 200 || req.status == 304) {
                                var win = window.open('', document.URL);
                                win.document.write('<pre>\n' + req.responseText.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '\n</pre>');
                            }
                        }
                        if (req.readyState == 4) return false;
                        req.send(null);
                        return false;
                    });
                    $('#turtle').click(function(event) {
                        event.preventDefault();
                        var req = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
                        req.open('GET', '?describe', true);
                        req.setRequestHeader("Accept", "text/turtle");
                        req.onreadystatechange = function () {
                            if (req.readyState != 4) return;
                            if (req.status == 200 || req.status == 304) {
                                var win = window.open('', document.URL);
                                win.document.write('<pre>\n' + req.responseText.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '\n</pre>');
                            }
                        }
                        if (req.readyState == 4) return false;
                        req.send(null);
                        return false;
                    });
                });
                // ]]>
                </script>
            </head>
            <body>
                <h1>
                    <xsl:call-template name="resource">
                        <xsl:with-param name="iri" select="$target"/>
                    </xsl:call-template>
                    <xsl:text> Resource</xsl:text>
                </h1>
                <div id="sidebar">
                    <aside>
                        <p>As <a href="#" id="rdfxml">RDF/XML</a></p>
                        <p>As <a href="#" id="turtle">Turtle</a></p>
                    </aside>
                </div>
                <div id="results">
                    <xsl:apply-templates />
                </div>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
