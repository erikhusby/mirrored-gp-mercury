<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="batch" type="org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>

    <div id="headerId" class="fourcolumn" style="padding: 0">
        <div>Vessel Label: ${batch.batchName}</div>
        <div>Create Date: <fmt:formatDate value="${batch.createdOn}" pattern="${bean.datePattern}"/></div>
        <div>Latest Event: ${batch.latestEvent.labEventType.name}</div>
        <div>Event Date: <fmt:formatDate value="${batch.latestEvent.eventDate}" pattern="${bean.datePattern}"/></div>
        <div>Event Location: ${batch.latestEvent.eventLocation}</div>
        <div>Event Operator: ${bean.getUserFullName(batch.latestEvent.eventOperator)}</div>
        <div>Sample Instance Count: ${fn:length(batch.bucketEntries)}</div>
    </div>
</stripes:layout-definition>
