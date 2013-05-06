<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Billing Session" sectionTitle="View Billing Session: ${actionBean.editSession.businessKey}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#quoteReporting').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'desc']],
                    "aoColumns": [
                        {"bSortable": true},                   // Quote
                        {"bSortable": true},                   // Platform
                        {"bSortable": true},                   // Category
                        {"bSortable": true},                   // Price Item
                        {"bSortable": true},                   // Quantity
                        {"bSortable": true, "sType": "date"},  // Work Completed
                        {"bSortable": true, "sType": "date"},  // Work Reported
                        {"bSortable": false}]                  // Billed Message
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form action="/billing/session.action" id="orderForm" class="form-horizontal">
            <stripes:hidden name="sessionKey" value="${actionBean.sessionKey}"/>

            <security:authorizeBlock roles="<%= roles(Developer, BillingManager) %>">
                <c:if test="${actionBean.editSession.billedDate == null}">
                    <stripes:submit name="bill" value="Bill Work in Broad Quotes" class="btn" style="margin-right:30px;"/>
                </c:if>

                <stripes:submit name="downloadTracker" value="Download Tracker" class="btn" style="margin-right:30px;"/>
                <stripes:submit name="downloadQuoteItems" value="Download Quote Items" class="btn" style="margin-right:30px;"/>

                <c:if test="${actionBean.editSession.billedDate == null}">
                    <stripes:submit name="endSession" value="End Billing Session" class="btn" style="margin-right:15px;px;"/>
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
            Quote Items
        </div>

        <table id="quoteReporting" class="table simple">
            <thead>
            <tr>
                <th width="90">Quote</th>
                <th>Platform</th>
                <th width="40">Category</th>
                <th width="40">Price Item</th>
                <th width="40">Quote Price Type</th>
                <th width="60">Quantity</th>
                <th width="100">Work Completed</th>
                <th width="40">Work Reported</th>
                <th width="40">Billing Message</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.quoteImportItems}" var="item">
                <tr>
                    <td width="50">${item.quoteId}</td>
                    <td width="100">${item.priceItem.platform}</td>
                    <td width="50">${item.priceItem.category}</td>
                    <td>${item.priceItem.name}</td>
                    <td width="70">${item.quotePriceType}</td>
                    <td width="50">${item.quantity}</td>
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
