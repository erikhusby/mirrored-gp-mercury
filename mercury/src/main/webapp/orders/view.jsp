<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Review Product Order: ${actionBean.editOrder.title}"
                       sectionTitle="Review Product Order: ${actionBean.editOrder.title}"
                       businessKeyValue="${actionBean.editOrder.businessKey}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                updateFundsRemaining();

                setupDialogs();

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getSummary=&productOrder=${actionBean.editOrder.businessKey}",
                    dataType: 'json',
                    success: showSummary
                });

                bspDataCount = $j(".sampleName").length;

                var sampleNameFields = $j(".sampleName");

                var CHUNK_SIZE = 100;

                // If there are no samples, set up the filter, otherwise kick off some javascript
                if (sampleNameFields.length > 0) {
                    var i, j, tempArray;
                    for (i=0, j = sampleNameFields.length; i < j; i += CHUNK_SIZE) {
                        tempArray = sampleNameFields.slice(i, i + CHUNK_SIZE);

                        updateBspInformation(tempArray);
                    }
                }

                $j("#shippingLocation").tokenInput(
                        "${ctxpath}/orders/order.action?shippingLocationAutocomplete=", {
                            hintText: "Search for shipping location",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.bspShippingLocationTokenInput.completeData)},
                            resultsFormatter: formatInput,
                            tokenDelimiter: "${actionBean.bspShippingLocationTokenInput.separator}",
                            tokenLimit: 1
                        }
                );

                $j("#kitCollection").tokenInput(
                        "${ctxpath}/orders/order.action?groupCollectionAutocomplete=", {
                            hintText: "Search for group and collection",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.bspGroupCollectionTokenInput.completeData)},
                            resultsFormatter: formatInput,
                            tokenDelimiter: "${actionBean.bspGroupCollectionTokenInput.separator}",
                            tokenLimit: 1
                        }
                );

                $j("#notifyingUserList").tokenInput(
                        "${ctxpath}/orders/order.action?anyUsersAutocomplete=", {
                            hintText: "Enter a user name",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.notifyingUserListTokenInput.completeData)},
                            tokenDelimiter: "${actionBean.notifyingUserListTokenInput.separator}",
                            preventDuplicates: true,
                            resultsFormatter: formatInput
                        }
                );
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

                $j("#recalculateRiskDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: [
                        {
                            id: "recalculateRiskOkButton",
                            text: "OK",
                            click: function () {
                                $j(this).dialog("close");
                                $j("#recalculateRiskOkButton").attr("disabled", "disabled");

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
                                $j("#riskComment").attr("value", $j("#riskCommentId").val());

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
                    url: "${ctxpath}/orders/order.action?getBspData=&productOrder=${actionBean.editOrder.businessKey}&" + sampleIdString,
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
                    $j('#rin-' + sampleId).text(sampleData[x].rin);
                    $j('#total-' + sampleId).text(sampleData[x].total);
                    $j('#picoDate-' + sampleId).text(sampleData[x].picoDate);
                    $j('#picoDate-' + sampleId).attr("title", sampleData[x].picoDate);

                    if (sampleData[x].hasFingerprint) {
                        $j('#fingerprint-' + sampleId).html('<img src="${ctxpath}/images/check.png" title="Yes"/>');
                    } else {
                        // Need to replace &#160; with empty string.
                        $j('#fingerprint-' + sampleId).text('');
                    }

                    if (sampleData[x].hasSampleKitUploadRackscanMismatch) {
                        $j('#sampleKitUploadRackscanMismatch-' + sampleId).html('<img src="${ctxpath}/images/error.png" title="Yes"/>');
                    } else {
                        // Need to replace &#160; with empty string.
                        $j('#sampleKitUploadRackscanMismatch-' + sampleId).text('');
                    }

                    bspDataCount--;
                }

                if (bspDataCount < 1) {
                    var oTable = $j('#sampleData').dataTable( {
                        "oTableTools": ttExportDefines,
                        "aaSorting": [[1, 'asc']],
                        "aoColumns": [
                            {"bSortable": false},                           // Checkbox
                            {"bSortable": true, "sType": "numeric"},        // Position
                            {"bSortable": true, "sType": "html"},           // ID
                            {"bSortable": true},                            // Collaborator Sample ID
                            {"bSortable": true},                            // Participant ID
                            {"bSortable": true},                            // Collaborator Participant ID
                            {"bSortable": true, "sType": "numeric"},        // Volume
                            {"bSortable": true, "sType": "numeric"},        // Concentration

                        <c:if test="${actionBean.supportsRin}">
                            {"bSortable": true, "sType": "numeric"},        // RIN
                        </c:if>

                        <c:if test="${actionBean.supportsPico}">
                            {"bSortable": true, "sType": "title-us-date"},  // Pico Run Date
                        </c:if>
                            {"bSortable": true, "sType": "numeric"},        // Yield Amount
                            {"bSortable": true, "sType": "title-string"},   // FP Status
                            {"bSortable": true},                            // sample kit upload/rackscan mismatch
                            {"bSortable": true},                            // On Risk
                            {"bSortable": true},                            // Status
                            {"bSortable": true}                             // Comment
                        ]
                    });

                    includeAdvancedFilter(oTable, "#sampleData");
                    $j('.dataTables_filter input').clearable();

                    oneYearAgo = new Date();
                    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);

                    almostOneYearAgo = new Date(oneYearAgo);
                    almostOneYearAgo.setMonth(oneYearAgo.getMonth( ) + 1);

                    $j('.picoRunDate').each(getHighlightClass);
                }
            }

            var oneYearAgo;
            var almostOneYearAgo;

            function getHighlightClass() {

                var theDateString = $j(this).text();

                if (theDateString) {
                    var theDate = new Date(theDateString);

                    if (theDate) {
                        if ((theDate == 'No Pico') || (theDate < oneYearAgo)) {
                            $j(this).addClass("label label-important");
                        } else if (theDate < almostOneYearAgo) {
                            $j(this).addClass("label label-warning");
                        }
                    }
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

            function showRecalculateRiskDialog() {
                var numChecked = $("input.shiftCheckbox:checked").size();
                if (numChecked) {
                    $j("#dialogAction").attr("name", "recalculateRisk");
                    $j("#recalculateRiskSelectedCountId").text(numChecked);
                    $j("#recalculateRiskDialog").dialog("open").dialog("option", "width", 600);
                } else {
                    $j("#noneSelectedDialogMessage").text("Recalculate Risk");
                    $j("#noneSelectedDialog").dialog("open");
                }
            }

            function showRiskDialog() {
                var numChecked = $("input.shiftCheckbox:checked").size();
                if (numChecked) {
                    $j("#dialogAction").attr("name", "setRisk");
                    $j("#manualRiskSelectedCountId").text(numChecked);
                    $j("#riskDialog").dialog("open").dialog("option", "width", 600);
                } else {
                    $j("#noneSelectedDialogMessage").text("Update Risk");
                    $j("#noneSelectedDialog").dialog("open");
                }
            }

            function showDeleteConfirm(action) {
                $j("#dialogAction").attr("name", action);
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
                $j("#dialogAction").attr("name", action);
                $j("#confirmDialogMessage").text(actionPrompt);

                if (level) {
                    $j("#abandonConfirmation").parent().css("border-color:red;");
                    $j("#abandonConfirmation").text("This Product Order has billed samples. Are you sure you want to abandon it?");
                    $j("#abandonConfirmation").prev().addClass("ui-state-error");
                }

                $j("#abandonConfirmation").dialog("open");
            }

            function formatInput(item) {
                var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div style="display:none" id="confirmDialog">
            <p>Are you sure you want to <span id="confirmDialogMessage"></span> the <span id="dialogNumSamples"></span> selected samples?</p>
        </div>

        <div id="riskDialog" style="width:600px;display:none;">
            <p>Manually Update Risk (<span id="manualRiskSelectedCountId"> </span> selected)</p>
            <p><span style="float:left; width:185px;">Update status to:</span>
                <input type="radio" id="onRiskDialogId" name="riskRadio" value="true" checked="checked" style="float:left;margin-right:5px;">
                <label style="float:left;width:60px;" for="onRiskDialogId">On Risk</label>
                <input type="radio" id="notOnRiskDialogId" name="riskRadio" value="false" style="float:left;margin-right:5px;">
                <label style="float:left;margin-right:10px;width:auto;" for="notOnRiskDialogId">Not On Risk</label>
            <p style="clear:both">
            <label for="riskCommentId">Comment:</label>
            </p>

            <textarea id="riskCommentId" name="comment" class="controlledText" cols="80" rows="4"> </textarea>
        </div>

        <div id="recalculateRiskDialog" style="width:600px;display:none;">
            <p>Recalculate Risk (<span id="recalculateRiskSelectedCountId"> </span> selected)</p>
            <p><span style="float:left;">Recalculate On Risk status for selected samples. This will clear out all previous statuses.</span>
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
            <stripes:hidden name="productOrder" value="${actionBean.editOrder.businessKey}"/>
            <stripes:hidden id="dialogAction" name=""/>
            <stripes:hidden id="riskStatus" name="riskStatus" value=""/>
            <stripes:hidden id="riskComment" name="riskComment" value=""/>

            <div class="actionButtons">
                <c:choose>
                    <c:when test="${actionBean.editOrder.draft}">
                        <%-- PDOs can be placed by PM or PDMs, so the security tag accepts either of those roles for 'Place Order'. --%>
                        <%--'Validate' is also under that same security tag since that has the power to alter 'On-Riskedness' --%>
                        <%-- for PDO samples. --%>
                        <security:authorizeBlock roles="<%= roles(Developer, PDM, PM) %>">
                            <stripes:submit name="placeOrder" value="Validate and Place Order"
                                            disabled="${!actionBean.canPlaceOrder}" class="btn"/>
                            <stripes:submit name="validate" value="Validate" style="margin-left: 3px;" class="btn"/>
                        </security:authorizeBlock>

                        <stripes:link title="Click to edit ${actionBean.editOrder.title}"
                                      beanclass="${actionBean.class.name}" event="edit" class="btn"
                                      style="text-decoration: none !important; margin-left: 10px;">
                            <%=ProductOrderActionBean.EDIT_ORDER%>
                            <stripes:param name="productOrder" value="${actionBean.editOrder.businessKey}"/>
                        </stripes:link>

                        <security:authorizeBlock roles="<%= roles(Developer, PDM, PM) %>">
                            <stripes:button onclick="showDeleteConfirm('deleteOrder')" name="deleteOrder"
                                            value="Delete Draft" style="margin-left: 10px;" class="btn"/>
                        </security:authorizeBlock>
                    </c:when>
                    <c:otherwise>
                        <%-- Do not show abandon button at all for DRAFTs, do show for Submitted *or later states* --%>
                        <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                            <c:choose>
                                <c:when test="${actionBean.abandonable}">
                                    <c:set var="abandonTitle" value="Click to abandon ${actionBean.editOrder.title}"/>
                                    <c:set var="abandonDisable" value="false"/>
                                    <stripes:hidden name="selectedProductOrderBusinessKeys" value="${actionBean.editOrder.businessKey}"/>
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

                        <security:authorizeBlock roles="<%= roles(Developer, PDM, BillingManager) %>">
                            <stripes:param name="selectedProductOrderBusinessKeys" value="${actionBean.editOrder.businessKey}"/>
                            <stripes:submit name="downloadBillingTracker" value="Download Billing Tracker" class="btn" style="margin-right:5px;"/>
                        </security:authorizeBlock>

                        <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                            <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.UploadTrackerActionBean" event="view">
                                Upload Billing Tracker
                            </stripes:link>
                        </security:authorizeBlock>
                    </c:otherwise>
                </c:choose>

            </div>

            <div style="both:clear"> </div>

            <div class="row-fluid">
                <div class="form-horizontal span7">
                    <div class="view-control-group control-group">
                        <label class="control-label label-form">Order ID</label>
                        <div class="controls">
                            <div class="form-value">
                                <c:choose>
                                    <c:when test="${actionBean.editOrder.draft}">
                                        &#160;
                                    </c:when>
                                    <c:otherwise>
                                        <a id="orderId" href="${actionBean.jiraUrl(actionBean.editOrder.jiraTicketKey)}" class="external" target="JIRA">${actionBean.editOrder.jiraTicketKey}</a>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>

                    <c:if test="${not empty actionBean.editOrder.requisitionName}">
                        <div class="view-control-group control-group">
                            <label class="control-label label-form">Requisition</label>
                            <div class="controls">
                                <div class="form-value">
                                    <c:out value="${actionBean.editOrder.requisitionName}"/>
                                    (<a id="requisitionKey" href="${actionBean.portalRequisitionUrl(actionBean.editOrder.requisitionKey)}" class="external" target="Portal">${actionBean.editOrder.requisitionKey}</a>)
                                </div>
                            </div>
                        </div>
                    </c:if>

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
                        <label class="control-label label-form">Product Family</label>
                        <div class="controls">
                            <div class="form-value">
                                <c:if test="${actionBean.editOrder.product != null}">
                                    ${actionBean.editOrder.product.productFamily.name}
                                </c:if>
                            </div>
                        </div>
                    </div>


                    <div class="view-control-group control-group">
                        <label class="control-label label-form">Order Status</label>
                        <div class="controls">
                            <div class="form-value">
                                <c:if test="${actionBean.editOrder.draft}"><span class="label label-info"></c:if>
                                    ${actionBean.editOrder.orderStatus}
                                <c:if test="${actionBean.editOrder.draft}"></span></c:if>
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
                                    (<a target="JIRA" href="${actionBean.jiraUrl(actionBean.editOrder.researchProject.jiraTicketKey)}" class="external" target="JIRA">
                                    ${actionBean.editOrder.researchProject.jiraTicketKey}
                                    </a>)
                                </c:if>
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

                    <c:if test="${actionBean.editOrder.placedDate != null}">
                        <div class="view-control-group control-group">
                            <label class="control-label label-form">Placed Date</label>
                            <div class="controls">
                                <div class="form-value">
                                    <fmt:formatDate value="${actionBean.editOrder.placedDate}" pattern="${actionBean.datePattern}"/>
                                </div>
                            </div>
                        </div>
                    </c:if>

                    <div class="view-control-group control-group">
                        <label class="control-label label-form">Funding Deadline</label>
                        <div class="controls">
                            <div class="form-value">
                                <fmt:formatDate value="${actionBean.editOrder.fundingDeadline}" pattern="${actionBean.datePattern}"/>
                            </div>
                        </div>
                    </div>

                    <div class="view-control-group control-group">
                        <label class="control-label label-form">Publication Deadline</label>
                        <div class="controls">
                            <div class="form-value">
                                <fmt:formatDate value="${actionBean.editOrder.publicationDeadline}" pattern="${actionBean.datePattern}"/>
                            </div>
                        </div>
                    </div>

                    <c:if test="${actionBean.editOrder.product.productFamily.supportsNumberOfLanes}">
                        <div class="view-control-group control-group">
                            <label class="control-label label-form">Number of Lanes Per Sample</label>

                            <div class="controls">
                                <div class="form-value">${actionBean.editOrder.laneCount}</div>
                            </div>
                        </div>
                    </c:if>

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
                            <div class="form-value">${actionBean.editOrder.comments}</div>
                        </div>
                    </div>
                </div>

                <c:if test="${actionBean.sampleInitiation && actionBean.editOrder.draft}">
                    <div class="form-horizontal span5">
                        <fieldset>
                            <legend><h4>Sample Kit Request</h4></legend>

                            <div class="control-group">
                                <stripes:label for="tubesPerKit" class="control-label">
                                    Number of Samples
                                </stripes:label>
                                <div class="controls">
                                    <stripes:text id="tubesPerKit" name="numberOfSamples"
                                                  class="defaultText" title="Enter the number of samples"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="kitType" class="control-label">
                                    Kit Type
                                </stripes:label>
                                <div class="controls">
                                    <stripes:select id="kitType" name="plasticware">
                                        <stripes:option value="0.75mL">0.75mL</stripes:option>
                                    </stripes:select>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="kitCollection" class="control-label">
                                    Group and Collection
                                </stripes:label>
                                <div class="controls">
                                    <stripes:text
                                            id="kitCollection" name="bspGroupCollectionTokenInput.listOfKeys"
                                            class="defaultText"
                                            title="Search for collection and group"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="shippingLocation" class="control-label">
                                    Shipping Location
                                </stripes:label>
                                <div class="controls">
                                    <stripes:text
                                            id="shippingLocation" name="bspShippingLocationTokenInput.listOfKeys"
                                            class="defaultText"
                                            title="Search for shipping location"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <stripes:label for="materialInfo" class="control-label">
                                    Material Type
                                </stripes:label>
                                <div class="controls">
                                    <stripes:select name="materialInfo">
                                        <stripes:option label="Choose"/>
                                        <stripes:options-collection
                                                collection="${actionBean.dnaMatrixMaterialTypes}" label="bspName"/>
                                    </stripes:select>
                                </div>
                            </div>
                            <div class="control-group">
                                <stripes:label for="sourceMaterialInfo" class="control-label">
                                    Source Material Type
                                </stripes:label>
                                <div class="controls">
                                    <stripes:select name="sourceMaterialInfo">
                                        <stripes:option label="Choose"/>
                                        <stripes:options-collection
                                                collection="${actionBean.dnaMatrixMaterialTypes}" label="bspName"/>
                                    </stripes:select>
                                </div>
                            </div>
                            <!-- Users to Notify because of shipment completion -->
                            <div class="control-group">
                                <stripes:label for="notifyingUserList" class="control-label">Notify Complete</stripes:label>
                                <div class="controls">
                                    <stripes:text id="notifyingUserList" name="notifyingUserListTokenInput.listOfKeys" />
                                </div>
                            </div>
                        </fieldset>
                    </div>
                </c:if>
            </div>

            <div class="borderHeader">
                <h4 style="display:inline">Samples</h4>

                <c:if test="${!actionBean.editOrder.draft}">
                    <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                        <span class="actionButtons">
                            <stripes:button name="deleteSamples" value="Delete Samples" class="btn"
                                        style="margin-left:30px;" onclick="showConfirm('deleteSamples', 'delete')"/>

                            <stripes:button name="abandonSamples" value="Abandon Samples" class="btn"
                                        style="margin-left:15px;" onclick="showConfirm('abandonSamples', 'abandon')"/>

                            <stripes:button name="recalculateRisk" value="Recalculate Risk" class="btn"
                                            style="margin-left:15px;" onclick="showRecalculateRiskDialog()"/>

                            <stripes:button name="setRisk" value="Set Risk" class="btn"
                                            style="margin-left:5px;" onclick="showRiskDialog()"/>
                        </span>

                        <div class="pull-right">
                            <stripes:text size="100" name="addSamplesText" style="margin-left:15px;"/>
                            <stripes:submit name="addSamples" value="Add Samples" class="btn" style="margin-right:15px;"/>
                        </div>
                    </security:authorizeBlock>
                </c:if>
            </div>

            <div id="summaryId" class="fourcolumn" style="margin-bottom:5px;">
                <img src="${ctxpath}/images/spinner.gif" alt="spinner"/>
            </div>

            <c:if test="${not empty actionBean.editOrder.samples}">
                <table id="sampleData" class="table simple">
                    <thead>
                        <tr>
                            <th width="20">
                                <c:if test="${!actionBean.editOrder.draft}">
                                    <input for="count" type="checkbox" class="checkAll"/><span id="count" class="checkedCount"></span>
                                </c:if>
                            </th>
                            <th width="10">#</th>
                            <th width="90">ID</th>
                            <th width="110">Collaborator Sample ID</th>
                            <th width="60">Participant ID</th>
                            <th width="110">Collaborator Participant ID</th>
                            <th width="40">Volume</th>
                            <th width="40">Concentration</th>

                            <c:if test="${actionBean.supportsRin}">
                                <th width="40">RIN</th>
                            </c:if>

                            <c:if test="${actionBean.supportsPico}">
                                <th width="70">Last Pico Run Date</th>
                            </c:if>
                            <th width="40">Yield Amount</th>
                            <th width="60">FP Status</th>
                            <th width="60"><abbr title="Sample Kit Upload/Rackscan Mismatch">Rackscan Mismatch</abbr></th>
                            <th>On Risk</th>
                            <th width="40">Status</th>
                            <th width="200">Comment</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${actionBean.editOrder.samples}" var="sample">
                            <tr>
                                <td>
                                    <c:if test="${!actionBean.editOrder.draft}">
                                        <stripes:checkbox title="${sample.samplePosition}" class="shiftCheckbox" name="selectedProductOrderSampleIds" value="${sample.productOrderSampleId}"/>
                                    </c:if>
                                </td>
                                <td>
                                    ${sample.samplePosition + 1}
                                </td>
                                <td id="sampleId-${sample.productOrderSampleId}" class="sampleName">
                                    <%--@elvariable id="sampleLink" type="org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink"--%>
                                    <c:set var="sampleLink" value="${actionBean.getSampleLink(sample)}"/>
                                    <c:choose>
                                        <c:when test="${sampleLink.hasLink}">
                                            <stripes:link class="external" target="${sampleLink.target}" title="${sampleLink.label}" href="${sampleLink.url}">
                                                ${sample.name}
                                            </stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            ${sample.name}
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td id="collab-sample-${sample.productOrderSampleId}">&#160; </td>
                                <td id="patient-${sample.productOrderSampleId}">&#160;  </td>
                                <td id="collab-patient-${sample.productOrderSampleId}">&#160; </td>
                                <td id="volume-${sample.productOrderSampleId}">&#160; </td>
                                <td id="concentration-${sample.productOrderSampleId}">&#160; </td>

                                <c:if test="${actionBean.supportsRin}">
                                    <td id="rin-${sample.productOrderSampleId}">&#160; </td>
                                </c:if>

                                <c:if test="${actionBean.supportsPico}">
                                    <td>
                                        <div class="picoRunDate" id="picoDate-${sample.productOrderSampleId}" style="width:auto">&#160;</div>
                                    </td>
                                </c:if>

                                <td id="total-${sample.productOrderSampleId}">&#160; </td>
                                <td id="fingerprint-${sample.productOrderSampleId}" style="text-align: center">&#160; </td>
                                <td id="sampleKitUploadRackscanMismatch-${sample.productOrderSampleId}" style="text-align: center">&#160; </td>
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
