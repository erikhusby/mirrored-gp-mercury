<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LinkDenatureTubeToFlowcellActionBean"/>
<c:choose>
    <c:when test="${actionBean.denatureTube == null}">
        Mercury does not recognize tube barcode ${actionBean.denatureTubeBarcode}.
    </c:when>
    <c:otherwise>
        <div id="summaryId" class="borderHeader">
            Denature Tube Details
        </div>

        <div class="fourcolumn" style="margin-bottom:5px;">
            Nearest Lab Batches
            <ul>
                <c:forEach items="${actionBean.denatureTube.nearestLabBatches}" var="batch">
                    <li>
                            ${batch.batchName}
                    </li>
                </c:forEach>
            </ul>
            Index Count
            <ul>
                <li> ${actionBean.denatureTube.indexesCount}</li>
            </ul>
            <ul>
                Last Event
                <li>${actionBean.denatureTube.latestEvent.labEventType.name}</li>
            </ul>
        </div>

    </c:otherwise>
</c:choose>
