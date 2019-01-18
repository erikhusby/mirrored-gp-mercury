<%--suppress ALL --%>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderSampleBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.datatables.DatatablesStateSaver" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product Order: ${actionBean.editOrder.title}"
                       dataTablesVersion="1.10" withColVis="true" withColReorder="true" withSelect="true"
                       sectionTitle="View Product Order: ${actionBean.editOrder.title}"
                       businessKeyValue="${actionBean.editOrder.businessKey}">
    <c:set var="columnHeaderSampleId" value="<%= ProductOrderSampleBean.SAMPLE_ID %>"/>
    <c:set var="columnHeaderPDOSampleId" value="<%= ProductOrderSampleBean.PRODUCT_ORDER_SAMPLE_ID %>"/>
    <c:set var="columnHeaderPosition" value="<%= ProductOrderSampleBean.POSITION %>"/>
    <c:set var="columnHeaderCollaboratorSampleId" value="<%= ProductOrderSampleBean.COLLABORATOR_SAMPLE_ID %>"/>
    <c:set var="columnHeaderCollaboratorParticipantId" value="<%= ProductOrderSampleBean.COLLABORATOR_PARTICIPANT_ID %>"/>
    <c:set var="columnHeaderParticipantId" value="<%= ProductOrderSampleBean.PARTICIPANT_ID %>"/>
    <c:set var="columnHeaderVolume" value="<%= ProductOrderSampleBean.VOLUME %>"/>
    <c:set var="columnHeaderReceivedDate" value="<%= ProductOrderSampleBean.RECEIVED_DATE %>"/>
    <c:set var="columnHeaderSampleType" value="<%= ProductOrderSampleBean.SAMPLE_TYPE %>"/>
    <c:set var="columnHeaderShippedDate" value="<%= ProductOrderSampleBean.SHIPPED_DATE %>"/>
    <c:set var="columnHeaderPicoRunDate" value="<%= ProductOrderSampleBean.PICO_RUN_DATE %>"/>
    <c:set var="columnHeaderConcentration" value="<%= ProductOrderSampleBean.CONCENTRATION %>"/>
    <c:set var="columnHeaderMaterialType" value="<%= ProductOrderSampleBean.MATERIAL_TYPE %>"/>
    <c:set var="columnHeaderRackscanMismatch" value="<%= ProductOrderSampleBean.RACKSCAN_MISMATCH_DETAILS %>"/>
    <c:set var="columnHeaderOnRisk" value="<%= ProductOrderSampleBean.ON_RISK %>"/>
    <c:set var="columnHeaderYieldAmount" value="<%= ProductOrderSampleBean.YIELD_AMOUNT %>"/>
    <c:set var="columnHeaderRin" value="<%= ProductOrderSampleBean.RIN %>"/>
    <c:set var="columnHeaderRqs" value="<%= ProductOrderSampleBean.RQS %>"/>
    <c:set var="columnHeaderDv2000" value="<%= ProductOrderSampleBean.DV2000 %>"/>
    <c:set var="columnHeaderOnRiskDetails" value="<%= ProductOrderSampleBean.ON_RISK_DETAILS %>"/>
    <c:set var="columnHeaderOnRiskString" value="<%= ProductOrderSampleBean.RISK_STRING %>"/>
    <c:set var="columnHeaderProceedOutOfSpec" value="<%= ProductOrderSampleBean.PROCEED_OOS %>"/>
    <c:set var="columnHeaderStatus" value="<%= ProductOrderSampleBean.STATUS %>"/>
    <c:set var="columnHeaderCompletelyBilled" value="<%= ProductOrderSampleBean.BILLED_DETAILS %>"/>
    <c:set var="columnHeaderComment" value="<%= ProductOrderSampleBean.COMMENT %>"/>


<stripes:layout-component name="extraHead">
    <style type="text/css">
        th.no-min-width {
            min-width: initial !important;
        }
        .ui-widget-header{
            border: 1px solid #c4eec0;
            background: #c4eec0;
            height: 15px;
        }
        /* css used to indicate slow columns */
        .em-turtle {
            background-image: url("${ctxpath}/images/turtle.png") !important;
            background-size: 16px 16px;
            background-repeat: no-repeat;
            background-position: right 5px center;
        }
        .image-small {
            width: 16px;
            height: 16px;
        }
        .slow-colunms-hidden {
            box-shadow: inset 0 0 10px orange
        }

        .sampleDataProgressText{
            position: absolute;
            margin-left: 1em;
            font-size: 12px;
            font-style: oblique;
        }
        .ui-progressbar { height:15px}
        #sampleData_info {font-weight: bold}
    </style>
