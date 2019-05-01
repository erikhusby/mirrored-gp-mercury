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
             * Handles configuration and assignment of target racks
             */
            handleTargets = function(evt){
                alert( evt.delegateTarget.id);
            };

            /**
             * Handles the layout of target rack barcode inputs
             */
            layoutTargets = function(evt){
                alert( evt.delegateTarget.id);
            }

            /**
             * Sets up DOM plugins (batch multi-list and datatable) and event listeners
             */
            $j(document).ready(function () {
                initBatches();
                if (pickerDataJson.length > 0) {
                    $j("#tblPickList").dataTable({
                        data: pickerDataJson,
                        paging: false,
                        scrollY: 480,
                        searching: false,
                        info: true,
                        columns: [
                            {data: "batchName", title: "SRS Batch"},
                            {data: "storageLocPath", title: "Storage Location"},
                            {data: "sourceVessel", title: "Rack Barcode"},
                            {data: "targetRack", title: "Target Rack"},
                            {data: "totalVesselCount", title: "Total Samples"},
                            {data: "srsVesselCount", title: "Samples to Pull"}
                        ],
                        columnDefs: [
                            {targets: [0,1,2,3], className: "dt-head-left"},
                            {targets: [4,5], className: "dt-right"}
                        ]
                    });
                    $j("#divAssignTargets").css("display", "block");
                    $j("#btnAssignTargets").click(handleTargets);
                    $j("#txtTubesPerRack").change(layoutTargets);
                    $j("#cbSplitRacks").change(layoutTargets);
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
                    <p>Split Racks by Batch:  <input type="checkbox" name="splitRacks" id="cbSplitRacks" style="margin:0px 0px 0px 8px"/></p>
<div id="divRackAssignments"></div>
                </fieldset></div>
        </div>
        </div><%--container-fluid--%>
    </stripes:layout-component>

</stripes:layout-render>