<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.roles" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<c:set var="editOrder" scope="page" value="${actionBean.editOrder}" />


<stripes:layout-render name="/layout.jsp" pageTitle="Product Order ${editOrder.title}" sectionTitle="Product Order ${editOrder.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {

                updateFundsRemaining();

                setupDialogs();

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getSummary=&productOrder=${editOrder.businessKey}",
                    dataType: 'json',
                    success: showSummary
                });

                bspDataCount = $j(".sampleName").length;

                var sampleNameFields = $j(".sampleName");

                // If there are no samples, set up the filter, otherwise kick off some javascript
                if (sampleNameFields.length > 0) {
                    var i,j,tempArray,chunk = 50;
                    for (i=0,j=sampleNameFields.length; i<j; i+=chunk) {
                        tempArray = sampleNameFields.slice(i,i+chunk);

                        updateBspInformation(tempArray);
                    }
                }
            });

            var bspDataCount = 0;

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
                                $j("#orderSamplesForm").submit();
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

                $j("#riskDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: [
                        {
                            id: "riskOkButton",
                            text: "OK",
                            click: function () {
                                $j(this).dialog("close");
                                $j("#riskOkButton").attr("disabled", "disabled");
                                $j("#riskStatus").attr("value", $j("#onRiskDialogId").attr("checked") != undefined);
                                $j("#onlyNew").attr("value", $j("#onlyNewDialogId").attr("checked") != undefined);
                                $j("#riskComment").attr("value", $j("#riskCommentId").val());

                                $j("#orderSamplesForm").submit();
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

                $j("#deleteConfirmation").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: [
                        {
                            id: "deleteOKButton",
                            text: "OK",
                            click: function () {
                                $j(this).dialog("close");

                                $j("#deleteOKButton").attr("disabled", "disabled");
                                $j("#orderForm").submit();
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

                $j("#abandonConfirmation").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: [
                        {
                            id: "abandonOkButton",
                            text: "OK",
                            click: function () {
                                $j(this).dialog("close");
                                $j("#abandonOkButton").attr("disabled", "disabled");
                                $j("#orderForm").submit();
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

            function updateBspInformation(chunkOfSamples) {
                var sampleIdString = "";
                $j(chunkOfSamples).each(function(index, sampleIdCell) {
                    sampleIdString += "&sampleIdsForGetBspData=" + $j(sampleIdCell).attr('id').split("-")[1];
                });

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getBspData=&productOrder=${editOrder.businessKey}&" + sampleIdString,
                    dataType: 'json',
                    success: showSamples
                });
            }

            function showSamples(sampleData) {
                for(var x=0; x<sampleData.length; x++) {

                    var sampleId = sampleData[x].sampleId;

                    $j('#collab-sample-' + sampleId).text(sampleData[x].collaboratorSampleId);
                    $j('#patient-' + sampleId).text(sampleData[x].patientId);
                    $j('#collab-patient-' + sampleId).text(sampleData[x].collaboratorParticipantId);
                    $j('#volume-' + sampleId).text(sampleData[x].volume);
                    $j('#concentration-' + sampleId).text(sampleData[x].concentration);
                    $j('#total-' + sampleId).text(sampleData[x].total);

                    if (sampleData[x].hasFingerprint) {
                        $j('#fingerprint-' + sampleId).html('<img src="${ctxpath}/images/check.png" title="Yes"/>');
                    }

                    bspDataCount--;
                }

                if (bspDataCount < 1) {
                    var oTable = $j('#sampleData').dataTable( {
                        "oTableTools": ttExportDefines,
                        "aaSorting": [[0, 'asc']],
                        "aoColumns": [
                            {"bSortable": true, "sType": "title-numeric"},  // Position and checkbox
                            {"bSortable": true},                            // ID
                            {"bSortable": true},                            // Collaborator Sample ID
                            {"bSortable": true},                            // Participant ID
                            {"bSortable": true},                            // Collaborator Participant ID
                            {"bSortable": true, "sType": "numeric"},        // Volume
                            {"bSortable": true, "sType": "numeric"},        // Concentration
                            {"bSortable": true, "sType": "numeric"},        // Yield Amount
                            {"bSortable": true, "sType" : "title-string"},  // FP Status
                            {"bSortable": true},                            // On Risk
                            {"bSortable": true},                            // Status
                            {"bSortable": true}                             // Comment
                        ]
                    }) ;

                    includeAdvancedFilter(oTable, "#sampleData");
                }
            }

            function updateFundsRemaining() {
                var quoteIdentifier = $j("#quote").value;
                if ($j.trim(quoteIdentifier)) {
                    $j.ajax({
                        url: "${ctxpath}/orders/order.action?getQuoteFunding=&quoteIdentifier=${editOrder.quoteId}",
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

            function showDeleteConfirm(action) {
                $j("#orderDialogAction").attr("name", action);
                $j("#deleteConfirmation").dialog("open");
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

            function showAbandonConfirm(action, actionPrompt, level) {
                $j("#orderDialogAction").attr("name", action);
                $j("#confirmDialogMessage").text(actionPrompt);

                if (level) {
                    $j("#abandonConfirmation").parent().css("border-color:red;");
                    $j("#abandonConfirmation").text("This Product Order has billed samples. Are you sure you want to abandon it?");
                    $j("#abandonConfirmation").prev().addClass("ui-state-error");
                }

                $j("#abandonConfirmation").dialog("open");
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

    <div style="display:none" id="confirmDialog">
        <p>Are you sure you want to <span id="confirmDialogMessage"></span> the <span id="dialogNumSamples"></span> selected samples?</p>
    </div>

    <div style="display:none" id="riskDialog" style="width:600px;">
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

    <div style="display:none" id="deleteConfirmation">
        <p>This will permanently remove this draft. Are you sure?</p>
    </div>

    <div style="display:none" id="abandonConfirmation">
        <p>This will abandon this Product Order. Are you sure?</p>
    </div>

    <div style="display:none" id="noneSelectedDialog">
        <p>You must select at least one sample to <span id="noneSelectedDialogMessage"></span>.</p>
    </div>

    <stripes:form action="/orders/order.action" id="orderForm" class="form-horizontal">
        <stripes:hidden name="productOrder" value="${editOrder.businessKey}"/>
        <stripes:hidden id="orderDialogAction" name=""/>

        <div class="actionButtons">
            <%-- Do not show abandon button at all for DRAFTs, do show for Submitted *or later states* --%>
            <c:if test="${not editOrder.draft}">
                <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                    <c:choose>
                        <c:when test="${actionBean.abandonable}">
                            <c:set var="abandonTitle" value="Click to abandon ${editOrder.title}"/>
                            <c:set var="abandonDisable" value="false"/>
                            <stripes:hidden name="selectedProductOrderBusinessKeys" value="${editOrder.businessKey}"/>
                        </c:when>
                        <c:otherwise>
                            <c:set var="abandonTitle"
                                   value="Cannot abandon this order because ${actionBean.abandonDisabledReason}"/>
                            <c:set var="abandonDisable" value="true"/>
                        </c:otherwise>
                    </c:choose>
                    <stripes:button name="abandonOrders" id="abandonOrders" value="Abandon Order"
                                    onclick="showAbandonConfirm('abandonOrders', 'abandon', ${actionBean.abandonWarning})"
                                    class="btn padright" title="${abandonTitle}" disabled="${abandonDisable}"/>
                </security:authorizeBlock>
            </c:if>
            <c:if test="${editOrder.draft}">
                <%-- MLC PDOs can be placed by PM or PDMs, so I'm making the security tag accept either of those roles for 'Place Order'.
                     I am also putting 'Validate' under that same security tag since I think that may have the power to alter 'On-Riskedness'
                     for PDO samples --%>
                <security:authorizeBlock roles="<%= roles(Developer, PDM, PM) %>">

                    <stripes:submit name="placeOrder" value="Validate and Place Order"
                                    disabled="${!actionBean.canPlaceOrder}" class="btn"/>
                    <stripes:submit name="validate" value="Validate" style="margin-left: 5px;" class="btn"/>
                </security:authorizeBlock>

                <stripes:link title="Click to edit ${editOrder.title}"
                              beanclass="${actionBean.class.name}" event="edit" class="btn"
                              style="text-decoration: none !important;">
                    <span class="icon-shopping-cart"></span> <%=ProductOrderActionBean.EDIT_ORDER%>
                    <stripes:param name="productOrder" value="${editOrder.businessKey}"/>
                </stripes:link>

                <security:authorizeBlock roles="<%= roles(Developer, PDM, PM) %>">
                    <stripes:button onclick="showDeleteConfirm('deleteOrder')" name="deleteOrder"
                                    value="Delete Draft" style="margin-left: 5px;" class="btn"/>
                </security:authorizeBlock>
            </c:if>
        </div>
    </stripes:form>

        <%-- PDO edit should only be available to PDMs, i.e. not PMs. --%>
        <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
            <c:if test="${!editOrder.draft}">
                <stripes:link title="Click to edit ${editOrder.title}"
                    beanclass="${actionBean.class.name}" event="edit" class="pull-right">
                    <span class="icon-shopping-cart"></span> <%=ProductOrderActionBean.EDIT_ORDER%>
                    <stripes:param name="productOrder" value="${editOrder.businessKey}"/>
                </stripes:link>
            </c:if>
        </security:authorizeBlock>

        <div style="both:clear"> </div>

        <stripes:form action="/orders/order.action" id="orderSamplesForm" class="form-horizontal">
            <stripes:hidden name="productOrder" value="${editOrder.businessKey}"/>
            <stripes:hidden id="dialogAction" name=""/>

            <stripes:hidden id="riskStatus" name="riskStatus" value=""/>
            <stripes:hidden id="riskComment" name="riskComment" value=""/>
            <stripes:hidden id="onlyNew" name="onlyNew" value=""/>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Order ID</label>
                <div class="controls">
                    <div class="form-value">
                        <c:choose>
                            <c:when test="${editOrder.draft}">
                                &#160;
                            </c:when>
                            <c:otherwise>
                                <a target="JIRA" href="${actionBean.jiraUrl(editOrder.jiraTicketKey)}" class="external" target="JIRA">${editOrder.jiraTicketKey}</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Product</label>
                <div class="controls">
                    <div class="form-value">
                        <c:if test="${editOrder.product != null}">
                            <stripes:link title="Product" href="${ctxpath}/products/product.action?view">
                                <stripes:param name="product" value="${editOrder.product.partNumber}"/>
                                ${editOrder.product.productName}
                            </stripes:link>
                        </c:if>
                    </div>
                </div>
            </div>


            <div class="view-control-group control-group">
                <label class="control-label label-form">Product Family</label>
                <div class="controls">
                    <div class="form-value">
                        <c:if test="${editOrder.product != null}">
                            ${editOrder.product.productFamily.name}
                        </c:if>
                    </div>
                </div>
            </div>


            <div class="view-control-group control-group">
                <label class="control-label label-form">Order Status</label>
                <div class="controls">
                    <div class="form-value">
                        <c:if test="${editOrder.draft}"><span class="label label-info"></c:if>
                            ${editOrder.orderStatus}
                        <c:if test="${editOrder.draft}"></span></c:if>
                    </div>
                </div>
            </div>


            <div class="view-control-group control-group">
                <label class="control-label label-form">Research Project</label>
                <div class="controls">
                    <div class="form-value">
                        <c:if test="${editOrder.researchProject != null}">
                            <stripes:link title="Research Project"
                                          beanclass="<%=ResearchProjectActionBean.class.getName()%>"
                                          event="view">
                                <stripes:param name="<%=ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER%>" value="${editOrder.researchProject.businessKey}"/>
                                ${editOrder.researchProject.title}
                            </stripes:link>
                            (<a target="JIRA" href="${actionBean.jiraUrl(editOrder.researchProject.jiraTicketKey)}" class="external" target="JIRA">
                            ${editOrder.researchProject.jiraTicketKey}
                            </a>)
                        </c:if>
                    </div>
                </div>
            </div>


            <div class="view-control-group control-group">
                <label class="control-label label-form">Owner</label>
                <div class="controls">
                    <div class="form-value">
                            ${actionBean.getUserFullName(editOrder.createdBy)}
                    </div>
                </div>
            </div>


            <c:if test="${editOrder.placedDate != null}">
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Placed Date</label>
                    <div class="controls">
                        <div class="form-value">
                            <fmt:formatDate value="${editOrder.placedDate}"/>
                        </div>
                    </div>
                </div>
            </c:if>

            <c:if test="${editOrder.product.productFamily.supportsNumberOfLanes}">
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Number of Lanes Per Sample</label>

                    <div class="controls">
                        <div class="form-value">${editOrder.count}</div>
                    </div>
                </div>
            </c:if>


            <div class="view-control-group control-group">
                <label class="control-label label-form">Add-ons</label>
                <div class="controls">
                    <div class="form-value">${editOrder.addOnList}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Quote ID</label>
                <div class="controls">
                    <div class="form-value">
                        <a href="${actionBean.quoteUrl}" class="external" target="QUOTE">
                                ${editOrder.quoteId}
                        </a>
                        <span id="fundsRemaining" style="margin-left: 20px;"> </span>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Can Bill</label>
                <div class="controls">
                    <div class="form-value">
                        <c:choose>
                            <c:when test="${actionBean.eligibleForBilling}">
                                <c:choose>
                                    <c:when test="${actionBean.billingSessionBusinessKey == null}">
                                        Yes, No Billing Session
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"
                                                      event="view">Yes,
                                            <stripes:param name="sessionKey" value="${actionBean.billingSessionBusinessKey}"/>
                                            ${actionBean.billingSessionBusinessKey}
                                        </stripes:link>
                                    </c:otherwise>
                                </c:choose>

                            </c:when>
                            <c:otherwise>
                                No
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Sample Status</label>
                <div class="controls">
                    <div class="form-value">
                        <div class="barFull view" title="${actionBean.percentInProgress}% In Progress">
                            <span class="barAbandon"
                                title="${actionBean.percentAbandoned}% Abandoned"
                                style="width: ${actionBean.percentAbandoned}%"> </span>
                            <span class="barComplete"
                                title="${actionBean.percentCompleted}% Completed"
                                style="width: ${actionBean.percentCompleted}%"> </span>
                        </div>
                        ${actionBean.progressString}
                    </div>
                </div>
            </div>


            <div class="view-control-group control-group">
                <label class="control-label label-form">Description</label>
                <div class="controls">
                    <div class="form-value">${editOrder.comments}</div>
                </div>
            </div>

            <div class="borderHeader">
                Samples

                <c:if test="${!editOrder.draft}">
                    <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
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

            <c:if test="${not empty actionBean.editOrder.samples}">
                <table id="sampleData" class="table simple">
                    <thead>
                        <tr>
                            <th width="40">
                                <c:if test="${!editOrder.draft}">
                                    <input for="count" type="checkbox" class="checkAll"/><span id="count" class="checkedCount"></span>
                                </c:if>
                            </th>
                            <th width="90">ID</th>
                            <th width="110">Collaborator Sample ID</th>
                            <th width="60">Participant ID</th>
                            <th width="110">Collaborator Participant ID</th>
                            <th width="40">Volume</th>
                            <th width="40">Concentration</th>
                            <th width="40">Yield Amount</th>
                            <th width="60">FP Status</th>
                            <th>On Risk</th>
                            <th width="40">Status</th>
                            <th width="200">Comment</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${editOrder.samples}" var="sample">
                            <tr>
                                <td>
                                    <c:if test="${!editOrder.draft}">
                                        <stripes:checkbox title="${sample.samplePosition}" class="shiftCheckbox" name="selectedProductOrderSampleIds" value="${sample.productOrderSampleId}"/>
                                    </c:if>
                                    ${sample.samplePosition + 1}
                                </td>
                                <td id="sampleId-${sample.productOrderSampleId}" class="sampleName">
                                    <c:choose>
                                        <c:when test="${sample.inBspFormat}">
                                            <stripes:link class="external" target="BSP_SAMPLE" title="BSP Sample" href="${actionBean.sampleSearchUrlForBspSample(sample)}">
                                                ${sample.sampleName}
                                            </stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            ${sample.sampleName}
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td id="collab-sample-${sample.productOrderSampleId}">&#160; </td>
                                <td id="patient-${sample.productOrderSampleId}">&#160;  </td>
                                <td id="collab-patient-${sample.productOrderSampleId}">&#160; </td>
                                <td id="volume-${sample.productOrderSampleId}">&#160; </td>
                                <td id="concentration-${sample.productOrderSampleId}">&#160; </td>
                                <td id="total-${sample.productOrderSampleId}">&#160; </td>
                                <td id="fingerprint-${sample.productOrderSampleId}" style="text-align: center">&#160; </td>
                                <td>${sample.riskString}</td>
                                <td>${sample.deliveryStatus.displayName}</td>
                                <td>${sample.sampleComment}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
