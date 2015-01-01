
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<%-- Reusable layout-definition used for rack scan selection. --%>

<stripes:layout-definition>

    <!-- Selects the lab, which then populates the scanner device list. -->
    <div class="control-group">
        <stripes:label for="selectLab" class="control-label">Lab</stripes:label>
        <div class="controls">
            <stripes:select name="labToFilterBy" id="selectLab" onchange="labChanged()">
                <stripes:option value="" label="Select One" />
                <stripes:options-collection collection="${actionBean.allLabs}" label="labName" value="name"/>
            </stripes:select>
        </div>
    </div>

    <!-- Selects the scanner, using a dynamically generated list of scanner device names. -->
    <div class="control-group">
        <stripes:label id="selectScannerLabel" for="selectScanner" class="control-label">Rack Scanner</stripes:label>
        <div class="controls">
            <stripes:select name="rackScanner" id="selectScanner"/>
        </div>
    </div>


    <!-- Selects a csv file, dynamically exposed/hidden depending on the lab selection. -->
    <div class="control-group">
        <stripes:label id="simulationFileLabel" for="simulationFile" class="control-label">Choose a csv file</stripes:label>
        <div class="controls">
            <stripes:file name="simulatedScanCsv" id="simulationFile"/>
        </div>
    </div>

</stripes:layout-definition>
