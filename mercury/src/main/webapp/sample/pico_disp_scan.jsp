
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ page import="org.broadinstitute.bsp.client.rackscan.RackScanner" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean" %>

<%-- Allows selecting a rack scan lab, rack scanner device, and user-input simulated rack scan file. --%>

<stripes:layout-render name="/layout.jsp" pageTitle="Review Pico Dispositions via Rack Scan"
                       sectionTitle="Review Pico Dispositions via Rack Scan" showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            function hideDependentControls() {
                $j('#selectScanner').css('display','none');
                $j('#selectScannerLabel').css('display','none');
                $j('#simulationFile').css('display','none');
                $j('#simulationFileLabel').css('display','none');
                $j('#scanBtn').css('display','none');
            }

            $j(document).ready(function () {
                $j('#selectLab').val("");
                hideDependentControls();
            });

            function labChanged() {
                if ($j('#selectLab').val() == '') {
                    hideDependentControls();
                } else {
                    // POST invokes the action bean event to get the scanner devices, using these params.
                    actionBeanParams = { getScannersForLab : '', labToFilterBy : $j('#selectLab').val() };
                    $j.post(window.location.pathname, actionBeanParams, function (data) {
                        // Rewrites the option tags in the selectScanner dropdown.
                        $j('#selectScanner').html(data);
                    });
                    $j('#selectScannerLabel').css('display','block');
                    $j('#selectScanner').css('display','block');
                    // The csv file selector used for simulation.
                    if ($j('#selectLab').val() == "<%= RackScanner.RackScannerLab.RACK_SCAN_SIMULATOR_LAB.getName() %>") {
                        $j('#simulationFileLabel').css('display','block');
                        $j('#simulationFile').css('display','block');
                    } else {
                        $j('#simulationFileLabel').css('display','none');
                        $j('#simulationFile').css('display','none');
                    }
                    $j('#scanBtn').css('display','block');
                }
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="selectionForm" class="form-horizontal">
            <div class="form-horizontal">

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

                <!-- Dynamically generated list of scanner device names. -->
                <div class="control-group">
                    <stripes:label id="selectScannerLabel" for="selectScanner" class="control-label">Rack Scanner</stripes:label>
                    <div class="controls">
                        <stripes:select name="rackScanner" id="selectScanner"/>
                    </div>
                </div>


                <!-- Dynamically exposed/hidden file browse control. -->
                <div class="control-group">
                    <stripes:label id="simulationFileLabel" for="simulationFile" class="control-label">Choose a csv file</stripes:label>
                    <div class="controls">
                        <stripes:file name="simulatedScanCsv" id="simulationFile"/>
                    </div>
                </div>

                <div class="controls">
                    <stripes:submit value="Scan" class="btn btn-primary"
                                    name="<%= RackScanActionBean.SCAN_EVENT %>"/>
                </div>
            </div>

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
