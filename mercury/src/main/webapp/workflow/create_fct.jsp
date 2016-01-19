<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create FCT Ticket" sectionTitle="Create FCT Ticket">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            var numFlowcellLanes = 0;

            $j(document).ready(function () {
                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [1, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":true}, // barcode
                        {"bSortable":true},  // lcset
                        {"bSortable":true},  // tube (event) type
                        {"bSortable":true},  // enter number lanes
                        {"bSortable":true},  // enter loading conc
                        {"bSortable":true},  // tube create (event) date
                        {"bSortable":true},  // product
                    ]
                });
                updateFlowcell();
                // Clears the lcset names field.
                $j('#lcsetText').removeAttr('value');
            });

            // After the flowcell type is changed, looks up the number of lanes.
            function updateFlowcell() {
                var flowcellName = $j('#flowcellTypeSelect').val();
                numFlowcellLanes = $j("input[name=" + flowcellName + "Count]").val();
                updateSumOfLanes();
            }

            // After a tube is selected, sums up the number of lanes to determine if
            // the sum of number of lanes is a multiple of the flowcell's lane count.
            function updateSumOfLanes() {
                var sumOfLanes = 0;
                var dTable = $j('#tubeList').dataTable();
                $j('.numLanes', dTable.fnGetNodes()).each(function() {
                    var laneCount = $j(this).val();
                    if ($.isNumeric(laneCount)) {
                        sumOfLanes += parseInt(laneCount, 10);
                    } else {
                        alert("Number of Lanes must be a number");
                        $j(this).val("0");
                    }
                });
                $('#sumOfLanesDisplayed').text(sumOfLanes);

                if (sumOfLanes > 0 && (sumOfLanes % numFlowcellLanes == 0)) {
                    $j('#createFctButton').removeAttr("disabled");
                    $j('#createFctButton').removeAttr("style");
                } else {
                    $j('#createFctButton').attr("disabled", "disabled");
                    $j('#createFctButton').attr("style", "background-color: #C0C0F0");
                }
            }

            // After the default loading conc is set, updates any existing rows having a
            // zero (or blank) loading conc.
            function updateLoadingConc() {
                var defaultLoadingConc = $j('#defaultConcId').prop('value');
                if (!$.isNumeric(defaultLoadingConc)) {
                    alert("Default Loading Conc '" + defaultLoadingConc + "' must be a number.");
                    $j('#defaultConcId').val('0');
                    defaultLoadingConc = 0;
                }
                tubeListNodes = $j('#tubeList').dataTable().fnGetNodes();
                $j('.loadConc').each(function() {
                    if (! $.isNumeric($j(this).val()) || $j(this).val() == 0) {
                        $j(this).val(defaultLoadingConc);
                    }
                });
            }
        </script>
        <style type="text/css">
            /* Fixed width columns except for product name. */
            #tubeList { table-layout: fixed; }
            .fixedWidthColumn { width: 8em; }
            .widerFixedWidthColumn { width: 10em; }
        </style>

    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">

            <!-- These hidden inputs are populated by action bean so the javascript can get a map of flowcell name to lane count. -->
            <c:forEach items="${actionBean.flowcellTypes}" var="flowcell">
                <stripes:hidden name="${flowcell}Count" value="${flowcell.vesselGeometry.rowCount}"/>
            </c:forEach>
            <input type="hidden" name="hasCrsp" value="${actionBean.hasCrsp}"/>

            <div class="control-group">
                <stripes:label for="lcsetText" name="LCSet Names" class="control-label"/>
                <div class="controls">
                    <stripes:text id="lcsetText" name="lcsetNames"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls actionButtons">
                    <stripes:submit id="loadDenatureBtn" name="loadDenature" value="Load Denature Tubes" class="btn btn-mini"/>
                    <stripes:submit id="loadNormBtn" name="loadNorm" value="Load Norm Tubes" class="btn btn-mini"/>
                    <stripes:submit id="loadPoolNormBtn" name="loadPoolNorm" value="Load Pooled Norm Tubes" class="btn btn-mini"/>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="flowcellTypeSelect" name="Flowcell Type" class="control-label"/>
                <div class="controls">
                    <stripes:select id="flowcellTypeSelect" name="selectedFlowcellType" onchange="updateFlowcell()">
                        <stripes:options-collection label="displayName" collection="${actionBean.flowcellTypes}"/>
                    </stripes:select>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="defaultConcId" name="Default Loading Conc" class="control-label"/>
                <div class="controls">
                    <stripes:text id="defaultConcId" name="defaultLoadingConc" onchange="updateLoadingConc()"/>
                </div>
            </div>
            <div class="control-group">
                <h5 style="margin-left: 50px;">FCT Ticket Info</h5>
                <hr style="margin: 0; margin-left: 50px"/>
            </div>
            <div class="control-group" style="margin-left: 50px">
                <table id="tubeList" class="table simple">
                    <thead>
                    <tr>
                        <th class="fixedWidthColumn">Tube Barcode</th>
                        <th class="fixedWidthColumn">LCSET</th>
                        <th class="fixedWidthColumn">Tube Type</th>
                        <th class="fixedWidthColumn">Number of Lanes</th>
                        <th class="fixedWidthColumn">Loading Conc</th>
                        <th class="widerFixedWidthColumn">Tube Created On</th>
                        <th>Product</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.rowDtos}" var="rowDto" varStatus="item">
                        <tr>
                            <td>${rowDto.barcode}</td>
                            <td>${rowDto.lcset}</td>
                            <td>${rowDto.tubeType}</td>
                            <td><input style='width:8em' class="numLanes" name="rowDtos[${item.index}].numberLanes"
                                       value="${rowDto.numberLanes}" onchange="updateSumOfLanes()"/></td>
                            <td><input style='width:8em' class="loadConc" name="rowDtos[${item.index}].loadingConc"
                                       value="${rowDto.loadingConc}"/></td>
                            <td>${rowDto.eventDate}</td>
                            <td>${rowDto.product}</td>
                            <input type="hidden" name="rowDtos[${item.index}].barcode" value="${rowDto.barcode}"/>
                            <input type="hidden" name="rowDtos[${item.index}].lcset" value="${rowDto.lcset}"/>
                            <input type="hidden" name="rowDtos[${item.index}].tubeType" value="${rowDto.tubeType}"/>
                            <input type="hidden" name="rowDtos[${item.index}].eventDate" value="${rowDto.eventDate}"/>
                            <input type="hidden" name="rowDtos[${item.index}].product" value="${rowDto.product}"/>
                            <input type="hidden" name="rowDtos[${item.index}].startingBatchVessel" value="${rowDto.startingBatchVessel}"/>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="control-group" style="margin-left: 200px">
                <strong>Sum of Lanes: <span id="sumOfLanesDisplayed">0</span></strong>&nbsp; &nbsp; &nbsp;
                <stripes:submit id="createFctButton" name="save" value="Create FCT Tickets"
                                disabled="disabled" class="btn btn-primary"/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
