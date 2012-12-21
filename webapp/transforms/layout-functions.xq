xquery version "1.0" encoding "utf-8";

module namespace calli = "http://callimachusproject.org/rdf/2009/framework#";

declare copy-namespaces preserve, inherit;
declare default element namespace "http://www.w3.org/1999/xhtml";

declare variable $calli:realm external;
declare variable $calli:html := /html;

declare function calli:styles() as element() {calli:styles(<link rel="stylesheet" />)};
declare function calli:styles($link as element()) as element() {
    element {node-name($link)} {
        attribute href {resolve-uri('/callimachus/styles.css')},
        $link/@*[name()!='href'],
        $link/node()
    }
};
declare function calli:scripts() as element() {calli:scripts(<script type="text/javascript" />)};
declare function calli:scripts($script as element()) as element() {
    element {node-name($script)} {
        attribute src {resolve-uri('/callimachus/scripts.js')},
        $script/@*[name()!='src'],
        $script/node()
    }
};
declare function calli:lookup($placeholder as xs:string) as element(form) {
    <form class="form-search" method="GET" action="{$calli:realm}">
        <input type="text" name="q" class="search-query" placeholder="{$placeholder}" />
    </form>
};
declare function calli:home($a as element()) as element() {
    element {node-name($a)} {
        attribute href {$calli:realm},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:folder($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?view')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:changes($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?changes')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:pageLinks($a as element(), $divider as element()) as element()* {
    let $links := $calli:html/head/link[@title and @href]
    return if ($links) then
        for $link in $links
        return (calli:substitute($a, $link/@href, $link/@title), $divider)
    else
        $links
};
declare function calli:substitute($a as element(), $href as xs:string, $title as xs:string) {
    if (local-name($a)='a') then
        element {node-name($a)} {
            attribute href {$href},
            $a/@*[name()!='href'],
            $title
        }
    else if ($a/self::*) then
        element {node-name($a)} {
            $a/@*,
            for $node in $a/node()
            return calli:substitute($node, $href, $title)
        }
    else
        $a
        
};
declare function calli:whatlinkshere($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"javascript:location='?whatlinkshere'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:relatedchanges($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"javascript:location='?relatedchanges'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:permissions($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"javascript:location='?permissions'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:introspect($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"javascript:location='?introspect'"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:login($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?login')},
        attribute id {"login-link"},
        $a/@*[name()!='href' and name()!='id'],
        $a/node()
    }
};
declare function calli:profile($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?profile')},
        attribute id {"profile-link"},
        $a/@*[name()!='href' and name()!='id'],
        $a/node()
    }
};
declare function calli:logout($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?logout')},
        attribute id {"logout-link"},
        $a/@*[name()!='href' and name()!='id'],
        $a/node()
    }
};

declare function calli:breadcrumb($breadcrumb as element()) as element(nav) {
    <nav id="calli-breadcrumb">{$breadcrumb}</nav>
};
declare function calli:activateNav($nav as element()) as element(nav) {
    <nav id="calli-access">{$nav}</nav>
};
declare function calli:lastmod($time as element()) as element(div) {
    <div id="calli-lastmod">{$time}</div>
};
declare function calli:error($div as element()) as element(div) {
    <div id="calli-error">
        <div id="calli-error-template" style="display:none">{$div}</div>
    </div>
};
declare function calli:generator($p as element()) as element() {
    element { node-name($p) } {
        $p/@*,
        $p/node(), 
        <a href="http://callimachusproject.org/" title="Callimachus">
            <img src="{resolve-uri('/callimachus/callimachus-powered.png')}" alt="Callimachus" width="98" height="35" />
        </a>
    }
};

declare function calli:head() as node()* {
    $calli:html/head/node()
};
declare function calli:bodyAttributes() as attribute(*)* {
    $calli:html/body/@*
};

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
