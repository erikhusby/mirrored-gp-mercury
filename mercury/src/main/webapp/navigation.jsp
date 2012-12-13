<%@ include file="/resources/layout/taglibs.jsp" %>

<ul>
    <li class="ui-icon ui-icon-home">Projects</li>
        <ul>
        <li><a href="/projects/list">list</a></li>
        <li><a href="/projects/create">create</a></li>
        </ul>
    <li>Orders</li>
    <ul>
        <li><a href="/orders/list">list</a></li>
        <li><a href="/orders/create">create</a></li>
    </ul>
    <li>Products</li>
    <ul>
        <li><a href="/products/list"/></li>
        <div id="security:authorizeBlock ${actionBean.userBean.developerRole} ${actionBean.userBean.productManagerRole}">
            <li><a href="/products/create">create</a></li>
        </div>
    </ul>
    <li>Search</li>
    <ul>
        <li></li>
    </ul>
    <div id="security:authorizeBlock ${actionBean.userBean.developerRole}">
    <li>Admin</li>
    <ul>
        <li><a href="/administration/page_administration">page config</a></li>
        <li><a href="/reagent/list">list</a></li>
        <li><a href="/reagent/edit">edit</a></li>
    </ul>
     </div>
</ul>