<%@ include file="/resources/layout/taglibs.jsp" %>
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
                    <li><a tabindex="-1" href="${ctxpath}/reagent/list">list</a></li>
                    <li><a tabindex="-1" href="${ctxpath}/reagent/create">create</a></li>
                </ul>
            </li>
        </ul>
        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchPlasticActionBean" style="height:35px;">
            <ul class="nav pull-right global-search">
                <li>
                    <input type="text" name="barcode" title="enter a barcode to search" class="defaultText" style="margin-top: 5px;vertical-align: top;height:14px;"/>
                    <input type="submit" name="search" value="Search" class="btn btn-mini"/>
                </li>
                <li style="float: none; line-height: 5px;">
                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchPlasticActionBean"
                              event="view">advanced</stripes:link>
                </li>
            </ul>
        </stripes:form>
    </div>
</header>