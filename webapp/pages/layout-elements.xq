xquery version "1.0" encoding "utf-8";

module namespace calli = "http://callimachusproject.org/rdf/2009/framework#";

declare default element namespace "http://www.w3.org/1999/xhtml";

declare variable $calli:realm external;

declare function calli:login($text) as element(a) {
    <a id="login-link" href="{$calli:realm}?login">{$text}</a>
};

declare function calli:profile($text) as element(a) {
    <a id="profile-link" href="{$calli:realm}?profile">{$text}</a>
};

declare function calli:logout($text) as element(a) {
    <a id="login-link" href="{$calli:realm}?logout">{$text}</a>
};

declare function calli:lastmod($time as element(p)) as element(div) {
    <div id="resource-lastmod">{$time}</div>
};

declare variable $calli:breadcrumb := 
    <nav id="breadcrumbs" class="nav">
        <span>...</span>
        <span class="divider">&#187;</span>
        <a href="?up">Up</a>
        <span class="divider">&#187;</span>
        <span class="active">Here</span>
    </nav>;

declare variable $calli:head :=
    let $first_script := /html/head/script[1]
    let $before :=
        if ($first_script) then
            $first_script/preceding-sibling::node()
        else
            /html/head/node()
    let $after := $first_script|$first_script/following-sibling::node()
    let $web_bundle := <script type="text/javascript" src="{resolve-uri('../scripts/web_bundle?source')}" />
    let $form_bundle := <script type="text/javascript" src="{resolve-uri('../scripts/form_bundle?source')}" />
    return if (//form) then
        ($before, $web_bundle, $form_bundle, $after)
    else
        ($before, $web_bundle, $after)
;

declare variable $calli:sidebar := /html/body/div[@id='sidebar'];
declare variable $calli:body := /html/body/@*;
declare variable $calli:hgroup :=
    let $h1 := (/html/body/*[self::h1 or self::hgroup])[1]
    return if ($calli:sidebar) then
        $calli:sidebar/preceding-sibling::node()
    else
        ($h1/preceding-sibling::node(), $h1);
declare variable $calli:content :=
    if ($calli:sidebar) then
        $calli:sidebar/following-sibling::node()
    else if (/html/body/h1|/html/body/hgroup) then
        (/html/body/*[self::h1 or self::hgroup])[1]/following-sibling::node()
    else if (/html/body) then
        /html/body/node()
    else /node();
