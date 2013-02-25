<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Product Orders" sectionTitle="List Product Orders">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {

                $j('#productOrderList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[8,'desc']],
                    "aoColumns": [
                        {"bSortable": false},                    // checkbox
                        {"bSortable": true, "sType": "html"},    // Name
                        {"bSortable": true, "sType": "title-jira"},   // ID
                        {"bSortable": true},                    // Product
                        {"bSortable": true},                    // Product Family
                        {"bSortable": true},                    // Status
                        {"bSortable": true},                    // Research Project
                        {"bSortable": true},                    // Owner
                        {"bSortable": true, "sType": "date"},   // Placed
                        {"bSortable": true, "sType": "title-numeric"},   // % Complete
                        {"bSortable": true, "sType": "numeric"},   // Count
                        {"bSortable": true},                   // Billing Session ID
                        {"bSortable": true, "sType" : "title-string"}]  // eligible for billing
                });

                setupDialogs();
            });

            function setupDialogs() {
                $j("#confirmDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: [
                        {
                            id: "confirmOkButton",
                            text: "OK",
                            click: function () {
                                $j(this).dialog("close");
                                $j("#confirmOkButton").attr("disabled", "disabled");
                                $j("#createForm").submit();
                            }
                        },
                        {
                            text: "Cancel",
                            click : function () {
                                $j(this).dialog("close");
                            }
                        }
                    ]
                });

                $j("#noneSelectedDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {
                        OK: function () {
                            $j(this).dialog("close");
                        }
                    }
                });
            }

            function showConfirm(action, actionPrompt) {
                var numChecked = $("input.shiftCheckbox:checked").size();
                if (numChecked) {
                    $j("#dialogAction").attr("name", action);
                    $j("#confirmDialogMessage").text(actionPrompt);
                    $j("#dialogNumProductOrders").text(numChecked);
                    $j("#confirmDialog").dialog("open");
                } else {
                    $j("#noneSelectedDialogMessage").text(actionPrompt);
                    $j("#noneSelectedDialog").dialog("open");
                }
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div style="display:none;" id="confirmDialog">
            <p>Are you sure you want to <span id="confirmDialogMessage"></span> the <span id="dialogNumProductOrders"></span> Product Order(s)?</p>
        </div>

        <div style="display:none;" id="noneSelectedDialog">
            <p>You must select at least one Product Order to <span id="noneSelectedDialogMessage"></span>.</p>
        </div>

        <p>
            <stripes:link title="<%=ProductOrderActionBean.CREATE_ORDER%>" beanclass="${actionBean.class.name}" event="create" class="pull-right">
                <span class="icon-shopping-cart"></span> <%=ProductOrderActionBean.CREATE_ORDER%>
            </stripes:link>
        </p>

        <div class="clearfix"></div>

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden id="dialogAction" name=""/>
            <div class="actionButtons">

                <security:authorizeBlock roles="<%=new String[] {Role.Developer.name}%>">
                    <stripes:button name="abandonOrders" value="Abandon Orders" class="btn" onclick="showConfirm('abandonOrders', 'abandon')" style="margin-right:30px;"/>
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%=new String[] {Role.Developer.name, Role.BillingManager.name}%>">
                    <stripes:submit name="startBilling" value="Start Billing Session" class="btn" style="margin-right:30px;"/>
                </security:authorizeBlock>

                <stripes:submit name="downloadBillingTracker" value="Download Billing Tracker" class="btn" style="margin-right:5px;"/>

                <security:authorizeBlock roles="<%=new String[] {Role.Developer.name, Role.PDM.name}%>">
                    <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.UploadTrackerActionBean" event="view">
                        Upload Billing Tracker
                    </stripes:link>
                </security:authorizeBlock>
            </div>

            <table id="productOrderList" class="table simple">
                <thead>
                    <tr>
                        <th width="40">
                            <input for="count" type="checkbox" class="checkAll"/><span id="count" class="checkedCount"></span>
                        </th>
                        <th>Name</th>
                        <th width="100">ID</th>
                        <th>Product</th>
                        <th>Product Family</th>
                        <th width="80">Status</th>
                        <th width="150">Research Project</th>
                        <th width="120">Owner</th>
                        <th width="70">Placed</th>
                        <th width="80">%&nbsp;Complete</th>
                        <th width="25">Sample Count</th>
                        <th width="35">Billing Session</th>
                        <th width="25">Can Bill</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.allProductOrders}" var="order">
                        <tr>
                            <td>
                                <c:if test="${!order.draft}">
                                    <stripes:checkbox class="shiftCheckbox" name="selectedProductOrderBusinessKeys" value="${order.businessKey}"/>
                                </c:if>
                            </td>
                            <td>
                                <stripes:link beanclass="${actionBean.class.name}" event="view">
                                    <stripes:param name="productOrder" value="${order.businessKey}"/>
                                    ${order.title}
                                </stripes:link>
                            </td>
                            <td>
                                <%--
                                   Real JIRA tickets IDs for PDOs have a "PDO-" prefix followed by digits.  Draft PDOs don't have a ticket ID,
                                   Messaging tests have "PDO-" followed by arbitrary text.
                                 --%>
                                <c:choose>
                                    <%-- draft PDO --%>
                                    <c:when test="${order.draft}">
                                        <span title="DRAFT">&nbsp;</span>
                                    </c:when>
                                    <c:otherwise>
                                        <a target="JIRA" title="${order.jiraTicketKey}"
                                           href="${actionBean.jiraUrl(order.jiraTicketKey)}" class="external"
                                           target="JIRA">
                                                ${order.jiraTicketKey}
                                        </a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>${order.productName}</td>
                            <td>${order.productFamilyName}</td>
                            <td>${order.orderStatus}</td>
                            <td>${order.researchProjectTitle}</td>
                            <td>${actionBean.getUserFullName(order.ownerId)}</td>
                            <td>
                                <fmt:formatDate value="${order.placedDate}"/>
                            </td>
                            <td align="center">
                                <div class="barFull" title="${actionBean.progressFetcher.getPercentInProgress(order.businessKey)}% In Progress">
                                    <span class="barAbandon"
                                          title="${actionBean.progressFetcher.getPercentAbandoned(order.businessKey)}% Abandoned"
                                          style="width: ${actionBean.progressFetcher.getPercentAbandoned(order.businessKey)}%"> </span>
                                    <span class="barComplete"
                                          title="${actionBean.progressFetcher.getPercentCompleted(order.businessKey)}% Completed"
                                          style="width: ${actionBean.progressFetcher.getPercentCompleted(order.businessKey)}%"> </span>
                                </div>
                            </td>
                            <td>${actionBean.progressFetcher.getNumberOfSamples(order.businessKey)}</td>
                            <td>
                                <c:if test="${order.billingSessionBusinessKey != null}">
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"
                                                  event="view">
                                        <stripes:param name="sessionKey" value="${order.billingSessionBusinessKey}"/>
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
