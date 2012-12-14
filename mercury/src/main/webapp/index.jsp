<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:layout-render name="/layout.jsp" pageTitle="Mercury">
    <stripes:layout-component name="content">
        <div class="hero-unit" style="width:900px; margin:0px auto;">
            <h1>Welcome to Mercury!</h1>

            <p>This page allows you to manage a variety of things, just select an item below or navigate directly to
                where you need to go from the menubar.</p>
        </div>

        <div class="row-fluid" style="width:800px; margin:0px auto;">
            <div class="span3">
                <h2>Research Projects</h2>

                <p>Review and manage all your research projects.</p>

                <p><a class="btn" href="/projects/list">View details &#187;</a></p>
            </div>
            <div class="span3">
                <h2>Product orders</h2>

                <p>Create or manage all your product orders.</p>

                <p><a class="btn" href="/orders/list">View details &#187;</a></p>
            </div>
            <div class="span3">
                <h2>Products</h2>

                <p>Review and manage all your products.</p>

                <p><a class="btn" href="/products/list">View details &#187;</a></p>
            </div>
            <div class="span3">
                <h2>Billing</h2>

                <p>Review and manage all your billing and quotes.</p>

                <p><a class="btn" href="/billing/sessions">View details &#187;</a></p>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


