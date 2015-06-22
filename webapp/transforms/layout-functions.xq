xquery version "1.0" encoding "utf-8";
(:
 : Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 :
 : Licensed under the Apache License, Version 2.0 (the "License");
 : you may not use this file except in compliance with the License.
 : You may obtain a copy of the License at
 :
 :     http://www.apache.org/licenses/LICENSE-2.0
 :
 : Unless required by applicable law or agreed to in writing, software
 : distributed under the License is distributed on an "AS IS" BASIS,
 : WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 : See the License for the specific language governing permissions and
 : limitations under the License.
 :
 :)

module namespace calli = "http://callimachusproject.org/rdf/2009/framework#";

declare copy-namespaces preserve, inherit;
declare default element namespace "http://www.w3.org/1999/xhtml";

declare variable $calli:realm external;

(: template nodes :)
declare variable $calli:html-element := /html;

declare function calli:head-nodes() as node()* {
    $calli:html-element/head/node()
};
declare function calli:body-attributes() as attribute(*)* {
    $calli:html-element/body/@*
};
declare function calli:body-nodes() as node()* {
    $calli:html-element/body/node()
};
declare function calli:head-links($a as element(), $divider as element()) as element()* {
    let $links := $calli:html-element/head/link[@title and @href]
    return if ($links) then
        (
            for $link in $links
            return calli:add-href($a, $link, $link/@title),
            $divider
        )
    else
        $links
};
declare function calli:add-href($a as element(), $link as element(), $label as xs:string) {
    if (local-name($a)='a') then
        element {node-name($a)} {
            $a/@*[name()!='class'],
            for $attr in $link/@*[name()!='id']
            return if ($a/@*[name()=name($attr)]) then ()
            else $attr,
            if ($a/@class and $link/@class) then attribute class {concat($a/@class,' ',$link/@class)}
            else if ($a/@class) then $a/@class
            else (),
            $label
        }
    else if ($a/self::*) then
        element {node-name($a)} {
            $a/@*,
            for $node in $a/node()
            return calli:add-href($node, $link, $label)
        }
    else
        $a
        
};

(: static markup functions :)
declare function calli:styles-href($link as element()) as element() {
    element {node-name($link)} {
        attribute href {resolve-uri('../styles/callimachus.less?less')},
        $link/@*[name()!='href'],
        $link/node()
    }
};
declare function calli:scripts-src($script as element()) as element() {
    element {node-name($script)} {
        attribute src {resolve-uri('../../scripts.js')},
        $script/@*[name()!='src'],
        $script/node()
    }
};
declare function calli:lookup-href($link as element()) as element() {
    element {node-name($link)} {
        attribute href {concat($calli:realm, '?q')},
        $link/@*[name()!='href'],
        $link/node()
    }
};
declare function calli:lookup-form($placeholder as xs:string) as element(form) {
    <form class="navbar-form" role="search" method="GET" action="{$calli:realm}">
        <div class="input-group">
            <input type="text" name="q" class="form-control pull-right" placeholder="{$placeholder}" />
            <span class="input-group-btn">
                <button class="btn btn-default" type="submit">
                    <span class="glyphicon glyphicon-search" />
                    <span class="sr-only">Search</span>
                </button>
            </span>
        </div>
    </form>
};
declare function calli:callimachus-about-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {'http://callimachusproject.org/'},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:callimachus-getting-started-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {resolve-uri('../../getting-started-with-callimachus')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:callimachus-feedback-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {'http://groups.google.com/group/callimachus-discuss'},
        $a/@*[name()!='href'],
        $a/node()
    }
};

declare function calli:breadcrumb-list($ol as element()) as element(nav) {
    <nav id="calli-if-breadcrumb">{
        element {node-name($ol)} {
            $ol/@*,
            $ol/node(),
            <nav id="calli-breadcrumb"><li><a/></li><li class="active"/></nav>
        }
    }</nav>
};
declare function calli:breadcrumb-links($breadcrumb as element(), $divider as item(), $active as element()) as element(nav) {
    <nav id="calli-breadcrumb">{$breadcrumb}{$divider}{$active}</nav>
};
declare function calli:breadcrumb-links($breadcrumb as item()*, $active as element()) as element(nav) {
    <nav id="calli-breadcrumb">{$breadcrumb}{$active}</nav>
};
declare function calli:activate-nav($nav as node()*) as element(nav) {
    <nav id="calli-access">{$nav}</nav>
};
declare function calli:lastmod-time($time as element()) as element(div) {
    <div id="calli-lastmod">{$time}</div>
};
declare function calli:error-alert($div as element()) as element(div) {
    <div id="calli-error">
        <div id="calli-error-template" style="display:none">{$div}</div>
    </div>
};
declare function calli:generator-p($p as element()) as element() {
    element { node-name($p) } {
        $p/@*,
        $p/node(), 
        <a href="http://callimachusproject.org/" title="Callimachus">
            <img src="{resolve-uri('../../callimachus-powered.png')}" alt="Callimachus" width="98" height="35" />
        </a>
    }
};

