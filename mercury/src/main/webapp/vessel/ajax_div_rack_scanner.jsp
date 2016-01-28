<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<%-- Reusable layout-definition used for ajax overlay rack scan functionality. --%>

    <script language="JavaScript">

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
                $j.post("${ctxpath}/search/ConfigurableSearch.action", actionBeanParams, function (data) {
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
                $j.post("${ctxpath}/search/ConfigurableSearch.action", actionBeanParams, function (data) {
                    // Overwrites the option tags in the selectScanner dropdown.
                    $j('#rack_scan_inputs').html(data);
                });
            }
        }

        $("#ajaxScanForm").submit(function (e) {
            e.preventDefault();
            var formData = new FormData();
            formData.append("labToFilterBy", $j('#selectLab').val());
            formData.append("rackScanner", $j('#selectScanner').val());
            if($j('#simulationFile')) {
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
                url: "${ctxpath}/search/ConfigurableSearch.action",
                type: 'POST',
                data: formData,
                async: true,
                success: function (results) {
                    $j("#rack_scan_overlay").data("results",results);
                    $j("#doScanBtn").removeAttr("disabled");
                    rackScanComplete();
                },
                error: function(results){
                    $j("#rackScanError").text("A server error occurred");
                    $j("#doScanBtn").removeAttr("disabled");
                },
                cache: false,
                datatype: "text",
                processData: false,
                contentType: false
            });
        });

    </script>
    <div id="rackScanError" style="color:red"/>
    <stripes:form method="post" id="ajaxScanForm" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean">
    <!-- Selects the lab, which then populates the scanner device list. -->
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

    <!-- Selects the scanner, using a dynamically generated list of scanner device names. -->
<c:if test="${not empty actionBean.labToFilterBy}">
    <div class="control-group">
        <label id="selectScannerLabel" for="selectScanner" class="control-label">Rack Scanner</label>
        <div class="controls">
            <select name="rackScanner" id="selectScanner" onchange="scannerChanged();">
                <option value="" label="Select One" />
                <c:forEach items="${actionBean.rackScanners}" var="scanner">
                    <option value="${scanner.name}"${scanner.name == actionBean.rackScanner?" selected":""}>${scanner.scannerName}</option>
                    <c:if test="${not empty actionBean.labToFilterBy and actionBean.labToFilterBy eq 'RACK_SCAN_SIMULATOR_LAB'}">
                        <option value="${scanner.name}" selected>${scanner.scannerName}</option>
                    </c:if>
                </c:forEach>
            </select>
        </div>
    </div>
</c:if>
<c:if test="${not empty actionBean.labToFilterBy and actionBean.labToFilterBy eq 'RACK_SCAN_SIMULATOR_LAB'}">
    <!-- Selects a csv file, dynamically exposed/hidden depending on the lab selection. -->
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
</stripes:form>


