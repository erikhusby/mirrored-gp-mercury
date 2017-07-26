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

<%-- todo jmt fix copy / paste of styles --%>
<style>
    .btn {
        background-image: none;
    }

    .btn-primary {
        background-color: #5a86de;
        background-image: none;
    }

    .btn[disabled], button {
        background-color: #5a86de;
        background-image: none;
    }

    .btn-xs {
        padding: 0.5px 2px;
        font-size: 8px;
        border-radius: 8px;
        text-align: right;
        background-image: none;
    }

    .xs-col {
        background-color: #33cc00;
    }

    .xs-all {
        background-color: #c266ff;
    }

    .xs-row {
        padding: 0.5px 2px;
        font-size: 8px;
        border-radius: 8px;
        text-align: right;
        background-image: none;
        background-color: #ff8c1a;
    }

</style>

<div class="control-group vessel-container" id="container0" data-direction="${source ? "src" : "dest"}" data-event-index="${stationEventIndex}">
    <c:if test="${vesselTypeGeometry.barcoded}">
        <input type="hidden"
               name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate[0]' : 'plate[0]'}.physType"
               value="${plate[0].physType}"/>
        <label for="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}">${plate[0].physType} Barcode</label>
        <input type="text" id="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}" autocomplete="off"
               name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate[0]' : 'plate[0]'}.barcode"
               value="${plate[0].barcode}"
               class="container-barcode clearable barcode unique" ${stationEventIndex == 0 ? "required" : ""}/>
    </c:if>

    <c:if test="${not empty positionMap}">
        <c:set var="geometry" value="${vesselTypeGeometry.vesselGeometry}"/>
        <%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
        <table id="${source ? 'src' : 'dest'}_TABLE_STRIP_TUBE_${vesselTypeGeometry.vesselGeometry}">
            <tr>
                <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                    <%--Select strip tube button--%>
                    <td align="right">
                        <div style="float:left;">STRIP ${columnName}</div>
                        <div style="float:right;">
                            <button id="btn_${source ? 'src' : 'dest'}_col_${columnName}" type="button"
                                    class="btn btn-primary btn-xs xs-col">Select</button>
                        </div>
                    </td>
                </c:forEach>
            </tr>
            <tr>
                <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                    <%--Strip tube barcode--%>
                    <c:set var="receptacleIndex" value="${columnStatus.index}"/>
                    <td align="right">
                        <div>BARCODE</div>
                        <input type="text"
                               id="${source ? 'src' : 'dest'}RcpBcd${stationEventIndex}_${receptacleIndex}"
                               name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap[0]' : 'positionMap[0]'}.receptacle[${receptacleIndex}].barcode"
                               value="${actionBean.findReceptacleAtPosition(positionMap[0], columnStatus.count).barcode}"
                               class="clearable smalltext unique" autocomplete="off"/>
                        <input type="hidden"
                               name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap[0]' : 'positionMap[0]'}.receptacle[${receptacleIndex}].position"
                               value="${columnStatus.count}"/>
                        <input type="hidden"
                                name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}[0].receptacle[${receptacleIndex}].receptacleType"
                                value="${source ? actionBean.labEventType.manualTransferDetails.sourceBarcodedTubeType : actionBean.labEventType.manualTransferDetails.targetBarcodedTubeType}"/>
                    </td>
                </c:forEach>
            </tr>
            <tr>
                <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                    <%--Strip tube flow cell ticket--%>
                    <c:set var="receptacleIndex" value="${columnStatus.index}"/>
                    <td align="right">
                        <div>FCT</div>
                        <input id="${source ? 'src' : 'dest'}RcpBcd${stationEventIndex}_${receptacleIndex}_FCT"
                               name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap[0]' : 'positionMap[0]'}.receptacle[${receptacleIndex}].metadata[0].value"
                               value="${actionBean.findReceptacleAtPosition(positionMap[0], columnStatus.count).metadata[0].value}"
                               type="text" class="clearable smalltext unique" autocomplete="off"/>
                        <input type="hidden"
                               name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap[0]' : 'positionMap[0]'}.receptacle[${receptacleIndex}].metadata[0].name"
                               value="FCT"/>
                    </td>
                </c:forEach>
            </tr>
            <%--Buttons representing the individual tubes in the strip tube rack--%>
            <c:forEach items="${geometry.rowNames}" var="rowName" varStatus="rowStatus">
                <tr>
                    <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                        <td>
                            <div style="float:right;">
                                <button id="${rowName}${columnName}_${source ? 'src' : 'dest'}_RcpBcd${stationEventIndex}_${columnStatus.index}"
                                        type="button" data-position="${rowName}${columnName}"
                                        class="${source ? 'src' : 'dest'}_col_${columnStatus.index} ${source ? 'src' : 'dest'}_row_${rowStatus.index} btn btn-primary btn-xs">Select</button>
                            </div>
                        </td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </table>
    </c:if>
</div>
