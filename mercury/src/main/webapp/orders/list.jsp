<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Product Orders" sectionTitle="List Product Orders">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#productOrderList').dataTable( {
                    "oTableTools": ttExportDefines,
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
                        {"bSortable": true, "sType" : "title-string"}]  // eligible for billing
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <p>
            <stripes:link title="New Product Order" beanclass="${actionBean.class.name}" event="create" class="pull-right">
                New order
            </stripes:link>
        </p>

        <div class="clearfix"></div>

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <div class="actionButtons">
                <%--security:authorizeBlock roles="${actionBean.userBean.developerRole}, ${actionBean.userBean.billingManagerRole}">--%>
                    <stripes:submit name="startBilling" value="Start Billing Session" style="margin-right:30px;"/>
                <%--/security:authorizeBlock>--%>

                <stripes:submit name="downloadBillingTracker" value="Download Billing Tracker" style="margin-right:5px;"/>

                <%--security:authorizeBlock roles="${actionBean.userBean.developerRole}, ${actionBean.userBean.productManagerRole}">--%>
                    <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.UploadTrackerActionBean" event="view">
                        Upload Billing Tracker
                    </stripes:link>
                <%--/security:authorizeBlock>--%>
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
                                <stripes:checkbox class="shiftCheckbox" name="selectedProductOrderBusinessKeys" value="${order.businessKey}"/>
                            </td>
                            <td>
                                <stripes:link beanclass="${actionBean.class.name}" event="view">
                                    <stripes:param name="businessKey" value="${order.businessKey}"/>
                                    ${order.title}
                                </stripes:link>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${order.draft}">
                                        DRAFT
                                        (
                                            <stripes:link title="Place Order" beanclass="${actionBean.class.name}" event="placeOrder">
                                                <stripes:param name="businessKey" value="${order.businessKey}"/>
                                                Place Order
                                            </stripes:link>
                                        )
                                    </c:when>
                                    <c:otherwise>
                                        <a target="JIRA" href="${actionBean.jiraUrl}${order.jiraTicketKey}" class="external" target="JIRA">
                                                ${order.jiraTicketKey}
                                        </a>
                                    </c:otherwise>
                                </c:choose>
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
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"
                                                  event="view">
                                        <stripes:param name="billingSession" value="${order.billingSessionBusinessKey}"/>
                                        ${order.billingSessionBusinessKey}
                                    </stripes:link>
                                </c:if>
                            </td>
                            <td>
                                <c:if test="${order.eligibleForBilling}">
                                    <stripes:image name="" title="Yes" src="/images/check.png"/>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
