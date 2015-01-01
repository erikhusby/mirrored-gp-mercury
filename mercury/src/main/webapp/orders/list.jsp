<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Product Orders" sectionTitle="List Product Orders" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {

                $j("#owner").tokenInput(
                        "${ctxpath}/projects/project.action?usersAutocomplete=", {
                            hintText: "Type a name",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.owner.completeData)},
                            tokenDelimiter: "${actionBean.owner.separator}",
                            resultsFormatter: formatInput
                        }
                );

                $j("#product").tokenInput(
                        "${ctxpath}/orders/order.action?productAutocomplete=", {
                            hintText: "Type a Product name or Part Number   ",
                            resultsFormatter: formatInput,
                            prePopulate: ${actionBean.ensureStringResult(actionBean.productTokenInput.completeData)},
                            tokenDelimiter: "${actionBean.productTokenInput.separator}"
                        }
                );

                $j('#productOrderList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[8,'desc']],
                    "aoColumns": [
                        {"bSortable": false},                           // checkbox
                        {"bSortable": true, "sType": "html"},           // Name
                        {"bSortable": true, "sType": "title-jira"},     // ID
                        {"bSortable": true},                            // Product
                        {"bSortable": true},                            // Product Family
                        {"bSortable": true},                            // Status
                        {"bSortable": true},                            // Research Project
                        {"bSortable": true},                            // Owner
                        {"bSortable": true, "sType": "date"},           // Placed
                        {"bSortable": true, "sType": "title-numeric"},  // % Complete
                        {"bSortable": true, "sType": "numeric"},        // Sample Count
                        {"bSortable": true, "sType": "numeric"},        // Lane Count
                        {"bSortable": true, "sType": "html"},           // Quote
                        {"bSortable": true, "sType": "html"}]           // Ledger Status
                }).fnSetFilteringDelay(300);

                setupDialogs();

                statusChange();
            });

            function statusChange() {
                if ($j(".selectedStatuses[value='Draft']").attr('checked')) {
                    $j("#draftMessage").show();
                } else {
                    $j("#draftMessage").hide();
                }
            }

            function formatInput(item) {
                var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }

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

        <stripes:form beanclass="${actionBean.class.name}" id="searchForm">
            <div class="search-horizontal">

                <div class="control-group">
                    <stripes:label for="productFamily" class="control-label">
                        Product Family
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="productFamilyId" id="productFamily" class="search-select" style="margin-top:3px;">
                            <stripes:option value="">Any</stripes:option>
                            <stripes:options-collection collection="${actionBean.productFamilies}" label="name"
                                                        value="productFamilyId"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="product" class="control-label">
                        Product
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="product" name="productTokenInput.listOfKeys" class="defaultText search-input"
                                      style="width: 250px;" title="Enter the product name for this order"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="statusGroup" class="control-label">
                        Ledger Status
                    </stripes:label>
                    <div class="controls">
                        <c:forEach items="${actionBean.ledgerStatuses}" var="ledgerStatus">
                            <div style="margin-top: 5px; margin-right: 15px; float: left; width: auto;">
                                <stripes:checkbox class="search-checkbox selectedStatuses" name="selectedLedgerStatuses" value="${ledgerStatus}" id="${ledgerStatus}-id"/>
                                <stripes:label class="search-checkbox-label" for="${ledgerStatus}-id">
                                    ${ledgerStatus.displayName}
                                </stripes:label>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="statusGroup" class="control-label">
                        Order Status
                    </stripes:label>
                    <div class="controls">
                        <c:forEach items="${actionBean.orderStatuses}" var="orderStatus">
                            <div style="margin-top: 5px; margin-right: 15px; float: left; width: auto;">
                                <stripes:checkbox onchange="statusChange()" class="search-checkbox selectedStatuses" name="selectedStatuses" value="${orderStatus}" id="${orderStatus}-id"/>
                                <stripes:label class="search-checkbox-label" for="${orderStatus}-id">
                                    ${orderStatus.displayName}
                                </stripes:label>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="dateRangeDiv" class="control-label">
                        Date Placed
                    </stripes:label>
                    <div class="controls">
                        <div id="dateRangeDiv"
                             rangeSelector="${actionBean.dateRange.rangeSelector}"
                             startString="${actionBean.dateRange.startStr}"
                             endString="${actionBean.dateRange.endStr}">
                        </div>
                        <div id="draftMessage" class="help-text" style="margin-left: 10px;margin-top: -10px; margin-bottom: 5px; display: none;">
                            Matching Draft Orders are displayed for any date selection
                        </div>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="owner" class="control-label">
                        Owner
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="owner" name="owner.listOfKeys" class="search-input" style="width:250px"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="control-label">&#160;</div>
                    <div class="controls actionButtons">
                        <stripes:submit name="list" value="Search" style="margin-right: 10px;margin-top:10px;" class="btn btn-mini"/>
                    </div>
                </div>
            </div>

        </stripes:form>


        <div class="clearfix"></div>

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden id="dialogAction" name=""/>
            <div class="actionButtons">

                <security:authorizeBlock roles="<%= roles(Developer) %>">
                    <stripes:button name="abandonOrders" value="Abandon Orders" class="btn" onclick="showConfirm('abandonOrders', 'abandon')" style="margin-right:30px;"/>
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%= roles(Developer, BillingManager) %>">
                    <stripes:submit name="startBilling" value="Start Billing Session" class="btn" style="margin-right:30px;"/>
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%= roles(Developer, PDM, BillingManager) %>">
                    <stripes:submit name="downloadBillingTracker" value="Download Billing Tracker" class="btn" style="margin-right:5px;"/>
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
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
                        <th style="min-width: 55px;">ID</th>
                        <th style="min-width: 80px;">Product</th>
                        <th>Product Family</th>
                        <th width="80">Status</th>
                        <th width="150">Research Project</th>
                        <th width="120">Owner</th>
                        <th width="70">Placed</th>
                        <th width="80">Complete</th>
                        <th width="25">Sample Count</th>
                        <th width="25">Lane Count</th>
                        <th style="min-width: 60px;">Quote</th>
                        <th width="35">Ledger Status</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.displayedProductOrderListEntries}" var="order">
                        <tr>
                            <td>
                                <c:if test="${order.canBill()}">
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
                                        <span title="DRAFT">&#160;</span>
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
                                <fmt:formatDate value="${order.placedDate}" pattern="${actionBean.datePattern}"/>
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
                                <c:if test="${order.product.supportsNumberOfLanes}">
                                    ${order.laneCount}
                                </c:if>
                            </td>
                            <td>
                                <a href="${actionBean.getQuoteUrl(order.quoteId)}" class="external" target="QUOTE">
                                        ${order.quoteId}
                                </a>
                            </td>
                            <td>
                                <!-- Do ready for review first because if there is ANYTHING auto created, then it
                                     cannot be billed until a review happens. -->
                                <c:choose>
                                    <c:when test="${order.readyForReview}">
                                        <span class="badge badge-warning">
                                            <%=ProductOrderListEntry.LedgerStatus.READY_FOR_REVIEW.getDisplayName()%>
                                        </span>
                                    </c:when>
                                    <c:when test="${order.billing}">
                                        <span class="badge badge-info">
                                            <%=ProductOrderListEntry.LedgerStatus.BILLING.getDisplayName()%>
                                        </span>
                                    </c:when>
                                    <c:when test="${order.readyForBilling}">
                                        <span class="badge badge-success">
                                            <%=ProductOrderListEntry.LedgerStatus.READY_TO_BILL.getDisplayName()%>
                                        </span>
                                    </c:when>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
