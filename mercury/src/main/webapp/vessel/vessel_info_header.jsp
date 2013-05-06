<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>

    <div id="headerId" class="fourcolumn" style="margin-bottom:5px;">
        <div>Vessel Type: ${vessel.type.name}</div>
        <div>Vessel Label: ${vessel.label}</div>
        <div>Create Date: <fmt:formatDate value="${vessel.createdOn}" pattern="yyyy.MM.dd"/></div>
        <div>Latest Event: ${vessel.latestEvent.labEventType.name}</div>
        <div>Event Date: <fmt:formatDate value="${vessel.latestEvent.eventDate}" pattern="yyyy.MM.dd"/></div>
        <div>Event Location: ${vessel.latestEvent.eventLocation}</div>
        <div>Event Operator: ${bean.getUserFullName(vessel.latestEvent.eventOperator)}</div>
    </div>
</stripes:layout-definition>
