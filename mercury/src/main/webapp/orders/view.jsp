<%@ page import="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.datatables.DatatablesStateSaver" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product Order: ${actionBean.editOrder.title}"
                       dataTablesVersion="1.10" withColVis="true" withColReorder="true" withSelect="true"
                       sectionTitle="View Product Order: ${actionBean.editOrder.title}"
                       businessKeyValue="${actionBean.editOrder.businessKey}">
    <c:set var="columnHeaderSampleId" value="ID"/>
    <c:set var="columnHeaderCollaboratorSampleId" value="Collaborator Sample ID"/>
    <c:set var="columnHeaderCollaboratorParticipantId" value="Collaborator Participant ID"/>
    <c:set var="columnHeaderParticipantId" value="Participant ID"/>
    <c:set var="columnHeaderVolume" value="Volume"/>
    <c:set var="columnHeaderReceivedDate" value="Received Date"/>
    <c:set var="columnHeaderSampleType" value="Sample Type"/>
    <c:set var="columnHeaderShippedDate" value="Shipped Date"/>
    <c:set var="columnHeaderPicoRunDate" value="Last Pico Run Date"/>
    <c:set var="columnHeaderConcentration" value="Concentration"/>
    <c:set var="columnHeaderMaterialType" value="Material Type"/>
    <c:set var="columnHeaderRackscanMismatch" value="Rackscan Mismatch"/>
    <c:set var="columnHeaderOnRisk" value="On Risk"/>
    <c:set var="columnHeaderYieldAmount" value="Yield Amount"/>
    <c:set var="columnHeaderRin" value="RIN"/>
    <c:set var="columnHeaderRqs" value="RQS"/>
    <c:set var="columnHeaderDv2000" value="DV2000"/>
    <c:set var="columnHeaderOnRiskDetails" value="On Risk Details"/>
    <c:set var="columnHeaderProceedOutOfSpec" value="Proceed OOS"/>
    <c:set var="columnHeaderStatus" value="Status"/>
    <c:set var="columnHeaderCompletelyBilled" value="Billed"/>
    <c:set var="columnHeaderComment" value="Comment"/>

<stripes:layout-component name="extraHead">
    <style type="text/css">
        th.no-min-width {
            min-width: initial !important;
        }
        #sampleData_info {font-weight: bold}
    </style>
    <script src="${ctxpath}/resources/scripts/columnSelect.js"></script>
    <script src="${ctxpath}/resources/scripts/chosen_v1.6.2/chosen.jquery.min.js" type="text/javascript"></script>

<script type="text/javascript">
var kitDefinitionIndex = 0;
$j(document).ready(function () {

    updateFundsRemaining();
    setupDialogs();

    showSummary();
    // Only show the fill kit detail information for sample initiation PDOs. With the collaboration portal, there
    // can be kit definitions but since that is all automated, we do not want to show that. It is fairly irrelevant
    // after the work request happens. Adding a work request id field to the UI when there is a work request with
    // a non-sample initiation PDO.
    <c:if test="${actionBean.editOrder.sampleInitiation}">
    <c:forEach items="${actionBean.editOrder.productOrderKit.kitOrderDetails}" var="kitDetail">
    showKitDetail('${kitDetail.numberOfSamples}', '${kitDetail.kitType.displayName}',
            '${kitDetail.organismName}', '${kitDetail.bspMaterialName}',
            '${kitDetail.getPostReceivedOptionsAsString("<br/>")}');
    </c:forEach>
    </c:if>

    // if there are no sample kit details, just show one empty detail section
    if (kitDefinitionIndex == 0) {
        showKitDetail();
    }
    enableDefaultPagingOptions();

    function renderPico(data, type, row, meta) {
        if (type === 'display') {
            var $data = $j(data);
            if (sampleData[x].hasSampleKitUploadRackscanMismatch) {
                $j('#sampleKitUploadRackscanMismatch-' + sampleId).html('<img src="${ctxpath}/images/error.png" title="Yes"/>');
            }
        }
        return data
    }

    function renderBilled(data, type, row, meta) {
        if (type === 'display') {
            if (data) {
                return $j("<img/>", {src: "${ctxpath}/images/check.png", title: "Yes"})
            }
        }
        return data;
        <%--if (sampleData[x].completelyBilled) {--%>
        <%--$j('#completelyBilled-' + sampleId).html('<img src="${ctxpath}/images/check.png" title="Yes"/>');--%>
        <%--}--%>
    }
    var localStorageKey = 'DT_productOrderView';

    if ($j("#sampleData tbody>tr").length > 0) {
        var oTable = $j('#sampleData').dataTable({
            'paging':true,
            "scrollX": "940px",
              "scrollCollapse": true,
            "deferLoading": true,
            'colReorder': true,
            "stateSave": true,
            "pageLength": 25,
            'buttons': [{
                'extend': 'colvis',
                'text': "Show or Hide Columns",
                'columns': ':gt(1)',
                'prefixButtons': [{
                    'extend': 'colvis', 'text': 'Show All',
                    action: function (event, dt, node, config) {
                        dt.columns(config.columns).visible(true);
                    }
                }],
            }, standardButtons()],
            "columns": [
                {"orderable": false, 'class': 'no-min-width'},      // Checkbox
                {"orderable": true, 'class': 'no-min-width'},       // Position
                {"title": "${columnHeaderSampleId}", "orderable": true, "sType": "html"},
                {"title": "${columnHeaderCollaboratorSampleId}", "orderable": true},
                {"title": "${columnHeaderParticipantId}", "orderable": true},
                {"title": "${columnHeaderCollaboratorParticipantId}", "orderable": true},
                {"title": "${columnHeaderShippedDate}", "orderable": true},
                {"title": "${columnHeaderReceivedDate}", "orderable": true},
                {"title": "${columnHeaderSampleType}", "orderable": true},
                {"title": "${columnHeaderMaterialType}", "orderable": true},
                {"title": "${columnHeaderVolume}", "orderable": true},
                {"title": "${columnHeaderConcentration}", "orderable": true},
                <c:if test="${actionBean.supportsRin}">
                {"title": "${columnHeaderRin}", "orderable": true},
                {"title": "${columnHeaderRqs}", "orderable": true},
                {"title": "${columnHeaderDv2000}", "orderable": true},
                </c:if>

                <c:if test="${actionBean.supportsPico}"> {
                    "title": "${columnHeaderPicoRunDate}",
                    "orderable": true,
                    "type": "title-us-date",
                    render: getHighlightClass
                },</c:if>
                {"title": "${columnHeaderYieldAmount}", "orderable": true},
                {"title": "${columnHeaderRackscanMismatch}", "orderable": true},
                {"title": "${columnHeaderOnRisk}", "orderable": true},
                {"title": "${columnHeaderOnRiskDetails}", "orderable": false},
                {"title": "${columnHeaderProceedOutOfSpec}", "orderable": true},
                {"title": "${columnHeaderStatus}", "orderable": true},
                {"title": "${columnHeaderCompletelyBilled}", "orderable": true, "sType": "title-string",
                    render: renderBilled}, {"title": "${columnHeaderComment}", "orderable": true}
            ],
            "preDrawCallback": function (settings) {
                function imageForBoolean(selector, image) {
                    var $api = $j.fn.dataTable.Api(settings);
                    var nodes = $api.column(selector).nodes();
                    for (var i = 0; i < nodes.length; i++) {
                        var $cell = $j(nodes[i]);
                        if ($cell.text().trim() === 'true') {
                            $cell.html('<img src="' + image + '" title="Yes"/>');
                        } else {
                            $cell.empty();
                        }
                    }
                    return nodes;
                }

                imageForBoolean(".rackscanMismatch", "${ctxpath}/images/error.png");
                imageForBoolean(".completelyBilled", "${ctxpath}/images/check.png");
            },
            stateSaveCallback: function (settings, data) {
                var api = new $j.fn.dataTable.Api(settings);
                for (var index = 0; index < data.columns.length; index++) {
                    var item = data.columns[index];
                    var header = $j(api.column(index).header()).text();
                    if (header) {
                        item.headerName = header.escapeJson();
                    }
                }
                var tableData;
                try {
                    tableData = JSON.stringify(data).escapeJson();
                } catch (e) {
                    console.log("data could not be jsonized", e);

                }
                localStorage.setItem(localStorageKey, tableData);
                var stateData = {
                    "<%= DatatablesStateSaver.TABLE_STATE_KEY %>": tableData
                };
                $j.ajax({
                    'url': "${ctxpath}/orders/order.action?<%= DatatablesStateSaver.SAVE_SEARCH_DATA %>=",
                    'data': stateData,
                    dataType: 'json',
                    type: 'POST'
                });
            },
            "stateLoadCallback": function (settings, data) {
                var storedJson = '${actionBean.preferenceSaver.tableStateJson}';
                var useLocalData = true;
                if (storedJson && storedJson !== '{}') {
                    // if bad data was stored in the preferences it will cause problems here, so wrap
                    // it around an exception.
                    try {
                        data = JSON.parse(storedJson.escapeJson());
                        useLocalData = false;
                    } catch (e) {
                        console.log("data could not be rebigulated", e);
                    }
                }
                if (useLocalData) {
                    storedJson = localStorage.getItem(localStorageKey);
                    if (storedJson) {
                        data = JSON.parse(storedJson);
                    }
                }
                return data;
            }
        });
    }

    includeAdvancedFilter(oTable, "#sampleData");

    $j('#orderList').dataTable({
        "paging": false,
    });

    bspDataCount = $j(".sampleName").length;

    $j('div.onRisk').popover();
});

