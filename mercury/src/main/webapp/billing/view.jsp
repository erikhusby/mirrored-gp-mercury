<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Billing Sessions" sectionTitle="List Billing Sessions">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#sessionList').dataTable( {
                    "aaSorting": [[2,'desc']],
                    "aoColumns": [
                        {"bSortable": true},                    // Quote Identifier
                        {"bSortable": true},                    // Platform
                        {"bSortable": true},                    // Category
                        {"bSortable": true},                    // Price Item
                        {"bSortable": true},                    // Quantity
                        {"bSortable": true, "sType": "date"},   // Created Date
                        {"bSortable": true, "sType": "date"},   // Work Reported Date
                        {"bSortable": false}]                   // Billing Message
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form action="/orders/order.action" id="createForm" class="form-horizontal">

            <div class="borderHeader">
                Quote Items
            </div>

            <table id="sessionList" class="table simple">
                <thead>
                    <tr>
                        <th>Quote</th>
                        <th>Platform</th>
                        <th>Category</th>
                        <th>Price Item</th>
                        <th>Quantity</th>
                        <th>Work Completed</th>
                        <th>Work Reported</th>
                        <th>Billing Message</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.editSession.quoteImportItems}" var="quoteItem">
                        <tr>
                            <td>${quoteItem.quoteId}</td>
                            <td>${quoteItem.priceItem.platform}</td>
                            <td>${quoteItem.priceItem.category}</td>
                            <td>${actionBean.quotePriceItemNameMap[quoteItem.priceItem.quoteServerId]}</td>
                            <td>
                                ${quoteItem.numSamples}<br/>
                                <fmt:formatDate value="${quoteItem.startRange}" pattern="MM/dd/yyyy"/>
                                    -
                                <fmt:formatDate value="${quoteItem.endRange}" pattern="MM/dd/yyyy"/>
                            </td>
                            <td>
                                <fmt:formatDate value="${quoteItem.workCompleteDate}" pattern="MM/dd/yyyy"/>
                            </td>
                            <td>${quoteItem.billingMessage}</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
