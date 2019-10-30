<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Initial Pico Sample Disposition"
                       sectionTitle="Initial Pico Sample Disposition"
                       showCreate="false" dataTablesVersion="1.10">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/DataTables-1.10.12/js/processing.js"></script>

        <script type="text/javascript">

            /**
             * Show modal overlay.  level:  Error, Info,
             */
            var showAlertDialog = function (level, content) {
                var theDialog = $j("#dialog-message");
                theDialog.attr("title", level);
                var theOutput = $j("#dialog-message span");
                theOutput.html(content);
                theOutput.attr("class", "alert-" + level.toLowerCase());
                theDialog.dialog("open");
            };

            /**
             * Displays only the destination inputs required by the data after a rack scan ajax call
             */
            var setUpDestBarcodeInputs = function () {
                var normCell = $j("#NORMCell");
                var undilCell = $j("#UNDILUTEDCell");
                var repeatCell = $j("#REPEATCell");
                normCell.hide();
                undilCell.hide();
                repeatCell.hide();
                var tData = theDataTableApi.data();
                if (tData.length > 0) {
                    $j.each(tData, function (index, value) {
                        if (value.userDestRackBarcode === "NORM") {
                            normCell.show();
                        } else if (value.userDestRackBarcode === "UNDILUTED") {
                            undilCell.show();
                        } else if (value.userDestRackBarcode === "REPEAT") {
                            repeatCell.show();
                        }
                    });
                }
            };

            function rackScanComplete() {
                var barcodes = $j("#rack_scan_overlay").data("results");
                if (barcodes == null) {
                    return;
                }
                theDataTableApi.clear().draw();
                // Hide the destination rack barcode inputs and CSV download UI elements
                $j('#targetRacks').hide();
                var formData = new FormData();
                formData.append("buildScanTableData", "");
                formData.append("rackScanJson", barcodes);
                formData.append("<csrf:tokenname/>", "<csrf:tokenvalue/>");
                formData.append("_sourcePage", $j("#ajaxScanForm input[name='_sourcePage']").val());
                theDataTableApi.processing(true);
                $j.ajax({
                    url: "${ctxpath}/sample/PicoDisposition.action",
                    type: 'POST',
                    data: formData,
                    async: true,
                    success: function (results) {
                        theDataTableApi.processing(false);
                        if ((results).errors != undefined) {
                            showAlertDialog("Error", (results).errors.join(" <br/>"));
                        } else if ((results).length == 0) {
                            showAlertDialog("info", "No initial pico quants available for vessels");
                        } else {
                            theDataTableApi.rows.add(results).draw();
                            $j('#targetRacks').show();
                            setUpDestBarcodeInputs();
                        }
                    },
                    error: function (results) {
                        theDataTableApi.processing(false);
                        $j("#doScanBtn").removeAttr("disabled");
                        setUpDestBarcodeInputs();  // hides destination inputs
                        showAlertDialog("Error", "A server error occurred");
                    },
                    cache: false,
                    datatype: "json",
                    processData: false,
                    contentType: false
                });
                $j("#rack_scan_overlay").dialog("close");
                $j("#rack_scan_inputs").html("");
            }

            var theDataTableApi;

            /**
             * Shades checkbox cell when selected for pick
             * At table initialization metricId arg is 0, don't try to change datatable values
             * At user check/uncheck modify JSON object data accordingly by metric ID
             */
            var shadeCell = function (domCb, metricId) {
                if (domCb == undefined) {
                    // Datatable init: PASS cells are empty
                    return;
                }
                var isChecked = domCb.checked;
                var jqCell = $j(domCb).parent();
                if (isChecked) {
                    jqCell.css({backgroundColor: "#f2dede"});
                } else {
                    // Reset it to adjacent
                    jqCell.css("background-color", jqCell.prev().css("background-color"));
                }
                if (metricId != 0) {  // Change data on user check/uncheck
                    theDataTableApi.row("#" + metricId).data().toBePicked = isChecked;
                }
            };

            var setSrcBarcode = function (theInput) {
                var srcBarcode = theInput.val();
                var tData = theDataTableApi.data();
                if (tData.length > 0) {
                    $j.each(tData, function (index, value) {
                        value.srcRackBarcode = srcBarcode;
                        // Ugh! Force the row to update
                        theDataTableApi.row("#" + value.metricId).data(value);
                    });
                    theDataTableApi.draw();
                }
            };

            var setDestBarcode = function (theInput) {
                var sysBarcode = theInput.attr("placeholder");
                var userBarcode = theInput.val();
                var tData = theDataTableApi.data();
                if (tData.length > 0) {
                    $j.each(tData, function (index, value) {
                        if (value.sysDestRackBarcode === sysBarcode) {
                            value.userDestRackBarcode = userBarcode;
                            // Force the row to update
                            theDataTableApi.row("#" + value.metricId).data(value);
                        }
                    });
                    theDataTableApi.draw();
                }
            };

            $j(document).ready(function () {

                $j("#dialog-message").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {
                        Ok: function () {
                            $j(this).dialog("close");
                        }
                    }
                });
                $j("wait-overlay").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {},
                    closeOnEscape: false,
                    classes: {"ui-dialog": "no-close"},
                    title: ""
                });

                theDataTableApi = $j('#dispositions').DataTable({
                    "paging": false,
                    "scrollY": 580,
                    "searching": true,
                    "info": true,
                    "processing": true,
                    "data": <enhance:out escapeXml="false">${actionBean.listItemsJson}</enhance:out>,
                    "rowId": function (row) {
                        return row.metricId;
                    },
                    "dom": "<'row-fluid'<'span3'i><'span2'f><'span5'><'span2'B>><'row-fluid'<'span12'rt>>",
                    buttons: ['excel', 'csv'],
                    "columns": [
                        {"data": "metricId", "visible": false},
                        {"title": "Rack", "data": "srcRackBarcode"},
                        {"title": "Position", "data": "position"},
                        {"title": "Barcode", "data": "barcode"},
                        {"title": "Sample ID", "data": "sampleId"},
                        {"title": "Volume", "data": "volume", "type": "num", "className": "dt-center"},
                        {"title": "Concentration", "data": "concentration", "type": "num", "className": "dt-center"},
                        {"title": "Decision", "data": "decision"},
                        {"title": "Rework Disposition", "data": "reworkDisposition"},
                        {"title": "Pick Destination", "data": "userDestRackBarcode", "className": "dt-center"},
                        // sysDestRackBarcode stores value set by server ( NORM_1..NORM_n, REWORK_1..REWORK_n, UNDILUTED_1..UNDILUTED_n )
                        {
                            "title": "Pick From Rack", "data": "toBePicked", "className": "dt-center",
                            "render": function (data, type, row, meta) {
                                if (type === 'display') {
                                    if (row.destinationRackType === "NONE") {
                                        return "";
                                    } else {
                                        return '<input style="float:none" type="checkbox" name="ck_Ignore" value="' + row.metricId + '" checked="true" onchange="shadeCell(this, ' + row.metricId + ')" \>';
                                    }
                                } else {
                                    return data.toString();
                                }
                            }
                        }
                    ],
                    "columnDefs": [
                        {"targets": [1, 2, 3, 4, 7, 8], "className": "dt-head-left"},
                        {"orderData": [7, 8], "targets": [7]}
                    ]
                });

                /*
                 * Conditionally show the destination rack barcode inputs and CSV download UI elements
                 */
                theDataTableApi.on('draw', function (e) {
                    var jqTable = $j(e.delegateTarget);
                    var tData = theDataTableApi.data();
                    if (tData.length > 0) {
                        var jqTbody = jqTable.children("tbody")[0];
                        $j.each($j(jqTbody).children("tr"), function (index, value) {
                            shadeCell($j(value).children("td").last().children("input")[0], 0);
                        });
                        $j('#targetRacks').show();
                    } else {
                        $j('#targetRacks').hide();
                    }
                });

                $j("#rackScanPickerForm").submit(function (event) {
                    var messages = [];
                    // Some inputs may not be visible if rack scan
                    $j.each($j("#targetRacks input").filter(":visible"), function (index, txtInput) {
                        txtInput = $j(txtInput);
                        if (txtInput.val().length == 0) {
                            messages.push(txtInput.attr("Placeholder") + " destination rack barcode required");
                        }
                    });
                    if (messages.length > 0) {
                        showAlertDialog("error", messages.join("<br/>"));
                        return false;
                    }
                    var tData = theDataTableApi.data();
                    if (tData.length > 0) {
                        var pickJson = [];
                        // Ship the checked rows back to server
                        $j.each(tData, function (index, value) {
                            if (value.toBePicked) {
                                pickJson.push(value);
                            }
                        });
                        $j("#listItemsJson").val(JSON.stringify(pickJson));
                        return true;
                    } else {
                        return false;
                    }
                });

                <c:if test="${actionBean.isRackScanEnabled()}">$j('#targetRacks').hide();
                </c:if> <%-- Hide destination inputs prior to a rack scan --%>
                <c:if test="${not actionBean.isRackScanEnabled()}">theDataTableApi.draw();
                </c:if> <%-- Trigger a draw to shade pick cells --%>

            });  // End doc ready logic
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="row-fluid">
            <form method="POST" action="/Mercury/sample/PicoDisposition.action" id="rackScanPickerForm">
                <c:if test="${actionBean.isRackScanEnabled()}"><%-- This button is only valid for a page supporting a rack scan, NOT for redirect from the upload page --%>
                <div class="span2"><input type="button" id="rackScanBtn" name="rackScanBtn" class="btn btn-primary"
                                          value="Scan and Review Rack" onclick="startRackScan(this)"/></div>
                </c:if>
                <fieldset class="span10" id="targetRacks">
                    <legend><h4 style="margin-top:0px">Picker Racks</h4></legend>
                    <table>
                        <tr>
                            <c:if test="${actionBean.isRackScanEnabled()}">
                                <td style="padding-left:30px">Source: <input class="input-small" type="text"
                                                                             name="srcRackBarcode"
                                                                             id="srcRackBarcode" placeholder="SOURCE"
                                                                             onchange="setSrcBarcode($j(this));"/></td>
                            </c:if>
                            <c:forEach var="destRack" items="${actionBean.destRacks}" varStatus="row">
                                <td style="padding-left:20px" id="${destRack}Cell">${destRack} Rack: <input
                                        class="input-small" type="text"
                                        placeholder="${destRack}" id="${destRack}Barcode"
                                        onchange="setDestBarcode($j(this));"/></td>
                            </c:forEach>
                            <td style="padding-left:30px"><input type="hidden" id="listItemsJson" name="listItemsJson"/>
                                <input id="btnPickerCsv" name="buildPickFile" type="submit" class="btn"
                                       value="Download Picker CSV"/></td>
                        </tr>
                    </table>
                </fieldset>
            </form>
        </div>
        <hr/>
        <table class="display compact" id="dispositions"></table>
        <div id="dialog-message" title="Error"><p><span class="alert-error"
                                                        style="float:left; margin:0 7px 50px 0;"></span></p></div>
        <div id="rack_scan_overlay">
            <%@include file="/vessel/ajax_div_rack_scanner.jsp" %>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
