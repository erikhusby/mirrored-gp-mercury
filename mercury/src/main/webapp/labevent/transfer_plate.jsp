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
<c:set var="sourceMassRemove" value="${source and actionBean.labEventType.manualTransferDetails.sourceMassRemoved()}"/>
<div class="control-group" id="container0">
    <label>Type </label>${plate.physType}
    <input type="hidden" name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate' : 'plate'}.physType"
            value="${plate.physType}"/>
    <c:if test="${vesselTypeGeometry.barcoded}">
        <label for="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}">Barcode</label>
    </c:if>
    <input type="${vesselTypeGeometry.barcoded ? 'text' : 'hidden'}" id="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}" autocomplete="off"
            name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate' : 'plate'}.barcode"
            value="${plate.barcode}" class="clearable barcode unique" ${stationEventIndex == 0 ? "required" : ""}/>
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
        <c:if test="${sourceMassRemove eq 'true'}">
            <stripes:label for="srcDepleteAll">Deplete All</stripes:label>
            <input type="checkbox" id="srcDepleteAll${stationEventIndex}"
                   name="depleteAll[${stationEventIndex}]"
                   class="clearable smalltext" autocomplete="off"
                    <c:if test="${actionBean.depleteAll[stationEventIndex]}"> checked="checked" </c:if>/>
        </c:if>
    </c:if>
    <c:if test="${not empty positionMap and vesselTypeGeometry.class.simpleName != 'PlateType'}">
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
        <table id="${source ? 'src' : 'dest'}_${vesselTypeGeometry.vesselGeometry}">
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
                    <c:set var="paddingStyle" value="${(sourceMassRemove eq 'true') ? 'style=\"padding-top: 10px\"' : ''}" />
                    <td ${paddingStyle}>${rowName}</td>
                    <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                        <c:set var="receptacleIndex"
                                value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                        <td align="left" ${paddingStyle}>
                            <c:if test="${empty rowName}">${geometry.vesselPositions[receptacleIndex]}</c:if>
                            <input type="text" id="${source ? 'src' : 'dest'}RcpBcd${stationEventIndex}_${receptacleIndex}"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].barcode"
                                    value="${actionBean.findReceptacleAtPosition(positionMap, geometry.vesselPositions[receptacleIndex]).barcode}"
                                    class="clearable smalltext unique" autocomplete="off" placeholder="barcode"/>
                            <input type="hidden"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].position"
                                    value="${geometry.vesselPositions[receptacleIndex]}"/>
                            <input type="hidden"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].receptacleType"
                                    value="${source ? actionBean.labEventType.manualTransferDetails.sourceBarcodedTubeType : actionBean.labEventType.manualTransferDetails.targetBarcodedTubeType}"/>
                            <input type="${(source && actionBean.labEventType.manualTransferDetails.sourceVolume()) || (!source && actionBean.labEventType.manualTransferDetails.targetVolume()) ? 'text' : 'hidden'}"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].volume"
                                    id="${source ? 'src' : 'dest'}RcpVol${stationEventIndex}_${receptacleIndex}"
                                    value="${actionBean.findReceptacleAtPosition(positionMap, geometry.vesselPositions[receptacleIndex]).volume}"
                                    class="clearable smalltext" autocomplete="off" placeholder="volume"/>
                            <input type="${(sourceMassRemove eq 'true') ? 'text' : 'hidden'}"
                                   name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].mass"
                                   id="${source ? 'src' : 'dest'}RcpMass${stationEventIndex}_${receptacleIndex}"
                                   value="${actionBean.findReceptacleAtPosition(positionMap, geometry.vesselPositions[receptacleIndex]).mass}"
                                   class="clearable smalltext" autocomplete="off" placeholder="mass removed"/>
                            <input type="${(sourceMassRemove eq 'true') ? 'checkbox' : 'hidden'}"
                                   id="${source ? 'src' : 'dest'}RcpMassChk${stationEventIndex}_${receptacleIndex}"
                                   name="mapPositionToDepleteFlag[${geometry.vesselPositions[receptacleIndex]}]"
                                   class="clearable smalltext" autocomplete="off"
                                    <c:if test="${source && actionBean.mapPositionToDepleteFlag[geometry.vesselPositions[receptacleIndex]]}"> checked="checked" </c:if>/>
                            <c:if test="${not empty actionBean.labEventType.resultingMaterialType && !source}">
                                <%-- This is primarily for messages forwarded to BSP --%>
                                <input type="hidden"
                                        name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}.receptacle[${receptacleIndex}].materialType"
                                        value="${actionBean.labEventType.resultingMaterialType.displayName}"/>
                            </c:if>
                        </td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </table>
    </c:if>
</div>
