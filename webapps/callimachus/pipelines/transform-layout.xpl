<?xml version="1.0" encoding="UTF-8" ?>
 <p:library
    version     ="0.5"
    xmlns:p     ="http://www.w3.org/ns/xproc"
    xmlns:c     ="http://www.w3.org/ns/xproc-step"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
    xmlns:l     ="http://xproc.org/library">
    <p:declare-step type="calli:transform-layout" name="transform-layout">
        <p:input    port="source" />
        <p:output   port="result" />
        <p:option   name="this"     required="true" />
        <p:option   name="query"    required="true" />        
        <p:option   name="systemId" required="true" />
        <p:variable name="xsltId"   select="resolve-uri('../template.xsl')"/>
        <p:xslt>
            <p:input port="source">
                <p:pipe step="transform-layout" port="source"/>
            </p:input>        
            <p:input port="stylesheet">
                <p:document href="../template.xsl" />
            </p:input>
            <p:with-param name="systemId" select="$systemId" />
            <p:with-param name="xsltId"   select="$xsltId"   />
            <p:with-param 
                name="realm"
                select="doc(concat('../queries/find-realm.rq?results&amp;this=', encode-for-uri($this)))
                    /sparql:sparql/sparql:results/sparql:result/sparql:binding[@name='realm']/*"/>
        </p:xslt>
        <p:xslt>
            <p:input port="stylesheet">
                <p:document href="../transforms/page.xsl" />
            </p:input>
            <p:with-param name="this" select="$this"/>
            <p:with-param name="query" select="$query"/>
        </p:xslt>
        <p:xslt>
            <!-- Needed to avoid http://www.w3.org/TR/xproc/#err.S0055  -->
            <p:with-param name="mode" select="'debug'"/> 
            
            <p:input port="stylesheet">
                <p:document href="../transforms/xhtml-to-html.xsl" />
            </p:input>
        </p:xslt>
    </p:declare-step>
</p:library>