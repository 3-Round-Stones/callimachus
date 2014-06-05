xquery version "1.0" encoding "utf-8";

declare namespace sparql = "http://www.w3.org/2005/sparql-results#";

declare variable $query external;
declare variable $target external;
declare variable $mode as xs:string external;
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
return <select xmlns="http://www.w3.org/1999/xhtml" class="form-control">
{
    if (contains($mode,"multiple"))
        then attribute multiple {'multiple'}
        else ()
}
{
    if ($property)
        then attribute onchange {concat('calli.updateProperty(event, "', $property, '")')}
        else if ($rel) then attribute onchange {concat('calli.updateResource(event, "', $rel, '")')}
        else ()
}
{
    if ($id)
        then attribute id {$id}
        else ()
}
{
    if ($name)
        then attribute name {$name}
        else ()
}
{
    for $result in $sparql//sparql:result
    let $value := $result/sparql:binding[@name=$first]/*
    let $label := $result/sparql:binding[@name=$second]/*
    let $vattr := $value/self::sparql:literal/@*
    let $lattr := $label/self::sparql:literal/@*
    let $text := if ($label) then $label/text() else $value/text()
    return if ($value/self::sparql:literal)
        then <option value="{$value/text()}">{$vattr}{$text}</option>
        else <option value="{$value/text()}">{$lattr}{$text}</option>
}
</select>
