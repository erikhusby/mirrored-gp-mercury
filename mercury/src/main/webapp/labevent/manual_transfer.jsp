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
        <stripes:form beanclass="${actionBean.class.name}" id="transferForm">

            <stripes:select name="stationEvent.eventType" id="eventType">
                <stripes:options-collection collection="${actionBean.manualEventTypes}" label="name"/>
            </stripes:select>
            <stripes:submit name="chooseEventType" value="Choose Event Type" class="btn btn-primary"/>
            <c:if test="${not empty actionBean.stationEvent}">
                <h5>Reagents</h5>
                <c:forEach items="${actionBean.stationEvent.reagent}" var="reagent" varStatus="loop">
                    <%--@elvariable id="reagent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType"--%>
                    <p>
                        <stripes:label for="rgtType${loop.index}">Type </stripes:label>
                        <stripes:text id="rgtType${loop.index}" name="stationEvent.reagent[${loop.index}].kitType"
                                value="${reagent.kitType}"/>
                        <stripes:label for="rgtBcd${loop.index}">Barcode </stripes:label>
                        <stripes:text id="rgtBcd${loop.index}" name="stationEvent.reagent[${loop.index}].barcode"
                                value="${reagent.barcode}" size="10"/>
                        <stripes:label for="rgtExp${loop.index}">Expiration </stripes:label>
                        <stripes:text id="rgtExp${loop.index}" name="stationEvent.reagent[${loop.index}].expiration"
                                value="${reagent.expiration}" size="12"/>
                    </p>
                </c:forEach>

                <c:choose>
                    <c:when test="${actionBean.stationEvent.class.simpleName == 'PlateTransferEventType'}">
                        <c:set var="plateTransfer" value="${actionBean.stationEvent}"/>
                        <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>
                        <h4>Plate Transfer</h4>
                        <h5>Source</h5>

                        <p>
                            <label>Type </label>${plateTransfer.sourcePlate.physType}
                            <stripes:hidden name="stationEvent.sourcePlate.physType"
                                    value="${plateTransfer.sourcePlate.physType}"/>
                            <stripes:label for="srcPltBcd">Barcode</stripes:label>
                            <stripes:text id="srcPltBcd" name="stationEvent.sourcePlate.barcode"
                                    value="${plateTransfer.sourcePlate.barcode}"/>
                            <c:choose>
                                <c:when test="${empty plateTransfer.sourcePositionMap}">
                                    <stripes:label for="sourceSection">Section</stripes:label>
                                    <stripes:select name="stationEvent.sourcePlate.section" id="sourceSection">
                                        <stripes:options-enumeration
                                                enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"
                                                label="sectionName"/>
                                    </stripes:select>
                                </c:when>
                                <c:otherwise>
                                    <%-- todo jmt set this with javascript --%>
                                    <stripes:hidden name="stationEvent.sourcePositionMap.barcode" value="dummy"/>
                                    <c:forEach items="${plateTransfer.sourcePositionMap.receptacle}" var="receptacle"
                                            varStatus="loop">
                                        <%--@elvariable id="receptacle" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType"--%>
                                        <stripes:text
                                                name="stationEvent.sourcePositionMap.receptacle[${loop.index}].barcode"
                                                value="${receptacle.barcode}"/>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </p>

                        <h5>Destination</h5>

                        <p>
                            <label>Type </label>${plateTransfer.plate.physType}
                            <stripes:hidden name="stationEvent.plate.physType" value="${plateTransfer.plate.physType}"/>
                            <stripes:label for="dstPltBcd">Barcode</stripes:label>
                            <stripes:text id="dstPltBcd" name="stationEvent.plate.barcode" value="${plateTransfer.plate.barcode}"/>

                            <c:choose>
                                <c:when test="${empty plateTransfer.sourcePositionMap}">
                                    <stripes:label for="destSection">Section</stripes:label>
                                    <stripes:select name="stationEvent.plate.section" id="destSection">
                                        <stripes:options-enumeration
                                                enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"
                                                label="sectionName"/>
                                    </stripes:select>
                                </c:when>
                                <c:otherwise>
                                    <%-- todo jmt set this with javascript --%>
                                    <stripes:hidden name="stationEvent.sourcePositionMap.barcode" value="dummy"/>
                                    <c:forEach items="${plateTransfer.sourcePositionMap.receptacle}" var="receptacle"
                                            varStatus="loop">
                                        <%--@elvariable id="receptacle" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType"--%>
                                        <stripes:text
                                                name="stationEvent.sourcePositionMap.receptacle[${loop.index}].barcode"
                                                value="${receptacle.barcode}"/>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </c:when>
                    <c:when test="${actionBean.stationEvent.class.simpleName == 'ReceptacleTransferEventType'}">
                        <%--@elvariable id="receptacleTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType"--%>
                        <h5>Tube Transfer</h5>
                        <p>
                        <!-- todo jmt specify tube types -->
                        <stripes:label for="srcRcpBcd">Source</stripes:label>
                        <stripes:text id="srcRcpBcd" name="stationEvent.sourceReceptacle.barcode"
                                value="${receptacleTransfer.sourceReceptacle.barcode}"/>
                        <stripes:label for="destRcpBcd">Destination</stripes:label>
                        <stripes:text id="destRcpBcd" name="stationEvent.receptacle.barcode"
                                value="${receptacleTransfer.receptacle.barcode}"/>
                        </p>
                    </c:when>
                </c:choose>
                <stripes:submit name="transfer" value="Transfer" class="btn btn-primary"/>
            </c:if>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
