xquery version "1.0" encoding "utf-8";

module namespace calli = "http://callimachusproject.org/rdf/2009/framework#";

declare copy-namespaces preserve, inherit;

declare default element namespace "http://www.w3.org/1999/xhtml";

declare variable $calli:realm external;

declare function calli:styles() as element(link) {
    <link rel="stylesheet" href="{resolve-uri('/callimachus/styles.css')}" />
};
declare function calli:scripts() as element(script) {
    <script type="text/javascript" src="{resolve-uri('/callimachus/scripts.js')}" />
};
declare function calli:lookup($placeholder as xs:string) as element(form) {
    <form class="form-search" method="GET" action="{$calli:realm}">
        <input type="text" name="q" class="search-query" placeholder="{$placeholder}" />
    </form>
};
declare function calli:home($a as element(a)) as element(a) {
    element a {
        attribute href {$calli:realm},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:folder($a as element(a)) as element(a) {
    element a {
        attribute href {concat($calli:realm,'?view')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:changes($a as element(a)) as element(a) {
    element a {
        attribute href {concat($calli:realm,'?changes')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:whatlinkshere($a as element(a)) as element(a) {
    element a {
        attribute href {"javascript:location='?whatlinkshere'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:relatedchanges($a as element(a)) as element(a) {
    element a {
        attribute href {"javascript:location='?relatedchanges'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:permissions($a as element(a)) as element(a) {
    element a {
        attribute href {"javascript:location='?permissions'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:introspect($a as element(a)) as element(a) {
    element a {
        attribute href {"javascript:location='?introspect'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:login($a as element(a)) as element(a) {
    element a {
        attribute href {concat($calli:realm,'?login')},
        attribute id {"login-link"},
        $a/@*[name()!='href' and name()!='id'],
        $a/node()
    }
};
declare function calli:profile($a as element(a)) as element(a) {
    element a {
        attribute href {concat($calli:realm,'?profile')},
        attribute id {"profile-link"},
        $a/@*[name()!='href' and name()!='id'],
        $a/node()
    }
};
declare function calli:logout($a as element(a)) as element(a) {
    element a {
        attribute href {concat($calli:realm,'?logout')},
        attribute id {"logout-link"},
        $a/@*[name()!='href' and name()!='id'],
        $a/node()
    }
};

declare function calli:breadcrumb($divider) as element(nav) {
    <nav id="calli-breadcrumb">{$divider}</nav>
};
declare function calli:activateNav($nav as element()) as element(nav) {
    <nav id="calli-access">{$nav}</nav>
};
declare function calli:lastmod($time as element()) as element(div) {
    <div id="calli-lastmod">{$time}</div>
};
declare function calli:error($div as element()) as element() {
    <div id="calli-error">
        <div id="calli-error-template" style="display:none">{$div}</div>
    </div>
};
declare function calli:generator() as element(p) {calli:generator(<p />)};
declare function calli:generator($p as element()) as element() {
    element { node-name($p) } {
        $p/@*,
        $p/node(), 
        <a href="http://callimachusproject.org/" title="Callimachus">
            <img src="{resolve-uri('/callimachus/callimachus-powered.png')}" alt="Callimachus" width="98" height="35" />
        </a>
    }
};

declare variable $calli:html := /html;

declare function calli:head() as node()* {
    $calli:html/head/node()
};
declare function calli:bodyAttributes() as attribute(*)* {
    $calli:html/body/@*
};

declare function calli:sidebar() as element(div)? {calli:sidebar(<div />)};
declare function calli:sidebar($div as element()) as element()? {
    let $sidebar := $calli:html/body/div[@id='sidebar'][1]
    return if ($sidebar) then
        element { node-name($div) } { $div/@*, $div/node(), $sidebar/node() }
    else
        $sidebar
};
declare function calli:hgroup() as element()? {
    ($calli:html/body/*[self::h1 or self::hgroup])[1]
};
declare function calli:content() as node()* {
    let $hgroup := ($calli:html/body/*[self::h1 or self::hgroup])[1]
    return $calli:html/body/node()[not(self::div[@id='sidebar']) and (not(self::h1 or self::hgroup) or preceding-sibling::h1 or preceding-sibling::hgroup)]
};
