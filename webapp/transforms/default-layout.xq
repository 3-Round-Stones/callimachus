xquery version "1.0" encoding "utf-8";

import module namespace calli = "http://callimachusproject.org/rdf/2009/framework#" at "/callimachus/layout-functions.xq";

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
<head>
    <meta name="viewport" content="width=device-width,height=device-height,initial-scale=1.0,target-densityDpi=device-dpi"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <link rel="icon" href="/favicon.ico" />
    <!--[if gt IE 6]><!-->
    {calli:styles()}
    {calli:scripts()}
    <!--[if lt IE 9]>
        <script>document.documentElement.className+=' ie'</script>
        <script src="//html5shim.googlecode.com/svn/trunk/html5.js"></script>
        <script src="//cdn-delivery.commondatastorage.googleapis.com/spiders-design/theme/js/selectivizr.js"></script>
    <![endif]-->
    <!--<![endif]-->
    {calli:head()}
</head>
<body>
    {calli:bodyAttributes()}
    <div class="until-navbar-large">
        <div class="navbar navbar-static-top hidden-iframe">
            <header class="navbar-inner">
                <div class="container">
                    {calli:home(<a class="brand">Callimachus</a>)}
                    <menu type="toolbar" class="nav pull-right">
                        <li class="hidden-login">
                            {calli:login(<a>Sign in</a>)}
                        </li>
                        <li class="hidden-logout dropdown">
                            <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                                <i class="icon-cog"></i>
                                <i class="caret"></i>
                            </a>
                            <menu type="list" class="dropdown-menu">
                                <li>{calli:folder(<a>Home folder</a>)}</li>
                                <li>{calli:changes(<a>Recent changes</a>)}</li>
                                <li class="divider"></li>
                                <li><a href="http://callimachusproject.org/">About Callimachus</a></li>
                                <li><a href="http://callimachusproject.org/docs/1.0/getting-started-with-callimachus.docbook?view">Getting started</a></li>
                                <li><a href="http://groups.google.com/group/callimachus-discuss">Send feedback</a></li>
                                <li class="divider"></li>
                                {calli:pageLinks(<li><a /></li>,<li class="divider" />)}
                                <li>{calli:whatlinkshere(<a>What links here</a>)}</li>
                                <li>{calli:relatedchanges(<a>Related changes</a>)}</li>
                                <li>{calli:permissions(<a>Permissions</a>)}</li>
                                <li>{calli:introspect(<a>Introspect resource</a>)}</li>
                                <li><a href="javascript:print()">Print this page</a></li>
                                <li class="divider"></li>
                                <li>{calli:profile(<a>Account</a>)}</li>
                                <li>{calli:logout(<a>Sign out</a>)}</li>
                            </menu>
                        </li>
                    </menu>
                    <div class="navbar-search pull-right hidden-logout">{calli:lookup('Lookup...')}</div>
                </div>
            </header>
        </div>
        <div class="container">
            {calli:breadcrumb(<nav class="breadcrumb"><a class="muted"/><span class="divider">&#187;</span><span class="active"/></nav>)}
            {calli:hgroup()}
            {calli:sidebar(<div id="sidebar" class="pull-right" />)}
            <div>
                {calli:activateNav(<nav id="access" class="hidden-logout hidden-iframe nav-tabs">
                    <a tabindex="1" href="?view" onclick="location.replace(href);return false">View</a>
                    <a tabindex="2" href="?edit" onclick="location.replace(href);return false">Edit</a>
                    <a tabindex="3" href="?discussion" onclick="location.replace(href);return false">Discussion</a>
                    <a tabindex="4" href="?describe" onclick="location.replace(href);return false">Describe</a>
                    <a tabindex="5" href="?history" onclick="location.replace(href);return false">History</a>
                </nav>)}

                <div id="container">
                    {calli:error(<div class="alert alert-error alert-block">
                        <button type="button" class="close" data-dismiss="alert">×</button>
                        <h4>Oops!</h4>
                    </div>)}
                    <div id="content">{calli:content()}</div>
                </div><!-- container -->
            </div>
        </div><!-- page -->
    </div><!-- wrapper -->

    <div class="navbar navbar-large navbar-relative-bottom hidden-iframe">
        <footer class="navbar-inner">
            <div class="container">
                {calli:generator(<p class="pull-right" />)}
                {calli:lastmod(<p id="resource-lastmod">This resource was last modified at <time class="abbreviated"/></p>)}
            </div>
        </footer>
    </div>
    <!--[if lt IE 9]>
        <script src="//raw.github.com/chuckcarpenter/REM-unit-polyfill/master/js/rem.js"></script>
    <![endif]-->
</body>
</html>