(: navigation links :)
declare function calli:home-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {$calli:realm},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:folder-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?view')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:changes-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?changes')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:view-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?view"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:edit-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?edit"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:discussion-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?discussion"},
        $a/@*[name()!='href'],
        <script type="text/javascript">
            calli.insertDiscussionBadge('?discussion', $('script').get(-1).parentNode);
        </script>,
        $a/node()
    }
};
declare function calli:describe-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?describe"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:history-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?history"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:whatlinkshere-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?whatlinkshere"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:relatedchanges-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?relatedchanges"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:permissions-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?permissions"},
        if ($a/@onclick) then $a/@onclick
            else attribute onclick {"if(location.search!='?view')return true;calli.openDialog(this.href,this.textContent);return false;"},
        $a/@*[name()!='href' and name()!='click'],
        $a/node()
    }
};
declare function calli:introspect-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {"?introspect"},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:admin-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {resolve-uri('../pages/invite-users.xhtml?view')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:login-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {concat($calli:realm,'?login')},
        $a/@*[name()!='href'],
        $a/node()
    }
};
declare function calli:profile-href($a as element()) as element()+ {
    (element {node-name($a)} {
        attribute href {concat($calli:realm,'?profile')},
        $a/@*[name()!='href'],
        $a/node()
    }, <script type="text/javascript">
        calli.resolve($('{node-name($a)}').last()).then(function(node){{
            calli.getCurrentUserName().then(function(username){{
                node.text(username || '');
            }});
        }});
    </script>)
};
declare function calli:logout-href($a as element()) as element() {
    element {node-name($a)} {
        attribute href {$calli:realm},
        attribute onclick {concat('calli.logout("',$calli:realm,'").then(location.assign.bind(location,"',$calli:realm,'")).then(undefined, calli.loading(event.target));return false')},
        $a/@*[name()!='href' and name()!='id'],
        $a/node()
    }
};

(: deprecated since 1.3 :)
declare function calli:body-sidebar($div as element()) as element()? {
    ()
};
declare function calli:body-hgroup() as element()? {
    ()
};

(: deprecated since 1.2 :)
declare function calli:breadcrumb-nav($breadcrumb as element()) as element(nav) {
    <nav id="calli-breadcrumb">{$breadcrumb}</nav>
};

(: deprecated since 1.0.1 :)
declare function calli:styles() as element() {calli:styles-href(<link rel="stylesheet" />)};
declare function calli:scripts() as element() {calli:scripts-src(<script type="text/javascript" />)};
declare function calli:lookup($placeholder as xs:string) as element(form) {calli:lookup-form($placeholder)};
declare function calli:home($a as element()) as element() {calli:home-href($a)};
declare function calli:folder($a as element()) as element() {calli:folder-href($a)};
declare function calli:changes($a as element()) as element() {calli:changes-href($a)};
declare function calli:pageLinks($a as element(), $divider as element()) as element()* {calli:head-links($a, $divider)};
declare function calli:whatlinkshere($a as element()) as element() {calli:whatlinkshere-href($a)};
declare function calli:relatedchanges($a as element()) as element() {calli:relatedchanges-href($a)};
declare function calli:permissions($a as element()) as element() {calli:permissions-href($a)};
declare function calli:introspect($a as element()) as element() {calli:introspect-href($a)};
declare function calli:login($a as element()) as element() {calli:login-href($a)};
declare function calli:profile($a as element()) as element() {calli:profile-href($a)};
declare function calli:logout($a as element()) as element() {calli:logout-href($a)};
declare function calli:breadcrumb($breadcrumb as element()) as element(nav) {calli:breadcrumb-nav($breadcrumb)};
declare function calli:activateNav($nav as node()*) as element(nav) {calli:activate-nav($nav)};
declare function calli:lastmod($time as element()) as element(div) {calli:lastmod-time($time)};
declare function calli:error($div as element()) as element(div) {calli:error-alert($div)};
declare function calli:generator($p as element()) as element() {calli:generator-p($p)};
declare function calli:head() as node()* {calli:head-nodes()};
declare function calli:bodyAttributes() as attribute(*)* {calli:body-attributes()};
declare function calli:sidebar($div as element()) as element()? {calli:body-sidebar($div)};
declare function calli:hgroup() as element()? {calli:body-hgroup()};
declare function calli:content() as node()* {calli:body-nodes()};
