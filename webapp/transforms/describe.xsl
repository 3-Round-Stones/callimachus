<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="xhtml"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <xsl:import href="graph.xsl" />
    <xsl:param name="target" />
    <xsl:param name="query" />
    <xsl:template match="/">
        <html>
            <head>
                <title>
                    <xsl:call-template name="resource">
                        <xsl:with-param name="iri" select="/rdf:RDF/rdf:Description/@rdf:about[1]"/>
                    </xsl:call-template>
                </title>
    <link rel="help" href="{resolve-uri('../../callimachus-for-web-developers#Describe_tab')}" target="_blank" title="Help" />
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
                    $('#rdfxml').click(function(event) {
                        event.preventDefault();
                        var req = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
                        req.open('GET', calli.getPageUrl(), true);
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
                        req.open('GET', calli.getPageUrl(), true);
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
                <div class="container">
                    <div class="row">
                        <div class="col-sm-8">
                            <h1>
                                <xsl:call-template name="resource">
                                    <xsl:with-param name="iri" select="/rdf:RDF/rdf:Description/@rdf:about[1]"/>
                                </xsl:call-template>
                                <xsl:text> Resource</xsl:text>
                            </h1>
                        </div>
                        <div class="col-sm-4">
                            <aside class="well">
                                <p>As <a href="#" id="rdfxml">RDF/XML</a></p>
                                <p>As <a href="#" id="turtle">Turtle</a></p>
                            </aside>
                        </div>
                    </div>
                    <div id="results">
                        <xsl:apply-templates />
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>
    <xsl:template mode="describe" match="*[@rdf:resource]">
        <xsl:choose>
            <xsl:when test="'describe'!=$query">
                <xsl:text> </xsl:text>
                <a href="?uri={encode-for-uri(@rdf:resource)}" class="describe">»</a>
            </xsl:when>
            <xsl:when test="contains(@rdf:resource, '?')" />
            <xsl:when test="not(starts-with(@rdf:resource, 'http'))" />
            <xsl:when test="contains(@rdf:resource, '#')">
                <xsl:text> </xsl:text>
                <a href="{substring-before(@rdf:resource,'#')}?describe#{substring-after(@rdf:resource,'#')}" class="describe">»</a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text> </xsl:text>
                <a href="{@rdf:resource}?describe" class="describe">»</a>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
