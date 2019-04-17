<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Billing Session: ${actionBean.editSession.businessKey}"
                       sectionTitle="View Billing Session: ${actionBean.editSession.businessKey}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#quoteReporting').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'desc']],
                    "aoColumns": [
                        {"bSortable": true},                   // Quote
                        {"bSortable": true},                   // PDOs
                        {"bSortable": true},                   // quote server work items
                        {"bSortable": true},                   // SAP Server document ID
                        {"bSortable": true},                   // Platform
                        {"bSortable": true},                   // Category
                        {"bSortable": true},                   // Price Item
                        {"bSortable": true},                   // Quantity
                        {"bSortable": true, "sType": "date"},  // Work Completed
                        {"bSortable": true, "sType": "date"},  // Work Reported
                        {"bSortable": false}]                  // Billed Message
                })
            });

            $j(window).load(function() {
                var workItemIdToHighlight = '#'.concat(${actionBean.workItemIdToHighlight});
                // if the url contains a quote server work item, highlight the corresponding row
                $j(workItemIdToHighlight).attr('class','highlighted');
                $('html, body').scrollTop($(workItemIdToHighlight).offset().top);
            });

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form action="/billing/session.action" id="orderForm" class="form-horizontal">
            <stripes:hidden name="sessionKey" value="${actionBean.sessionKey}"/>

            <security:authorizeBlock roles="<%= roles(Developer, BillingManager) %>">
                <c:if test="${actionBean.editSession.billedDate == null}">
                    <stripes:submit name="bill" value="Bill Work in Broad SAP/Quotes" class="btn"
                                    style="margin-right:30px;" disabled="${actionBean.isBillingSessionLocked()}"/>
                </c:if>

                <stripes:submit name="downloadQuoteItems" value="Download Quote Items" class="btn" style="margin-right:30px;"/>

                <c:if test="${actionBean.editSession.billedDate == null}">
                    <stripes:submit name="endSession" value="End Billing Session" class="btn"
                                    style="margin-right:15px;px;" disabled="${actionBean.isBillingSessionLocked()}"/>
                </c:if>
            </security:authorizeBlock>

            <div style="margin-top:10px;" class="view-control-group control-group">
                <label class="control-label label-form">ID</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editSession.businessKey}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Created By</label>
                <div class="controls">
                    <div class="form-value">${actionBean.getUserFullName(actionBean.editSession.createdBy)}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Created Date</label>
                <div class="controls">
                    <div class="form-value">
                        <fmt:formatDate value="${actionBean.editSession.createdDate}"/>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Billed Date</label>
                <div class="controls">
                    <div class="form-value">
                        <fmt:formatDate value="${actionBean.editSession.billedDate}" pattern="${actionBean.datePattern}"/>
                    </div>
                </div>
            </div>
        </stripes:form>

        <div class="borderHeader">
            <h4 style="display:inline">Quote Items</h4>
        </div>

        <table id="quoteReporting" class="table simple" style="table-layout: fixed;">
            <thead>
            <tr>
                <th width="60">Quote</th>
                <th width="250">PDOs</th>
                <th width="50">Work Items</th>
                <th width="90">SAP<br/>Document ID(s)</th>
                <th>Platform</th>
                <th>Category</th>
                <th>Price Item</th>
                <th width="60">Quote Price Type</th>
                <th width="40">Quantity</th>
                <th width="70">Work Completed</th>
                <th width="40">Work Reported</th>
                <th>Billing Message</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.quoteImportItems}" var="item">
                <tr id="${item.singleWorkItem}">
                    <td>
                        <c:choose>
                            <c:when test="${item.sapOrder}">
                                Sap Quote:
                                <a href="${actionBean.getSapQuoteUrl(item.quoteId)}" class="external" target="QUOTE">
                                        ${item.quoteId}
                                </a>
                            </c:when>
                            <c:otherwise>
                                <a href="${actionBean.getQuoteUrl(item.quoteId)}" class="external" target="QUOTE">
                                        ${item.quoteId}
                                </a>

                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:forEach items="${item.orderKeys}" var="pdoBusinessKey">
                            <span style="white-space: nowrap">
                                <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" event="view">
                                    <stripes:param name="productOrder" value="${pdoBusinessKey}"/>
                                    ${pdoBusinessKey}
                                </stripes:link>
                                : quantity ${item.getChargedAmountForPdo(pdoBusinessKey)} for ${item.getNumberOfSamples(pdoBusinessKey)} samples </span>
                            <br>
                        </c:forEach>
                    </td>
                    <td>
                        <c:forEach items="${item.workItems}" var="quoteServerWorkItem">
                            <a href="${actionBean.getQuoteWorkItemUrl(item.quoteId,quoteServerWorkItem)}" target="QUOTE">
                                ${quoteServerWorkItem}<br>
                            </a>
                        </c:forEach>
                    </td>
                    <td>
                        <c:forEach items="${item.sapItems}" var="sapWorkItem">
                                ${sapWorkItem}<br>
                        </c:forEach>
                    </td>
                    <td>${item.priceItem.platform}</td>
                    <td>${item.priceItem.category}</td>
                    <td>${item.priceItem.name}</td>
                    <td>${item.quotePriceType}</td>
                    <td>${item.getRoundedQuantity()}</td>
                    <td>${item.numSamples}</td>
                    <td>
                        <fmt:formatDate value="${item.workCompleteDate}" pattern="${actionBean.datePattern}"/>
                    </td>
                    <td>${item.billingMessage}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
