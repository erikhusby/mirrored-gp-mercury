<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.PickWorkspaceActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Pick Verify" sectionTitle="SRS Pick Verify" showCreate="false"
                       dataTablesVersion="1.10">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/jquery-ui-1.9.2.custom.min.js"></script>
        <script type="text/javascript">
                <enhance:out escapeXml="false">let pickerDataJson = ${actionBean.pickerData};
            </enhance:out>

            /**
             * level:  Error, Info,
             */
            let showAlertDialog = function (level, content) {
                let theDialog = $j("#dialog-message");
                theDialog.attr("title", level);
                let theOutput = $j("#dialog-message span");
                theOutput.html(content);
                theOutput.attr("class", "alert-" + level.toLowerCase());
                theDialog.dialog("open");
            };

            /**
             * Display a confirmation message and provide a function to execute if user clicks OK
             */
            let showConfirmDialog = function (content, acceptAction) {
                let theDialog = $j("#dialog-confirm");
                theDialog.data("acceptAction", acceptAction);
                $j("span", theDialog).html(content);
                theDialog.dialog("open");
            };

            /**
             *  Builds HTML for each rack scan input
             */
            let buildNextScanInput = function () {
                let scanElements = $j("#scanList li");
                let idx = scanElements.length;
                // Avoid continually appending empty fields
                if (idx == 0 || $j(scanElements[idx - 1]).data("scanData") != undefined) {
                    let inputHTML =
                        "<li id='scanItem_" + idx + "' style='padding-bottom:8px'><input style='width:100px' placeholder='RACK' type='text'> <input type='button' id='rackScanBtn_" + idx + "' class='btn btn-primary' value='Scan' onclick='registerAndStartRackScan(this)' /><span style='margin-left:20px;font-weight: bold'></span></li>";
                    $j("#scanList").append(inputHTML);
                }
            };

            /**
             * Process to update status counters and build data structures containing expected and scanned tubes
             */
            let updateScanStats = function () {
                let table = $j("#tblPickList").DataTable();

                // Build associative array keyed by all scanned barcodes, properties rack and position
                let allScanDataByBarcode = new Object();
                let scanListElement = $j("#scanList");
                let scanTargetElements = scanListElement.children("li");
                for (let i = 0; i < scanTargetElements.length; i++) {
                    let scanTargetElement = $j(scanTargetElements[i]);
                    let rackBarcode = $j(scanTargetElement.children("input[type='text']")).val();
                    let barcodes = scanTargetElement.data("scanData");
                    if (barcodes != undefined) {
                        for (let j = 0; j < barcodes.scans.length; j++) {
                            let tubeAndPosition = barcodes.scans[j];
                            allScanDataByBarcode[tubeAndPosition.barcode] = {
                                rack: rackBarcode,
                                position: tubeAndPosition.position
                            };
                        }
                    }
                }

                // Build associative array keyed by all expected barcodes, properties batch, rack, position, and isPicked
                let allPickDataByBarcode = new Object();
                for (let i = 0; i < pickerDataJson.length; i++) {
                    let row = pickerDataJson[i];
                    let pickCount = 0;
                    let rowId = row.sourceVessel + "_" + row.batchId;
                    for (let j = 0; j < row.pickerVessels.length; j++) {
                        let pickerVessel = row.pickerVessels[j];
                        let isPicked = (allScanDataByBarcode[pickerVessel.sourceVessel] != undefined);
                        if (isPicked) {
                            pickCount++;
                        }
                        allPickDataByBarcode[pickerVessel.sourceVessel] = {
                            batch: row.batchName,
                            rack: row.sourceVessel,
                            position: pickerVessel.sourcePosition,
                            isPicked: isPicked
                        };
                    }
                    // TODO JMS Add some formatting for complete/incomplete display in table
                    $j("#" + rowId + " :last-child").text(pickCount);
                }
                // Attach to consolidation report element
                $j("#misMatchList").data("allScanDataByBarcode", allScanDataByBarcode);
                $j("#misMatchList").data("allPickDataByBarcode", allPickDataByBarcode);

            };

            /**
             * List out all discrepancies between batches and scans
             */
            let buildConsolidationReport = function () {
                let rptParentElement = $j("#misMatchList");
                rptParentElement.children().remove();
                let allScanDataByBarcode = rptParentElement.data("allScanDataByBarcode");
                let allPickDataByBarcode = rptParentElement.data("allPickDataByBarcode");
                if (allPickDataByBarcode == undefined || allScanDataByBarcode == undefined) {
                    showAlertDialog("Error", "No scan data available.");
                    return;
                }
                rptParentElement.append("<div style='font-weight: bold'>Not picked:</div>");
                let pick = "";
                let count = 0;
                for (pick in allPickDataByBarcode) {
                    let pickObj = allPickDataByBarcode[pick];
                    if (!pickObj.isPicked) {
                        count++;
                        rptParentElement.append("<li>Batch: " + pickObj.batch + ", Rack: " + pickObj.rack + ", Barcode: " + pick + ", Position: " + pickObj.position + "</li>");
                    }
                }
                if (count == 0) {
                    rptParentElement.append("<li>(All Tubes Picked)</li>");
                }
                rptParentElement.append("<div style='font-weight: bold'>Picked but not in SRS Batch(s):</div>");
                let scan = "";
                count = 0;
                for (scan in allScanDataByBarcode) {
                    let scanObj = allScanDataByBarcode[scan];
                    if (allPickDataByBarcode[scan] == undefined) {
                        count++;
                        rptParentElement.append("<li>Rack: " + scanObj.rack + ", Barcode: " + scan + ", Position: " + scanObj.position + "</li>");
                    }
                }
                if (count == 0) {
                    rptParentElement.append("<li>(No Extra Tubes)</li>");
                }
            };

            /**
             * Posts data to register layouts of source and destination racks after batch complete
             * */
            let registerTransfers = function () {
                let scans = [];
                let isValid = true;
                $j("#scanList li").each(function (idx, item) {
                    let jqItem = $j(item);
                    let scan = jqItem.data("scanData");
                    let rackBarcode = jqItem.children("input[type='text']").val();
                    if (scan != undefined && rackBarcode === "") {
                        showAlertDialog("Error", "A rack barcode is missing for a scan.")
                        isValid = false;
                        return false;
                    } else if (scan == undefined && rackBarcode !== "") {
                        showAlertDialog("Error", "A scan is missing for a rack barcode.")
                        isValid = false;
                        return false;
                    } else if (scan == undefined && idx == 0) {
                        showAlertDialog("Error", "No scans available.")
                        isValid = false;
                        return false;
                    } else if (scan != undefined) { // Avoid empty last entry
                        // Add rackBarcode property to scan and append to list
                        scan["rackBarcode"] = rackBarcode;
                        scans[scans.length] = scan;
                    }
                });
                if (!isValid) {
                    return;
                }
                $j("#pickerDataInput").val(JSON.stringify(pickerDataJson));
                $j("#scanDataInput").val(JSON.stringify(scans));
                $j("#registerForm").submit();
            };

            /**
             * Register the scan target with the list element and do the scan
             */
            let registerAndStartRackScan = function (btnClicked) {
                let itemRef = $j(btnClicked).parent();
                if (itemRef.children().first().val() == "") {
                    showAlertDialog("Error", "Scan target rack barcode required");
                    return;
                }
                $j("#scanList").data("scanTargetElement", itemRef);
                startRackScan(btnClicked);
            };

            /**
             * Callback from rack scan
             */
            let rackScanComplete = function () {
                let barcodes = $j("#rack_scan_overlay").data("results");
                let scanTargetElement = $j("#scanList").data("scanTargetElement");
                if (barcodes != null) {
                    // Put scan data on same li element as clicked button
                    let scanObj = JSON.parse(barcodes);
                    scanTargetElement.data("scanData", scanObj);
                    scanTargetElement.children().last().text("Scanned " + scanObj.scans.length + " vessels.");
                    scanTargetElement.children("input[type='button']").replaceWith("<img src='/Mercury/images/error.png' alt='Remove' onclick='removeScan(this)'>");
                    updateScanStats();
                    buildNextScanInput();
                } else {
                    showAlertDialog("Error", "No barcodes scanned");
                }
                $j("#rack_scan_overlay").dialog("close");
                $j("#rack_scan_inputs").html("");
            };

            let removeScan = function (clickedElement) {
                $j(clickedElement).parent().remove();
                updateScanStats();
            };

            /**
             * Sets up DOM plugins (datatable) and event listeners
             */
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
                $j("#dialog-confirm").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {
                        Ok: function () {
                            $j(this).dialog("close");
                            $j(this).data("acceptAction")();
                        },
                        Cancel: function () {
                            $j(this).dialog("close");
                        }
                    }
                });
                if (pickerDataJson.length > 0) {
                    $j("#tblPickList").dataTable({
                        data: pickerDataJson,
                        rowId: function (row) {
                            return row.sourceVessel + "_" + row.batchId;
                        },
                        paging: false,
                        scrollY: 300,
                        searching: false,
                        info: true,
                        columns: [
                            {data: "batchName", title: "SRS Batch"},
                            {data: "sourceVessel", title: "Rack Barcode"},
                            {data: "totalVesselCount", title: "Total Samples"},
                            {data: "srsVesselCount", title: "Samples to Pull"},
                            {"data": null, defaultContent: "0", name: "pickedVessels", title: "Samples Pulled"}  // Not data bound - don't add to data
                        ],
                        columnDefs: [
                            {targets: [0, 1], className: "dt-head-left"},
                            {targets: [2, 3, 4], className: "dt-right"}
                        ]
                    });
                    buildNextScanInput();
                } else {
                    // Should always hit this page with batches
                }
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
            <div class="row-fluid">
                <div class="span12">
                    <table id="tblPickList" class="display compact"></table>
                    <p>${actionBean.rackCount} Racks, ${actionBean.pickSampleCount} Tubes</p></div>
            </div>
            <div class="row-fluid">
                <div class="span12">
                    <hr/>
                </div>
            </div>
            <div class="row-fluid">
                <div class="span2" style="text-align: right"><h5>Scans: </h5></div>
                <div class="span6">
                    <ol id="scanList" style="margin-top: 10px"></ol>
                </div>
                <div class="span4">
                    <input type="button" id="rptButton" class="btn btn-primary" value="View Consolidation Report"
                           onclick="buildConsolidationReport()"> <input type="button" id="rptRegister"
                                                                        class="btn btn-primary"
                                                                        value="Register Transfers"
                                                                        onclick="registerTransfers()">
                    <hr/>
                    <ul id="misMatchList"></ul>
                </div>
            </div>
            <form method="post" id="registerForm" action="/Mercury/storage/pickWorkspace.action"><input type="hidden"
                                                                                                        name="pickerData"
                                                                                                        id="pickerDataInput"/><input
                    type="hidden" name="scanData" id="scanDataInput"/><input type="hidden" name="registerTransfers"
                                                                             id="registerTransfersInput"/></form>
        </div><%--container-fluid--%>
        <div id="dialog-message" title="Error"><p><span class="alert-error"
                                                        style="float:left; margin:0 7px 50px 0;"></span></p></div>
        <div id="dialog-confirm" title="Confirm"><p><span class="alert-warning"
                                                          style="float:left; margin:0 7px 50px 0;"></span></p></div>

        <div id="rack_scan_overlay">
            <%@include file="/vessel/ajax_div_rack_scanner.jsp" %>
        </div>

    </stripes:layout-component>

</stripes:layout-render>