<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="batch" type="org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>

    <div id="headerId" class="fourcolumn" style="padding: 0">
        <div>Batch Name: <a target="JIRA" href="${batch.jiraTicket.browserUrl}"
                            class="external" target="JIRA">
                ${batch.businessKey}
        </a></div>
        <div>Create Date: <fmt:formatDate value="${batch.createdOn}" pattern="${bean.datePattern}"/></div>
        <div>Sample Count: ${fn:length(batch.startingBatchLabVessels)}</div>
    </div>
</stripes:layout-definition>
