<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library">

    <p:serialization port="result" media-type="text/csv" encoding="UTF-8" method="text" />

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/sparql-results-csv.xsl" />
        </p:input>
    </p:xslt>

</p:pipeline>