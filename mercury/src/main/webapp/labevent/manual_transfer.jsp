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
            <stripes:select name="stationEvent.eventType" id="eventType">
                <stripes:options-collection collection="${actionBean.manualEventTypes}" label="name"/>
            </stripes:select>
            <stripes:submit name="chooseEventType" value="Choose Event Type" class="btn btn-primary"/>
        </stripes:form>

        <stripes:form beanclass="${actionBean.class.name}" id="transferForm">
            <c:if test="${not empty actionBean.stationEvent}">
                <stripes:hidden name="stationEvent.eventType" value="${actionBean.stationEvent.eventType}"/>
                <h5>Reagents</h5>
                Expiration date format is mm/dd/yyyy.
                <c:forEach items="${actionBean.stationEvent.reagent}" var="reagent" varStatus="loop">
                    <%--@elvariable id="reagent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType"--%>
                    <div class="control-group">
                    <stripes:label for="rgtType${loop.index}">Type </stripes:label>
                    <stripes:text id="rgtType${loop.index}" name="stationEvent.reagent[${loop.index}].kitType"
                            value="${reagent.kitType}"/>
                    <stripes:label for="rgtBcd${loop.index}">Barcode </stripes:label>
                    <stripes:text id="rgtBcd${loop.index}" name="stationEvent.reagent[${loop.index}].barcode"
                            value="${reagent.barcode}" size="10"/>
                    <stripes:label for="rgtExp${loop.index}">Expiration </stripes:label>
                    <stripes:text id="rgtExp${loop.index}" name="stationEvent.reagent[${loop.index}].expiration"
                            value="${reagent.expiration}" size="12"/>
                    </div>
                </c:forEach>

                <c:choose>
                    <c:when test="${actionBean.stationEvent.class.simpleName == 'PlateTransferEventType' or actionBean.stationEvent.class.simpleName == 'PlateEventType'}">
                        <c:set var="plateTransfer" value="${actionBean.stationEvent}"/>
                        <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>
                        <c:if test="${actionBean.stationEvent.class.simpleName == 'PlateTransferEventType'}">
                            <h4>Plate Transfer</h4>
                            <h5>Source</h5>

                            <div class="control-group">
                            <label>Type </label>${plateTransfer.sourcePlate.physType}
                            <stripes:hidden name="stationEvent.sourcePlate.physType"
                                    value="${plateTransfer.sourcePlate.physType}"/>
                            <stripes:label for="srcPltBcd">Barcode</stripes:label>
                            <stripes:text id="srcPltBcd" name="stationEvent.sourcePlate.barcode"
                                    value="${plateTransfer.sourcePlate.barcode}"/>
                            <stripes:label for="sourceSection">Section</stripes:label>
                            <stripes:select name="stationEvent.sourcePlate.section" id="sourceSection">
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
                                                    <stripes:text name="stationEvent.sourcePositionMap.receptacle[${receptacleIndex}].barcode"
                                                            size="12" style="width: 90px;"/>
                                                    <stripes:hidden name="stationEvent.sourcePositionMap.receptacle[${receptacleIndex}].position"
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
                        <label>Type </label>${plateTransfer.plate.physType}
                        <stripes:hidden name="stationEvent.plate.physType" value="${plateTransfer.plate.physType}"/>
                        <c:if test="${actionBean.labEventType.targetVesselTypeGeometry.barcoded}">
                            <stripes:label for="dstPltBcd">Barcode</stripes:label>
                            <stripes:text id="dstPltBcd" name="stationEvent.plate.barcode"
                                    value="${plateTransfer.plate.barcode}"/>
                        </c:if>
                        <c:if test="${actionBean.stationEvent.class.simpleName == 'PlateTransferEventType'}">
                            <stripes:label for="destSection">Section</stripes:label>
                            <stripes:select name="stationEvent.plate.section" id="destSection">
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
                                                <stripes:text name="stationEvent.positionMap.receptacle[${receptacleIndex}].barcode"
                                                        size="12" style="width: 90px;"/>
                                                <stripes:hidden name="stationEvent.positionMap.receptacle[${receptacleIndex}].position"
                                                        value="${geometry.vesselPositions[receptacleIndex]}"/>
                                            </td>
                                        </c:forEach>
                                    </tr>
                                </c:forEach>
                            </table>
                        </c:if>
                        </div>
                    </c:when>
                    <c:when test="${actionBean.stationEvent.class.simpleName == 'ReceptacleTransferEventType'}">
                        <c:set var="receptacleTransfer" value="${actionBean.stationEvent}"/>
                        <%--@elvariable id="receptacleTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType"--%>
                        <h4>Tube Transfer</h4>

                        <div class="control-group">
                        <h5>Source</h5>
                        <label>Type</label>
                        ${receptacleTransfer.sourceReceptacle.receptacleType}
                        <stripes:hidden name="stationEvent.sourceReceptacle.receptacleType"
                                value="${receptacleTransfer.sourceReceptacle.receptacleType}"/>
                        <stripes:label for="srcRcpBcd">Barcode</stripes:label>
                        <stripes:text id="srcRcpBcd" name="stationEvent.sourceReceptacle.barcode"
                                value="${receptacleTransfer.sourceReceptacle.barcode}"/>
                        <!-- Can't use stripes:text because the value in the request takes precedence over the value
                        set in the action bean. -->
                        <label for="srcRcpVol">Volume</label>
                        <input type="text" id="srcRcpVol" name="stationEvent.sourceReceptacle.volume"
                                value="${receptacleTransfer.sourceReceptacle.volume}"/> ul
                        </div>
                        <div class="control-group">
                        <h5>Destination</h5>
                        <label>Type</label>
                        ${receptacleTransfer.receptacle.receptacleType}
                        <stripes:hidden name="stationEvent.receptacle.receptacleType"
                                value="${receptacleTransfer.receptacle.receptacleType}"/>
                        <stripes:label for="destRcpBcd">Barcode</stripes:label>
                        <stripes:text id="destRcpBcd" name="stationEvent.receptacle.barcode"
                                value="${receptacleTransfer.receptacle.barcode}"/>
                        <label for="destRcpVol">Volume</label>
                        <input type="text" id="destRcpVol" name="stationEvent.receptacle.volume"
                                value="${receptacleTransfer.receptacle.volume}"/> ul
                        </div>
                    </c:when>
                </c:choose>
                <stripes:submit name="fetchExisting" value="Fetch Existing" class="btn"/>
                <stripes:submit name="transfer" value="Transfer" class="btn btn-primary"/>
            </c:if>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
