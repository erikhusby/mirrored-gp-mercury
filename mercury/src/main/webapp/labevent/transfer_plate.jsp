<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean" %>
<%--
This fragment is used by manual_transfer.jsp to re-use code when displaying a source plate / rack and a target
plate / rack.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="stationEvent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType"--%>
<%--@elvariable id="plate" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType"--%>
<%--@elvariable id="positionMap" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType"--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"--%>
<%--@elvariable id="vesselTypeGeometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry"--%>
<%--@elvariable id="section" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"--%>
<%--@elvariable id="stationEventIndex" type="java.lang.Integer"--%>
<%--@elvariable id="source" type="java.lang.Boolean"--%>
<div class="control-group">
    <label>Type </label>${plate.physType}
    <input type="hidden" name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate' : 'plate'}.physType"
            value="${plate.physType}"/>
    <c:if test="${vesselTypeGeometry.barcoded}">
        <label for="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}">Barcode</label>
        <input type="text" id="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}" autocomplete="off"
                name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate' : 'plate'}.barcode"
                value="${plate.barcode}" class="clearable barcode unique"/>
    </c:if>
    <c:if test="${stationEvent.class.simpleName == 'PlateTransferEventType'}">
        <stripes:label for="${source ? 'src' : 'dst'}Section">Section</stripes:label>
        <c:choose>
            <c:when test="${not empty section}">
                ${section.sectionName}
                <stripes:hidden name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate' : 'plate'}.section"
                        value="${section.sectionName}"/>
            </c:when>
            <c:otherwise>
                <stripes:select name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate' : 'plate'}.section"
                        id="${source ? 'src' : 'dst'}Section${stationEventIndex}">
                    <stripes:options-enumeration
                            enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"
                            label="sectionName"/>
                </stripes:select>
            </c:otherwise>
        </c:choose>
    </c:if>
    <c:if test="${not empty positionMap}">
        <c:set var="geometry" value="${vesselTypeGeometry.vesselGeometry}"/>
        <%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
        <c:if test="${vesselTypeGeometry.displayName.startsWith('Matrix96')}">
            <!-- Adds the dropdowns for lab and scanner, and possibly a file chooser. -->
            <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
            <div class="controls">
                <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                        onclick="this.form['scanIndex'].value='${stationEventIndex}';this.form['scanSource'].value='${source}';"
                        name="<%= ManualTransferActionBean.RACK_SCAN_EVENT %>"/>
            </div>
            Or hand scan 2D barcodes.
        </c:if>
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
                            <input type="text" id="${source ? 'src' : 'dest'}RcpBcd${stationEventIndex}_${receptacleIndex}"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].barcode"
                                    value="${actionBean.findReceptacleAtPosition(positionMap, geometry.vesselPositions[receptacleIndex]).barcode}"
                                    class="clearable smalltext unique" autocomplete="off"/>
                            <input type="hidden"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].position"
                                    value="${geometry.vesselPositions[receptacleIndex]}"/>
                        </td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </table>
    </c:if>
</div>
