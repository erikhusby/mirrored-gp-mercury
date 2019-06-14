<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%-- Reusable layout-definition used for ajax overlay rack scan functionality.
     Parent page must include (in order):
       1:  JavaScript rackScanComplete() function implementation to handle scan results
        function rackScanComplete() {
            var barcodes = $j("#rack_scan_overlay").data("results");
            if( barcodes != null ) {
                // Do something meaningful here other than:
                alert(barcodes);
            }
            $j("#rack_scan_overlay").dialog("close");
            $j("#rack_scan_inputs").html("");
        }
       2:  HTML div element to hold the modal dialog overlay - outside of the working form on the page
        <div id="rack_scan_overlay">
            <%@include file="/vessel/ajax_div_rack_scanner.jsp"%>
        </div>
       3:  A button to initiate the scan:
       <input type="button" id="rackScanBtn" name="rackScanBtn" class="btn btn-primary" value="Scan" onclick="startRackScan(this)" />

   (Use /vessel/rack_scanner_list_with_sim_part1.jsp and /vessel/rack_scanner_list_with_sim_part2.jsp
     for a more static implementation UI)
--%>
<script language="JavaScript">

    $j(document).ready( function(){
        $j( "#rack_scan_overlay" ).dialog({
            title: "Rack Scan Barcodes",
            autoOpen: false,
            height: 350,
            width: 350,
            modal: true,
            close: cancelRackScan,
            open: function(){
                $j.ajax({
                    url: '${ctxpath}/vessel/AjaxRackScan.action?ajaxLabSelect=',
                    type: 'get',
                    dataType: 'html',
                    success: function (returnData) {
                        $j("#rack_scan_inputs").html(returnData);
                    }
                })
            }
        });
    });

    // Basically avoids server round trip when un-selecting lab at any stage
    function hideDependentControls() {
        $j('#selectScanner').css('display','none');
        $j('#selectScannerLabel').css('display','none');
        $j('#simulationFile').css('display','none');
        $j('#simulationFileLabel').css('display','none');
        $j('#scanBtn').css('display','none');
    }

    function labChanged() {
        if ($j('#selectLab').val() == '') {
            hideDependentControls();
        } else {
            // Call the action bean event to get the scanner devices, using these params.
            actionBeanParams = { ajaxLabSelect : '', labToFilterBy : $j('#selectLab').val() };
            $j.post("${ctxpath}/vessel/AjaxRackScan.action", actionBeanParams, function (data) {
                // Overwrites the option tags in the selectScanner dropdown.
                $j('#rack_scan_inputs').html(data);
            });
        }
    }

    function scannerChanged() {
        if ($j('#selectLab').val() == '') {
            hideDependentControls();
        } else {
            // Call the action bean event to get the scanner devices, using these params.
            actionBeanParams = { ajaxLabSelect : '', labToFilterBy : $j('#selectLab').val(), rackScanner : $j('#selectScanner').val() };
            $j.post("${ctxpath}/vessel/AjaxRackScan.action", actionBeanParams, function (data) {
                // Overwrites the option tags in the selectScanner dropdown.
                $j('#rack_scan_inputs').html(data);
            });
        }
    }

    $j("#ajaxScanForm").submit(function (e) {
        e.preventDefault();
        var formData = new FormData();
        formData.append("labToFilterBy", $j('#selectLab').val());
        formData.append("rackScanner", $j('#selectScanner').val());
        if( $j('#simulationFile').length > 0 ) {
            if($j('#simulationFile')[0].files[0]){
                formData.append("simulatedScanCsv", $j('#simulationFile')[0].files[0]);
            } else {
                $j("#rackScanError").text("Choose a simulator file");
                return;
            }
        }
        formData.append("ajaxScan", $j('#doScanBtn').val());

        $j("#doScanBtn").attr("disabled","disabled");

        $j("#rack_scan_overlay").removeData("results");

        $j.ajax({
            url: "${ctxpath}/vessel/AjaxRackScan.action",
            type: 'POST',
            data: formData,
            async: true,
            success: function (results) {
                $j("#doScanBtn").removeAttr("disabled");
                if( results.startsWith("Failure")) {
                    $j("#rackScanError").text(results);
                } else {
                    $j("#rack_scan_overlay").data("results", results);
                    rackScanComplete();
                }
            },
            error: function(results){
                $j("#doScanBtn").removeAttr("disabled");
                $j("#rackScanError").text("A server error occurred");
            },
            cache: false,
            datatype: "text",
            processData: false,
            contentType: false
        });
    });

    function startRackScan(sourceButton){
        rackScanSrcBtn = sourceButton;
        $j("#rack_scan_overlay").dialog( "open" );
    }

    function cancelRackScan(){
        rackScanSrcBtn = null;
        $j("#rack_scan_inputs").html("");
        $j("#rack_scan_overlay").removeData("results");
    }

</script>

<div id="rack_scan_inputs">
    <div id="rackScanError" style="color:red"/>
    <stripes:form method="post" id="ajaxScanForm" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.AjaxRackScanActionBean">
    <c:if test="${actionBean['class'].simpleName eq 'AjaxRackScanActionBean'}">
    <%-- Selects the lab, which then populates the scanner device list. --%>
    <div class="control-group">
        <label for="selectLab" class="control-label">Lab</label>
        <div class="controls">
            <select name="labToFilterBy" id="selectLab" onchange="labChanged()">
                <option value="" label="Select One" />
                <c:forEach items="${actionBean.allLabs}" var="lab">
                    <option value="${lab.name}"${lab.name == actionBean.labToFilterBy?" selected":""}>${lab.labName}</option>
                </c:forEach>
            </select>
        </div>
    </div>

<%-- Selects the scanner, using a dynamically generated list of scanner device names.
     Backing bean must have logic to select simulation scanner if simulation lab is selected --%>
<c:if test="${not empty actionBean.labToFilterBy}">
    <div class="control-group">
        <label id="selectScannerLabel" for="selectScanner" class="control-label">Rack Scanner</label>
        <div class="controls">
            <select name="rackScanner" id="selectScanner" onchange="scannerChanged();">
                <option value="" label="Select One" />
                <c:forEach items="${actionBean.rackScanners}" var="scanner">
                    <option value="${scanner.name}"${scanner.name == actionBean.rackScanner?" selected":""}>${scanner.scannerName}</option>
                </c:forEach>
            </select>
        </div>
    </div>
</c:if>
<%-- Selects a csv file, dynamically exposed/hidden depending on the lab selection. --%>
<c:if test="${not empty actionBean.labToFilterBy and actionBean.labToFilterBy eq 'RACK_SCAN_SIMULATOR_LAB'}">
    <div class="control-group">
        <label id="simulationFileLabel" for="simulationFile" class="control-label">Choose a csv file</label>
        <div class="controls">
            <stripes:file name="simulatedScanCsv" id="simulationFile"/>
        </div>
    </div>
</c:if>
<div class="control-group">
        <input type="button" value="Cancel" name="scanCancelBtn" id="scanCancelBtn" class="btn btn-primary" onclick="$j('#rack_scan_overlay').dialog('close');"/>&nbsp;&nbsp;
        <c:if test="${not empty actionBean.labToFilterBy and not empty actionBean.rackScanner}">
            <input type="submit" value="Scan" name="ajaxScan" id="doScanBtn" class="btn btn-primary"/>
        </c:if>
    </div>
</c:if>
</stripes:form>
</div>

