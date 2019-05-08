<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.PickWorkspaceActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Workspace" sectionTitle="SRS Workspace" showCreate="false" dataTablesVersion = "1.10">

    <stripes:layout-component name="extraHead">
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

            /**
             * Post back the checkbox state to add or remove batches
             */
            processBatches = function () {
                var theform = $j("#formPickType");
                theform.append('<input type="hidden" name="processBatches" value=""/>')
                    .append('<input type="hidden" name="batchSelectionList" id="batchSelectionList" />')
                    .append('<input type="hidden" name="pickerData" id="pickerData" />');
                $j("#batchSelectionList", theform).val(JSON.stringify(batchListJson));
                $j("#pickerData", theform).val(JSON.stringify(pickerDataJson));
                theform.submit();
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
                (arrayIdx+1).toString() + ": <input style='width:100px' placeholder='DEST" + (arrayIdx+1)
                    + "' type='text' id='targetRack_DEST" + (arrayIdx+1) + "' name='targetRack[" + arrayIdx
                    + "]' data-previous-value='DEST" + (arrayIdx+1) + "' onChange='assignTargets(this);'/>";
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
                for( var i = 0; i < targetRackCount; i += 2 ) {
                    var paraDiv = targetBarcodesDiv.append( "<p>" );
                    paraDiv.append( buildTargetInput(i) );
                    paraDiv.append( "&nbsp;&nbsp;&nbsp;&nbsp;" );
                    if( i + 1 < targetRackCount ) {
                        paraDiv.append( buildTargetInput(i+1) );
                    }
                }
            }

            /**
             * Sets up DOM plugins (batch multi-list and datatable) and event listeners
             */
            $j(document).ready(function () {
                initBatches();
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
                } else {
                    $j("#divAssignTargets").css("display", "none");
                }
                $j("#btnProcessBatches").click(processBatches);
            });

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
        <div class="row-fluid">
            <stripes:form id="formPickType" name="formPickType" action="/vessel/pickWorkspace.action">
            <div class="span3">
                <fieldset>
                    <legend>Batches</legend>
                    <ul id="batchListParent"></ul>
                </fieldset>
            </div>
            <div class="span2">
                        <div class="control-label"><label for="btnProcessBatches">&nbsp;</label></div>
                        <div class="controls"><input type="button" id="btnProcessBatches" value="Update Batches"/></div>
            </div>
            <div class="span7">
                <div id="ajaxError" class="alert alert-error" style="margin-left:20%;margin-right:20%;display: none"><div id="ajaxErrorText"></div><button type="button" class="close" data-dismiss="alert">×</button></div>
                <div id="ajaxInfo" class="alert alert-info" style="margin-left:20%;margin-right:20%;display: none"><div id="ajaxInfoText"></div><button type="button" class="close" data-dismiss="alert">×</button></div>
                <div id="ajaxWarning" class="alert alert-warning" style="margin-left:20%;margin-right:20%;display: none"><div id="ajaxWarningText"></div><button type="button" class="close" data-dismiss="alert">×</button></div>
            </div>
            </stripes:form>
        </div><%--row-fluid--%>
        <div class="row-fluid"><div class="span12"><hr/></div></div>
        <div class="row-fluid">
            <div class="span9"><table id="tblPickList" class="display compact"></table></div>
            <div class="span3" id="divAssignTargets">
                <fieldset style="padding-left: 8px">
                    <legend>Target Racks</legend>
                    <p>Max Tubes per Rack:  <input type="text" name="tubesPerRack" id="txtTubesPerRack" style="width:40px;height:20px;padding:2px; margin:0px 0px 0px 8px"/></p>
                    <p>Split Racks by Batch:  <input type="checkbox" name="splitRacks" id="cbSplitRacks" style="margin:0px 40px 0px 8px"/> <input type="button" id="btnReBalance" value="Re-Balance"/></p>
                    <div id="targetRackAssignments"></div>
                </fieldset></div>
        </div>
        </div><%--container-fluid--%>
    </stripes:layout-component>

</stripes:layout-render>