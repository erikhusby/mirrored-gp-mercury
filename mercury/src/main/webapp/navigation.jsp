<%@ include file="/resources/layout/taglibs.jsp" %>
<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2013 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>

<header class="navbar">
    <div class="navbar-inner">
        <ul class="nav" role="navigation">
            <li class="dropdown">
                <a id="projectNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-home"></span> Projects <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/projects/project.action?list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/projects/project.action?create">create</a></li>
                </ul>
            </li>
            <li class="dropdown">
                <a id="orderNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-shopping-cart"></span> Orders <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/orders/order.action?list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/orders/order.action?create">create</a></li>
                    <li class="divider"></li>
                    <li><a tabindex="-1" href="${ctxpath}/billing/session.action?list">billing sessions</a></li>
                </ul>
            </li>
            <li class="dropdown">
                <a id="productNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-tags"></span> Products <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/products/product.action?list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/products/product.action?create">create</a></li>
                </ul>
            </li>

            <!-- security:authorizeBlock ${actionBean.userBean.developerRole} -->
            <li class="dropdown">
                <a id="adminNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-cog"></span> Admin <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/administration/page_administration">page config</a></li>
                    <li class="divider"></li>
                    <li><a tabindex="-1" href="${ctxpath}/reagent/list_reagent_designs.xhtml">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/reagent/edit_reagent_designs.xhtml">create</a></li>
                </ul>
            </li>
        </ul>
        <ul class="nav pull-right">
            <li id="fat-menu" class="dropdown">
                <a href="#" id="drop3" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-search"></span> Search <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop3">
                    <li><a tabindex="-1" href="${ctxpath}/search/search_plastic">Barcode</a></li>
                </ul>
            </li>
        </ul>
    </div>
</header>
