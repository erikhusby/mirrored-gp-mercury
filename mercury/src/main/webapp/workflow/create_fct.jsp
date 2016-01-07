<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create FCT Ticket" sectionTitle="Create FCT Ticket">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            var numFlowcellLanes = 0;
            var laneCountIsExactMultiple = false;

            $j(document).ready(function () {
                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [1, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":false}, // barcode
                        {"bSortable":true},  // lcset
                        {"bSortable":true},  // enter number lanes
                        {"bSortable":true},  // enter loading conc
                        {"bSortable":true, "sType":"date"},  // denature date
                        {"bSortable":true},  // product
                    ]
                });
                updateSumOfLanes();
            });


            function updateSumOfLanes() {
                sumOfLanes = 0;
                // Iterates on tubeList rows to sum up the number of lanes.
                tubeListTable = $j('#tubeList').dataTable();
                tubeListNodes = tubeListTable.fnGetNodes();
                for (var i = 0; i < tubeListNodes.length; ++i) {
                    laneCount = $(tubeListNodes[i]).find('#numLanesId').attr('value');
                    if ($.isNumeric(laneCount)) {
                        sumOfLanes += parseInt(laneCount, 10);
                    } else {
                        alert("Please change Number Lanes '" + laneCount + "' to a number");
                    }
                }
                //var tubeListRows = tubeListTable.fnGetData();
                //for (var i = 0; i < tubeListRows.length; ++i) {
                //    sumOfLanes += Integer.parseInt($($(tubeListRows[i])[2]).attr("value"), 10);
                //}
                $('#sumOfLanesDisplayed').text(sumOfLanes);
                // Determines if the sum of lanes is a multiple of the flowcell's lane count.
                var flowcellType = $j('#flowcellSelect').val();
                numFlowcellLanes = ${actionBean.getFlowcellLaneCount(flowcellType)};
                laneCountIsExactMultiple = (sumOfLanes % numFlowcellLanes == 0);
                if (sumOfLanes > 0 && laneCountIsExactMultiple) {
                    $j('#createFctButton').enable();
                }
            }

        </script>

    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
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
                    <stripes:select id="flowcellTypeSelect" name="selectedFlowcellType" onchange="updateSumOfLanes()">
                        <stripes:options-collection label="displayName" collection="${actionBean.flowcellTypes}"/>
                    </stripes:select>
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
                        <th>Tube Barcode</th>
                        <th>LCSET</th>
                        <th>Number Lanes</th>
                        <th>Loading Conc</th>
                        <th>Tube Created On</th>
                        <th>Product</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.rowDtos}" var="rowDto" varStatus="item">
                        <tr>
                            <td>${rowDto.barcode}
                                <input type="hidden" name="rowDtos[${item.index}].barcode" value="${rowDto.barcode}"/>
                            </td>
                            <td>${rowDto.lcset}
                                <input type="hidden" name="rowDtos[${item.index}].lcset" value="${rowDto.lcset}"/>
                            </td>
                            <td><input id="numLanesId" name="rowDtos[${item.index}].numberLanes"
                                       value="${rowDto.numberLanes}" onchange="updateSumOfLanes()"/></td>
                            <td><input id="loadingConcId" name="rowDtos[${item.index}].loadingConc"
                                       value="${rowDto.loadingConc}"/></td>
                            <td><fmt:formatDate value="${rowDto.eventDate}"
                                                pattern="${actionBean.dateTimePattern}"/>
                                <input type="hidden" name="rowDtos[${item.index}].eventDate" value="${rowDto.eventDate}"/>
                            </td>
                            <td>${rowDto.product}
                                <input type="hidden" name="rowDtos[${item.index}].product" value="${rowDto.product}"/>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="control-group" style="margin-left: 200px">
                <strong>Sum of Lanes: <span id="sumOfLanesDisplayed">0</span></strong>
            </div>
            <div class="control-group" style="margin-left: 50px">
                <div class="controls actionButtons">
                    <stripes:submit id="createFctBtn" name="save" value="Create FCT Tickets"
                                    class="btn btn-primary"/>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
