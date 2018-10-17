<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean"/>
<stripes:layout-render name="/layout.jsp">
    <stripes:layout-component name="content">
        <div class="hero-unit" style="width:900px; margin:0 auto; background-image: url('${ctxpath}/images/labhero.jpg'); background-size: 100%;">
            <p style="font-size: 28px;">Mercury allows you to manage a variety of project and lab oriented activities.  Just select an item below, or navigate directly to
                where you need to go from the menu.</p>
        </div>

        <div class="row-fluid" style="width:800px; margin:0 auto;">
            <div class="span3">
                <h3 style="min-height: 80px;">Research Projects</h3>

                <p>Review and manage all your research projects.</p>

                <p><a class="btn" style="text-decoration: none !important" href="${ctxpath}/projects/project.action?list=">View Projects</a></p>
            </div>
            <div class="span3">
                <h3 style="min-height: 80px;">Product Orders</h3>

                <p>Create and manage all your product orders.</p>

                <p><a class="btn" style="text-decoration: none !important" href="${ctxpath}/orders/order.action?list=">View Orders</a></p>
            </div>
            <div class="span3">
                <h3 style="min-height: 80px;">Products</h3>

                <p>Review and manage all your products.</p>

                <p><a class="btn" style="text-decoration: none !important" href="${ctxpath}/products/product.action?list=">View Products</a></p>
            </div>
            <security:authorizeBlock roles="<%= roles(Developer, BillingManager, PDM) %>">
                <div class="span3">
                    <h3 style="min-height: 80px;">Billing</h3>

                    <p>Review and manage all your billing and quotes.</p>

                    <p><a class="btn" style="text-decoration: none !important"
                          href="${ctxpath}/billing/session.action?list=">View Billing Sessions</a></p>
                </div>
            </security:authorizeBlock>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


