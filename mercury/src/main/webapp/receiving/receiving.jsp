<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receive By Kit Scan" sectionTitle="Receive By Kit Scan">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#samplesTable').DataTable({
                    "oTableTools": {
                        "aButtons": ["copy", "csv"]
                    },
                    "aoColumns": [
                        {"bSortable": false},
                        {"bSortable": true} ,
                        {"bSortable": true} ,
                        {"bSortable": true} ,
                        {"bSortable": true}
                    ]
                });

                $j('.sample-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'sample-checkAll',
                    countDisplayClass:'sample-checkedCount',
                    checkboxClass:'sample-checkbox'});

            });
        </script>

        <style type="text/css">
            label {
                display: inline;
                font-weight: bold;
            }
            input[type="text"].smalltext {
                width: 70px;
                font-size: 12px;
                padding: 2px 2px;
            }
            input[type='text'].barcode {
                width: 100px;
                font-size: 12px;
            }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}"
                      id="rackScanForm" class="form-horizontal">
            <div class="control-group">
                <label for="rackBarcode" class="control-label">SK-ID</label>
                <div class="controls">
                    <input type="text" id="rackBarcode" autocomplete="off" name="rackBarcode" value="${actionBean.rackBarcode}"
                           class="clearable barcode unique" required="" aria-required="true">
                </div>
            </div>
            <c:choose>
                <c:when test="${actionBean.showRackScan}">
                    <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
                    <stripes:hidden name="showRackScan"  value="${actionBean.showRackScan}"/>
                    <div class="control-group">
                        <div class="controls">
                            <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                                            name="rackScan"/>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <stripes:submit value="Search" id="scanBtn" class="btn btn-primary"
                                    name="findSkId"/>
                </c:otherwise>
            </c:choose>

        </stripes:form>
        <c:if test="${actionBean.showLayout}">
            <stripes:form beanclass="${actionBean.class.name}"
                          id="showScanForm" class="form-horizontal">
                <c:set var="geometry" value="${actionBean.vesselGeometry}"/>
                <stripes:hidden name="rackBarcode"  value="${actionBean.rackBarcode}"/>
                <stripes:hidden name="showRackScan"  value="${actionBean.showRackScan}"/>
                <table>
                    <c:forEach items="${geometry.rowNames}" var="rowName" varStatus="rowStatus">
                        <c:if test="${rowStatus.first}">
                            <tr>
                                <td></td>
                                <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                                    <td>${columnName}</td>
                                </c:forEach>
                            </tr>
                        </c:if>
                        <tr>
                            <td>${rowName}</td>
                            <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                                <c:set var="receptacleIndex"
                                       value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                                <td align="right">
                                    <c:if test="${empty rowName}">${geometry.vesselPositions[receptacleIndex]}</c:if>
                                    <input type="text"
                                           id="sampleInfos[${receptacleIndex}].manufacturerBarcode"
                                           name="sampleInfos[${receptacleIndex}].manufacturerBarcode"
                                           value="${actionBean.scanPositionToSampleInfo[geometry.vesselPositions[receptacleIndex]].manufacturerBarcode}"
                                           class="clearable smalltext unique" autocomplete="off" readonly/>
                                    <input type="hidden"
                                           id="sampleInfos[${receptacleIndex}].wellPosition"
                                           name="sampleInfos[${receptacleIndex}].wellPosition"
                                           value="${geometry.vesselPositions[receptacleIndex].name()}"/>
                                    <input type="hidden"
                                           id="sampleInfos[${receptacleIndex}].sampleId"
                                           name="sampleInfos[${receptacleIndex}].sampleId"
                                           value="${actionBean.scanPositionToSampleInfo[geometry.vesselPositions[receptacleIndex]].sampleId}"/>
                                </td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </table>
                <br/>
                <div class="row">
                    <table id="samplesTable" class="sample-checkbox table simple">
                        <thead>
                        <tr>
                            <th width="30px">
                                <input type="checkbox" class="sample-checkAll" title="Check All"/>
                                <span id="count" class="sample-checkedCount"></span>
                            </th>
                            <th>Sample Info</th>
                            <th>Sample Kit</th>
                            <th>Status</th>
                            <th>Material Type</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.sampleRows}" var="sampleData">
                            <tr class="sample-row">
                                <td>
                                    <stripes:checkbox class="sample-checkbox" name="selectedSampleIds"
                                                      value="${sampleData.sampleId}"/>
                                </td>
                                <td>${sampleData.sampleId}</td>
                                <td>${sampleData.sampleKitId}</td>
                                <td>${sampleData.sampleStatus}</td>
                                <td>${sampleData.originalMaterialType}</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <stripes:submit id="receiveToBsp" name="receiveKitToBsp" value="Receive To BSP"
                                class="btn btn-primary"/>
            </stripes:form>
        </c:if>
    </stripes:layout-component>

</stripes:layout-render>