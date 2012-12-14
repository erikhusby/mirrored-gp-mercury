<%@ include file="/resources/layout/taglibs.jsp" %>
<header class="navbar">
    <div class="navbar-inner">
        <ul class="nav" role="navigation">
            <li class="dropdown">
                <a id="projectNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown">Projects <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/projects/list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/projects/create">create</a></li>
                </ul>
            </li>
            <li class="dropdown">
                <a id="orderNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown">Orders <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/orders/list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/orders/create">create</a></li>
                </ul>
            </li>
            <li class="dropdown">
                <a id="productNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown">Products <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/products/list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/products/create">create</a></li>
                </ul>
            </li>


            <!-- security:authorizeBlock ${actionBean.userBean.developerRole} -->
            <li class="dropdown">
                <a id="adminNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown">Admin <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li><a tabindex="-1" href="${ctxpath}/administration/page_administration">page config</a></li>
                    <li class="divider"></li>
                    <li><a tabindex="-1" href="${ctxpath}/reagent/list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/reagent/create">create</a></li>
                </ul>
            </li>
        </ul>
        <ul class="nav pull-right">
            <li id="fat-menu" class="dropdown">
                <a href="#" id="drop3" role="button" class="dropdown-toggle" data-toggle="dropdown">Search <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop3">
                    <li><a tabindex="-1" href="${ctxpath}/search/search_plastic">Barcode</a></li>
                </ul>
            </li>
        </ul>
    </div>
</header>