<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receiving" sectionTitle="Receiving">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

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
                <stripes:label for="rackBarcode" name="Rack Barcode" class="control-label"/>
                <div class="controls">
                    <input type="text" id="rackBarcode" autocomplete="off" name="rackBarcode" value="${actionBean.rackBarcode}"
                           class="clearable barcode unique" required="" aria-required="true">
                </div>
            </div>
            <c:choose>
                <c:when test="${actionBean.showRackScan}">
                    <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
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
                                           value="${actionBean.scanPositionToSampleInfo[geometry.vesselPositions[receptacleIndex]].manufacturerBarcode}"/>
                                </td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </table>
                <div class="control-group">
                    <div class="controls">
                            <stripes:submit id="receiveToBsp" name="receiveKitToBsp" value="Receive To BSP"
                                            class="btn btn-primary"/>
                    </div>
                </div>
            </stripes:form>
        </c:if>
    </stripes:layout-component>

</stripes:layout-render>