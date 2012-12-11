<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0" name="pipeline" type="calli:page-template"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:xinclude name="xinclude" fixup-xml-base="true" fixup-xml-lang="true" />
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/make-paths-absolute.xsl" />
        </p:input>
    </p:xslt>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/page-form-expressions.xsl" />
        </p:input>
    </p:xslt>
</p:pipeline>
