<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create FCT Ticket" sectionTitle="Create FCT Ticket">

    <stripes:layout-component name="extraHead">
        function flowcellTypeChanged() {
            var numFlowcellLanes = ${actionBean.getFlowcellLaneCount($j('#flowcellSelect').val())});
            var remainder = $j('#numberOfLanes').val())}) % numFlowcellLanes;
            if (remainder == 0) {
                $j('#createFctButton').enable();
            }
        }

        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [2, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":false}, // barcode
                        {"bSortable":true},  // lcset
                        {"bSortable":true},  // enter number lanes
                        {"bSortable":true},  // enter loading conc
                        {"bSortable":true, "sType":"date"},  // denature date
                        {"bSortable":true},  // read length
                        {"bSortable":true},  // product
                    ]
                });
                $j('.tube-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'tube-checkAll',
                    countDisplayClass:'tube-checkedCount',
                    checkboxClass:'tube-checkbox'});
            });
        </script>

    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
            <div class="control-group">
                <stripes:label for="lcsetText" name="LCSet Names" class="control-label"/>
                <div class="controls">
                    <stripes:text id="lcsetText" name="lcsetName"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls actionButtons">
                    <stripes:submit id="loadDenatureBtn" name="loadDenature" value="Load Denature Tubes" class="btn btn-mini"/>
                    <stripes:submit id="loadNormBtn" name="loadNorm" value="Load Norm Tubes" class="btn btn-mini"/>
                    <stripes:submit id="loadPoolNormBtn" name="loadPoolNorm" value="Load Pooled Norm Tubes" class="btn btn-mini"/>
                </div>
            </div>
            <div class="control-group" style="margin-left: 50px">
                <div class="controls">
                    <stripes:label for="flowcellTypeSelect" name="Flowcell Type" class="control-label"/>
                    <stripes:select id="flowcellTypeSelect" name="selectedFlowcellType" onchange="flowcellTypeChanged()">
                        <stripes:options-collection label="displayName" collection="${actionBean.flowcellTypes}"/>
                    </stripes:select>
                </div>
            </div>
            <!--
                    <stripes:label for="numLanesText" name="Number of Lanes" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="numLanesText" name="numberOfLanes"/>
                    </div>
                    <stripes:label for="loadingConcText" name="Loading Concentration" class="control-label"/>
                    <div class="controls" style="margin-bottom: 20px">
                        <stripes:text id="loadingConcText" name="loadingConc"/>
                    </div>
            -->
            <c:if test="${not empty actionBean.rowDtos}">
                <div class="control-group">
                    <h5 style="margin-left: 50px;">FCT Ticket Info</h5>
                    <hr style="margin: 0; margin-left: 50px"/>
                </div>
                <div class="control-group" style="margin-left: 50px">
                    <table id="tubeList" class="table simple">
                        <thead>
                        <tr>
                            <th width="40">
                                <input type="checkbox" class="tube-checkAll"/><span id="count" class="tube-checkedCount"></span>
                            </th>
                            <th>Tube Barcode</th>
                            <th>LCSET</th>
                            <th>Number Lanes</th>
                            <th>Loading Conc</th>
                            <th>Created On</th>
                            <th>Read Length</th>
                            <th>Product</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.rowDtos}" var="rowDto">
                            <c:forEach items="${rowDto.value}" var="eventVessel">
                                <tr>
                                    <td>
                                        <stripes:checkbox class="tube-checkbox" name="selectedVesselLabels"
                                                          value="${rowDto.barcode}"/>
                                    </td>
                                    <td>${rowDto.barcode}</td>
                                    <td>${rowDto.lcset}</td>
                                    <td>${rowDto.numberLanes}</td>
                                    <td>${rowDto.loadingConc}</td>
                                    <td><fmt:formatDate value="${rowDto.eventDate}"
                                                        pattern="${actionBean.dateTimePattern}"/></td>
                                    <td>${rowDto.readLength}</td>
                                    <td>${rowDto.product}</td>
                                </tr>
                            </c:forEach>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="control-group" style="margin-left: 50px">
                    <div class="controls actionButtons">
                        <stripes:submit id="createFctBtn" name="save" value="Create FCT Tickets"
                                        class="btn btn-primary"/>
                    </div>
                </div>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
