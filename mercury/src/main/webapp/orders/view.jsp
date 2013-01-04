<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product Order" sectionTitle="View Product Order: ${actionBean.editOrder.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#sampleData').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},   // ID
                        {"bSortable": true},                    // Participant ID
                        {"bSortable": true},                    // Volume
                        {"bSortable": true},                    // Concentration
                        {"bSortable": true},                    // Yield Amount
                        {"bSortable": true, "sSortDataType" : "title-string-asc"},   // FP Status
                        {"bSortable": true},                    // Status
                        {"bSortable": true},                    // Eligible
                        {"bSortable": true},                    // Billed
                        {"bSortable": true},                    // Abandoned
                        {"bSortable": true},                    // Price Item 1
                        {"bSortable": true},                    // Price Item 2
                        {"bSortable": false}]                   // Comment
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
            <p>
                <stripes:link title="Click to edit ${actionBean.editOrder.title}"
                    beanclass="${actionBean.class.name}" event="edit" class="pull-right">
                    <span class="icon-home"></span> Edit product order
                    <stripes:param name="businessKey" value="${actionBean.editOrder.businessKey}"/>
                </stripes:link>
            </p>

        <stripes:form action="/orders/order.action" id="orderForm" class="form-horizontal">

            <div class="view-control-group control-group">
                <label class="control-label label-form">Name</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editOrder.title}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Order Barcode</label>
                <div class="controls">
                    <div class="form-value">
                        <a target="JIRA" href="${actionBean.jiraUrl}${actionBean.editOrder.jiraTicketKey}" class="external" target="JIRA">
                            ${actionBean.editOrder.jiraTicketKey}
                        </a>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Research Project</label>
                <div class="controls">
                    <div class="form-value">
                        <stripes:link title="Research Project" href="${ctxpath}/projects/project.action?view=">
                            <stripes:param name="businessKey" value="${actionBean.editOrder.researchProject.businessKey}"/>
                            ${actionBean.editOrder.researchProject.title}</stripes:link>
                            (<a target="JIRA" href="${actionBean.jiraUrl}${actionBean.editOrder.researchProject.jiraTicketKey}" class="external" target="JIRA">
                                ${actionBean.editOrder.researchProject.jiraTicketKey}
                            </a>)
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Product</label>
                <div class="controls">
                    <div class="form-value">
                        <stripes:link title="Product" href="${ctxpath}/products/product.action?view">
                            <stripes:param name="product" value="${actionBean.editOrder.product.partNumber}"/>
                            ${actionBean.editOrder.product.productName}
                        </stripes:link>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Add-ons</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editOrder.addOnList}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Quote ID</label>
                <div class="controls">
                    <div class="form-value">
                        <a href="${actionBean.quoteUrl}" class="external" target="QUOTE">
                            ${actionBean.editOrder.quoteId}
                        </a>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Number of Lanes</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editOrder.count}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Comments</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editOrder.comments}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Samples</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editOrder.sampleBillingSummary}</div>
                </div>
            </div>

            <div class="borderHeader">
                Samples
            </div>

            <div class="fourcolumn">
                <ul>
                    <c:forEach items="${actionBean.editOrder.sampleSummaryComments}" var="comment">
                        <li>${comment}</li>
                    </c:forEach>
                </ul>
            </div>

            <table id="sampleData" class="table simple">
                <thead>
                    <tr>
                        <th width="90">ID</th>
                        <th>Participant ID</th>
                        <th width="40">Volume</th>
                        <th width="40">Concentration</th>
                        <th width="40">Yield Amount</th>
                        <th width="60">FP Status</th>
                        <th width="100">Status</th>
                        <th width="40">Eligible</th>
                        <th width="40">Billed</th>
                        <th width="40">Abandoned</th>
                        <th>Price Item 1</th>
                        <th>Price Item 2</th>
                        <th width="140">Comment</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.editOrder.samples}" var="sample">
                        <tr>
                            <td>
                                <c:choose>
                                    <c:when test="${sample.inBspFormat}">
                                        <stripes:link class="external" target="BSP_SAMPLE" title="BSP Sample" href="${actionBean.editOrderSampleSearchUrl}${sample.stripBspName}">
                                            ${sample.sampleName}
                                        </stripes:link>
                                    </c:when>
                                    <c:otherwise>
                                        ${sample.sampleName}
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td width="100">${sample.bspDTO.patientId}</td>
                            <td width="50">${sample.bspDTO.volume}</td>
                            <td width="50">${sample.bspDTO.concentration}</td>
                            <td width="70">${sample.bspDTO.total}</td>
                            <td width="60">
                                <c:if test="${sample.bspDTO.hasFingerprint}">
                                    <stripes:image name="" alt="Yes" src="/images/check.png"/>
                                </c:if>
                            </td>
                            <td width="100">${sample.billingStatus.displayName}</td>
                            <td width="70">&#160;</td>
                            <td width="70">&#160;</td>
                            <td width="70">&#160;</td>
                            <td width="100">&#160;</td>
                            <td width="100">&#160;</td>
                            <td width="200">${sample.sampleComment}</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
