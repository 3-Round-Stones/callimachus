<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library">

    <p:serialization port="result" media-type="text/html" encoding="UTF-8" method="html" doctype-system="about:legacy-compat" />

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/sparql-results-html.xsl" />
        </p:input>
    </p:xslt>

</p:pipeline>