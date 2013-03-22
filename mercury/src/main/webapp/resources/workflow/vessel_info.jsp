<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>
<c:choose>
    <c:when test="${actionBean.labVessel == null}">
        <div class="fourcolumn">
            Mercury does not recognize tube barcode ${actionBean.vesselLabel}.
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach items="${actionBean.labVessel.nearestWorkflowLabBatches}" var="batch">
            <div class="fourcolumn">
                <div>Batch:</div>
                <div><stripes:link target="JIRA"
                                   href="${batch.jiraTicket.browserUrl}"
                                   class="external">${batch.batchName}</stripes:link>
                </div>
            </div>
        </c:forEach>
        <div class="fourcolumn">
            <div>Workflow Name:</div>
            <div>${actionBean.workflowName}</div>
        </div>
        <div class="fourcolumn">
            <div>Sample Count:</div>
            <div>${actionBean.labVessel.sampleInstanceCount}</div>
        </div>
        <div class="fourcolumn">
            <stripes:label name="Rework To Bucket" for="selectedBucket"/>
            <select id="selectedBucket">
                <c:forEach items="${actionBean.buckets}" var="bucket">
                    <option>${bucket.name}</option>
                </c:forEach>
            </select>
        </div>
    </c:otherwise>
</c:choose>