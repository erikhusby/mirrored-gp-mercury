<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
    <div id="headerId" class="fourcolumn" style="padding: 0">
        <div>Vessel Type: ${vessel.type.name}</div>
        <div>Vessel Label: ${vessel.label}</div>
        <div>Create Date: <fmt:formatDate value="${vessel.createdOn}" pattern="${bean.datePattern}"/></div>
        <div>Latest Event: ${vessel.latestEvent.labEventType.name}</div>
        <div>Event Date: <fmt:formatDate value="${vessel.latestEvent.eventDate}" pattern="${bean.datePattern}"/></div>
        <div>Event Location: ${vessel.latestEvent.eventLocation}</div>
        <div>Script: ${vessel.latestEvent.programName}</div>
        <div>Event Operator: ${bean.getUserFullName(vessel.latestEvent.eventOperator)}</div>
        <div>Sample Instance Count: ${vessel.sampleInstancesV2.size()}</div>
        <div>Unique Index Count: ${vessel.uniqueIndexesCount}</div>
    </div>
</stripes:layout-definition>
