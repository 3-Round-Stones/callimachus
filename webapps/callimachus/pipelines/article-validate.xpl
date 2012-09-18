<?xml version="1.0" encoding="UTF-8" ?>
 <p:pipeline
        name="pipeline"
        version="1.0"
        xmlns:p     ="http://www.w3.org/ns/xproc"
        xmlns:c     ="http://www.w3.org/ns/xproc-step"
        xmlns:l     ="http://xproc.org/library">

    <p:serialization port="result" media-type="text/text" method="text" />
    
    <p:try>
        <p:group>
            <p:validate-with-relax-ng>
                <p:input port="schema">
                    <p:document href="../schemas/docbookxi.rng" />
                </p:input>                
            </p:validate-with-relax-ng>
            <!-- 
                Validation was successful, so output an empty document instead of
                validation source
              -->
            <p:identity>
                <p:input port="source">
                    <p:inline>
                        <c:data content-type="text/plain" />
                    </p:inline>
                </p:input>
            </p:identity>
        </p:group>
        <p:catch name="error-state">
            <!-- 
                Validation was not successful, output the error document 
                (it's text content will be output since the pipeline is set to text serialization )
              -->
            <p:identity>
                <p:input port="source">
                    <p:pipe step="error-state" port="error" />
                </p:input>                                                
            </p:identity>
        </p:catch>
    </p:try>
</p:pipeline>
