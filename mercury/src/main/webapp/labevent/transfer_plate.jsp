<%--
This fragment is used by manual_transfer.jsp to re-use code when displaying a source plate / rack and a target
plate / rack.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<div class="control-group">
    <label>Type </label>${plateTransfer.plate.physType}
    <input type="hidden" name="stationEvents[${stationEventStatus.index}].plate.physType"
            value="${plateTransfer.plate.physType}"/>
    <c:if test="${actionBean.labEventType.manualTransferDetails.targetVesselTypeGeometry.barcoded}">
        <label for="dstPltBcd${stationEventStatus.index}">Barcode</label>
        <input type="text" id="dstPltBcd${stationEventStatus.index}"
                name="stationEvents[${stationEventStatus.index}].plate.barcode"
                value="${plateTransfer.plate.barcode}" class="clearable barcode"/>
    </c:if>
    <c:if test="${stationEvent.class.simpleName == 'PlateTransferEventType'}">
        <stripes:label for="destSection">Section</stripes:label>
        <c:choose>
            <c:when test="${not empty actionBean.labEventType.manualTransferDetails.targetSection}">
                ${actionBean.labEventType.manualTransferDetails.targetSection.sectionName}
                <stripes:hidden name="stationEvents[${stationEventStatus.index}].plate.section"
                        value="${actionBean.labEventType.manualTransferDetails.targetSection.sectionName}"/>
            </c:when>
            <c:otherwise>
                <stripes:select name="stationEvents[${stationEventStatus.index}].plate.section"
                        id="destSection${stationEventStatus.index}">
                    <stripes:options-enumeration
                            enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"
                            label="sectionName"/>
                </stripes:select>
            </c:otherwise>
        </c:choose>
    </c:if>
    <c:if test="${not empty plateTransfer.positionMap}">
        <%--todo jmt reduce copy / paste--%>
        <c:set var="geometry"
                value="${actionBean.labEventType.manualTransferDetails.targetVesselTypeGeometry.vesselGeometry}"/>
        <%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
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
                                    name="stationEvents[${stationEventStatus.index}].positionMap.receptacle[${receptacleIndex}].barcode"
                                    class="clearable smalltext"/>
                            <input type="hidden"
                                    name="stationEvents[${stationEventStatus.index}].positionMap.receptacle[${receptacleIndex}].position"
                                    value="${geometry.vesselPositions[receptacleIndex]}"/>
                        </td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </table>
    </c:if>
</div>
