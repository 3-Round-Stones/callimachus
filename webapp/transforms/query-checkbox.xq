xquery version "1.0" encoding "utf-8";

declare namespace sparql = "http://www.w3.org/2005/sparql-results#";

declare variable $mode as xs:string external;

declare variable $query external;
declare variable $target external;
declare variable $id as xs:string external;
declare variable $name as xs:string external;
declare variable $property as xs:string external;
declare variable $rel as xs:string external;

let $url := if ($target)
    then concat($query,'&amp;target=',$target)
    else $query
let $sparql := doc($url)/sparql:sparql
let $head := $sparql/sparql:head
let $first := $head/sparql:variable[1]/@name
let $second := $head/sparql:variable[2]/@name
let $onchange := if ($property)
    then attribute onchange {concat('calli.updateProperty(event, "', $property, '")')}
    else if ($rel) then attribute onchange {concat('calli.updateResource(event, "', $rel, '")')}
    else ()
return <div xmlns="http://www.w3.org/1999/xhtml">
{
    if ($id)
        then attribute id {$id}
        else ()
}
{
    let $nattr := if ($name)
        then attribute name {$name}
        else ()
    let $type := replace($mode, "\W.*", "")
    for $result in $sparql//sparql:result
    let $value := $result/sparql:binding[@name=$first]/*
    let $label := $result/sparql:binding[@name=$second]/*
    let $vattr := $value/self::sparql:literal/@*
    let $lattr := if ($label) then $label/self::sparql:literal/@* else $vattr
    let $text := if ($label) then $label/text() else $value/text()
    return if (contains($mode,"inline"))
        then <label class="{$type}-inline">
            <input type="{$type}" value="{$value/text()}">{$onchange}{$nattr}{$vattr}</input>
            <span>{$lattr}{$text}</span>
        </label>
        else <div class="{$type}">
            <label>
                <input type="{$type}" value="{$value/text()}">{$onchange}{$nattr}{$vattr}</input>
                <span>{$lattr}{$text}</span>
            </label>
        </div>
}
</div>
