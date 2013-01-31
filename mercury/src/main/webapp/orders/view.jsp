<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.DB" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product Order" sectionTitle="View Product Order">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {

                updateFundsRemaining();

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getSummary=&productOrder=${actionBean.editOrder.businessKey}",
                    dataType: 'json',
                    success: showSummary
                });

                bspDataCount = $j(".sampleName").length;
                $j(".sampleName").each(updateBspInformation);

                $j("#confirmDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {
                        OK: function () {
                            $j("#orderSamplesForm").submit();
                        },
                        Cancel: function () {
                            $j(this).dialog("close");
                        }
                    }
                });

                $j("#riskDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {
                        OK: function () {
                            $j("#riskStatus").attr("value", $j("#onRiskDialogId").attr("checked") != undefined);
                            $j("#onlyNew").attr("value", $j("#onlyNewDialogId").attr("checked") != undefined);
                            $j("#riskComment").attr("value", $j("#riskCommentId").val());

                            $j("#orderSamplesForm").submit();
                        },
                        Cancel: function () {
                            $j(this).dialog("close");
                        }
                    }
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
            });

            var bspDataCount = 0;

            function updateBspInformation(index, sampleIdCell) {
                var sampleId = $j(sampleIdCell).attr('id').split("-")[1];

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getBspData=&productOrder=${actionBean.editOrder.businessKey}&sampleId=" + sampleId,
                    dataType: 'json',
                    success: showSample
                });
            }

            function showSample(sampleData) {
                var sampleId = sampleData.sampleId;

                $j('#patient-' + sampleId).text(sampleData.patientId);
                $j('#volume-' + sampleId).text(sampleData.volume);
                $j('#concentration-' + sampleId).text(sampleData.concentration);
                $j('#total-' + sampleId).text(sampleData.total);

                if (sampleData.hasFingerprint) {
                    $j('#fingerprint-' + sampleId).html('<img src="${ctxpath}/images/check.png" title="Yes"/>');
                }

                bspDataCount--;

                if (bspDataCount < 1) {
                    $j('#sampleData').dataTable( {
                        "oTableTools": ttExportDefines,
                        "bSort": false
                    });
                }
            }

            function updateFundsRemaining() {
                var quoteIdentifier = $j("#quote").val();
                if ($j.trim(quoteIdentifier)) {
                    $j.ajax({
                        url: "${ctxpath}/orders/order.action?getQuoteFunding=&quoteIdentifier=${actionBean.editOrder.quoteId}",
                        dataType: 'json',
                        success: updateFunds
                    });
                } else {
                    $j("#fundsRemaining").text('');
                }
            }

            function updateFunds(data) {
                if (data.fundsRemaining) {
                    $j("#fundsRemaining").text('Funds Remaining: ' + data.fundsRemaining);
                } else {
                    $j("#fundsRemaining").text('Error: ' + data.error);
                }
            }

            function showSummary(data) {
                var dataList = '<ul>';
                data.map( function(item) { dataList += '<li>' + item.comment + '</li>'} );
                dataList += '</ul>';

                $j('#summaryId').html(dataList);
            }

            function showRiskDialog() {
                var numChecked = $("input.shiftCheckbox:checked").size();
                if (numChecked) {
                    $j("#dialogAction").attr("name", "setRisk");
                    $j("#selectedCountId").text(numChecked);
                    $j("#riskDialog").dialog("open").dialog("option", "width", 600);
                } else {
                    $j("#noneSelectedDialogMessage").text("Update Risk");
                    $j("#noneSelectedDialog").dialog("open");
                }
            }

            function showConfirm(action, actionPrompt) {
                var numChecked = $("input.shiftCheckbox:checked").size();
                if (numChecked) {
                    $j("#dialogAction").attr("name", action);
                    $j("#confirmDialogMessage").text(actionPrompt);
                    $j("#dialogNumSamples").text(numChecked);
                    $j("#confirmDialog").dialog("open");
                } else {
                    $j("#noneSelectedDialogMessage").text(actionPrompt);
                    $j("#noneSelectedDialog").dialog("open");
                }
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

    <div id="confirmDialog">
        <p>Are you sure you want to <span id="confirmDialogMessage"></span> the <span id="dialogNumSamples"></span> selected samples?</p>
    </div>

    <div id="riskDialog" style="width:600px;">
        <p>Manually Update Risk (<span id="selectedCountId"> </span> selected)</p>
        <p><span style="float:left; width:185px;">Update status to:</span>
            <input type="radio" id="onRiskDialogId" name="riskRadio" value="true" checked="checked" style="float:left;margin-right:5px;">
            <label style="float:left;width:60px;" for="onRiskDialogId">On Risk</label>
            <input type="radio" id="notOnRiskDialogId" name="riskRadio" value="false" style="float:left;margin-right:5px;">
            <label style="float:left;margin-right:10px;width:auto;" for="notOnRiskDialogId">Not On Risk</label>
            <input type="hidden" id="allDialogId" name="sampleRadio" value="false">
        <p style="clear:both">
            Comment:
        </p>

        <textarea id="riskCommentId" name="comment" class="controlledText" cols="80" rows="4"> </textarea>
    </div>


    <div id="noneSelectedDialog">
        <p>You must select at least one sample to <span id="noneSelectedDialogMessage"></span>.</p>
    </div>

        <stripes:form action="/orders/order.action" id="orderForm" class="form-horizontal">
            <stripes:hidden name="productOrder" value="${actionBean.editOrder.businessKey}"/>

            <div class="actionButtons">
                <c:if test="${actionBean.editOrder.draft}">
                    <%-- MLC PDOs can be placed by PM or PDMs, so I'm making the security tag accept either of those roles for 'Place Order'.
                         I am also putting 'Validate' under that same security tag since I think that may have the power to alter 'On-Riskedness'
                         for PDO samples --%>
                    <security:authorizeBlock roles="<%=new String[] {DB.Role.Developer.name, DB.Role.PDM.name, DB.Role.PM.name}%>">
                        <stripes:submit name="placeOrder" value="Validate and Place Order" disabled="${!actionBean.canPlaceOrder}" class="btn"/>
                        <stripes:submit name="validate" value="Validate" style="margin-left: 5px;" class="btn"/>
                    </security:authorizeBlock>

                    <%-- MLC GPLIM-802 says PDO edit should only be available to PDMs, i.e. not PMs. --%>
                    <security:authorizeBlock roles="<%=new String[] {DB.Role.Developer.name, DB.Role.PDM.name}%>">

                        &#160;
                        <stripes:link title="Click to edit ${actionBean.editOrder.title}"
                                      beanclass="${actionBean.class.name}" event="edit" class="btn"
                                      style="text-decoration: none !important;">
                            <span class="icon-shopping-cart"></span> <%=ProductOrderActionBean.EDIT_ORDER%>
                            <stripes:param name="productOrder" value="${actionBean.editOrder.businessKey}"/>
                        </stripes:link>
                    </security:authorizeBlock>

                    <security:authorizeBlock roles="<%=new String[] {DB.Role.Developer.name}%>">
                        <stripes:submit name="deleteOrder" value="Delete" style="margin-left: 5px;" class="btn"/>
                    </security:authorizeBlock>
                </c:if>
            </div>
        </stripes:form>

        <security:authorizeBlock roles="<%=new String[] {DB.Role.Developer.name, DB.Role.PDM.name}%>">
            <c:if test="${!actionBean.editOrder.draft}">
                <stripes:link title="Click to edit ${actionBean.editOrder.title}"
                    beanclass="${actionBean.class.name}" event="edit" class="pull-right">
                    <span class="icon-shopping-cart"></span> <%=ProductOrderActionBean.EDIT_ORDER%>
                    <stripes:param name="productOrder" value="${actionBean.editOrder.businessKey}"/>
                </stripes:link>
            </c:if>
        </security:authorizeBlock>

    <div style="both:clear"> </div>

        <stripes:form action="/orders/order.action" id="orderSamplesForm" class="form-horizontal">
            <stripes:hidden name="productOrder" value="${actionBean.editOrder.businessKey}"/>
            <stripes:hidden id="dialogAction" name=""/>

            <stripes:hidden id="riskStatus" name="riskStatus" value=""/>
            <stripes:hidden id="riskComment" name="riskComment" value=""/>
            <stripes:hidden id="onlyNew" name="onlyNew" value=""/>

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
                                &nbsp;
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
                    <div class="form-value">
                        <c:if test="${actionBean.editOrder.draft}"><span class="label label-info"></c:if>
                            ${actionBean.editOrder.orderStatus}
                        <c:if test="${actionBean.editOrder.draft}"></span></c:if>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Owner</label>
                <div class="controls">
                    <div class="form-value">
                            ${actionBean.getUserFullName(actionBean.editOrder.createdBy)}
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Research Project</label>
                <div class="controls">
                    <div class="form-value">
                        <c:if test="${actionBean.editOrder.researchProject != null}">
                            <stripes:link title="Research Project"
                                          beanclass="<%=ResearchProjectActionBean.class.getName()%>"
                                          event="view">
                                <stripes:param name="<%=ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER%>" value="${actionBean.editOrder.researchProject.businessKey}"/>
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
                        <span id="fundsRemaining" style="margin-left: 20px;"> </span>
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

                <c:if test="${!actionBean.editOrder.draft}">
                    <security:authorizeBlock roles="<%=new String[] {DB.Role.Developer.name, DB.Role.PDM.name}%>">
                        <span class="actionButtons">
                            <stripes:button name="deleteSamples" value="Delete Samples" class="btn"
                                        style="margin-left:30px;" onclick="showConfirm('deleteSamples', 'delete')"/>

                            <stripes:button name="abandonSamples" value="Abandon Samples" class="btn"
                                        style="margin-left:15px;" onclick="showConfirm('abandonSamples', 'abandon')"/>

                            <stripes:button name="setRisk" value="Set Risk" class="btn"
                                            style="margin-left:15px;" onclick="showRiskDialog()"/>
                        </span>

                        <div class="pull-right">
                            <stripes:text size="100" name="addSamplesText" style="margin-left:15px;"/>
                            <stripes:submit name="addSamples" value="Add Samples" class="btn" style="margin-right:15px;"/>
                        </div>
                    </security:authorizeBlock>
                </c:if>
            </div>

            <div id="summaryId" class="fourcolumn" style="margin-bottom:5px;">
                <img src="${ctxpath}/images/spinner.gif"/>
            </div>

            <table id="sampleData" class="table simple">
                <thead>
                    <tr>
                        <c:if test="${!actionBean.editOrder.draft}">
                            <th width="40">
                                <input for="count" type="checkbox" class="checkAll"/><span id="count" class="checkedCount"></span>
                            </th>
                        </c:if>
                        <th width="90">ID</th>
                        <th width="90">Participant ID</th>
                        <th width="40">Volume</th>
                        <th width="40">Concentration</th>
                        <th width="40">Yield Amount</th>
                        <th width="60">FP Status</th>
                        <th>On Risk</th>
                        <th width="40">Eligible</th>
                        <th width="40">Billed</th>
                        <th width="40">Status</th>
                        <th width="200">Comment</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.editOrder.samples}" var="sample">
                        <tr>
                            <c:if test="${!actionBean.editOrder.draft}">
                                <td>
                                    <stripes:checkbox class="shiftCheckbox" name="selectedProductOrderSampleIndices" value="${sample.samplePosition}"/>
                                </td>
                            </c:if>
                            <td id="sampleId-${sample.productOrderSampleId}" class="sampleName">
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
                            <td id="patient-${sample.productOrderSampleId}">&nbsp;</td>
                            <td id="volume-${sample.productOrderSampleId}">&nbsp;</td>
                            <td id="concentration-${sample.productOrderSampleId}">&nbsp;</td>
                            <td id="total-${sample.productOrderSampleId}">&nbsp;</td>
                            <td id="fingerprint-${sample.productOrderSampleId}" style="text-align: center">&nbsp;</td>
                            <td>${sample.riskString}</td>
                            <td>&#160;</td>
                            <td>&#160;</td>
                            <td>${sample.deliveryStatus.displayName}</td>
                            <td>${sample.sampleComment}</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
