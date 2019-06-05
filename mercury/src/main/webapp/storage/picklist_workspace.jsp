<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.PickWorkspaceActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Workspace" sectionTitle="SRS Workspace" showCreate="false" dataTablesVersion = "1.10">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/jquery-ui-1.9.2.custom.min.js"></script>
        <link rel="stylesheet" type="text/css"
              href="${ctxpath}/resources/scripts/multi-list/multi-list.css"/>
        <script src="${ctxpath}/resources/scripts/multi-list/multi-list.js"></script>
        <style></style>
        <script type="text/javascript">
            <enhance:out escapeXml="false">var batchListJson = ${actionBean.batchSelectionList};</enhance:out>
            <enhance:out escapeXml="false">var pickerDataJson = ${actionBean.pickerData};</enhance:out>

            /**
             * Supports matrix 96 racks only
             * Function index argument is 1 based to correlate with vessel count
             */
            getRackPosition = function(i) {
                var rows = ["A", "B", "C", "D", "E", "F", "G", "H"];
                var cols = ["01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"];
                if( i < 1 || i > 96 ) {
                    throw "Invalid matrix 96 position (" + i + "), must be in range 1-96";
                } else {
                    var zeroBased = i - 1;
                    return rows[Math.floor(zeroBased/12)] + cols[zeroBased%12];
                }
            };

            var resetFeedbackAlerts = function() {
                $j("#ajaxError").css("display", "none");
                $j("#ajaxErrorText").html("");
                $j("#ajaxInfo").css("display", "none");
                $j("#ajaxInfoText").html("");
                $j("#ajaxWarning").css("display", "none");
                $j("#ajaxWarningText").html("");
            };

            var dismissAlert = function(level) {
                var id = "#ajax" + level;
                $j(id).css("display", "none");
                $j(id + "Text").html("");
            };

            /**
             * level:  Error, Info,
             */
            var showFeedbackAlerts = function(level, content) {
                    var id = "#ajax" + level;
                    $j(id).css("display", "block");
                    $j(id + "Text").html(content);
                };

            /**
             * level:  Error, Info,
             */
            var showAlertDialog = function(level, content) {
                var theDialog = $j("#dialog-message");
                theDialog.attr("title", level);
                var theOutput = $j("#dialog-message span");
                theOutput.text(content);
                theOutput.attr("class", "alert-" + level.toLowerCase());
                theDialog.dialog("open");
            };

            /**
             * Load the JSON batch and vessel data into main form fields before any submit
             */
            preBatchFormSubmit = function () {
                var theform = $j("#formPickType");
                $j("#batchSelectionList", theform).val(JSON.stringify(batchListJson));
                $j("#pickerData", theform).val(JSON.stringify(pickerDataJson));
                return true;
            };

            /**
             * Manipulates the batch list JSON state to reflect batch check/uncheck
             */
            handleBatchCheckEvent = function(event, txtBatchId, batchName) {
                $j.each( batchListJson, function(i,batch){
                    if( String(batch.batchId) === txtBatchId ) {
                        batch.selected = (event.namespace === "elementChecked");
                        return false;
                    }
                } );
            };

            /**
             * Initialize checkbox list items and state based upon back end JSON
             */
            initBatches = function () {
                var batchListParent = $j("#batchListParent");
                $j.each( batchListJson, function(i,val){
                    batchListParent.append('<li value="' + val.batchId + '">' + val.batchName + '</li>');
                } );
                batchListParent.multiList();
                $j.each( batchListJson, function(i,val){
                    if( val.wasSelected ) {
                        batchListParent.multiList('select', val.batchId);
                    }
                } );
                batchListParent.on('multiList.elementChecked', handleBatchCheckEvent);
                batchListParent.on('multiList.elementUnchecked', handleBatchCheckEvent);
            };

            /**
             * Gets all robot pickable container data broken down by batch name
             */
            getPickableDataByBatch = function(){
                var batchPickerData = [];
                for( var i = 0; i < pickerDataJson.length; i++ ) {
                    var pickerDataRow = pickerDataJson[i];
                    if( pickerDataRow.rackScannable ) {
                        if( batchPickerData[pickerDataRow.batchName] == undefined ) {
                            batchPickerData[pickerDataRow.batchName] = [];
                        }
                        batchPickerData[pickerDataRow.batchName].push(pickerDataRow);
                    }
                }
                return batchPickerData;
            }

            /**
             * Handles assignment of target rack barcodes
             */
            assignTargets = function(src){
                var theInput = $j("#" + src.id);
                var previousValue = theInput.data("previousValue");
                var newValue = theInput.val();
                if(newValue === "") {
                    return;
                } else {
                    theInput.data("previousValue", newValue);
                }
                var table = $j("#tblPickList").DataTable();
                racks:
                    for( var j = 0; j < pickerDataJson.length; j++ ) {
                        var batchRackData = pickerDataJson[j];
                        vessels:
                            for( var k = 0; k < batchRackData.pickerVessels.length ; k++ ) {
                                var pickerVessel = batchRackData.pickerVessels[k];
                                if( pickerVessel.targetVessel == previousValue ) {
                                    pickerVessel.targetVessel = newValue;
                                }
                            }
                        table.row("#" + batchRackData.sourceVessel + "|" + batchRackData.batchId ).data(batchRackData);
                    }

                table.draw();
            };

            /**
             *  Builds HTML for target rack input
             */
            buildTargetInput = function(arrayIdx) {
                var inputHTML = "" +
                "<li style='padding-bottom:8px'><input style='width:100px' placeholder='DEST" + (arrayIdx+1)
                    + "' type='text' id='targetRack_DEST" + (arrayIdx+1) + "' name='targetRack[" + arrayIdx
                    + "]' data-previous-value='DEST" + (arrayIdx+1) + "' onChange='assignTargets(this);'></li>";
                return inputHTML;
            };

            /**
             * Handles the layout of target rack barcode inputs
             */
            layoutTargets = function(evt){
                var table = $j("#tblPickList").DataTable();
                var targetBarcodesDiv = $j("#targetRackAssignments");
                targetBarcodesDiv.empty();

                var doBatchSplit = $j("#cbSplitRacks").prop( "checked" );
                var tubesPerRack = Number( $j("#txtTubesPerRack").val() );
                if( tubesPerRack !== tubesPerRack || tubesPerRack == 0 ) {
                    alert( "Invalid tubes per rack value.");
                    evt.delegateTarget.focus();
                    return false;
                }

                var batchPickerData = getPickableDataByBatch();
                var targetRackCount = 0;
                var targetRackTubeCount = 0;
                batches:
                for( var batchName in batchPickerData ) {
                    if( doBatchSplit || targetRackCount == 0 ) {
                        targetRackTubeCount = 0;
                        targetRackCount++;
                    }
                    var batchRacks = batchPickerData[batchName];
                    racks:
                    for( var j = 0; j < batchRacks.length; j++ ) {
                        var batchRackData = batchRacks[j];
                        vessels:
                        for( var k = 0; k < batchRackData.pickerVessels.length ; k++ ) {
                            var pickerVessel = batchRackData.pickerVessels[k];
                            targetRackTubeCount++;

                            if( targetRackTubeCount > tubesPerRack ) {
                                targetRackTubeCount = 1;
                                targetRackCount++;
                            }

                            pickerVessel.targetVessel = "DEST" + targetRackCount;
                            pickerVessel.targetPosition = getRackPosition(targetRackTubeCount);
                        }
                        table.row("#" + batchRackData.sourceVessel + "|" + batchRackData.batchId ).data(batchRackData);
                    }
                }

                table.draw();

                // Build barcode inputs
                for( var i = 0; i < targetRackCount; i++ ) {
                    targetBarcodesDiv.append( buildTargetInput(i) );
                }
            };

            /**
             * Is a vessel part of multiple SRS batches?
             */
            var checkConflict = function(){
                resetFeedbackAlerts();
                $j("#btnConflicts").data("conflictState", "pending");

                // Batch list keyed by tube barcode
                var tubeBatches = [[]];
                // Rack keyed by tube barcode
                var tubeRack = [];

                // batch_racks:
                for( var j = 0; j < pickerDataJson.length; j++ ) {
                    var batchRackData = pickerDataJson[j];
                    // vessels:
                    for( var k = 0; k < batchRackData.pickerVessels.length ; k++ ) {
                        var pickerVessel = batchRackData.pickerVessels[k];
                        if(tubeBatches[pickerVessel.sourceVessel] == undefined ) {
                            tubeBatches[pickerVessel.sourceVessel] = [batchRackData.batchName];
                            tubeRack[pickerVessel.sourceVessel] = [batchRackData.sourceVessel];
                        } else {
                            var batches = tubeBatches[pickerVessel.sourceVessel];
                            batches[batches.length] = batchRackData.batchName;
                        }
                    }
                }
                var feedback = "";
                for( label in tubeBatches) {
                    var batches = tubeBatches[label];
                    if( batches.length > 1 ) {
                        feedback += (feedback.length>0?"<br/>":"") + "Rack: " + tubeRack[label] + ", Vessel: " + label + " in batches: " + batches;
                    }
                }

                if( feedback == 0 ) {
                    showFeedbackAlerts("Info", "No vessel - batch conflicts");
                    $j("#btnConflicts").data("conflictState", "success");
                } else {
                    showFeedbackAlerts("Warning", feedback);
                    $j("#btnConflicts").data("conflictState", "fail");
                }
            };

            var doXferFileBuild = function(){
                // Can't build a transfer file if source vessels in multiple batches
                var conflictState = $j("#btnConflicts").data("conflictState");
                if( "pending" == conflictState ) {
                    checkConflict();
                    resetFeedbackAlerts();
                    conflictState = $j("#btnConflicts").data("conflictState");
                }
                if( "fail" == conflictState ) {
                    showAlertDialog("Error", "Cannot continue - Conflicts exist for SRS batch vessels");
                    return false;
                }
                // Can't build a transfer file if target barcodes not fully assigned (Or start with 'DEST')
                //batch_racks:
                for( var j = 0; j < pickerDataJson.length; j++ ) {
                    var batchRackData = pickerDataJson[j];
                    if( !batchRackData.rackScannable ) {
                        continue;
                    }
                    // vessels:
                    for( var k = 0; k < batchRackData.pickerVessels.length ; k++ ) {
                        var pickerVessel = batchRackData.pickerVessels[k];
                        if( pickerVessel.targetVessel == null || pickerVessel.targetVessel.trim() == "" || pickerVessel.targetVessel.indexOf("DEST") >= 0 ) {
                            resetFeedbackAlerts();
                            showAlertDialog("Error", "Cannot continue - Target rack barcodes are unassigned");
                            return false;
                        }
                    }
                }
                // We're OK to produce a transfer file
                $j("#xferPickerData").val(JSON.stringify(pickerDataJson));
                return true;
            };

            /**
             * Sets up DOM plugins (batch multi-list and datatable) and event listeners
             */
            $j(document).ready(function () {
                initBatches();
                $j("#formPickType").submit(preBatchFormSubmit);
                $j( "#dialog-message" ).dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {
                        Ok: function() {
                            $j( this ).dialog( "close" );
                        }
                    }
                });
                if (pickerDataJson.length > 0) {
                    $j("#tblPickList").dataTable({
                        data: pickerDataJson,
                        rowId: function(row) {
                            return row.sourceVessel + "|" + row.batchId;
                        },
                        paging: false,
                        scrollY: 480,
                        searching: false,
                        info: true,
                        columns: [
                            {data: "batchName", title: "SRS Batch"},
                            {data: "storageLocPath", title: "Storage Location"},
                            {data: "sourceVessel", title: "Rack Barcode"},
                            {data: "targetRack", // Never any real value here, just a placeholder
                                name: "targetRack",
                                render: function(data, type, row){
                                    if(row.rackScannable) {
                                        var racks = [];
                                        vessels:
                                            for( var k = 0; k < row.pickerVessels.length ; k++ ) {
                                                var pickerVessel = row.pickerVessels[k];
                                                if( pickerVessel.targetVessel != null ) {
                                                    racks[pickerVessel.targetVessel] = pickerVessel.targetVessel;
                                                }
                                                var targets = Object.keys(racks);
                                                if( targets.length > 0 ) {
                                                    row.targetRack = targets.toString();
                                                }
                                            }
                                        return row.targetRack;
                                    } else {
                                        return "(Not Robot Pickable}";
                                    } },
                                title: "Target Rack"},
                            {data: "totalVesselCount", title: "Total Samples"},
                            {data: "srsVesselCount", title: "Samples to Pull"}
                        ],
                        columnDefs: [
                            {targets: [0,1,2,3], className: "dt-head-left"},
                            {targets: [4,5], className: "dt-right"}
                        ]
                    });
                    $j("#divAssignTargets").css("display", "block");
                    $j("#btnReBalance").click(layoutTargets);
                    $j("#btnConflicts").click(checkConflict);
                    $j("#btnBuildXferFile").click(doXferFileBuild);
                } else {
                    $j("#divAssignTargets").css("display", "none");
                }
            });

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
        <div class="row-fluid">
            <stripes:form id="formPickType" name="formPickType" action="/vessel/pickWorkspace.action"><input type="hidden" name="batchSelectionList" id="batchSelectionList" /><input type="hidden" name="pickerData" id="pickerData" />
            <div class="span3">
                <fieldset>
                    <legend>Batches</legend>
                    <ul id="batchListParent"></ul>
                </fieldset>
            </div>
            <div class="span2">
                        <div class="control-label"><label for="btnProcessBatches">&nbsp;</label></div>
                        <div class="controls"><p><input type="submit" name="processBatches" id="btnProcessBatches" value="View Batches" style="width:120px"/></p>
                            <p><input type="button" id="btnConflicts" value="Check Conflicts" data-conflict-state="pending" style="width:120px"/></p><%-- States are pending, success, fail --%></div>
            </div>
            <div class="span6">
                <div id="ajaxError" class="alert-error" style="margin-left:12px;margin-right:12px;display: none"><button type="button" class="close" onclick="dismissAlert('Error');">&times;</button><span id="ajaxErrorText"></span></div>
                <div id="ajaxInfo" class="alert-info" style="margin-left:12px;margin-right:12px;display: none"><button type="button" class="close" onclick="dismissAlert('Info');">&times;</button><span id="ajaxInfoText"></span></div>
                <div id="ajaxWarning" class="alert-warning" style="margin-left:12px;margin-right:12px;display: none"><button type="button" class="close" onclick="dismissAlert('Warning');">&times;</button><span id="ajaxWarningText"></span></div>
            </div>
            </stripes:form>
        </div><%--row-fluid--%>
        <div class="row-fluid"><div class="span12"><hr/></div></div>
        <div class="row-fluid">
            <div class="span9"><table id="tblPickList" class="display compact"></table>
                <p>${actionBean.rackCount} Racks, ${actionBean.pickSampleCount} Tubes</p></div>
            <div class="span3" id="divAssignTargets">
                <fieldset style="padding-left: 8px">
                    <legend>Target Racks</legend>
                    <p>Max Tubes per Rack:  <input type="text" name="tubesPerRack" id="txtTubesPerRack" style="width:40px;height:20px;padding:2px; margin:0px 0px 0px 8px"/></p>
                    <p>Split Racks by Batch:  <input type="checkbox" name="splitRacks" value="true" id="cbSplitRacks" style="margin:0px 40px 0px 8px"/> <input type="button" id="btnReBalance" value="Re-Balance"/></p>
                    <ol id="targetRackAssignments"></ol>
                    <p><form id="formBuildXferFile" name="formBuildXferFile" action="/Mercury/vessel/pickWorkspace.action"><input type="hidden" name="pickerData" id="xferPickerData"/><input type="submit" name="buildXferFile" id="btnBuildXferFile" value="Build Transfer File" style="width:120px"/></form></p>
                </fieldset></div>
        </div>
        </div><%--container-fluid--%>
        <div id="dialog-message" title="Error"><p><span class="alert-error" style="float:left; margin:0 7px 50px 0;"></span></p></div>
    </stripes:layout-component>

</stripes:layout-render>