<script type="text/javascript">
var kitDefinitionIndex = 0;

$j(document).ready(function () {
    $j('body').popover({selector: '[rel=popover]'});
    $j(document).on('click', '#ledgerLink', function () {
        window.stop();
    });
    enableDefaultPagingOptions();
    function loadBspData(settings, refresh=false) {
        var api = new $j.fn.dataTable.Api(settings);
        var table = api.table();
        var remainingSamples = [];
        table.rows().data().each(function (row) {
            var data = $j(row);
            data.each(function () {
                var pdoId = this.PRODUCT_ORDER_SAMPLE_ID;
                if ((!this.includeSampleData) && remainingSamples.indexOf(pdoId) < 0 || refresh) {
                    remainingSamples.push(pdoId);
                }
            });
        });
        sampleInfoBatchUpdate(remainingSamples, table);
        updateSampleSummary();
    }

    function updateSampleSummary(){
        $j.ajax({
            url: "${ctxpath}/orders/order.action?<%= ProductOrderActionBean.GET_SAMPLE_SUMMARY %>",
            data: {
                'productOrder': "${actionBean.editOrder.businessKey}",
            },
            method: 'POST',
            dataType: 'json',
            error: function (obj, error, ex) {
                console.log(error, obj.responseText, JSON.stringify(ex));
            },
            success: function (json) {
                if (json['summary']) {
                    writeSummaryData(json);
                }
                if (json['<%=ProductOrderSampleBean.SAMPLES_NOT_RECEIVED%>']) {
                    $j("#numberSamplesNotReceived").html(json['<%=ProductOrderSampleBean.SAMPLES_NOT_RECEIVED%>']);
                }
            }
        })

    }

    function sampleInfoBatchUpdate(samplesToFetch, settings) {
        var pdoSampleCount = samplesToFetch.length;
        if (pdoSampleCount === 0) {
            return;
        }
        var table = new $j.fn.dataTable.Api(settings).table();

        // When there are greater than 1000 samples split the call to updateSampleInformation
        fetchSize = pdoSampleCount > 2000 ? 2000: pdoSampleCount;
        while (samplesToFetch.length > 0) {
            (function (samples) {
                updateSampleInformation(samples, table, false);
            }(samplesToFetch.splice(0, fetchSize)));
        }
    }

    setupDialogs();
    renderCustomizationSummary();
    updateFundsRemaining();

    function renderCustomizationSummary() {
        var customJSONString = $j("#customizationJsonString").val();
        if(customJSONString !== null && customJSONString!==undefined && customJSONString !== "") {
            var customSettings = JSON.parse(customJSONString);
        }
        $j("#customizationContent").html(function() {
            var content = "";

            for  (part in customSettings) {
                content += "<b>"+part +"</b>";
                var price = customSettings[part]["price"];
                var quantity = customSettings[part]["quantity"];
                var customName = customSettings[part]["customName"];

                var firstSetting = true;

                if ((price !== undefined) && (price !== 'null') && (price.length > 0)) {

                    if(firstSetting) {
                        content += ": ";
                        firstSetting = false;
                    }

                    content += "Custom Price -- " + price;
                }
                if ((quantity !== undefined) && (quantity !== 'null') && (quantity.length > 0)) {
                    if(firstSetting) {
                        content += ": ";
                        firstSetting = false;
                    } else {

                        content += ", ";
                    }

                    content += "Custom Quantity -- " + quantity;
                }
                if ((customName !== undefined) && (customName !== 'null') && (customName.length > 0)) {
                    if(firstSetting) {
                        content += ": ";
                        firstSetting = false;
                    } else {

                        content += ", ";
                    }

                    content += "Custom Product Name -- " + customName;
                }
                content += "<BR>";
            }
            return content;
        });

    }

    var oTable = $j('#sampleData').dataTable({
        'dom': "<'row-fluid'<'span12'f>><'row-fluid'<'span5'l><'span2 sampleDataProgress'><'span5 pull-right'<'pull-right'B>>>rt<'row-fluid'<'span6'l><'span6 pull-right'p>>",
        'paging': true,
        "deferRender": true,
        'colReorder': {
            fixedColumnsLeft: 2
        },
            "stateSave": true,
            "pageLength": 100,
            'orderable': true,
            'rowId': "<%= ProductOrderSampleBean.UNIQUE_ROW_IDENTIFIER %>",
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
            order: [[ 1, 'asc' ]],
            ajax: {
                url: "${ctxpath}/orders/order.action?<%= ProductOrderActionBean.GET_SAMPLE_DATA %>",
                method: 'POST',
                dataType: 'json',
                data: function (data, settings) {
                    data.productOrder = "${actionBean.editOrder.businessKey}";
                    data.initialLoad = true;
                },
                error: function (obj, error, ex) {
                    console.log(error, obj.responseText, JSON.stringify(ex));
                },
                complete: function (json) {
                    var data = json.responseJSON;
                    var rowsWithSampleData = data['<%=ProductOrderSampleBean.SAMPLE_DATA_ROW_COUNT%>'];
                    var recordsTotal = data['<%=ProductOrderSampleBean.RECORDS_TOTAL%>'];
                    initSampleDataProgress(rowsWithSampleData, recordsTotal);
                },
            },
            "columns": [
                {"data": "${columnHeaderPDOSampleId}","orderable": false, 'class': 'no-min-width', render:renderCheckbox},
                {"data": "${columnHeaderPosition}", "title": "${columnHeaderPosition}", 'class': 'no-min-width'},
                {"data": "${columnHeaderSampleId}", "title": "${columnHeaderSampleId}", "class": "${fn:replace(columnHeaderSampleId,' ','').trim()}", "sType": "html", render: renderSampleLink},
                <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">
                {"data": "${columnHeaderCollaboratorSampleId}", "title": "${columnHeaderCollaboratorSampleId}", "class": "${fn:replace(columnHeaderCollaboratorSampleId,' ','').trim()}"},
                {"data": "${columnHeaderParticipantId}", "title": "${columnHeaderParticipantId}"},
                {"data": "${columnHeaderCollaboratorParticipantId}", "title": "${columnHeaderCollaboratorParticipantId}"},
                </security:authorizeBlock>

                {"data": "${columnHeaderShippedDate}", "title": "${columnHeaderShippedDate}"},
                {"data": "${columnHeaderReceivedDate}", "title": "${columnHeaderReceivedDate}", "class": "${fn:replace(columnHeaderReceivedDate,' ','').trim()}"},
                {"data": "${columnHeaderSampleType}", "title": "${columnHeaderSampleType}"},
                {"data": "${columnHeaderMaterialType}", "title": "${columnHeaderMaterialType}"},
                {"data": "${columnHeaderVolume}", "title": "${columnHeaderVolume}"},
                {"data": "${columnHeaderConcentration}", "title": "${columnHeaderConcentration}"},
                <c:if test="${actionBean.supportsRin}">
                {"data": "${columnHeaderRin}", "title": "${columnHeaderRin}"},
                {"data": "${columnHeaderRqs}", "title": "${columnHeaderRqs}"},
                {"data": "${columnHeaderDv2000}", "title": "${columnHeaderDv2000}"},
                </c:if>

                <c:if test="${actionBean.supportsPico}">
                {
                    "data": "${columnHeaderPicoRunDate}", "title": "${columnHeaderPicoRunDate}",
                    render: renderPico
                },
                </c:if>
                {"data": "${columnHeaderYieldAmount}", "title": "${columnHeaderYieldAmount}"},
                {"data": "${columnHeaderRackscanMismatch}", "title": "${columnHeaderRackscanMismatch}"},
                {"data": "${columnHeaderOnRiskString}", "title": "${columnHeaderOnRisk}"},
                {"data": "${columnHeaderProceedOutOfSpec}", "title": "${columnHeaderProceedOutOfSpec}"},
                {"data": "${columnHeaderStatus}", "title": "${columnHeaderStatus}"},
                { "data": "${columnHeaderCompletelyBilled}", "title": "${columnHeaderCompletelyBilled}"}
            ],
            "stateSaveCallback": function (settings, data) {
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
                <enhance:out escapeXml="false">
                var storedJson = '${actionBean.preferenceSaver.tableStateJson}';
                </enhance:out>
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
            },
            "initComplete": function (settings) {
                var api = $j.fn.dataTable.Api(settings);
                if (api.table().page.info().recordsTotal == 0) {
                    $j("#summaryId").hide();
                }
                loadBspData(settings);
                initColumnVisibility(settings);

//                postLoadSampleInfo();
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
            },
        "drawCallback": function(){
            $j(".shiftCheckbox").enableCheckboxRangeSelection();
        }
});

    function renderRackscanMismatch(data, type, row, meta) {
        if (type === 'display') {
            if (data === true) {
                return imageForBoolean(data, "${ctxpath}/images/error.png")
            } else {
                return "";
            }
        }
        return data;
    }

    function renderBilled(data, type, row, meta) {
        if (type === 'display') {
            if (data && data === true) {
                return imageForBoolean(data, "${ctxpath}/images/check.png");
            } else {
                return "";
            }
        }
        return data;
    }

    function imageForBoolean(data, imageSrc) {
        var result = data;
        if (data) {
            var image = document.createElement("img");
            image.src = imageSrc;
            image.title = true;
            result = image.outerHTML;
        }
        return result;
    }

    var localStorageKey = 'DT_productOrderView';

    function initColumnVisibility(settings) {
        var api=$j.fn.dataTable.Api(settings);
        var columnVisibilityChangedKey = "columnVisibilityChanged";
        var $sampleDataTable = $j('#sampleData');
        var $colVis = $j(".buttons-colvis");

        $sampleDataTable.on('column-visibility.dt', function (e, settings, column, state) {
            $j("body").data(columnVisibilityChangedKey, state);
        });


        $colVis.popover({
            trigger: "hover", placement: 'top', html: true, delay: { "show": 500, "hide": 100 },
            content: "Click to change column visibility. Columns marked with a <img src='${ctxpath}/images/turtle.png' class='image-small'/> will negatively impact page loading time.</div>"
        });

        function updateShowHideButton() {
            if (api.column(":hidden").length>0) {
                $colVis.addClass("slow-colunms-hidden");
                $colVis.attr('data-original-title','Some data columns are hidden.');
            } else {
                $colVis.removeClass("slow-colunms-hidden");
                $colVis.attr('data-original-title','Hide unneeded columns.');
            }
        }

        $j(".slow-colunms-hidden").on('hover', function(){
           this.setAttribute('tooltip', 'Some data columns are hidden')
        });
        $sampleDataTable.on('init.dt', updateShowHideButton);

        <enhance:out escapeXml="false">
        var slowColumns = ${actionBean.slowColumns}
        </enhance:out>

            // When the "Show or Hide" button is clicked
            $j(document.body).on("click", "a.buttons-colvis", function (event) {
                // When colvis modal is loading
                $j.when($j(event.target).load()).then(function (event2) {
                    var slowButtons = $j("a.buttons-columnVisibility").filter(function () {
                        var $button = $j(this);
                        // test if the column headers is in the slowColumns array.
                        // the check for undefined is to prevent this from being called very time
                        return $button.data('tooltip') === undefined && slowColumns.indexOf($button.text().trim()) >= 0;
                    });
                    slowButtons.addClass("em-turtle");
                    slowButtons.attr('title', 'Enabling this column may slow page loading');
                    slowButtons.tooltip();
                });
//                updateHowHideButton();
            });

        // If a column that was previously hidden but becomes visible the page
        // must be reloaded since there is no data in that column.
        $j(document.body).on("click", ".dt-button-background", function () {
            api.state.save();
            updateShowHideButton();

            var sessionVisibility = !undefined && $j("body").data(columnVisibilityChangedKey) || false;
            if (sessionVisibility) {
                loadBspData(settings, true);

                // After data reload, reset the columnVisibilityChanged flag to false.
                $j("body").data(columnVisibilityChangedKey, false);
            }
        });
    }

    function renderSampleLink(data,type, row) {
        var result = data;
        if (type === 'display') {
            result = row["<%= ProductOrderSampleBean.SAMPLE_LINK %>"]
        }
        return result;
    }

    function renderCheckbox(data, type, row) {
        var result = data;
        if (type === 'display') {
            var input = document.createElement("input");
            input.name = "selectedProductOrderSampleIds"
            input.className = "shiftCheckbox";
            input.type = "checkbox";
            input.value = data;
            input.setAttribute("data-sample-checkbox", row["<%= ProductOrderSampleBean.SAMPLE_ID %>"]);

            result = input.outerHTML;
        }
        return result;

    }

    includeAdvancedFilter(oTable, "#sampleData");

    $j('#orderList').dataTable({
        "paging": false,
    });
    bspDataCount = $j(".sampleName").length;
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

function writeSummaryData(json) {
    var dataList = '<ul>';
    json.summary.map(function (item) {
        dataList += '<li>' + item + '</li>'
    });
    dataList += '</ul>';
    $j('#summaryId').html(dataList);
}

function getSampleDataPctComplete($progressBar){
    var currentValue  = $progressBar.progressbar('value');
    var maxValue  = $progressBar.progressbar('option', 'max');
    if (maxValue===0){
        return 0;
    }
    return Math.floor(currentValue * 100 / maxValue);

}
function updateSampleDataProgress(incrementalValue, maxValue) {
    var $progressDiv = $j(".sampleDataProgress .sampleDataProgressBar");
    if ($progressDiv.length === 0){
        return;
    }
    var $progressBar = $progressDiv.progressbar('widget');
    var fetched = incrementalValue;
    var currentValue = $progressBar.progressbar('value');
    if (currentValue !== undefined) {
        fetched += currentValue;
        if (fetched > maxValue) {
            fetched = maxValue;
        }
    }
    $progressBar.progressbar('option', 'max', maxValue);
    $progressBar.progressbar('value', fetched);
}

function initSampleDataProgress(value, maxValue) {
    var $progressDiv = $j(".sampleDataProgress .sampleDataProgressBar");
    var $progressBar;
    var $sampleDataText;
    if ($progressDiv.length === 0) {
        $progressDiv = $j("<div></div>", {class: 'sampleDataProgressBar'}).appendTo($j(".sampleDataProgress"));
        $sampleDataText = $j("<span></span>", {class: 'sampleDataProgressText'}).appendTo($progressDiv);
    }
    $progressBar = $progressDiv.progressbar({
        value: 0,
        max: maxValue,
        change: function () {
            pctComplete = getSampleDataPctComplete($progressBar);
            $sampleDataText.text("Loading Sample Data: " + pctComplete + "%");
        },
        complete: function () {
            setTimeout(function () {
                $j(".sampleDataProgress").fadeOut({'duration': 800});
            }, 2000);
        }
    });
    $progressBar.progressbar('value', value);
}

// Keep track of the ajax connections because after all calls complete we need to redraw the table.
var ajaxConnections = 0;
function updateSampleInformation(samples, table, includeSampleSummary) {
        recordsTotal = table.page.info().recordsTotal;
        ajaxConnections++;
        $j.ajax({
            url: "${ctxpath}/orders/order.action?<%= ProductOrderActionBean.GET_SAMPLE_DATA %>",
            data: {
                'productOrder': "${actionBean.editOrder.businessKey}",
                'sampleIdsForGetBspData': samples,
                'includeSampleSummary': includeSampleSummary,
            },
            method: 'POST',
            dataType: 'json',
            error: function(obj, error, ex) {
                console.log(error, obj.responseText, JSON.stringify(ex));
            },
            success: function (json) {
                if (json) {
                    for (var item of json.data) {
                        var row = table.row("#"+item.rowId);
                        row.data(item);
                        row.invalidate;
                    }

                    updateSampleDataProgress(json.rowsWithSampleData, recordsTotal);
                }
            },
            complete: function () {
                ajaxConnections--;
                if (ajaxConnections === 0) {
                    table.rows().draw();
                }
            }
        })
}

function renderPico(data, type, row, meta) {
    var result = data;
    if (type === 'display') {
        var outerDiv = document.createElement("div");
        var picoSpan=document.createElement("span");
        if (result === "" && row.includeSampleData) {
            picoSpan.innerHTML = "No Pico";
        } else {
            var oneYearAgo = meta.settings.oneYearAgo;
            var almostOneYearAgo = meta.settings.almostOneYearAgo;
            if (oneYearAgo === undefined) {
                oneYearAgo = new Date();
                oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
                meta.settings.oneYearAgo = oneYearAgo;
            }
            if (almostOneYearAgo === undefined) {
                almostOneYearAgo = new Date(oneYearAgo);
                almostOneYearAgo.setMonth(oneYearAgo.getMonth() + 1);
                meta.settings.almostOneYearAgo = almostOneYearAgo;
            }

            var containerClass = "";
            picoDate = new Date(data);
            if (!picoDate || (picoDate.getTime() < oneYearAgo.getTime())) {
                containerClass = "label label-important";
            } else if (picoDate.getTime() < almostOneYearAgo.getTime()) {
                containerClass = "label label-warning";
            }
            picoSpan.innerHTML=data;
            picoSpan.className=containerClass;
        }
        outerDiv.appendChild(picoSpan);
        result = outerDiv.outerHTML;
    }
    return result;
}

function updateFundsRemaining() {
    var quoteIdentifier = '${actionBean.editOrder.quoteId}';
    var productOrderKey = $j("input[name='productOrder']").val();
    if ($j.trim(quoteIdentifier)) {
        $j.ajax({
            url: "${ctxpath}/orders/order.action?getQuoteFunding=&quoteIdentifier="+quoteIdentifier+"&productOrder=" + productOrderKey + "&quoteSource=" + ${actionBean.editOrder.quoteSource},
            dataType: 'json',
            success: updateFunds
        });
    } else {
        $j("#fundsRemaining").text('');
    }
}

function updateFunds(data) {

    var quoteWarning = false;

    if (data.fundsRemaining && !data.error) {
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
                    fundsRemainingNotification += ' in ' + fundingDetails[detailIndex].daysTillExpire +
                        ' days. If it is likely this work will not be completed by then, please work on updating the ' +
                        'Funding Source so Billing Errors can be avoided.';
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

<div id="unAbandonDialog" style="width:600px;display:none;">
    <p>Un-Abandon Samples (<span id="unAbandonSelectedSamplesCountId"> </span> selected)</p>

    <p style="clear:both">
        <label for="unAbandonSampleCommentId">Comment:</label>
    </p>

    <textarea id="unAbandonSampleCommentId" name="comment" class="controlledText" cols="80" rows="4"> </textarea>
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

    <div style="display:none" id="noReplacementsAvailableDialog">
        <p>There are no samples for you to replace.  Please open a new PDO to do any new work.</p>
    </div>

<stripes:form action="/orders/order.action" id="orderForm" class="form-horizontal">

<div id="placeConfirmation" style="display:none;" title="Place Order">
    <p>Click OK to place the order and make it available for lab work.</p>
    <span id="numberSamplesNotReceived"></span>
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
<stripes:hidden id="unAbandonComment" name="unAbandonComment" value=""/>
<stripes:hidden id="attestationConfirmed" name="editOrder.attestationConfirmed" value=""/>
<stripes:hidden name="customizationJsonString" id="customizationJsonString" />

<div class="actionButtons">
    <c:choose>
        <c:when test="${actionBean.editOrder.draft}">
            <%-- PDOs can be placed by PM or PDMs, so the security tag accepts either of those roles for 'Place Order'. --%>
            <%--'Validate' is also under that same security tag since that has the power to alter 'On-Riskedness' --%>
            <%-- for PDO samples. --%>
            <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">
                <stripes:submit name="placeOrder" value="Validate and Place Order"
                                disabled="${!actionBean.canPlaceOrder}" class="btn"/>
                <stripes:submit name="validate" value="Validate" style="margin-left: 3px;" class="btn"/>
            </security:authorizeBlock>

            <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">
                <stripes:link title="Click to edit ${actionBean.editOrder.title}"
                              beanclass="${actionBean.class.name}" event="edit" class="btn"
                              style="text-decoration: none !important; margin-left: 10px;">
                    <%=ProductOrderActionBean.EDIT_ORDER%>
                    <stripes:param name="productOrder" value="${actionBean.editOrder.businessKey}"/>
                </stripes:link>
            </security:authorizeBlock>

            <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">
                <stripes:button onclick="showDeleteConfirm('deleteOrder')" name="deleteOrder"
                                value="Delete Draft" style="margin-left: 10px;" class="btn"/>
            </security:authorizeBlock>
        </c:when>
        <c:otherwise>
            <c:if test="${actionBean.canPlaceOrder}">
                <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">
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
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">

                    &nbsp;&nbsp;&nbsp;&nbsp;
                    <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.BillingLedgerActionBean"><stripes:param name="orderId" value="${actionBean.editOrder.jiraTicketKey}"/>Online Billing Ledger</stripes:link>
                </security:authorizeBlock>

            </c:if>

            <security:authorizeBlock roles="<%= roles(PDM, GPProjectManager, PM, Developer) %>">

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
                    <c:if test="${actionBean.editOrder.orderType != null}">
                        ${actionBean.editOrder.orderTypeDisplay} --
                    </c:if>
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

    <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager) %>">
        <c:if test="${!actionBean.editOrder.priorToSAP1_5}">
            <div class="view-control-group control-group">
                <label class="control-label label-form">Order Customizations</label>

                <div class="controls">
                    <div class="form-value" id="customizationContent"></div>
                </div>
            </div>
        </c:if>
    </security:authorizeBlock>

    <div class="view-control-group control-group">
        <label class="control-label label-form">Quote ID</label>

        <div class="controls">
            <div class="form-value">
                <c:if test="${actionBean.editOrder.quoteIdSet}">
                    <c:if test="${ not actionBean.editOrder.hasSapQuote()}">
                        <a href="${actionBean.quoteUrl}" class="external" target="QUOTE">
                    </c:if>
                    <c:if test="${ actionBean.editOrder.hasSapQuote()}">
                        <b>SAP Quote: </b>
                    </c:if>
                    ${actionBean.editOrder.quoteId}
                    <c:if test="${ not actionBean.editOrder.hasSapQuote()}">
                        </a>
                    </c:if>
                </c:if>
                <div id="fundsRemaining"></div>
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
    <label class="control-label label-form">Analyze UMIs</label>
    <div class="controls">
        <div class="form-value">
                ${actionBean.editOrder.analyzeUmiOverride ? "Yes" : "No"}
        </div>
    </div>
</div>
<c:if test="${not empty actionBean.editOrder.product and not actionBean.editOrder.product.baitLocked and not empty actionBean.editOrder.reagentDesignKey}">
    <div class="view-control-group control-group">
        <label class="control-label label-form">Bait Design</label>
        <div class="controls">
            <div class="form-value">
                    ${actionBean.editOrder.reagentDesignKey}
            </div>
        </div>
    </div>
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

                        <stripes:button name="unAbandonSamples" value="Un-Abandon Samples" class="btn"
                                        style="margin-left:15px;"
                                        onclick="showUnAbandonDialog()"/>
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

        <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">
            <stripes:button name="setProceedOos" value="Set Proceed OOS" class="btn"
                    style="margin-left:5px;" onclick="showProceedOosDialog()"/>
        </security:authorizeBlock>

    </div>

    <c:if test="${not empty actionBean.editOrder.samples}">
        <div id="summaryId" class="fourcolumn" style="margin-bottom:10px;">
            <img src="${ctxpath}/images/spinner.gif" alt=""/> Sample Summary
        </div>
        <table id="sampleData" class="table display simple compact">
            <thead>
            <tr>
                <th width="20">
                    <input id="checkAllSamples" for="count" type="checkbox" class="checkAll"/><span id="count"
                                                                                                    class="checkedCount"></span>
                </th>
                <th width="10">#</th>
                <th width="90"></th>
                <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">

                <th width="110"></th>
                <th width="60"></th>
                <th width="110"></th>
                </security:authorizeBlock>

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

                <th class="sampleData"></th>
                <th width="40"></th>
                <th class="completelyBilled" width="40"></th>
                <th width="200"></th>
            </tr>
            </thead>
        </table>
        <div class="onRisk" rel="popover" data-trigger="hover" data-placement="left" data-html="true" style="display: none">
            <img src="${ctxpath}/images/check.png">...
        </div>

    </c:if>
</c:if>
</stripes:form>
</stripes:layout-component>
</stripes:layout-render>
