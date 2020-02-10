<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Billing Session: ${actionBean.editSession.businessKey}"
                       sectionTitle="View Billing Session: ${actionBean.editSession.businessKey}">
    <stripes:layout-component name="extraHead">
        <style type="text/css">
        .return-order {color: #96210d;}
        .return-order:before {content: '(';}
        .return-order:after {content: ')';}
        </style>
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#quoteReporting').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'desc']],
                    'aoColumnDefs': [
                        {'aTargets': [11], "sType": "date"},
                        {'aTargets': [12], "bSortable": false}
                    ],
                })
            });

            $j(window).load(function () {
                // if the url contains a quote server work item or sap document id, highlight the corresponding row
                if ("${actionBean.highlightRow}" == "") {
                    return;
                }
                var highlightRow = $j("[id='${actionBean.highlightRow}']");
                highlightRow.addClass('highlighted');
                var offset = highlightRow.offset();
                if (offset !== null && offset !== undefined) {
                    $('html, body').scrollTop(offset.top);
                }
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

    <div style="overflow-x: scroll">
        <table id="quoteReporting" class="table simple">
            <thead>
            <tr>
                <th>Quote</th>
                <th>PDOs</th>
                <th>Work Items</th>
                <th>SAP<br/>Document ID(s)</th>
                <th style="min-width: 15em">Product</th>
                <th>Platform</th>
                <th style="min-width: 10em">Category</th>
                <th style="min-width: 10em">Price Item</th>
                <th style="min-width: 3em">Quote Price Type</th>
                <th>Quantity</th>
                <th>Work Completed</th>
                <th>Work Reported</th>
                <th style="min-width: 15em">Billing Message</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.quoteImportItems}" var="item">
                <tr id="${item.tabularIdentifier}">
                    <td>
                        <c:choose>
                            <c:when test="${item.sapOrder}">
                                Sap Quote:
                                <a href="${actionBean.getSapQuoteUrl(item.quoteId)}" class="external" target="QUOTE">${item.quoteId}</a>
                            </c:when>
                            <c:otherwise>
                                <a href="${actionBean.getQuoteUrl(item.quoteId)}" class="external" target="QUOTE">${item.quoteId}</a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:forEach items="${item.orderKeys}" var="pdoBusinessKey">
                            <span class="billed-pdos" style="white-space: nowrap">
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
                            <c:choose><c:when test="${item.quoteServerOrder}">
                                <a class="billed-workItems"
                                   href="${actionBean.getQuoteWorkItemUrl(item.quoteId,quoteServerWorkItem)}"
                                   target="QUOTE">${quoteServerWorkItem}</a><br/>
                            </c:when>
                            <c:otherwise>${quoteServerWorkItem}</c:otherwise></c:choose>
                        </c:forEach>
                    </td>
                    <td>
                        <span class="sapDocumentIds">
                            ${item.sapItems}
                            <c:forEach items="${item.sapReturnOrders}" var="returnId" varStatus="stat">
                               <c:if test="${stat.first}"><br/></c:if>
                               <div class="return-order">${returnId}</div>
                            </c:forEach>
                        </span>
                    </td>
                    <%-- For astetic reasons, replace the basic hyphen with a non-breaking one so that part numbers aren't word-wrapped                    --%>
                    <td>${fn:replace(item.product.displayName,"-","&#x2011;")}</td>
                    <td>${item.priceItem.platform}</td>
                    <td>${item.priceItem.category}</td>
                    <td>${item.priceItem.name}</td>
                    <td>${item.quotePriceType}</td>
                    <td>${item.getRoundedQuantity()}</td>
                    <td>${item.numSamples}</td>
                    <td>
                        <fmt:formatDate value="${item.workCompleteDate}" pattern="${actionBean.datePattern}"/>
                    </td>
                    <td><span class="billingMessage">${item.billingMessage}</span></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    </stripes:layout-component>
</stripes:layout-render>
