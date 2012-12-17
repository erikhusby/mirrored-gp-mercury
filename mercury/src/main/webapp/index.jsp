<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:layout-render name="/layout.jsp" pageTitle="Mercury">
    <stripes:layout-component name="content">
        <div class="hero-unit" style="width:900px; margin:0px auto; background-image: url('${ctxpath}/images/labhero.jpg'); opacity: .75; background-size: 100%;">
            <p style="font-weight: 500">This page allows you to manage a variety of things, just select an item below or navigate directly to
                where you need to go from the menubar.</p>
        </div>

        <div class="row-fluid" style="width:800px; margin:0 auto;">
            <div class="span3">
                <h2>Research projects</h2>

                <p>Review and manage all your research projects.</p>

                <p><a class="btn" href="${ctxpath}/projects/project.action?list">View details &#187;</a></p>
            </div>
            <div class="span3">
                <h2>Product orders</h2>

                <p>Create or manage all your product orders.</p>

                <p><a class="btn" href="${ctxpath}/orders/order.action?list">View details &#187;</a></p>
            </div>
            <div class="span3">
                <h2>Products</h2>

                <p>Review and manage all your products.</p>

                <p><a class="btn" href="${ctxpath}/products/product.action?list">View details &#187;</a></p>
            </div>
            <div class="span3">
                <h2>Billing</h2>

                <p>Review and manage all your billing and quotes.</p>

                <p><a class="btn" href="/${ctxpath}/billings/billing.action?sessions">View details &#187;</a></p>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


