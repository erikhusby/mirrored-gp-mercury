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

            function rackScanComplete() {
                var barcodes = $j("#rack_scan_overlay").data("results");
                if (barcodes == null) {
                    return;
                }
                var dtTable = $j('#dispositions').DataTable();
                dtTable.clear().draw();
                var formData = new FormData();
                formData.append("buildTableData", "");
                formData.append("rackScanJson", barcodes);
                formData.append("_sourcePage", $j("#ajaxScanForm input[name='_sourcePage']").val());
                dtTable.processing(true);
                $j.ajax({
                    url: "${ctxpath}/sample/PicoDisposition.action",
                    type: 'POST',
                    data: formData,
                    async: true,
                    success: function (results) {
                        dtTable.processing(false);
                        if ((results).errors != undefined) {
                            showAlertDialog("Error", (results).errors.join(" <br/>"));
                        } else {
                            dtTable.rows.add(results).draw();
                        }
                    },
                    error: function (results) {
                        dtTable.processing(false);
                        $j("#doScanBtn").removeAttr("disabled");
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
             * Shades exclude checkbox cell
             * When initializing color, metricId arg is 0, don't try to change datatable values
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

                /*
                 * Initially hide the destination rack barcode inputs and CSV download UI elements
                 */
                $j('#targetRacks').hide();

                theDataTableApi = $j('#dispositions').DataTable({
                    "paging": false,
                    "scrollY": 580,
                    "searching": true,
                    "info": true,
                    "processing": true,
                    "data": [],
                    "rowId": function (row) {
                        return row.metricId;
                    },
                    "dom": "<'row-fluid'<'span3'i><'span2'f><'span5'><'span2'B>><'row-fluid'<'span12'rt>>",
                    buttons: ['excel', 'csv'],
                    "columns": [
                        {"data": "metricId", "visible": false},
                        {"title": "Position", "data": "position"},
                        {"title": "Barcode", "data": "barcode"},
                        {"title": "Sample ID", "data": "sampleId"},
                        {
                            "title": "Collab Patient ID", "data": "collaboratorPatientIds",
                            "render": function (data, type, row, meta) {
                                return (type === 'display' && data.length > 0) ? data.join(" ") : "";
                            }
                        },
                        {"title": "Concentration", "data": "concentration", "type": "num", "className": "dt-center"},
                        {"title": "Decision", "data": "decision"},
                        {"title": "Rework Disposition", "data": "reworkDisposition"},
                        {
                            "title": "Pick Destination", "data": "destinationRackType", "className": "dt-center",
                            "render": function (data, type, row, meta) {
                                return (type === 'display' && data !== "NONE") ? data : "";
                            }
                        },
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
                        {"targets": [1, 2, 3, 4, 6, 7], "className": "dt-head-left"},
                        {"orderData": [6, 7], "targets": [6]}
                    ]
                });

                /*
                 * Conditionally show the destination rack barcode inputs and CSV download UI elements
                 */
                $j('#dispositions').on('draw.dt', function (e) {
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

                $j("#pickerForm").submit(function (event) {
                    var tData = theDataTableApi.data();
                    if (tData.length > 0) {
                        var pickJson = [];
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
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <form method="POST" action="/Mercury/sample/PicoDisposition.action" id="pickerForm">
            <div class="row-fluid">
                <div class="span2"><input type="button" id="rackScanBtn" name="rackScanBtn" class="btn btn-primary"
                                          value="Scan and Review Rack" onclick="startRackScan(this)"/></div>
                <fieldset class="span10" id="targetRacks">
                    <legend><h4 style="margin-top:0px">Picker Racks</h4></legend>
                    <table>
                        <tr>
                            <td style="padding-left:30px">Source: <input class="input-small" type="text"
                                                                         name="srcRackBarcode" id="srcRackBarcode"
                                                                         value="SOURCE"/></td>
                            <td style="padding-left:30px">Norm Rack: <input class="input-small" type="text"
                                                                            name="destRacks['NORM']"
                                                                            id="normRackBarcode" value="NORM"/></td>
                            <td style="padding-left:30px">Undiluted Rack: <input class="input-small" type="text"
                                                                                 name="destRacks['UNDILUTED']"
                                                                                 id="undilRackBarcode"
                                                                                 value="UNDILUTED"/></td>
                            <td style="padding-left:30px">Repeat Rack: <input class="input-small" type="text"
                                                                              name="destRacks['REPEAT']"
                                                                              id="repeatRackBarcode" value="REPEAT"/>
                            </td>
                            <td style="padding-left:30px"><input type="hidden" id="listItemsJson" name="listItemsJson"/>
                                <input id="btnPickerCsv" name="buildPickFile" type="submit" class="btn"
                                       value="Download Picker CSV"/></td>
                        </tr>
                    </table>
                </fieldset>
            </div>
        </form>
        <hr/>
        <table class="display compact" id="dispositions"></table>
        <div id="dialog-message" title="Error"><p><span class="alert-error"
                                                        style="float:left; margin:0 7px 50px 0;"></span></p></div>
        <div id="rack_scan_overlay">
            <%@include file="/vessel/ajax_div_rack_scanner.jsp" %>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
