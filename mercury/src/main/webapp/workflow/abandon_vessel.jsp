<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean"/>
<c:set var="reasonCodes" value="${actionBean.reasonCodes}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Abandon Vessel" sectionTitle="Abandon Vessel">
    <stripes:layout-component name="extraHead">
        <style type="text/css">
            .plateMapContainer {
                border-top: 1px solid #d3d3d3;
                padding-top: 15px;
            }

            .emptyCell {
                background-color: #d3d3d3;
            }

            .abandonedCell {
                background-color: #f2dede;
            }

            .unabandonedCell {
                background-color: #dff0d8;
            }

            .cellSelected {
                background-image: url("${ctxpath}/images/check.png");
                background-repeat: no-repeat;
                background-position: right center;
            }

            .cellUnSelected {
                background-image: url("${ctxpath}/images/plus.png");
                background-repeat: no-repeat;
                background-position: right center;
            }

        </style>

        <c:if test="${actionBean.doLayout}"><%-- Only valid if a layout exists --%>
        <script src="${ctxpath}/resources/scripts/abandonVessel.js"></script>
        </c:if>

        <script type="text/javascript">
            // Rack scanner implementation: See /vessel/ajax_div_rack_scanner.jsp for dependent functionality
            // TODO Capture success in action bean and short circuit round trip with scan data?
            function rackScanComplete() {
                var barcodes = $j("#rack_scan_overlay").data("results");
                //alert(barcodes);
                // Post the scans back
                $j('#rackScanData').val(barcodes);
                $j('#requestedActionType').attr('name', 'processRackScan');
                $j('#abandonForm').submit();
            }

            function performSelectionAction( clickedButtonEvent ) {

                $j('#abandonActionFeedback').html("");

                var selectedCells = $j("#layoutMap .cellSelected");
                var filteredSelectedCells = [];
                for( var i = 0; i < selectedCells.length; i++ ) {
                    if( selectedCells[i].id.indexOf( 'cell_') >= 0 ) {
                        filteredSelectedCells[filteredSelectedCells.length] = selectedCells[i];
                    }
                }

                if( filteredSelectedCells.length === 0 ) {
                    $j('#abandonActionFeedback').html("No vessel(s)/position(s) selected.");
                    return;
                }

                if( clickedButtonEvent.delegateTarget.id === 'abandonBtn') {
                    $j( '#abandonDialogOverlay').dialog( "open" );
                    return;
                } else if( clickedButtonEvent.delegateTarget.id === 'unabandonBtn') {
                    $j('#requestedActionType').attr('name', 'unabandon');
                    // Continue on to submit logic
                } else if( clickedButtonEvent.delegateTarget.id === 'abandonAcceptBtn' ) {

                    var overlay =  $j('#abandonDialogOverlay');

                    if( overlay.find( '#reasonCode').val() === "") {
                        overlay.find( '#abandonError' ).html("Select a valid reason");
                        return;
                    }
                    overlay.dialog( 'close' );

                    $j('#requestedActionType').attr('name', 'abandon');
                    $j('#abandonActionReason').val( $j( '#reasonCode').val());
                    // Continue on to submit logic
                } else {
                    return;
                }
                // Submit logic
                var actionJson = {"containerBarcode":"","abandonCells":[]};
                actionJson.containerBarcode = $j("#redisplayVesselBarcode").val();
                for( var i = 0; i < filteredSelectedCells.length; i++ ) {
                    actionJson.abandonCells[actionJson.abandonCells.length] = jQuery.data(filteredSelectedCells[i], 'abandonData');
                }
                $j('#abandonForm #abandonActionJson').val(JSON.stringify(actionJson));
                $j('#abandonForm').submit();
            }

            $j(document).ready(function () {
                // Only valid if a layout exists
                <c:if test="${actionBean.doLayout}">
                var json = {"layout": <enhance:out escapeXml="false">${actionBean.layoutMap}</enhance:out>};
                console.log(json);
                $j('#layoutMap').layoutMap(json);

                $j('#abandonBtn').click(performSelectionAction);
                $j('#unabandonBtn').click(performSelectionAction);
                </c:if>
            });

        </script>

    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form id="abandonForm" enctype="multipart/form-data" action="AbandonVessel.action" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean" method="POST">

        <div id="searchInput" class="form-horizontal">
        <label for="vesselBarcode">Vessel Barcode</label>
        <input type="text" id="vesselBarcode" name="vesselBarcode">
        <input type="submit" id="vesselBarcodeSearch" name="vesselBarcodeSearch" class="btn btn-primary" value="Find" />&nbsp;&nbsp;&nbsp;<input type="button" id="rackScanBtn" name="rackScanBtn" class="btn btn-primary" value="Rack Scan" onclick="startRackScan(this)" />
        <input type="hidden" name="rackScanData" id="rackScanData" value="${fn:escapeXml(actionBean.rackScanData)}"/>
        <input type="hidden" name="redisplayVesselBarcode" id="redisplayVesselBarcode" value="${actionBean.vesselBarcode}"/>
        <input type="hidden" name="abandonActionJson" id="abandonActionJson"/>
        <input type="hidden" name="abandonActionReason" id="abandonActionReason"/>
        <input type="hidden" name="toBeChanged" id="requestedActionType"/>
        </div>

        </stripes:form>

        <div id="layoutMap" class="plateMapContainer">

        <c:if test="${actionBean.doLayout}">
                <c:if test="${actionBean.tubeLayout}"><%-- No row and column headers for a single tube --%>
                    <div style="width: 600px; padding-left: 100px">
                        <div id="abandonActionFeedback" style="color:red; font-weight: bold;background-color: #f2dede"></div>
                    <table class="table table-bordered table-condensed">
                        <tbody>
                        <tr><td id="actionHeader" style="text-align: right;padding-bottom: 10px"><button id="abandonBtn" class="btn btn-primary">Abandon Selected</button> <button id="unabandonBtn" class="btn btn-primary">Un-Abandon Selected</button></td></tr>
                        <tr><td id="cell_1_1"></td></tr>
                        <tr><th id="legendFooter" style="text-align: right;padding-top: 10px" colspan="1">Legend:&nbsp;&nbsp;<span class="emptyCell" style="text-align: center; padding-left: 20px;padding-right: 20px"> Empty/Error </span>&nbsp;&nbsp;&nbsp;<span class="abandonedCell" style="text-align: center; padding-left: 20px;padding-right: 20px"> Abandoned </span>&nbsp;&nbsp;&nbsp;<span class="unabandonedCell" style="text-align: center; padding-left: 20px;padding-right: 20px"> Not Abandoned </span> </th></tr>
                        </tbody>
                    </table>
                    </div>
                </c:if>

                <c:if test="${not actionBean.tubeLayout}"><%-- Layout row and column headers for a container --%>
                <div>
                    <div id="abandonActionFeedback" style="color:red; font-weight: bold;background-color: #f2dede"></div>
                <table class="table table-bordered table-condensed">
                    <tbody>
                    <tr><td id="actionHeader" style="text-align: right;padding-bottom: 10px" colspan="${actionBean.vesselGeometry.columnCount + 1}"><button id="abandonBtn" class="btn btn-primary">Abandon Selected</button> <button id="unabandonBtn" class="btn btn-primary">Un-Abandon Selected</button></td></tr>
                    <tr>
                        <th id="vesselBarcodeHeader" colspan="${actionBean.vesselGeometry.columnCount + 1}"
                            style="text-align: center">${actionBean.vesselBarcode}</th>
                    </tr>
                    <tr>
                        <th id="tableSelector" class="cellUnSelected"></th>
                        <c:forEach items="${actionBean.vesselGeometry.columnNames}"
                                   var="colName" varStatus="colLoop">
                            <th class="cellUnSelected" id="col_${colLoop.index + 1}">${colName}</th>
                        </c:forEach>
                    </tr>
                    <c:forEach items="${actionBean.vesselGeometry.rowNames}"
                               var="rowName" varStatus="rowLoop">
                        <tr>
                            <th id="row_${rowLoop.index + 1}" class="cellUnSelected">${rowName}</th>
                            <c:forEach items="${actionBean.vesselGeometry.columnNames}"
                                       var="colName" varStatus="colLoop">
                                <td id="cell_${rowLoop.index + 1}_${colLoop.index + 1}"></td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                    <tr>
                        <th id="legendFooter" style="text-align: right;padding-top: 10px" colspan="${actionBean.vesselGeometry.columnCount + 1}">Legend:&nbsp;&nbsp;<span style="text-align: center; padding-left: 20px;padding-right: 20px"> Selected: <img src="${ctxpath}/images/check.png"></span>&nbsp;&nbsp;&nbsp;<span class="emptyCell" style="text-align: center; padding-left: 20px;padding-right: 20px"> Empty/Error </span>&nbsp;&nbsp;&nbsp;<span class="abandonedCell" style="text-align: center; padding-left: 20px;padding-right: 20px"> Abandoned </span>&nbsp;&nbsp;&nbsp;<span class="unabandonedCell" style="text-align: center; padding-left: 20px;padding-right: 20px"> Not Abandoned </span> </th>
                    </tr>
                    </tbody>
                </table>
                </div>
                </c:if>
        </c:if>

    <div id="abandonDialogOverlay">
        <script language="JavaScript">

            $j(document).ready( function(){
                $j( '#abandonDialogOverlay' ).dialog({
                    title: "Abandon Selected Vessel(s)/Position(s)",
                    autoOpen: false,
                    height: 200,
                    width: 350,
                    modal: true,
                    open: function(){
                        var abandonDialog = $j( '#abandonDialogOverlay' );
                        abandonDialog.data( 'doSubmit', false);
                        abandonDialog.find( '#reasonCode').val("");
                        abandonDialog.find( '#abandonError' ).html("");
                    }
                });

                $j( '#abandonCancelBtn').click(cancelAbandonAction);
                $j( '#abandonAcceptBtn').click(performSelectionAction);
            });

            function cancelAbandonAction(sourceButtonEvent){
                $j('#abandonDialogOverlay').dialog( 'close' );
            }

        </script>
        <p>Abandon Selected Vessel(s)/Position(s)</p>
        <div id="abandonError" style="color:red"></div>
        <div class="control-group">
            <label for="reasonCode" class="control-label">Reason: </label>
            <select id="reasonCode" name="reasonCode">
            <option value="">-Select-</option>
            <c:forEach items="${reasonCodes}" var="reasonValue" varStatus="reasonStatus">
                <option value="${reasonValue}">${reasonValue.getDisplayName()}</option>
            </c:forEach>
        </select>
        </div>
        <div class="control-group">
            <button id="abandonCancelBtn" class="btn btn-primary">Cancel</button>&nbsp;&nbsp;<button id="abandonAcceptBtn" class="btn btn-primary">Continue</button>
        </div>
    </div>

    <%-- Adds the overlay elements for ajax rack scanner See: /vessel/ajax_div_rack_scanner.jsp --%>
    <div id="rack_scan_overlay">
        <%@include file="/vessel/ajax_div_rack_scanner.jsp"%>
    </div>
    </stripes:layout-component>
</stripes:layout-render>
