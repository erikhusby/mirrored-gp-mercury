
<%@ page import="org.broadinstitute.bsp.client.rackscan.RackScanner" %>

<%-- javascript to be included in jsp that selects a rack scan lab and device. --%>
<%-- Expects that rack_scanner_list_with_sim_part2 is present on the page. --%>

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
            // The action bean's getScannersForLab is expected to return the html option elements.
            actionBeanParams = { getScannersForLab : '', labToFilterBy : $j('#selectLab').val() };
            $j.post(window.location.pathname, actionBeanParams, function (data) {
                // Overwrites the option tags in the selectScanner dropdown.
                $j('#selectScanner').html(data);
            });
            $j('#selectScannerLabel').css('display','block');
            $j('#selectScanner').css('display','block');
            // Unhides the csv file selector if user selected a rack scan simulation lab.
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
