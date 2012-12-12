xquery version "1.0" encoding "utf-8";

import module namespace calli = "http://callimachusproject.org/rdf/2009/framework#" at "layout-elements.xq";

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
<head>
    <meta name="viewport" content="width=device-width,height=device-height,initial-scale=1.0,target-densityDpi=device-dpi"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
    <link rel="home" href="/" />
    <link rel="icon" href="/favicon.ico" />
    <!--[if gt IE 6]><!-->
    <link rel="stylesheet" href="{resolve-uri('../theme/theme.less?less')}" />
    <script type="text/javascript" src="{resolve-uri('/callimachus/scripts.js')}" />
    <script type="text/javascript" src="{resolve-uri('../scripts/discussion.js')}" />
    <!--[if lt IE 9]>
        <script>document.documentElement.className+=' ie'</script>
        <script src="//html5shim.googlecode.com/svn/trunk/html5.js"></script>
        <script src="//cdn-delivery.commondatastorage.googleapis.com/spiders-design/theme/js/selectivizr.js"></script>
    <![endif]-->
    <!--<![endif]-->
    {$calli:head}
</head>
<body>{$calli:body}
    <div id="wrapper">
        <div class="navbar hidden-iframe">
            <header class="navbar-inner">
                <div class="container">
                    <a id="branding" class="brand" href="/">Callimachus</a>
                    {calli:breadcrumb(<span class="divider">&#187;</span>)}
                    <menu type="toolbar" class="nav pull-right">
                        <li class="hidden-login">
                            {calli:login('Login')}
                        </li>
                        <li class="hidden-logout dropdown">
                            <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                                <i class="icon-cog"></i>
                                <i class="caret"></i>
                            </a>
                            <menu type="list" class="dropdown-menu">
                                <li><a href="/?view">Home folder</a></li>
                                <li><a href="/?changes">Recent changes</a></li>
                                <li class="divider"></li>
                                <li><a href="javascript:location='?whatlinkshere'">What links here</a></li>
                                <li><a href="javascript:location='?relatedchanges'">Related changes</a></li>
                                <li><a href="javascript:location='?permissions'">Permissions</a></li>
                                <li><a href="javascript:location='?introspect'">Introspect resource</a></li>
                                <li><a href="javascript:print()">Print this page</a></li>
                                <li class="divider"></li>
                                <li><a href="http://callimachusproject.org/">About Callimachus</a></li>
                                <li><a href="http://code.google.com/p/callimachus/wiki/GettingStarted">Getting started</a></li>
                                <li><a href="http://groups.google.com/group/callimachus-discuss">Send feedback</a></li>
                                <li class="divider"></li>
                                <li>{calli:profile('Account')}</li>
                                <li>{calli:logout('Sign out')}</li>
                            </menu>
                        </li>
                    </menu>
                    <form class="navbar-search pull-right hidden-logout" method="GET" action="/">
                        <input type="text" name="q" class="search-query" placeholder="Lookup..." />
                    </form>
                </div>
            </header>
        </div>
        <div id="page" class="container">
            {$calli:sidebar}
            <div>
                {calli:access(<nav id="access" class="hidden-logout hidden-iframe nav-tabs">
                    <a tabindex="1" href="?view" onclick="location.replace(href);return false">View</a>
                    <a tabindex="2" href="?edit" onclick="location.replace(href);return false">Edit</a>
                    <a tabindex="3" href="?discussion" onclick="location.replace(href);return false">Discussion</a>
                    <a tabindex="4" href="?describe" onclick="location.replace(href);return false">Describe</a>
                    <a tabindex="5" href="?history" onclick="location.replace(href);return false">History</a>
                </nav>)}

                <div id="container">
                    {$calli:hgroup}
                    <div id="flash"><!-- #flash is used to place error messages on the screen --></div>
                    <div id="content">{$calli:content}</div>
                </div><!-- container -->
            </div>
        </div><!-- page -->
    </div><!-- wrapper -->

    <footer id="footer" class="hidden-iframe">
        <div id="colophon" class="container">
            <p id="site-generator">
                <a href="http://callimachusproject.org/" title="Callimachus">
                    <img src="{resolve-uri('../images/callimachus-powered.png')}" alt="Callimachus" width="98" height="35" />
                </a>
            </p>

            {calli:lastmod(<p>This resource was last modified at <time class="abbreviated"/></p>)}
        </div>
    </footer>
    <!--[if lt IE 9]>
        <script src="//raw.github.com/chuckcarpenter/REM-unit-polyfill/master/js/rem.js"></script>
    <![endif]-->
</body>
</html>
