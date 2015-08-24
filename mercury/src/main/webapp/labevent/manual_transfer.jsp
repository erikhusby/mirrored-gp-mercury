<%--
  This page allows the user to record a manual transfer, i.e. a transfer not done on a liquid handling deck with
  messaging.
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manual Transfer" sectionTitle="Manual Transfer">

    <stripes:layout-component name="extraHead">
        <style type="text/css">
            label {
                display: inline;
                font-weight: bold;
            }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="eventForm">
            <stripes:select name="stationEvents[0].eventType" id="eventType">
                <stripes:options-collection collection="${actionBean.manualEventTypes}" label="name"/>
            </stripes:select>
            <stripes:submit name="chooseEventType" value="Choose Event Type" class="btn btn-primary"/>
        </stripes:form>

        <stripes:form beanclass="${actionBean.class.name}" id="transferForm">
            <c:if test="${not empty actionBean.stationEvents}">
                <h5>Reagents</h5>
                Expiration date format is mm/dd/yyyy.
                <c:forEach items="${actionBean.stationEvents[0].reagent}" var="reagent" varStatus="loop">
                    <%--@elvariable id="reagent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType"--%>
                    <div class="control-group">
                    <stripes:label for="rgtType${loop.index}">Type </stripes:label>
                    <stripes:text id="rgtType${loop.index}" name="stationEvents[0].reagent[${loop.index}].kitType"
                            value="${reagent.kitType}"/>
                    <stripes:label for="rgtBcd${loop.index}">Barcode </stripes:label>
                    <stripes:text id="rgtBcd${loop.index}" name="stationEvents[0].reagent[${loop.index}].barcode"
                            value="${reagent.barcode}" size="10"/>
                    <stripes:label for="rgtExp${loop.index}">Expiration </stripes:label>
                    <stripes:text id="rgtExp${loop.index}" name="stationEvents[0].reagent[${loop.index}].expiration"
                            value="${reagent.expiration}" size="12"/>
                    </div>
                </c:forEach>

                <c:forEach items="${actionBean.stationEvents}" var="stationEvent" varStatus="stationEventStatus">
                    <stripes:hidden name="stationEvents[${stationEventStatus.index}].eventType" value="${actionBean.stationEvents[stationEventStatus.index].eventType}"/>
                    <c:choose>
                        <c:when test="${stationEvent.class.simpleName == 'PlateTransferEventType' or stationEvent.class.simpleName == 'PlateEventType'}">
                            <c:set var="plateTransfer" value="${stationEvent}"/>
                            <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>
                            <c:if test="${stationEvent.class.simpleName == 'PlateTransferEventType'}">
                                <h4>Plate Transfer</h4>
                                <h5>Source</h5>

                                <div class="control-group">
                                <label>Type </label>${plateTransfer.sourcePlate.physType}
                                <stripes:hidden name="stationEvents[${stationEventStatus.index}].sourcePlate.physType"
                                        value="${plateTransfer.sourcePlate.physType}"/>
                                <stripes:label for="srcPltBcd${stationEventStatus.index}">Barcode</stripes:label>
                                <stripes:text id="srcPltBcd${stationEventStatus.index}"
                                        name="stationEvents[${stationEventStatus.index}].sourcePlate.barcode"
                                        value="${plateTransfer.sourcePlate.barcode}"/>
                                <stripes:label for="sourceSection">Section</stripes:label>
                                <stripes:select name="stationEvents[${stationEventStatus.index}].sourcePlate.section"
                                        id="sourceSection${stationEventStatus.index}">
                                    <stripes:options-enumeration
                                            enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"
                                            label="sectionName"/>
                                </stripes:select>
                                <c:if test="${not empty plateTransfer.sourcePositionMap}">
                                    <c:set var="geometry" value="${actionBean.labEventType.sourceVesselTypeGeometry.vesselGeometry}"/>
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
                                                    <c:set var="receptacleIndex" value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                                                    <td>
                                                        <stripes:text name="stationEvents[${stationEventStatus.index}].sourcePositionMap.receptacle[${receptacleIndex}].barcode"
                                                                size="12" style="width: 90px;"/>
                                                        <stripes:hidden name="stationEvents[${stationEventStatus.index}].sourcePositionMap.receptacle[${receptacleIndex}].position"
                                                                value="${geometry.vesselPositions[receptacleIndex]}"/>
                                                    </td>
                                                </c:forEach>
                                            </tr>
                                        </c:forEach>
                                    </table>
                                </c:if>
                                </div>

                                <h5>Destination</h5>
                            </c:if>
                            <div class="control-group">
                            <c:if test="${fn:length(actionBean.stationEvents) > 1}">
                                ${stationEventStatus.index + 1}
                                <stripes:hidden name="stationEvents[${stationEventStatus.index}].metadata[0].name" value="MessageNum"/>
                                <stripes:hidden name="stationEvents[${stationEventStatus.index}].metadata[0].value" value="${stationEventStatus.index + 1}"/>
                            </c:if>
                            <label>Type </label>${plateTransfer.plate.physType}
                            <stripes:hidden name="stationEvents[${stationEventStatus.index}].plate.physType" value="${plateTransfer.plate.physType}"/>
                            <c:if test="${actionBean.labEventType.targetVesselTypeGeometry.barcoded}">
                                <stripes:label for="dstPltBcd${stationEventStatus.index}">Barcode</stripes:label>
                                <stripes:text id="dstPltBcd${stationEventStatus.index}"
                                        name="stationEvents[${stationEventStatus.index}].plate.barcode"
                                        value="${plateTransfer.plate.barcode}"/>
                            </c:if>
                            <c:if test="${stationEvent.class.simpleName == 'PlateTransferEventType'}">
                                <stripes:label for="destSection">Section</stripes:label>
                                <stripes:select name="stationEvents[${stationEventStatus.index}].plate.section"
                                        id="destSection${stationEventStatus.index}">
                                    <stripes:options-enumeration
                                            enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"
                                            label="sectionName"/>
                                </stripes:select>
                            </c:if>
                            <c:if test="${not empty plateTransfer.positionMap}">
                                <%--todo jmt reduce copy / paste--%>
                                <c:set var="geometry" value="${actionBean.labEventType.targetVesselTypeGeometry.vesselGeometry}"/>
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
                                                <c:set var="receptacleIndex" value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                                                <td align="right">
                                                    <c:if test="${empty rowName}">${geometry.vesselPositions[receptacleIndex]}</c:if>
                                                    <stripes:text name="stationEvents[${stationEventStatus.index}].positionMap.receptacle[${receptacleIndex}].barcode"
                                                            size="12" style="width: 90px;"/>
                                                    <stripes:hidden name="stationEvents[${stationEventStatus.index}].positionMap.receptacle[${receptacleIndex}].position"
                                                            value="${geometry.vesselPositions[receptacleIndex]}"/>
                                                </td>
                                            </c:forEach>
                                        </tr>
                                    </c:forEach>
                                </table>
                            </c:if>
                            </div>
                        </c:when>
                        <c:when test="${stationEvent.class.simpleName == 'ReceptacleTransferEventType'}">
                            <c:set var="receptacleTransfer" value="${stationEvent}"/>
                            <%--@elvariable id="receptacleTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType"--%>
                            <h4>Tube Transfer</h4>

                            <div class="control-group">
                            <h5>Source</h5>
                            <label>Type</label>
                            ${receptacleTransfer.sourceReceptacle.receptacleType}
                            <stripes:hidden name="stationEvents[${stationEventStatus.index}].sourceReceptacle.receptacleType"
                                    value="${receptacleTransfer.sourceReceptacle.receptacleType}"/>
                            <stripes:label for="srcRcpBcd${stationEventStatus.index}">Barcode</stripes:label>
                            <stripes:text id="srcRcpBcd${stationEventStatus.index}"
                                    name="stationEvents[${stationEventStatus.index}].sourceReceptacle.barcode"
                                    value="${receptacleTransfer.sourceReceptacle.barcode}"/>
                            <!-- Can't use stripes:text because the value in the request takes precedence over the value
                            set in the action bean. -->
                            <label for="srcRcpVol${stationEventStatus.index}">Volume</label>
                            <input type="text" id="srcRcpVol${stationEventStatus.index}"
                                    name="stationEvents[${stationEventStatus.index}].sourceReceptacle.volume"
                                    value="${receptacleTransfer.sourceReceptacle.volume}"/> ul
                            </div>
                            <div class="control-group">
                            <h5>Destination</h5>
                            <label>Type</label>
                            ${receptacleTransfer.receptacle.receptacleType}
                            <stripes:hidden name="stationEvents[${stationEventStatus.index}].receptacle.receptacleType"
                                    value="${receptacleTransfer.receptacle.receptacleType}"/>
                            <stripes:label for="destRcpBcd${stationEventStatus.index}">Barcode</stripes:label>
                            <stripes:text id="destRcpBcd${stationEventStatus.index}"
                                    name="stationEvents[${stationEventStatus.index}].receptacle.barcode"
                                    value="${receptacleTransfer.receptacle.barcode}"/>
                            <label for="destRcpVol${stationEventStatus.index}">Volume</label>
                            <input type="text" id="destRcpVol${stationEventStatus.index}"
                                    name="stationEvents[${stationEventStatus.index}].receptacle.volume"
                                    value="${receptacleTransfer.receptacle.volume}"/> ul
                            </div>
                        </c:when>
                    </c:choose>
                </c:forEach>
                <stripes:submit name="fetchExisting" value="Fetch Existing" class="btn"/>
                <stripes:submit name="transfer" value="Transfer" class="btn btn-primary"/>
            </c:if>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
