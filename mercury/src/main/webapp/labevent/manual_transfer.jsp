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
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="transferForm">

            <stripes:select name="stationEvent.eventType" id="eventType">
                <stripes:options-enumeration
                        enum="org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType" label="name"/>
            </stripes:select>
            <stripes:submit name="chooseEventType" value="Choose Event Type" class="btn btn-primary"/>

            ${actionBean.stationEvent.station}
            <c:forEach items="${actionBean.stationEvent.reagent}" var="reagent">
                <%--@elvariable id="reagent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType"--%>
                ${reagent.kitType}
            </c:forEach>

            <c:if test="${actionBean.stationEvent.class.simpleName == 'PlateTransferEventType'}">
                <c:set var="plateTransfer" value="${actionBean.stationEvent}" />
                <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>

                Source type:
                <stripes:hidden name="stationEvent.sourcePlate.physType" value="${plateTransfer.sourcePlate.physType}"/>
                Source barcode:
                <stripes:text name="stationEvent.sourcePlate.barcode" value="${plateTransfer.sourcePlate.barcode}"/>
                <c:choose>
                    <c:when test="${empty plateTransfer.sourcePositionMap}">
                        Source section:
                        <stripes:select name="plateTransfer.sourcePlate.section" id="sourceSection">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection" label="sectionName"/>
                        </stripes:select>
                    </c:when>
                    <c:otherwise>
                        <%-- todo jmt set this with javascript --%>
                        <stripes:hidden name="stationEvent.sourcePositionMap.barcode" value="dummy"/>
                        <c:forEach items="${plateTransfer.sourcePositionMap.receptacle}" var="receptacle" varStatus="loop">
                            <%--@elvariable id="receptacle" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType"--%>
                            <stripes:text name="stationEvent.sourcePositionMap.receptacle[${loop.index}].barcode"
                                    value="${receptacle.barcode}"/>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>

                Destination type:
                <stripes:hidden name="stationEvent.plate.physType" value="${plateTransfer.plate.physType}"/>
                Destination barcode:
                <stripes:text name="stationEvent.plate.barcode" value="${plateTransfer.plate.barcode}"/>

                <c:choose>
                    <c:when test="${empty plateTransfer.sourcePositionMap}">
                        Destination section:
                        <stripes:select name="plateTransfer.plate.section" id="destSection">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection" label="sectionName"/>
                        </stripes:select>
                    </c:when>
                    <c:otherwise>
                        <%-- todo jmt set this with javascript --%>
                        <stripes:hidden name="stationEvent.sourcePositionMap.barcode" value="dummy"/>
                        <c:forEach items="${plateTransfer.sourcePositionMap.receptacle}" var="receptacle" varStatus="loop">
                            <%--@elvariable id="receptacle" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType"--%>
                            <stripes:text name="stationEvent.sourcePositionMap.receptacle[${loop.index}].barcode"
                                    value="${receptacle.barcode}"/>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>

            </c:if>
            <stripes:submit name="transfer" value="Transfer" class="btn btn-primary"/>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
