<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LinkDenatureTubeToReagentBlockActionBean"/>
<c:choose>
    <c:when test="${actionBean.denatureTube == null}">
        Mercury does not recognize tube barcode ${actionBean.denatureTubeBarcode}.
    </c:when>
    <c:otherwise>
        <div id="summaryId" class="borderHeader">
            Denature Tube Details
        </div>

        <div class="threecolumn" style="margin-bottom:5px;">
            <ul>
                <c:forEach items="${actionBean.denatureTube.nearestWorkflowLabBatches}" var="batch">
                    <li>
                        Batch: <stripes:link target="JIRA"
                                             href="${batch.jiraTicket.browserUrl}"
                                             class="external">${batch.batchName}</stripes:link>
                        Workflow Name: ${batch.workflowName}
                    </li>
                </c:forEach>
            </ul>
            <ul>

                <li> Sample Count: ${actionBean.denatureTube.sampleInstanceCount}</li>
            </ul>
            <ul>
                <li>Last Event: ${actionBean.denatureTube.latestEvent.labEventType.name}</li>
            </ul>
        </div>

    </c:otherwise>
</c:choose>
