<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" type="calli:docbook-inclusion"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:d="http://docbook.org/ns/docbook"
        xmlns:xl="http://www.w3.org/1999/xlink"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="application/docbook+xml" />

    <p:xinclude fixup-xml-base="true" fixup-xml-lang="true" />

    <p:make-absolute-uris match="@fileref|@xl:href" />

    <p:rename new-name="d:section" match="d:preface/d:article|d:partintro/d:article|d:article/d:article|d:chapter/d:article|d:appendix/d:article|d:section/d:article|d:topic/d:article" />

    <!-- rename duplicate IDs -->
    <p:xslt>
        <p:input port="stylesheet">
            <p:inline>
                <xsl:stylesheet version="1.0">
                    <xsl:template match="@*|node()">
                        <xsl:copy>
                            <xsl:apply-templates select="@*|node()"/>
                        </xsl:copy>
                    </xsl:template>
                    <xsl:template match="@xml:id">
                        <xsl:variable name="id" select="."/>
                        <xsl:variable name="preceding" select="count(../preceding::*[@xml:id = $id])"/>
                        
                        <xsl:choose>
                            <xsl:when test="$preceding != 0">
                                <xsl:attribute name="xml:id">
                                    <xsl:value-of select="concat($id, $preceding)"/>
                                </xsl:attribute>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:template>
                </xsl:stylesheet>
            </p:inline>
        </p:input>
    </p:xslt>

</p:pipeline>
