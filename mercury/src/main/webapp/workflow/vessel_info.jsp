<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>

<c:choose>
    <c:when test="${actionBean.labVessel == null}">
        <div class="control-group">
            <div class="controls">
                <div id="error" class="text-error">Mercury does not recognize tube barcode ${actionBean.vesselLabel}.
                </div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach items="${actionBean.labVessel.nearestWorkflowLabBatches}" var="batch">
            <div class="control-group">
                <stripes:label for="batch" class="control-label">
                    Batch
                </stripes:label>
                <div class="controls">
                    <div id="batch"><stripes:link target="JIRA"
                                                  href="${batch.jiraTicket.browserUrl}"
                                                  class="external">${batch.batchName}</stripes:link>
                    </div>
                </div>
            </div>
        </c:forEach>
        <div class="control-group">
            <stripes:label for="workflowName" class="control-label">
                Workflow Name
            </stripes:label>
            <div class="controls">
                <div id="workflowName`">${actionBean.workflowName}</div>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="sampleCount" class="control-label">
                Sample Count
            </stripes:label>
            <div class="controls">
                <div id="sampleCount">${actionBean.labVessel.getSampleInstanceCount("WITH_PDO", null)}</div>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="selectedBucket" class="control-label">
                Rework To Bucket
            </stripes:label>
            <div class="controls">

                <select id="selectedBucket" name="bucketName">
                    <c:forEach items="${actionBean.buckets}" var="bucket">
                        <option>${bucket.name}</option>
                    </c:forEach>
                </select>
            </div>
        </div>
    </c:otherwise>
</c:choose>
