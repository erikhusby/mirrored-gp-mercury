<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.DB" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product Order" sectionTitle="View Product Order">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#sampleData').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aoColumns": [
                        {"bSortable": false, "sType": "html"},   // ID
                        {"bSortable": false},                    // Participant ID
                        {"bSortable": false},                    // Volume
                        {"bSortable": false},                    // Concentration
                        {"bSortable": false},                    // Yield Amount
                        {"bSortable": false, "sType" : "title-string"},   // FP Status
                        {"bSortable": false},                    // Eligible
                        {"bSortable": false},                    // Billed
                        {"bSortable": false},                    // Abandoned
                        {"bSortable": false},                    // Price Item 1
                        {"bSortable": false},                    // Price Item 2
                        {"bSortable": false}]                   // Comment
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form action="/orders/order.action" id="orderForm" class="form-horizontal">
            <stripes:hidden name="productOrder" value="${actionBean.editOrder.businessKey}"/>

            <div class="actionButtons">
                <c:if test="${actionBean.editOrder.draft}">
                    <stripes:submit name="placeOrder" value="Place Order" disabled="${!actionBean.canPlaceOrder}" class="btn"/>
                </c:if>
            </div>
        </stripes:form>

        <stripes:link title="Click to edit ${actionBean.editOrder.title}"
            beanclass="${actionBean.class.name}" event="edit" class="pull-right">
            <span class="icon-shopping-cart"></span> <%=ProductOrderActionBean.EDIT_ORDER%>
            <stripes:param name="productOrder" value="${actionBean.editOrder.businessKey}"/>
        </stripes:link>

        <div style="both:clear"> </div>

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
                        <c:choose>
                            <c:when test="${actionBean.editOrder.draft}">
                                DRAFT
                            </c:when>
                            <c:otherwise>
                                <a target="JIRA" href="${actionBean.jiraUrl}${actionBean.editOrder.jiraTicketKey}" class="external" target="JIRA">${actionBean.editOrder.jiraTicketKey}</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Status</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editOrder.orderStatus}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Research Project</label>
                <div class="controls">
                    <div class="form-value">
                        <c:if test="${actionBean.editOrder.researchProject != null}">
                            <stripes:link title="Research Project"
                                          beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"
                                          event="view">
                                <stripes:param name="project" value="${actionBean.editOrder.researchProject.businessKey}"/>
                                ${actionBean.editOrder.researchProject.title}
                            </stripes:link>
                            (<a target="JIRA" href="${actionBean.jiraUrl}${actionBean.editOrder.researchProject.jiraTicketKey}" class="external" target="JIRA">
                                ${actionBean.editOrder.researchProject.jiraTicketKey}
                            </a>)
                        </c:if>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Product</label>
                <div class="controls">
                    <div class="form-value">
                        <c:if test="${actionBean.editOrder.product != null}">
                            <stripes:link title="Product" href="${ctxpath}/products/product.action?view">
                                <stripes:param name="product" value="${actionBean.editOrder.product.partNumber}"/>
                                ${actionBean.editOrder.product.productName}
                            </stripes:link>
                        </c:if>
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

            <c:if test="${actionBean.editOrder.product.productFamily.supportsNumberOfLanes}">
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Number of Lanes</label>

                    <div class="controls">
                        <div class="form-value">${actionBean.editOrder.count}</div>
                    </div>
                </div>
            </c:if>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Comments</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editOrder.comments}</div>
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
                            <td width="60" style="text-align: center">
                                <c:if test="${sample.bspDTO.hasFingerprint}">
                                    <img src="${ctxpath}/images/check.png" title="Yes"/>
                                </c:if>
                            </td>
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