var bspDataCount = 0;

function setupDialogs() {
    function handleCancelEvent () {
        $j(this).dialog("close");
        $j("#dialogAction").attr("name", "");
    }

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
                click: handleCancelEvent
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
                click: handleCancelEvent
            }
        ]
    });

    $j("#replaceSamplesDialog").dialog({
        modal: true,
        autoOpen: false,
        buttons: [
            {
                id: "replaceSamplesButton",
                text: "OK",
                click: function() {
                    $j(this).dialog("close");
                    $j("#replaceSamplesButton").attr("disabled", "disabled");
                    $j("#replacementSampleList").attr("value", $j("#replacementSampleListId").val());
                    $j("#orderForm").submit();
                }
            },
            {
                text: "cancel",
                click: handleCancelEvent
            }
        ]
    });

    $j("#noReplacementsAvailableDialog").dialog({
        modal: true,
        autoOpen: false,
        buttons: {
            OK: function () {
                $j(this).dialog("close");
            }
        }
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
                    $j("#riskStatus").attr("value", $j("#onRiskDialogId").is(':checked'));
                    $j("#riskComment").attr("value", $j("#riskCommentId").val());

                    $j("#orderForm").submit();
                }
            },
            {
                text: "Cancel",
                click: handleCancelEvent
            }
        ]
    });

    $j("#proceedOosDialog").dialog({
        modal: true,
        autoOpen: false,
        buttons: [
            {
                id: "oosOkButton",
                text: "OK",
                click: function () {
                    $j(this).dialog("close");
                    $j("#oosOkButton").attr("disabled", "disabled");
                    $j("#proceedOos").attr("value", $j("input:radio[name='pOosRadio']:checked").val());

                    $j("#orderForm").submit();
                }
            },
            {
                text: "Cancel",
                click: handleCancelEvent
            }
        ]
    });

    $j("#abandonDialog").dialog({
        modal: true,
        autoOpen: false,
        buttons: [
            {
                id: "abandonOkButton",
                text: "OK",
                click: function () {
                    $j(this).dialog("close");
                    $j("#abandonOkButton").attr("disabled", "disabled");
                    $j("#abandonStatus").attr("value", $j("#abandonDialogId").attr("checked") != undefined);
                    $j("#abandonComment").attr("value", $j("#abandonSampleCommentId").val());

                    $j("#orderForm").submit();
                }
            },
            {
                text: "Cancel",
                click: handleCancelEvent
            }
        ]
    });

    $j("#unAbandonDialog").dialog({
        modal: true,
        autoOpen: false,
        buttons: [
            {
                id: "unAbandonOkButton",
                text: "OK",
                click: function () {
                    $j(this).dialog("close");
                    $j("#unAbandonOkButton").attr("disabled", "disabled");
                    $j("#abandonStatus").attr("value", $j("#unAbandonDialogId").attr("checked") != undefined);
                    $j("#unAbandonComment").attr("value", $j("#unAbandonSampleCommentId").val());

                    $j("#orderForm").submit();
                }
            },
            {
                text: "Cancel",
                click: handleCancelEvent
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
                click: handleCancelEvent
            }
        ]
    });

    $j("#placeConfirmation").dialog({
        modal: true,
        autoOpen: false,
        width: 600,
        height: 260,
        buttons: [
            {
                id: "placeOrderOKButton",
                text: "OK",
                click: function () {
                    $j(this).dialog("close");
                    $j("#placeOrderOKButton").attr("disabled", "disabled");
                    $j("#attestationConfirmed").attr("value", $j("#placeConfirmAttestation").prop("checked"));
                    $j("#orderForm").submit();
                }
            },
            {
                text: "Cancel",
                click: handleCancelEvent
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
                click: handleCancelEvent
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

showSamples();
function showSamples(sampleData) {
    if (bspDataCount < 1) {
        $j('.dataTables_filter input').clearable();
        oneYearAgo = new Date();
        oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
        almostOneYearAgo = new Date(oneYearAgo);
        almostOneYearAgo.setMonth(oneYearAgo.getMonth() + 1);
        $j('.picoRunDate').each(getHighlightClass);

        <%----%>
        <%--if (sampleData[x].hasSampleKitUploadRackscanMismatch) {--%>
        <%--$j('#sampleKitUploadRackscanMismatch-' + sampleId).html('<img src="${ctxpath}/images/error.png" title="Yes"/>');--%>
        <%--}--%>

        <%--if (sampleData[x].completelyBilled) {--%>
        <%--$j('#completelyBilled-' + sampleId).html('<img src="${ctxpath}/images/check.png" title="Yes"/>');--%>
        <%--}--%>
    }
}

var oneYearAgo;
var almostOneYearAgo;

function getHighlightClass(data, type, row, meta) {
    if (type === 'display') {
        var theDate = $j(data).text().trim();
        var $data = $j(data);
        if (theDate) {
            if ((theDate == 'No Pico') || (theDate < oneYearAgo)) {
                $data.addClass("label label-important");
            } else if (theDate < almostOneYearAgo) {
                $data.addClass("label label-warning");
            }
        }
        data = $data[0].outerHTML;
    }
    return data;
}

function updateFundsRemaining() {
    var quoteIdentifier = '${actionBean.editOrder.quoteId}';
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

    var quoteWarning = false;

    if (data.fundsRemaining) {
        var fundsRemainingNotification = 'Status: ' + data.status + ' - Funds Remaining: ' + data.fundsRemaining +
                ' with ' + data.outstandingEstimate + ' unbilled across existing open orders';
        var fundingDetails = data.fundingDetails;

        if(data.status != "Funded" ||
                Number(data.outstandingEstimate.replace(/[^0-9\.]+/g,"")) > Number(data.fundsRemaining.replace(/[^0-9\.]+/g,""))) {
            quoteWarning = true;
        }

        for(var detailIndex in fundingDetails) {
            fundsRemainingNotification += '\n'+fundingDetails[detailIndex].grantTitle;
            if(fundingDetails[detailIndex].activeGrant) {
                fundsRemainingNotification += ' -- Expires ' + fundingDetails[detailIndex].grantEndDate;
                if(fundingDetails[detailIndex].daysTillExpire < 45) {
                    fundsRemainingNotification += ' in ' + fundingDetails[detailIndex].daysTillExpire + ' days';
                    quoteWarning = true;
                }
            } else {
                fundsRemainingNotification += ' -- Has Expired ' + fundingDetails[detailIndex].grantEndDate;
                quoteWarning = true;
            }
            if(fundingDetails[detailIndex].grantStatus != "Active") {
                quoteWarning = true;
            }
            fundsRemainingNotification += '\n';
        }
        $j("#fundsRemaining").text(fundsRemainingNotification);
    } else {
        $j("#fundsRemaining").text('Error: ' + data.error);
        quoteWarning = true;
    }

    if(quoteWarning) {
        $j("#fundsRemaining").addClass("alert alert-error");
    } else {
        $j("#fundsRemaining").removeClass("alert alert-error");
    }
}

function showSummary() {
    var data = ${actionBean.summary};
    var dataList = '<ul>';
    data.map(function (item) {
        dataList += '<li>' + item.comment + '</li>'
    });
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

function showProceedOosDialog() {
    var numChecked = $("input.shiftCheckbox:checked").size();
    if (numChecked) {
        $j("#dialogAction").attr("name", "setProceedOos");
        $j("#proceedOosSelectedCountId").text(numChecked);
        $j("#proceedOosDialog").dialog("open").dialog("option", "width", 600);
    }
}

function showAbandonDialog() {
    var numChecked = $("input.shiftCheckbox:checked").size();
    if (numChecked) {
        $j("#dialogAction").attr("name", "abandonSamples");
        $j("#abandonSelectedSamplesCountId").text(numChecked);
        $j("#abandonDialog").dialog("open").dialog("option", "width", 600);
    } else {
        $j("#noneSelectedDialogMessage").text("Abandon Samples");
        $j("#noneSelectedDialog").dialog("open");
    }
}

function showUnAbandonDialog() {
    var numChecked = $("input.shiftCheckbox:checked").size();
    if (numChecked) {
        $j("#dialogAction").attr("name", "unAbandonSamples");
        $j("#unAbandonSelectedSamplesCountId").text(numChecked);
        $j("#unAbandonDialog").dialog("open").dialog("option", "width", 600);
    } else {
        $j("#noneSelectedDialogMessage").text("Un-Abandon Samples");
        $j("#noneSelectedDialog").dialog("open");
    }
}

function showSampleReplacementDialog() {

    var availableReplacements = '${actionBean.editOrder.numberForReplacement}';

    if (availableReplacements >0) {
        $j("#dialogAction").attr("name", "replaceSamples");
        $j("#replaceSamplesDialog").dialog("open");
//        $j("#replaceSamplesDialog").dialog("open").dialog("option", "width", 800);
    } else {
        $j("#noReplacementsAvailableDialog").dialog("open");
    }
}

function showDeleteConfirm(action) {
    $j("#dialogAction").attr("name", action);
    $j("#deleteConfirmation").dialog("open");
}

function showPlaceConfirm(action) {
    $j("#dialogAction").attr("name", action);
    $j("#placeConfirmation").dialog("open");
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

/**
 * showKitDetail will generate the display fields for the specific kit definition details if the PDO being displayed
 * is a sample initiation PDO.
 *
 *
 * @param samples               Number of samples for the sample kit definition details.  Will be used to initialize
 *                              the sample display section.
 * @param kitType               Kit type (Receptacle type) for the sample kit definition details.  Will be used to
 *                              initialize the kit type display area.
 * @param organismName          Name of the organism for the sample kit definition details.  Will be used to initialize
 *                              the organism display area.
 * @param materialInfo          Type of biological material associated with the sample kit definition details.  Will
 *                              be used to initialize the sample display sections
 * @param postReceivedOptions   The chosen processing options for the sample kit definition details which will be
 *                              applied when the sample kit is received.  Will be used to initialize the sample display
 *                              sections
 */
function showKitDetail(samples, kitType, organismName, materialInfo, postReceivedOptions) {
    var detailInfo = '<div id="kitDetailInfo' + kitDefinitionIndex + '">';
    detailInfo += '<h5 >Kit Definition Info</h5>';
    // Number of Samples
    detailInfo += '<div class="view-control-group control-group">\n' +
            '<label for="kitNumberOfSamples' + kitDefinitionIndex + '" class="control-label label-form" >Samples Requested</label>\n' +
            '<div class="controls">\n' +
            '<div class="form-value" id="kitNumberOfSamples' + kitDefinitionIndex + '">\n';
    if (samples) {
        detailInfo += samples;
    }
    detailInfo += '</div>\n</div>\n</div>';

    // Kit Type

    detailInfo += '<div class="view-control-group control-group">\n' +
            '<label class="control-label label-form" for="kitKitType' + kitDefinitionIndex + '">Kit Type</label>\n' +
            '<div class="controls">\n' +
            '<div class="form-value" id="kitKitType' + kitDefinitionIndex + '">\n';
    if (kitType) {
        detailInfo += kitType;

    }
    detailInfo += '</div>\n</div>\n</div>\n';

    // Organism

    detailInfo += '<div class="view-control-group control-group">' +
            '<label for="kitOrganism' + kitDefinitionIndex + '" class="control-label label-form">Organism</label>' +
            '<div id="kitOrganism' + kitDefinitionIndex + '" class="controls">' +
            '<div class="form-value">';
    if (organismName) {
        detailInfo += organismName;
    }
    detailInfo += '</div></div></div>';

    // Material Info
    detailInfo += '<div class="view-control-group control-group">\n' +
            '<label class="control-label label-form" for="kitMaterialInfo' + kitDefinitionIndex + '">Material Information</label>\n' +

            '<div class="controls">\n' +
            '<div class="form-value" id="kitMaterialInfo' + kitDefinitionIndex + '">';
    if (materialInfo) {

        detailInfo += materialInfo;
    }

    detailInfo += '</div>\n</div>\n</div>\n';


    // Post Receipt Options
    detailInfo += '<div class="view-control-group control-group">' +
            '<label class="control-label label-form">Post-Received Options</label>' +
            '<div class="controls">' +
            '<div class="form-value">' +
            '<div class="form-value">';

    if (postReceivedOptions) {

        detailInfo += postReceivedOptions;
    }

    detailInfo += '</div></div></div></div></div>';

    $j("#sampleInitiationInfo").append(detailInfo);
    kitDefinitionIndex++;
}

function formatInput(item) {
    var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
    return "<li>" + item.dropdownItem + extraCount + '</li>';
}
</script>
</stripes:layout-component>

<stripes:layout-component name="content">

<div style="display:none" id="confirmDialog">
    <p>Are you sure you want to <span id="confirmDialogMessage"></span> the <span id="dialogNumSamples"></span> selected
        samples?</p>
</div>

    <div id="replaceSamplesDialog" style="display:none">

        <p>Add up to ${actionBean.editOrder.numberForReplacement} Replacement samples </p>

        <label for="replacementSampleListId">Replacement Samples:</label>

        <textarea rows="15" cols="120" id="replacementSampleListId" name="replacementSamples" > </textarea>

    </div>

<div id="riskDialog" style="width:600px;display:none;">
    <p>Manually Update Risk (<span id="manualRiskSelectedCountId"> </span> selected)</p>

    <p><span style="float:left; width:185px;">Update status to:</span>
        <input type="radio" id="onRiskDialogId" name="riskRadio" value="true" checked="checked"
               style="float:left;margin-right:5px;">
        <label style="float:left;width:60px;" for="onRiskDialogId">On Risk</label>
        <input type="radio" id="notOnRiskDialogId" name="riskRadio" value="false" style="float:left;margin-right:5px;">
        <label style="float:left;margin-right:10px;width:auto;" for="notOnRiskDialogId">Not On Risk</label>

    <p style="clear:both">
        <label for="riskCommentId">Comment:</label>
    </p>

    <textarea id="riskCommentId" name="comment" class="controlledText" cols="80" rows="4"> </textarea>
</div>

<div id="proceedOosDialog" style="width:600px;display:none;">
    <p>Proceed if Out of Spec (<span id="proceedOosSelectedCountId"> </span> selected)</p>

    <p><span style="float:left; width:185px;">Update status to:</span>
        <c:forEach items="<%=ProductOrderSample.ProceedIfOutOfSpec.values()%>" var="pOos">
            <label style="float:left;width:60px;">
                <input type="radio" name="pOosRadio" value="${pOos.toString()}" style="float:left;margin-right:5px;">
                ${pOos.displayName}
            </label>
        </c:forEach>
</div>

<div id="abandonDialog" style="width:600px;display:none;">
    <p>Abandon Samples (<span id="abandonSelectedSamplesCountId"> </span> selected)</p>

    <p style="clear:both">
        <label for="abandonSampleCommentId">Comment:</label>
    </p>

    <textarea id="abandonSampleCommentId" name="comment" class="controlledText" cols="80" rows="4"> </textarea>
</div>

<%--<div id="unAbandonDialog" style="width:600px;display:none;">--%>
    <%--<p>Un-Abandon Samples (<span id="unAbandonSelectedSamplesCountId"> </span> selected)</p>--%>

    <%--<p style="clear:both">--%>
        <%--<label for="unAbandonSampleCommentId">Comment:</label>--%>
    <%--</p>--%>

    <%--<textarea id="unAbandonSampleCommentId" name="comment" class="controlledText" cols="80" rows="4"> </textarea>--%>
<%--</div>--%>

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

    <div style="display:none" id="noReplacementsAvailableDialog">
        <p>There are no samples for you to replace.  Please open a new PDO to do any new work.</p>
    </div>

<stripes:form action="/orders/order.action" id="orderForm" class="form-horizontal">

<div id="placeConfirmation" style="display:none;" title="Place Order">
    <p>Click OK to place the order and make it available for lab work.</p>
    <c:choose>
        <c:when test="${actionBean.numberSamplesNotReceived == null}">
            <p>N/A</p>
        </c:when>
        <c:when test="${actionBean.numberSamplesNotReceived == 1}">
            <p>
                <em>NOTE:</em> There is one sample that has not yet been received. If the order is placed,
                this sample will be removed from the order.
            </p>
        </c:when>
        <c:when test="${actionBean.numberSamplesNotReceived > 1}">
            <p>
                <em>NOTE:</em> There are ${actionBean.numberSamplesNotReceived} samples that have not yet been received.
                If the order is placed, these samples will be removed from the order.
            </p>
        </c:when>
    </c:choose>

    <div class="form-horizontal span7">
        <div class="view-control-group control-group">
            <label class="control-label label-form">Regulatory Info</label>

            <div class="controls">
                <jsp:include page="regulatory_info_view.jsp"/>
            </div>
        </div>
    </div>

    <span class="control-group">
        <span class="controls">
            <stripes:checkbox id="placeConfirmAttestation" name="editOrder.attestationConfirmed"
                              class="controls controls-text" style="margin-top: 0px; margin-right: 5px;"/>
        </span>
        <stripes:label for="placeConfirmAttestation" class="controls control-label"
                       style="display: inline; position: absolute;">
            ${actionBean.attestationMessage}
        </stripes:label>
    </span>
</div>

<stripes:hidden name="productOrder" value="${actionBean.editOrder.businessKey}"/>
<stripes:hidden id="dialogAction" name=""/>
<stripes:hidden id="riskStatus" name="riskStatus" value=""/>
<stripes:hidden id="riskComment" name="riskComment" value=""/>
<stripes:hidden id="proceedOos" name="proceedOos" value=""/>
<stripes:hidden id="abandonComment" name="abandonComment" value=""/>
    <stripes:hidden name="replacementSampleList" id="replacementSampleList" value="" />
<%--<stripes:hidden id="unAbandonComment" name="unAbandonComment" value=""/>--%>
<stripes:hidden id="attestationConfirmed" name="editOrder.attestationConfirmed" value=""/>

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
            <c:if test="${actionBean.canPlaceOrder}">
                <security:authorizeBlock roles="<%= roles(Developer, PDM, PM) %>">
                    <stripes:button onclick="showPlaceConfirm('placeOrder')" name="placeOrder"
                                    value="Place Order" class="btn"/>
                </security:authorizeBlock>
            </c:if>
            <security:authorizeBlock roles="${actionBean.modifyOrderRoles}">
                <c:choose>
                    <c:when test="${actionBean.canAbandonOrder}">
                        <c:set var="abandonTitle" value="Click to abandon ${actionBean.editOrder.title}"/>
                        <c:set var="abandonDisable" value="false"/>
                        <stripes:hidden name="selectedProductOrderBusinessKeys"
                                        value="${actionBean.editOrder.businessKey}"/>
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

            <c:if test="${actionBean.editOrder.orderStatus.canBill()}">

                <security:authorizeBlock roles="<%= roles(Developer, PDM, BillingManager) %>">
                    <stripes:param name="selectedProductOrderBusinessKeys" value="${actionBean.editOrder.businessKey}"/>
                    <stripes:submit name="downloadBillingTracker" value="Download Billing Tracker" class="btn"
                                    style="margin-right:30px;"/>
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                    <c:choose>
                        <c:when test="${actionBean.productOrderListEntry.billing}">
                            <span class="disabled-link" title="Upload not allowed while billing is in progress">Upload Billing Tracker</span>
                        </c:when>
                        <c:otherwise>
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.UploadTrackerActionBean"
                                    event="view" >
                                Upload Billing Tracker
                            </stripes:link>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${actionBean.productOrderListEntry.billing}">
                        &#160;
                        Upload not allowed while billing is in progress
                    </c:if>

                    &nbsp;&nbsp;&nbsp;&nbsp;
                    <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.BillingLedgerActionBean"><stripes:param name="orderId" value="${actionBean.editOrder.jiraTicketKey}"/>Online Billing Ledger</stripes:link>
                </security:authorizeBlock>

            </c:if>

            <security:authorizeBlock roles="<%= roles(PDM, PM, Developer) %>">

                <c:if test="${!actionBean.editOrder.savedInSAP && !actionBean.editOrder.pending && !actionBean.editOrder.draft}">
                    &nbsp;&nbsp;&nbsp;&nbsp;
                    <stripes:submit name="${actionBean.publishSAPAction}" id="${actionBean.publishSAPAction}" value="Publish Product Order to SAP"
                                    class="btn padright" title="Click to Publish Product Order to SAP"/>

                </c:if>
            </security:authorizeBlock>



        </c:otherwise>
    </c:choose>

</div>

<div style="both:clear"></div>

<div class="row-fluid">
<div class="form-horizontal span7">

    <c:if test="${actionBean.editOrder.childOrder}">
        <div class="view-control-group control-group">
            <label class="control-label label-form">Parent Order</label>

            <div class="controls">
                <div class="form-value">
                    <stripes:link
                            beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                            event="view">
                        <stripes:param name="productOrder" value="${actionBean.editOrder.parentOrder.businessKey}"/>
                        ${actionBean.editOrder.parentOrder.title}
                    </stripes:link>
                </div>
            </div>
        </div>
    </c:if>

<div class="view-control-group control-group">
    <label class="control-label label-form">Order ID</label>

    <div class="controls">
        <div class="form-value">
            <c:choose>
                <c:when test="${actionBean.editOrder.draft}">
                    &#160;
                </c:when>
                <c:otherwise>
                    <a id="orderId" href="${actionBean.jiraUrl(actionBean.editOrder.jiraTicketKey)}" class="external"
                       target="JIRA">${actionBean.editOrder.jiraTicketKey}</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

    <div class="view-control-group control-group">
        <label class="control-label label-form">SAP Order ID</label>

        <div class="controls">
            <div class="form-value">
                    <%--<c:if test="${!actionBean.editOrder.draft && !actionBean.editOrder.pending && actionBean.editOrder.savedInSAP}">--%>
                    <c:if test="${actionBean.editOrder.savedInSAP}">
                        ${actionBean.editOrder.sapOrderNumber}
                    </c:if>
            </div>
        </div>
    </div>

    <c:if test="${not empty actionBean.editOrder.requisitionName}">
    <div class="view-control-group control-group">
        <label class="control-label label-form">Requisition</label>

        <div class="controls">
            <div class="form-value">
                <c:out value="${actionBean.editOrder.requisitionName}"/>
                (<a id="requisitionKey" href="${actionBean.portalRequisitionUrl(actionBean.editOrder.requisitionKey)}"
                    class="external" target="Portal">${actionBean.editOrder.requisitionKey}</a>)
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

<c:if test="${actionBean.editOrder.product.supportsNumberOfLanes}">
    <c:if test="${actionBean.editOrder.squidWorkRequest != null}">
        <div>
            <div class="control-group view-control-group">
                <label class="control-label label-form">Squid Work Request</label>

                <div class="controls">
                    <div class="form-value">
                        <a target="SQUID" href="${actionBean.squidWorkRequestUrl}"
                           class="external">${actionBean.editOrder.squidWorkRequest}</a>
                    </div>
                </div>
            </div>
        </div>
    </c:if>
</c:if>

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

<!-- If this order has a work request created by the collaboration portal, then display the work request as a link. -->
<c:if test="${actionBean.collaborationKitRequest}">
    <div class="view-control-group control-group">
        <label class="control-label label-form">Work Request</label>

        <div class="controls">
            <div class="form-value">
                <a href="${actionBean.workRequestUrl}" class="external" target="BSP">
                        ${actionBean.editOrder.productOrderKit.workRequestId}</a>
            </div>
        </div>
    </div>
</c:if>

<div class="view-control-group control-group">
    <label class="control-label label-form">Order Status</label>

    <div class="controls">
        <div class="form-value">
            <span class="${actionBean.editOrder.orderStatus.cssClass}">${actionBean.editOrder.orderStatus}</span>
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
                    <stripes:param name="<%=ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER%>"
                                   value="${actionBean.editOrder.researchProject.businessKey}"/>
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
    <label class="control-label label-form">Regulatory Information</label>

    <div class="controls">
        <div class="form-value">
            <jsp:include page="regulatory_info_view.jsp"/>
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

<c:if test="${actionBean.editOrder.requiresLaneCount()}">
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
            <div id="fundsRemaining"> </div>
        </div>
    </div>
</div>
<c:if test="${actionBean.editOrder.skipQuoteReason != null}">
    <div class="view-control-group control-group">
        <label class="control-label label-form">Quote Skip Reason</label>

        <div class="controls" id="quoteSkipReason">
            <div class="form-value">${actionBean.editOrder.skipQuoteReason}</div>
        </div>
    </div>
</c:if>


<div class="view-control-group control-group">
    <label class="control-label label-form">Can Bill / Ledger Status</label>

    <div class="controls">
        <div class="form-value">
            <c:choose>
                <c:when test="${actionBean.eligibleForBilling}">
                    <c:choose>
                        <c:when test="${actionBean.billingSessionBusinessKey == null}">
                            Yes, No Billing Session
                        </c:when>
                        <c:otherwise>
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"
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
            &nbsp;
            <c:choose>
                <c:when test="${actionBean.productOrderListEntry.readyForReview}">
                        <span class="badge badge-warning">
                            <%=ProductOrderListEntry.LedgerStatus.READY_FOR_REVIEW.getDisplayName()%>
                        </span>
                </c:when>
                <c:when test="${actionBean.productOrderListEntry.billing}">
                        <span class="badge badge-info">
                            <%=ProductOrderListEntry.LedgerStatus.BILLING_STARTED.getDisplayName()%>
                        </span>
                </c:when>
                <c:when test="${actionBean.productOrderListEntry.readyForBilling}">
                        <span class="badge badge-success">
                            <%=ProductOrderListEntry.LedgerStatus.READY_TO_BILL.getDisplayName()%>
                        </span>
                </c:when>
            </c:choose>
        </div>
    </div>
</div>

<div class="view-control-group control-group">
    <label class="control-label label-form">Sample Status</label>

    <div class="controls">
        <div class="form-value">
                <stripes:layout-render name="/orders/sample_progress_bar.jsp" status="${actionBean.progressFetcher.getStatus(actionBean.editOrder.businessKey)}" extraStyle="view"/>
                ${actionBean.progressString}
        </div>
    </div>
</div>
<c:if test="${actionBean.infinium}">
    <div class="view-control-group control-group">
        <label class="control-label label-form">Pipeline Location</label>

        <div class="controls">
            <div class="form-value">${actionBean.editOrder.pipelineLocation.displayName}</div>
        </div>
    </div>

    <c:forEach items="${actionBean.attributes}" var="item">
        <div class="view-control-group control-group">
            <stripes:label for="attributes[${item.key}]" class="control-label label-form">
                ${item.key}
            </stripes:label>
            <div class="controls">
                <div class="form-value">${item.value}</div>
            </div>
        </div>
    </c:forEach>
</c:if>
<div class="view-control-group control-group">
    <label class="control-label label-form">Description</label>

    <div class="controls">
        <div class="form-value">${actionBean.editOrder.comments}</div>
    </div>
</div>
</div>

    <div class="form-horizontal span5">


        <c:if test="${actionBean.editOrder.sampleInitiation}">
        <fieldset>
            <legend>
                <h4>
                    Sample Kit Request

                    <c:if test="${!actionBean.editOrder.draft}">
                        - <a href="${actionBean.workRequestUrl}" target="BSP">
                            ${actionBean.editOrder.productOrderKit.workRequestId}
                    </a>
                    </c:if>
                </h4>
            </legend>
            <div id="sampleInitiationInfo">

                <div class="view-control-group control-group">
                    <stripes:label for="kitCollection"
                                   class="control-label label-form">Group and Collection</stripes:label>
                    <div id="kitCollection" class="controls">
                        <div class="form-value">
                            <c:if test="${actionBean.editOrder.productOrderKit.sampleCollectionId != null}">
                                ${actionBean.editOrder.productOrderKit.sampleCollectionName}
                            </c:if>
                        </div>
                    </div>
                </div>

                <div class="view-control-group control-group">
                    <stripes:label for="kitSite" class="control-label label-form">Shipping Location</stripes:label>
                    <div id="kitSite" class="controls">
                        <div class="form-value">
                            <c:if test="${actionBean.editOrder.productOrderKit.siteId != null}">
                                ${actionBean.editOrder.productOrderKit.siteName}
                            </c:if>
                        </div>
                    </div>
                </div>
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Transfer Method</label>

                    <div class="controls">
                        <div class="form-value">
                            <div class="form-value">${actionBean.editOrder.productOrderKit.transferMethod.value}</div>
                        </div>
                    </div>
                </div>

                <div class="view-control-group control-group">
                    <label class="control-label label-form">Exome Express?</label>

                    <div class="controls">
                        <div class="form-value">
                            <div class="form-value">This is
                                <c:if test="${!actionBean.editOrder.productOrderKit.exomeExpress}"> not </c:if>
                                an Exome Express Kit
                            </div>
                        </div>
                    </div>
                </div>

                <div class="view-control-group control-group">
                    <label class="control-label label-form">Notification List</label>

                    <div class="controls">
                        <div class="form-value">
                                ${actionBean.getUserListString(actionBean.editOrder.productOrderKit.notificationIds)}
                        </div>
                    </div>
                </div>
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Comments</label>

                    <div class="controls">
                        <div class="form-value">
                            <div class="form-value">${actionBean.editOrder.productOrderKit.comments}</div>
                        </div>
                    </div>
                </div>
            </div>
        </fieldset>
        </c:if>
    </div>
</div>
    <div class="borderHeader">
        <h4 style="display:inline">Replacement Sample Orders</h4>
    </div>

    <table id="orderList" class="table simple">
            <thead>
            <tr>
                <th>Name</th>
                <th>Order ID</th>
                <th>Status</th>
                <th>Updated</th>
                <th width="80">%&nbsp;Complete</th>
                <th>Replacement Sample Count</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.editOrder.childOrders}" var="order">
                <tr>
                    <td>
                        <stripes:link
                                beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                event="view">
                            <stripes:param name="productOrder" value="${order.businessKey}"/>
                            ${order.title}
                        </stripes:link>
                    </td>
                    <td>
                        <c:choose>

                            <%-- draft PDO --%>
                            <c:when test="${order.draft}">
                                <span title="DRAFT">&#160;</span>
                            </c:when>
                            <c:otherwise>
                                <a class="external" target="JIRA" href="${actionBean.jiraUrl(order.jiraTicketKey)}"
                                   class="external" target="JIRA">
                                        ${order.jiraTicketKey}
                                </a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>${order.orderStatus}</td>
                    <td>
                        <fmt:formatDate value="${order.modifiedDate}" pattern="${actionBean.datePattern}"/>
                    </td>
                    <td align="center">
                        <stripes:layout-render name="/orders/sample_progress_bar.jsp"
                                               status="${actionBean.progressFetcher.getStatus(order.businessKey)}"/>
                    </td>
                    <td>${actionBean.progressFetcher.getNumberOfSamples(order.businessKey)}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
<c:if test="${!actionBean.editOrder.draft || !actionBean.editOrder.sampleInitiation}">

    <div class="borderHeader">
        <h4 style="display:inline">Samples</h4>

        <c:if test="${!actionBean.editOrder.draft}">
            <span class="actionButtons">
                <security:authorizeBlock roles="${actionBean.modifyOrderRoles}">
                    <stripes:button name="deleteSamples" value="Delete Samples" class="btn"
                                    style="margin-left:30px;"
                                    onclick="showConfirm('deleteSamples', 'delete')"/>

                    <c:if test="${!actionBean.editOrder.pending}">
                        <stripes:button name="abandonSamples" value="Abandon Samples" class="btn"
                                        style="margin-left:15px;"
                                        onclick="showAbandonDialog()"/>

                        <c:if test="${!actionBean.editOrder.childOrder}">
                            <stripes:button name="replaceOrderSamples" id="replaceOrderSamples" value="Replace Abandoned Samples"
                                            onclick="showSampleReplacementDialog()" class="btn padright"
                                            title="Click to add replacement samples for abandoned samples" />
                        </c:if>
                    </c:if>
                    <stripes:button name="recalculateRisk" value="Recalculate Risk" class="btn"
                                    style="margin-left:15px;" onclick="showRecalculateRiskDialog()"/>

                    <stripes:button name="setRisk" value="Set Risk" class="btn"
                                    style="margin-left:5px;" onclick="showRiskDialog()"/>

                </security:authorizeBlock>
                <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                    <c:if test="${actionBean.editOrder.product.supportsNumberOfLanes}">
                        <stripes:link beanclass="${actionBean.class.name}" event="<%= ProductOrderActionBean.SQUID_COMPONENTS_ACTION %>">
                            <stripes:param name="productOrder" value="${actionBean.editOrder.businessKey}"/>
                            Build Squid Components
                        </stripes:link>
                    </c:if>
                </security:authorizeBlock>
            </span>

            <security:authorizeBlock roles="${actionBean.modifyOrderRoles}">
                <div class="pull-right">
                    <stripes:text size="100" name="addSamplesText" style="margin-left:15px;"/>
                    <stripes:submit name="addSamples" value="Add Samples" class="btn" style="margin-right:15px;"/>
                </div>
            </security:authorizeBlock>

        </c:if>

        <security:authorizeBlock roles="<%= roles(Developer, PDM, PM) %>">
            <stripes:button name="setProceedOos" value="Set Proceed OOS" class="btn"
                    style="margin-left:5px;" onclick="showProceedOosDialog()"/>
        </security:authorizeBlock>
    </div>

    <div id="summaryId" class="fourcolumn" style="margin-bottom:10px;">
        <img src="${ctxpath}/images/spinner.gif" alt="spinner"/>
    </div>

    <c:if test="${not empty actionBean.editOrder.samples}">
        <table id="sampleData" class="table simple compact">
            <thead>
            <tr>
                <th width="20"><input id="checkAllSamples" for="count" type="checkbox" class="checkAll"
                /><span id="count" class="checkedCount"></span>
                </th>
                <th width="10">#</th>
                <th width="90"></th>
                <th width="110"></th>
                <th width="60"></th>
                <th width="110"></th>
                <th width="40"></th>
                <th width="40"></th>
                <th width="40"></th>
                <th width="40"></th>
                <th width="40"></th>
                <th width="40"></th>

                <c:if test="${actionBean.supportsRin}">
                    <th width="40"></th>
                    <th width="40"></th>
                    <th width="40"></th>
                </c:if>

                <c:if test="${actionBean.supportsPico}">
                    <th width="70"></th>
                </c:if>
                <th width="40"></th>
                <th class="sampleData rackscanMismatch" width="60"><abbr
                        title="Sample Kit Upload/Rackscan Mismatch"></abbr></th>
                <th class="sampleData"></th>
                <th style="display:none;"></th>
                <th class="sampleData"></th>
                <th width="40"></th>
                <th class="completelyBilled" width="40"></th>
                <th width="200"></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.editOrder.samples}" var="sample">
                <tr>
                    <td><stripes:checkbox title="${sample.samplePosition}" class="shiftCheckbox"
                                          name="selectedProductOrderSampleIds"
                                          value="${sample.productOrderSampleId}"/></td>
                    <td>${sample.samplePosition + 1}</td>
                    <td class="sampleName">
                            <%--@elvariable id="sampleLink" type="org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink"--%>
                        <c:set var="sampleLink" value="${actionBean.getSampleLink(sample)}"/>
                        <c:choose>
                            <c:when test="${sampleLink.hasLink}">
                                <stripes:link class="external" target="${sampleLink.target}" title="${sampleLink.label}"
                                              href="${sampleLink.url}">
                                    ${sample.name}
                                </stripes:link>
                            </c:when>
                            <c:otherwise>
                                ${sample.name}
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderCollaboratorSampleId)}">${sample.sampleData.collaboratorsSampleName}</c:if>
                    </td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderParticipantId)}">${sample.sampleData.patientId}</c:if>
                    </td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderCollaboratorParticipantId)}">${sample.sampleData.collaboratorParticipantId}
                    </c:if></td>

                    <td>
                        <c:if test="${actionBean.showColumn(columnHeaderShippedDate)}">${sample.labEventSampleDTO.samplePackagedDate}</c:if>
                    </td>
                    <td>
                        <c:if test="${actionBean.showColumn(columnHeaderReceivedDate)}">${sample.formattedReceiptDate}</c:if>
                    </td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderSampleType)}">${sample.sampleData.sampleType}</c:if></td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderMaterialType)}">${sample.latestMaterialType}</c:if></td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderVolume)}">${sample.sampleData.volume}</c:if></td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderConcentration)}">${sample.sampleData.concentration}</c:if></td>
                    <c:if test="${actionBean.supportsRin}">
                        <td><c:if
                                test="${actionBean.showColumn(columnHeaderRin)})">${sample.sampleData.rawRin}</c:if></td>
                        <td><c:if
                                test="${actionBean.showColumn(columnHeaderRqs)}">${sample.sampleData.rqs}</c:if></td>
                        <td><c:if
                                test="${actionBean.showColumn(columnHeaderDv2000)}">${sample.sampleData.dv200}</c:if></td>
                    </c:if>

                    <c:if test="${actionBean.supportsPico}">
                        <td>
                            <c:if test="${actionBean.showColumn(columnHeaderPicoRunDate)}">
                                <fmt:formatDate value="${sample.sampleData.picoRunDate}"
                                                var="formattedDate" pattern="${actionBean.datePattern}"/>
                                <div class="picoRunDate" style="width:auto"
                                     title="${sample.sampleData.picoRunDate}">${formattedDate}</div>
                            </c:if>
                        </td>
                    </c:if>
                    <td>
                        <c:if test="${actionBean.showColumn(columnHeaderYieldAmount)}">${sample.sampleData.total}</c:if>
                    </td>
                    <td style="text-align: center">
                        <c:if test="${actionBean.showColumn(columnHeaderRackscanMismatch)}">${sample.sampleData.hasSampleKitUploadRackscanMismatch}</c:if>
                    </td>
                    <td style="text-align: center">
                        <c:if test="${sample.onRisk && actionBean.showColumn(columnHeaderOnRisk)}">
                            <div class="onRisk" title="On Risk Details for ${sample.name}" rel="popover"
                                 data-trigger="hover" data-placement="left" data-html="true"
                                 data-content="<div style='text-align: left; white-space: normal; word-break: break-word;'>${sample.riskString}</div>">
                                <img src="${ctxpath}/images/check.png"> ...
                            </div>
                        </c:if>
                    </td>
                    <td style="display:none;"><c:if
                            test="${actionBean.showColumn(columnHeaderOnRiskDetails)}">${sample.riskString}</c:if></td>
                    <td><c:if
                            test="${actionBean.showColumn(columnHeaderProceedOutOfSpec)}">${sample.proceedIfOutOfSpec.displayName}</c:if></td>
                                    <td>${sample.deliveryStatus.displayName}</td>
                                    <td style="text-align: center">${sample.completelyBilled}</td>
                                    <td>${sample.sampleComment}</td>
                                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
</c:if>
</stripes:form>
</stripes:layout-component>
</stripes:layout-render>
