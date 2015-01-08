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
                        enum="org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType"/>
            </stripes:select>

            ${actionBean.stationEvent.station}

            <c:if test="${actionBean.stationEvent.class.simpleName == 'PlateTransferEventType'}">
                <c:set var="plateTransfer" value="${actionBean.stationEvent}" />
                <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>

                <stripes:text name="stationEvent.sourcePlate.barcode" value="${plateTransfer.sourcePlate.barcode}"/>
                <%-- todo jmt set this with javascript --%>
                <stripes:hidden name="stationEvent.sourcePositionMap.barcode" value="dummy"/>
                <c:forEach items="${plateTransfer.sourcePositionMap.receptacle}" var="receptacle" varStatus="loop">
                    <%--@elvariable id="receptacle" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType"--%>
                    <stripes:text name="stationEvent.sourcePositionMap.receptacle[${loop.index}].barcode"
                            value="${receptacle.barcode}"/>
                </c:forEach>
            </c:if>
            <stripes:submit name="transfer" value="Transfer" class="btn btn-primary"/>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
