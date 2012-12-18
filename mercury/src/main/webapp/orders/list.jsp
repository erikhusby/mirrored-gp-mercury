<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Product Orders" sectionTitle="List Product Orders">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#productOrderList').dataTable( {
                    "aaSorting": [[1,'asc']],
                    "aoColumns": [
                        {"bSortable": false},                   // checkbox
                        {"bSortable": true, "sType": "html"},   // Name
                        {"bSortable": true, "sType": "html"},   // ID
                        {"bSortable": true},                    // Product
                        {"bSortable": true},                    // Product Family
                        {"bSortable": false},                   // Status
                        {"bSortable": true},                    // Research Project
                        {"bSortable": true},                    // Owner
                        {"bSortable": true, "sType": "date"},   // Updated
                        {"bSortable": false},                   // Count
                        {"bSortable": false},                   // Billing Session ID
                        {"bSortable": false}],                  // Eligible For Billing
                    "oTableTools": {
                        "sSwfPath": "${ctxpath}/resources/scripts/DataTables-1.9.4/extras/TableTools/media/swf/copy_csv_xls.swf",
                        "aButtons": [ "copy", "csv", "print" ]
                    }
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <p>
            <stripes:link title="New Research Project" href="${ctxpath}/orders/order.action?create" class="pull-right">New order</stripes:link>
        </p>

        <div class="clearfix"></div>

        <stripes:form action="/orders/order.action" id="createForm" class="form-horizontal">
            <div style="align:left;">
                <!--security:authorizeBlock roles="${userBean.developerRole}, ${userBean.billingManagerRole}"-->
                <stripes:submit name="startBilling" value="Start Billing Session"/>
                <!--/security:authorizeBlock-->

                <stripes:submit name="downloadBillingTracker" value="Download Billing Tracker" style="margin-left:10px;"/>

                <!--security:authorizeBlock roles="${userBean.developerRole}, ${userBean.productManagerRole}"-->
                <stripes:link href="/orders/order.action" event="uploadBillingTracker" style="margin-left:10px;">
                    Upload Billing Tracker
                </stripes:link>
            </div>

            <table id="productOrderList" class="table simple">
                <thead>
                    <tr>
                        <th width="40">
                            <input for="count" type="checkbox" class="checkAll"/><span id="count" class="checkedCount"></span>
                        </th>
                        <th>Name</th>
                        <th width="100">ID</th>
                        <th width="200">Product</th>
                        <th width="220">Product Family</th>
                        <th>Status</th>
                        <th width="150">Research Project</th>
                        <th>Owner</th>
                        <th width="70">Updated</th>
                        <th width="25">Sample Count</th>
                        <th width="35">Billing Session</th>
                        <th width="25">Can Bill</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.allProductOrders}" var="order">
                        <tr>
                            <td>
                                <stripes:checkbox class="shiftCheckbox" name="selectedProductOrders" value="${order.businessKey}"/>
                            </td>
                            <td>
                                <stripes:link href="/orders/order.action" event="view">
                                    <stripes:param name="orderKey" value="${order.businessKey}"/>
                                    ${order.title}
                                </stripes:link>
                            </td>
                            <td>
                                <a class="external" target="JIRA" href="${actionBean.jiraUrl}${order.jiraTicketKey}" class="external" target="JIRA">
                                    ${order.jiraTicketKey}
                                </a>
                            </td>
                            <td>${order.productName}</td>
                            <td>${order.productFamilyName}</td>
                            <td>${order.orderStatus}</td>
                            <td>${order.researchProjectTitle}</td>
                            <td>${actionBean.fullNameMap[order.ownerId]}</td>
                            <td>
                                <fmt:formatDate value="${order.updatedDate}" pattern="MM/dd/yyyy"/>
                            </td>
                            <td>${order.pdoSampleCount}</td>
                            <td>
                                <c:if test="${order.billingSessionBusinessKey != null}">
                                    <stripes:link href="/billing/billing.action" event="view">
                                        <stripes:param name="billingSession" value="${order.billingSessionBusinessKey}"/>
                                        ${order.billingSessionBusinessKey}
                                    </stripes:link>
                                </c:if>
                            </td>
                            <td>
                                <c:if test="${order.eligibleForBilling}">
                                    <stripes:image name="" src="/images/check.png"/>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
