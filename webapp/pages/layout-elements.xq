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
    <a id="logout-link" href="{$calli:realm}?logout">{$text}</a>
};

declare function calli:lastmod($time as element()) as element(div) {
    <div id="calli-lastmod">{$time}</div>
};
declare function calli:activateLink($nav as element()) as element(nav) {
    <nav id="calli-access">{$nav}</nav>
};
declare function calli:breadcrumb($divider) as element(nav) {
    <nav id="calli-breadcrumb">{$divider}</nav>
};

declare variable $calli:html := /html;

declare function calli:head() as node()* {
    $calli:html/head/node()
};
declare function calli:bodyAttributes() as attribute(*)* {
    $calli:html/body/@*
};

declare function calli:sidebar() as element(div)? {
    $calli:html/body/div[@id='sidebar']
};
declare function calli:hgroup() as element()? {
    ($calli:html/body/*[self::h1 or self::hgroup])[1]
};
declare function calli:content() as node()* {
    let $hgroup := ($calli:html/body/*[self::h1 or self::hgroup])[1]
    return $calli:html/body/node()[not(self::*=$hgroup or self::div[@id='sidebar'])]
};
